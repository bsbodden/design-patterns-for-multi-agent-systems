package com.redis.demos.researchplanner;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.models.OpenAiModels;
import com.redis.demos.researchplanner.domain.EscalationReport;
import com.redis.demos.researchplanner.domain.ValidatedPlan;

/**
 * Standalone escalation actions that can be discovered by Open Mode.
 *
 * This is NOT part of the InterventionPlanner agent — it's a separate Spring
 * component. When Open Mode is active, the planner sees this action alongside
 * the InterventionPlanner actions and can compose them into a single plan.
 *
 * Demo flow: show the planner discovering and using this action automatically.
 */
@EmbabelComponent
public class EscalationActions {

    @Action(cost = 0.10)
    public EscalationReport escalate(ValidatedPlan plan, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_41)
                .createObject(
                        """
                        Create an executive escalation report in Portuguese for management.

                        Plan to escalate:
                        %s

                        Include:
                        - Executive summary (Resumo Executivo)
                        - Recommended actions (Ações Recomendadas)
                        - Timeline (Cronograma)
                        - Risk assessment (Avaliação de Riscos)

                        Write the entire report in Brazilian Portuguese.
                        """.formatted(plan.getContent()),
                        EscalationReport.class
                );
    }
}
