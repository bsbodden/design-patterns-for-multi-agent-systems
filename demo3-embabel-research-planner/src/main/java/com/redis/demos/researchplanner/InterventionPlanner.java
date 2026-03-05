package com.redis.demos.researchplanner;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.models.OpenAiModels;
import com.embabel.agent.domain.io.UserInput;
import com.redis.demos.researchplanner.domain.ImpactAssessment;
import com.redis.demos.researchplanner.domain.InterventionOptions;
import com.redis.demos.researchplanner.domain.InterventionPlan;
import com.redis.demos.researchplanner.domain.IssueClassification;
import com.redis.demos.researchplanner.domain.OperationalIssue;
import com.redis.demos.researchplanner.domain.ApprovedPlan;
import com.redis.demos.researchplanner.domain.ValidatedPlan;
import com.redis.demos.researchplanner.service.OlistDataService;
import com.embabel.agent.core.hitl.WaitFor;

/**
 * GOAP-based intervention planner for e-commerce operational issues.
 *
 * The 6 actions declare typed inputs (preconditions) and outputs (effects).
 * The A* planner discovers the optimal action sequence from UserInput to ApprovedPlan.
 *
 * Data access is via injected OlistDataService (Redis OM Spring) — the GOAP planner
 * doesn't know or care about data sources, it only reasons about typed preconditions/effects.
 */
@Agent(
        description = "Plan and validate operational interventions for e-commerce issues",
        scan = true
)
public class InterventionPlanner {

    private final OlistDataService dataService;

    public InterventionPlanner(OlistDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Parse the user input to extract an operational issue description with entities.
     */
    @Action(cost = 0.05)
    public OperationalIssue parseIssue(UserInput input, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_41_MINI)
                .createObject(
                        """
                        Extract the operational issue from this input.
                        Identify the seller ID (e.g., seller_42), state (e.g., SP), and issue description.
                        If not explicitly mentioned, infer reasonable defaults.

                        Input: %s
                        """.formatted(input.getContent()),
                        OperationalIssue.class
                );
    }

    /**
     * Classify the issue type and severity.
     */
    @Action(cost = 0.05)
    public IssueClassification classifyIssue(OperationalIssue issue, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_41_MINI)
                .createObject(
                        """
                        Classify this operational issue:
                        Description: %s
                        Seller: %s, State: %s

                        Determine:
                        - type: delivery_delay, quality_issue, seller_performance, regional_problem
                        - severity: critical, high, medium, low
                        - timeRange: e.g., "Q4 2017" or "last 30 days"
                        """.formatted(issue.description(), issue.sellerId(), issue.state()),
                        IssueClassification.class
                );
    }

    /**
     * Assess business impact using real data from Redis.
     * This action is GROUNDED — it fetches actual seller/order data before LLM assessment.
     */
    @Action(cost = 0.10)
    public ImpactAssessment assessImpact(IssueClassification classification, OperationalIssue issue, Ai ai) {
        // Fetch real data from Redis via OlistDataService
        String sellerData;
        String orderData;
        String regionalData;
        try {
            var seller = dataService.getSellerMetrics(classification.sellerId());
            sellerData = dataService.formatSellerSummary(seller);
            var orders = dataService.getAffectedOrders(classification.sellerId());
            orderData = dataService.formatAffectedOrdersSummary(orders);
            var regional = dataService.getSellersInRegion(classification.state());
            double regionalAvgOnTime = regional.stream()
                    .mapToDouble(s -> s.getOnTimePct() != null ? s.getOnTimePct() : 0.0)
                    .average().orElse(0.0);
            regionalData = "Regional sellers: %d, avg on-time: %.1f%%".formatted(regional.size(), regionalAvgOnTime);
        } catch (Exception e) {
            sellerData = "Seller data unavailable: " + e.getMessage();
            orderData = "Order data unavailable";
            regionalData = "Regional data unavailable";
        }

        return ai
                .withLlm(OpenAiModels.GPT_41)
                .createObject(
                        """
                        Assess business impact of this operational issue:
                        Issue: %s (severity: %s, type: %s)

                        Seller data: %s
                        Affected orders: %s
                        Regional context: %s

                        Provide revenue impact, customer satisfaction impact, and geographic scope.
                        """.formatted(issue.description(), classification.severity(),
                                classification.type(), sellerData, orderData, regionalData),
                        ImpactAssessment.class
                );
    }

    /**
     * Generate possible intervention options.
     */
    @Action(cost = 0.15)
    public InterventionOptions generateOptions(ImpactAssessment impact, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_41)
                .createObject(
                        """
                        Generate intervention options for this e-commerce operational issue:
                        %s

                        For each option provide: action description, cost level (low/medium/high),
                        expected effectiveness, and timeline.
                        Rank by cost-effectiveness ratio.
                        """.formatted(impact.getContent()),
                        InterventionOptions.class
                );
    }

    /**
     * Create a concrete intervention plan from the options.
     */
    @Action(cost = 0.15)
    public InterventionPlan planIntervention(InterventionOptions options, ImpactAssessment impact, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_41)
                .createObject(
                        """
                        Create a concrete intervention plan from these options:
                        Options: %s

                        Impact assessment: %s

                        Select the best combination of actions. Define sequence, dependencies,
                        timeline, and expected outcome.
                        """.formatted(options.getContent(), impact.getContent()),
                        InterventionPlan.class
                );
    }

    /**
     * Validate the plan against the original issue constraints.
     */
    @Action(cost = 0.20)
    public ValidatedPlan validatePlan(InterventionPlan plan, OperationalIssue original, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_41)
                .createObject(
                        """
                        Validate this intervention plan against the original issue:
                        Plan: %s
                        Original issue: %s

                        Check:
                        1. Does it address the root cause?
                        2. Is cost proportional to impact?
                        3. What are the risks?
                        4. What metrics should improve and by how much?

                        Set approved=true if the plan adequately addresses the issue.
                        """.formatted(plan.getContent(), original.description()),
                        ValidatedPlan.class
                );
    }

    /**
     * Human-in-the-loop gate: pause for human approval before execution.
     * This action achieves the agent's goal — the plan is cleared for execution only after
     * a human confirms it.
     */
    @AchievesGoal(
            description = "Human-approved intervention plan cleared for execution",
            export = @Export(
                    remote = true,
                    name = "interventionPlan",
                    startingInputTypes = {UserInput.class}
            )
    )
    @Action(cost = 0.05)
    public ApprovedPlan confirmExecution(ValidatedPlan plan) {
        WaitFor.confirmation(plan, "Approve this intervention plan for execution?");
        return ApprovedPlan.from(plan);
    }
}
