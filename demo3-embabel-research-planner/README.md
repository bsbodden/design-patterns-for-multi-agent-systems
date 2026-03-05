# Demo 3: Research Planner — Embabel

## Patterns Demonstrated

1. **Deliberative Planning** (#8) — GOAP A* planner discovers optimal action sequences
2. **Reflection** (#7) — OODA loop re-evaluates conditions and replans mid-execution
3. **Swarm / Emergent Composition** (#13) — Open Mode constructs agents dynamically from all available actions

## The Scenario

A **research assistant** that takes a topic and produces a well-researched writeup. Multiple actions are available (extract topic, search web, find academic papers, summarize findings, write draft, review draft). The planner discovers how to combine them.

This is the "wow" demo. The audience has just seen Spring AI (you wire everything) and LangChain4J (you compose workflows explicitly). Now they see Embabel: **you declare actions and goals, the A* planner finds the path**.

## What the Audience Sees

The demo uses the **Embabel Shell** with planning traces enabled (`-P` flag). The terminal shows the A* search in real time, then each action executing with typed inputs and outputs.

### Visual Design

```
┌─────────────────────────────────────────────────────────┐
│  RESEARCH PLANNER — Embabel Demo                        │
│  Patterns: GOAP Planning → OODA Reflection → Open Mode  │
└─────────────────────────────────────────────────────────┘


═══ ACT 1: GOAP PLANNING ════════════════════════════════

embabel> agents

Registered Agents:
  ★ ResearchPlanner
    "Research a topic and produce a reviewed writeup"
    Actions: 6 | Goals: 1

embabel> x "Research the current state of GOAP planning for AI agents" -P

┌─ PLANNING (A* Search) ─────────────────────────────────
│
│  Goal: "Produce a reviewed research writeup"
│  Start state: { userInput: ✓ }
│  Available actions: 6
│
│  A* Iteration 1:
│    State: { userInput: ✓ }
│    Evaluating: extractTopic ──→ ACHIEVABLE (cost: 0.05)
│    Evaluating: searchWeb ──→ BLOCKED (needs: topic)
│    Evaluating: writeDraft ──→ BLOCKED (needs: findings)
│    Best: extractTopic
│
│  A* Iteration 2:
│    State: { userInput: ✓, topic: ✓ }
│    Evaluating: searchWeb ──→ ACHIEVABLE (cost: 0.10)
│    Evaluating: findPapers ──→ ACHIEVABLE (cost: 0.08)
│    Best: findPapers (lower cost)
│
│  A* Iteration 3:
│    State: { userInput: ✓, topic: ✓, papers: ✓ }
│    Evaluating: searchWeb ──→ ACHIEVABLE (cost: 0.10)
│    Evaluating: summarize ──→ BLOCKED (needs: webResults)
│    Best: searchWeb
│
│  A* Iteration 4:
│    State: { userInput: ✓, topic: ✓, papers: ✓, webResults: ✓ }
│    Evaluating: summarize ──→ ACHIEVABLE
│    Best: summarize
│
│  A* Iteration 5:
│    State: { ..., findings: ✓ }
│    Evaluating: writeDraft ──→ ACHIEVABLE
│    Best: writeDraft
│
│  A* Iteration 6:
│    State: { ..., draft: ✓ }
│    Evaluating: reviewDraft ──→ ACHIEVABLE → SATISFIES GOAL ✓
│
│  ✓ PLAN FOUND in 6 iterations
│
│  Path: extractTopic → findPapers → searchWeb
│        → summarize → writeDraft → reviewDraft
│
│  Total estimated cost: 0.52
│  Plan is optimal: YES
│
└─────────────────────────────────────────────────────────

"I didn't write this workflow. The planner FOUND it by
 reasoning about what each action needs and produces."

┌─ EXECUTION ─────────────────────────────────────────────
│
│  [1/6] extractTopic
│    Input: UserInput("Research the current state of...")
│    LLM: gpt-4.1-mini (cheap model for extraction)
│    Output: Topic("GOAP planning for AI agents")
│    Time: 0.8s | Cost: $0.002
│
│  [2/6] findPapers
│    Input: Topic("GOAP planning for AI agents")
│    Tools: [WEB]
│    Output: Papers(3 results from arxiv/scholar)
│    Time: 2.1s | Cost: $0.008
│
│  [3/6] searchWeb
│    Input: Topic("GOAP planning for AI agents")
│    Tools: [WEB]
│    Output: WebResults(5 articles, blog posts)
│    Time: 1.9s | Cost: $0.006
│
│  [4/6] summarize
│    Input: Papers + WebResults
│    LLM: gpt-4.1 (capable model for synthesis)
│    Output: Findings("GOAP originated in game AI...")
│    Time: 3.2s | Cost: $0.024
│
│  [5/6] writeDraft
│    Input: Topic + Findings
│    LLM: gpt-4.1 (temperature: 0.7)
│    Output: Draft(markdown, ~800 words)
│    Time: 4.1s | Cost: $0.031
│
│  [6/6] reviewDraft ← @AchievesGoal
│    Input: Draft + Findings (for fact-checking)
│    LLM: gpt-4.1 (temperature: 0.1, strict)
│    Output: ReviewedWriteup(markdown, ~900 words)
│    Time: 3.4s | Cost: $0.028
│    ✓ GOAL ACHIEVED
│
│  Total: 15.5s | Cost: $0.099 | Tokens: 4,200 in / 1,800 out
│
└─────────────────────────────────────────────────────────


═══ ACT 2: OODA LOOP (Replanning) ═══════════════════════

embabel> x "Research quantum computing breakthroughs in 2026" -P

┌─ PLANNING ──────────────────────────────────────────────
│  Plan: extractTopic → findPapers → searchWeb
│        → summarize → writeDraft → reviewDraft
└─────────────────────────────────────────────────────────

┌─ EXECUTION ─────────────────────────────────────────────
│
│  [1/6] extractTopic
│    Output: Topic("quantum computing breakthroughs 2026")
│    ✓ Conditions still valid. Continuing plan.
│
│  [2/6] findPapers
│    Output: Papers(0 results — too recent for arxiv)
│    ⚠ CONDITION CHANGED: papers.isEmpty() = true
│
│  ┌─ REPLANNING (OODA) ──────────────────────────────────
│  │  Observe: papers is empty
│  │  Orient: summarize needs papers OR webResults
│  │  Decide: skip summarize's paper dependency, rely on web
│  │  Act: replan from current state
│  │
│  │  New plan: searchWeb → summarize → writeDraft → reviewDraft
│  │  (dropped findPapers from remaining plan)
│  └──────────────────────────────────────────────────────
│
│  [3/6] searchWeb
│    Output: WebResults(7 articles — web has recent content)
│    ✓ Conditions valid.
│
│  [4/5] summarize
│    Input: WebResults only (no papers)
│    Output: Findings(...)
│    ✓ Continuing.
│
│  ... (continues to completion)
│
│  ✓ GOAL ACHIEVED (replanned once, adapted to missing data)
│
└─────────────────────────────────────────────────────────

"The planner adapted. When academic papers weren't available
 for a 2026 topic, it replanned to rely on web sources only.
 No error handling code. No fallback logic. The planner
 figured it out."


═══ ACT 3: OPEN MODE ════════════════════════════════════

embabel> goals

Available Goals:
  1. "Produce a reviewed research writeup"  (ResearchPlanner)

embabel> actions

Available Actions (across all agents + platform):
  extractTopic, findPapers, searchWeb, summarize,
  writeDraft, reviewDraft
  + translateToSpanish  ← (NEW — just added as a @Bean)

"I just added a translateToSpanish @Action to the Spring
 context. Watch what Open Mode does with it."

embabel> x "Investiga GOAP planning y escribe en español" -o -P

┌─ OPEN MODE PLANNING ───────────────────────────────────
│
│  Intent analysis: User wants research in Spanish
│
│  Goal ranking:
│    1. "Produce a reviewed research writeup" (score: 0.92)
│       → Selected
│
│  Constructing dynamic agent from ALL available actions...
│
│  Plan: extractTopic → searchWeb → summarize
│        → writeDraft → reviewDraft → translateToSpanish
│                                      ↑
│                                      NEW action discovered!
│
│  "The planner found translateToSpanish and added it to
│   the plan. I never told it to. It saw the precondition
│   (needs: reviewedWriteup) matched the effect of
│   reviewDraft, and the user's intent was Spanish."
│
└─────────────────────────────────────────────────────────

┌─ EXECUTION ─────────────────────────────────────────────
│  [1/6] extractTopic → ... (same as before)
│  ...
│  [6/6] translateToSpanish ← DYNAMICALLY DISCOVERED
│    Input: ReviewedWriteup
│    Output: SpanishWriteup("# Planificación GOAP...")
│    ✓ GOAL ACHIEVED
└─────────────────────────────────────────────────────────

"I added one Spring bean. The planner discovered it and
 used it. No rewiring. No new workflow definition.
 That's emergent composition."
```

### Why This Visual Design Works

- **Act 1 (GOAP)**: The A* trace is the centerpiece. The audience watches the planner explore states, evaluate which actions are achievable, and build the path step by step. They SEE it's not random — it's A* finding the optimal sequence. "This is the same algorithm that pathfinds in video games."
- **Act 2 (OODA)**: The replan moment is dramatic. The plan was working, then papers came back empty, and the planner *adapted on its own*. No try/catch. No fallback code. The OODA loop is built in. "Every other framework would have thrown an error here."
- **Act 3 (Open Mode)**: Adding a `@Bean` and watching the planner discover it is the "mic drop." The audience realizes: "If I just add capabilities, the system gets smarter automatically."

## Architecture

```
                    User Input
                        │
                        ▼
                  ┌───────────┐
                  │   GOAP    │  A* search over
                  │  Planner  │  preconditions/effects
                  └─────┬─────┘
                        │ Plan: [action1, action2, ...]
                        ▼
                  ┌───────────┐
                  │  OODA     │  After each action:
                  │  Loop     │  Observe → Orient → Decide → Act
                  └─────┬─────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
    ┌───────────┐ ┌───────────┐ ┌───────────┐
    │@Action    │ │@Action    │ │@Action    │
    │extractTopic│ │findPapers │ │searchWeb  │
    │           │ │tools=[WEB]│ │tools=[WEB]│
    │in: Input  │ │in: Topic  │ │in: Topic  │
    │out: Topic │ │out: Papers│ │out: WebRes│
    └───────────┘ └───────────┘ └───────────┘
          │             │             │
          ▼             ▼             ▼
    ┌───────────┐ ┌───────────┐ ┌───────────┐
    │@Action    │ │@Action    │ │@Action    │
    │summarize  │ │writeDraft │ │reviewDraft│
    │           │ │temp: 0.7  │ │@AchievesGoal
    │in: Papers │ │in: Topic  │ │in: Draft  │
    │  + WebRes │ │  + Findings│ │  + Findings│
    │out:Findings│ │out: Draft │ │out: Writeup│
    └───────────┘ └───────────┘ └───────────┘

    Blackboard (typed domain objects):
    ┌────────────────────────────────────────┐
    │ UserInput → Topic → Papers → WebResults│
    │ → Findings → Draft → ReviewedWriteup   │
    └────────────────────────────────────────┘
```

## Implementation Plan

### Agent Definition

```java
@Agent(description = "Research a topic and produce a reviewed writeup",
       scan = true)
public class ResearchPlanner {

    @Action(cost = 0.05)
    public Topic extractTopic(UserInput input, OperationContext ctx) {
        return ctx.ai().withLlm(OpenAiModels.GPT_4_1_MINI)
            .createObject("Extract the research topic from: " + input.text());
    }

    @Action(cost = 0.08, toolGroups = {CoreToolGroups.WEB})
    public Papers findPapers(Topic topic) {
        // LLM uses web tools to search arxiv/scholar
    }

    @Action(cost = 0.10, toolGroups = {CoreToolGroups.WEB})
    public WebResults searchWeb(Topic topic) {
        // LLM uses web tools to search general web
    }

    @Action(cost = 0.15)
    public Findings summarize(Papers papers, WebResults webResults,
                              OperationContext ctx) {
        return ctx.ai().withLlm(OpenAiModels.GPT_4_1)
            .createObject("Synthesize these sources into key findings...");
    }

    @Action(cost = 0.20)
    public Draft writeDraft(Topic topic, Findings findings,
                           OperationContext ctx) {
        return ctx.ai().withLlm(OpenAiModels.GPT_4_1)
            .withTemperature(0.7)
            .createObject("Write a research writeup about " + topic.name());
    }

    @AchievesGoal(description = "Produce a reviewed research writeup")
    @Action(cost = 0.20)
    public ReviewedWriteup reviewDraft(Draft draft, Findings findings,
                                       OperationContext ctx) {
        return ctx.ai().withLlm(OpenAiModels.GPT_4_1)
            .withTemperature(0.1) // strict review
            .createObject("Review this draft for accuracy...");
    }
}
```

### The Translation Action (Added Live for Open Mode)

```java
@Component  // Just a Spring bean — the planner discovers it automatically
public class TranslationActions {

    @Action(cost = 0.10)
    public SpanishWriteup translateToSpanish(ReviewedWriteup writeup,
                                              OperationContext ctx) {
        return ctx.ai().withLlm(OpenAiModels.GPT_4_1_MINI)
            .createObject("Translate to Spanish: " + writeup.content());
    }
}
```

### Domain Objects

```java
public record UserInput(String text) {}
public record Topic(String name) {}
public record Papers(List<PaperReference> items) {}
public record WebResults(List<WebReference> items) {}
public record Findings(String content) {}
public record Draft(String content) implements HasContent {}
public record ReviewedWriteup(String content) implements HasContent {}
public record SpanishWriteup(String content) implements HasContent {}
```

### Key Classes

| Class | Purpose |
| --- | --- |
| `ResearchPlannerApp` | Spring Boot main + Embabel Shell |
| `ResearchPlanner` | The `@Agent` with 6 actions |
| `TranslationActions` | Standalone `@Component` with `@Action` for Open Mode demo |
| Domain records | `Topic`, `Papers`, `WebResults`, `Findings`, `Draft`, `ReviewedWriteup`, `SpanishWriteup` |

### Demo Script (What You Say While It Runs)

**Act 1 — GOAP Planning:**

> "I have 6 actions. Each declares what it needs and what it produces — just like GOAP in video games. Watch the A* planner search for the optimal path to our goal. It evaluates each action's preconditions against the current state, picks the cheapest achievable one, and builds forward. It found: extract → papers → web → summarize → draft → review. I didn't write this workflow. The planner *discovered* it."

**Act 2 — OODA Replanning:**

> "Same agent, but 'quantum computing 2026' is too recent for academic databases. Watch — papers comes back empty. The OODA loop kicks in: observe the new state, re-orient, re-decide. The planner drops papers from the remaining plan and proceeds with web sources only. No error handling code. No fallback logic. This is what reflection looks like when it's *built into the planner*."

**Act 3 — Open Mode:**

> "I just added a `translateToSpanish` action as a Spring bean. Nothing else changed. Now watch Open Mode — the user asks for research in Spanish. The planner searches ALL available actions across the entire platform, finds `translateToSpanish`, sees that its precondition (`ReviewedWriteup`) matches the effect of `reviewDraft`, and adds it to the plan. *Emergent composition*. Adding capabilities to the system is as simple as adding a Spring bean."

### Build Configuration

This uses Maven (Embabel's native build system):

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.embabel.agent</groupId>
        <artifactId>embabel-agent-starter-shell</artifactId>
    </dependency>
    <dependency>
        <groupId>com.embabel.agent</groupId>
        <artifactId>embabel-agent-starter-openai</artifactId>
    </dependency>
    <dependency>
        <groupId>com.embabel.agent</groupId>
        <artifactId>embabel-agent-starter-observability</artifactId>
    </dependency>
</dependencies>
```

### Environment Requirements

- Java 21+
- `OPENAI_API_KEY` environment variable
- Embabel agent framework (built from `repos/embabel-agent` or pulled from Maven Central)

### Observability (Optional Enhancement)

If Langfuse is configured, the audience can see a trace visualization after each run — showing the full action tree, LLM calls with prompts/completions, tool invocations, and cost breakdown. This adds visual impact but isn't required for the demo to work.

## Reference Repos

- `repos/embabel-agent/` — the framework itself (contains StarNewsFinder as the canonical example)
- `repos/embabel-agent-examples/` — official examples (fact checker, bank support, star news finder)
- `repos/tripper/` — Rod Johnson's travel planner demo (GOAP + MCP + multiple LLMs)
- `repos/langgraph-patterns/` — Embabel reimplementation of LangGraph patterns
