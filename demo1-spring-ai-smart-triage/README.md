# Demo 1: Smart Triage — Spring AI

## Patterns Demonstrated

1. **Routing** (#2) — LLM classifier dispatches queries to specialized handlers
2. **Tool Use + MCP** (#12) — Agents call tools, including Redis memory via MCP
3. **Generator-Critic** (#6) — Recursive Advisors produce self-improving responses

## The Scenario

A **developer support triage system** that receives mixed questions (billing, technical, product) and routes them intelligently. The technical handler uses tools (including Redis memory via MCP) to look up context. A quality advisor evaluates and improves every response before returning it.

This is a realistic scenario the audience relates to immediately: "I've built chatbots that answer questions. This one *triages, remembers, and self-improves*."

## What the Audience Sees

The demo runs as a Spring Boot CLI application with **structured, color-coded terminal output** that makes each pattern visually distinct.

### Visual Design

```
┌─────────────────────────────────────────────────────────┐
│  SMART TRIAGE — Spring AI Demo                          │
│  Patterns: Routing → Tool Use + MCP → Generator-Critic  │
└─────────────────────────────────────────────────────────┘

━━━ INCOMING QUERY ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"I was charged $49.99 but I'm on the free plan. Also, my
 API keys stopped working after the upgrade last night."
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─ STEP 1: ROUTING ───────────────────────────────────────
│  Classifier says: BILLING (confidence: 0.62)
│                   TECHNICAL (confidence: 0.38)
│  Decision: BILLING — handling the charge dispute first
│  Route: → billing-specialist
└─────────────────────────────────────────────────────────

┌─ STEP 2: TOOL USE ─────────────────────────────────────
│  Handler: billing-specialist
│
│  ⚡ Tool call: memory_search("user account plan history")
│    → MCP Server: redis-spring-ai-memory-server
│    → Found: "User upgraded from free to pro on 2026-02-20"
│
│  ⚡ Tool call: memory_store("billing dispute opened")
│    → Stored as episodic memory in Redis
│
│  Response generated (first draft)
└─────────────────────────────────────────────────────────

┌─ STEP 3: QUALITY REVIEW (Recursive Advisor) ───────────
│
│  Iteration 1:
│    Generator: "Your charge of $49.99 corresponds to..."
│    Evaluator: NEEDS_IMPROVEMENT (score: 2/4)
│    Feedback: "Missing: acknowledge the API key issue"
│
│  Iteration 2:
│    Generator: "I see two issues: the $49.99 charge AND..."
│    Evaluator: PASS (score: 4/4)
│    Feedback: "Addresses both concerns with next steps"
│
│  ✓ Approved after 2 iterations
└─────────────────────────────────────────────────────────

━━━ FINAL RESPONSE ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
I see two issues in your report:

1. **Billing**: The $49.99 charge corresponds to the Pro plan
   upgrade initiated on Feb 20. If this wasn't intentional,
   I've flagged it for a refund review...

2. **API Keys**: After plan changes, API keys need to be
   regenerated. Go to Settings → API Keys → Regenerate...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Summary: Routed to BILLING | 2 tool calls | 2 quality iterations
         Cost: $0.012 | Time: 4.3s
```

### Why This Visual Design Works

- **Step 1 (Routing)**: The audience sees the classifier pick a route with confidence scores — they immediately get that this is a classification problem, not a hardcoded switch.
- **Step 2 (Tool Use)**: The `memory_search` and `memory_store` calls to Redis via MCP are labeled clearly. The audience sees an agent that *reads and writes memory* — not just answering a question.
- **Step 3 (Generator-Critic)**: The iteration loop is the "wow" — they watch the response get better. Iteration 1 misses the API key concern. Iteration 2 nails it. The scoring makes quality measurable.

## Architecture

```
                    ┌──────────────┐
   User Query ───→  │   Router     │  (LLM classification)
                    │  ChatClient  │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Billing  │ │Technical │ │ Product  │
        │ Handler  │ │ Handler  │ │ Handler  │
        └────┬─────┘ └────┬─────┘ └──────────┘
             │            │
             ▼            ▼
        ┌──────────────────────┐
        │   Tool Call Advisor  │
        │  ┌────────────────┐  │
        │  │ @Tool methods  │  │    ┌──────────────────┐
        │  │ + MCP Client ──┼──┼──→ │ Redis Memory     │
        │  │   (memory_*)   │  │    │ MCP Server       │
        │  └────────────────┘  │    │ (8 tools)        │
        └──────────┬───────────┘    └──────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │  SelfRefine Advisor  │
        │  (Recursive)         │
        │                      │
        │  Generator ──→ Judge │
        │      ↑           │   │
        │      └───────────┘   │
        │  Loop until score≥3  │
        └──────────┬───────────┘
                   │
                   ▼
              Final Response
```

## Implementation Plan

### Key Classes

| Class | Purpose |
| --- | --- |
| `SmartTriageApp` | Spring Boot main class + CommandLineRunner |
| `TriageRouter` | Classifies input, returns route key + confidence |
| `TriageRouter.RoutingDecision` | Record: `{route, confidence, reasoning}` |
| `BillingHandler` | Specialized system prompt + tools for billing |
| `TechnicalHandler` | Specialized system prompt + tools for technical issues |
| `ProductHandler` | Specialized system prompt + tools for product questions |
| `DemoOutputFormatter` | Formats the structured terminal output with colors |

### Spring AI Components Used

| Component | Role in Demo |
| --- | --- |
| `ChatClient` | Core — used for routing classification and handler responses |
| `.entity(RoutingDecision.class)` | Structured output for route classification |
| `@Tool` annotation | Local tools (account lookup, ticket creation) |
| `ToolCallbackProvider` (MCP) | Redis memory tools discovered via MCP |
| `SelfRefineEvaluationAdvisor` | Recursive quality loop with separate judge model |
| `spring-ai-starter-mcp-client` | Connects to redis-spring-ai-memory-server |
| `spring-ai-starter-model-openai` | LLM provider |

### MCP Integration

The demo connects to `redis-spring-ai-memory-server` as an MCP server (SSE transport):

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        sse:
          connections:
            redis-memory:
              url: http://localhost:8080
              sse-endpoint: /mcp/messages
```

This exposes 8 Redis memory tools (`memory_store`, `memory_search`, `working_memory_add`, etc.) that the billing/technical handlers can call.

### Demo Script (What You Say While It Runs)

**Query 1** (Route to billing): "I was charged $49.99 but I'm on the free plan"

> "Watch the router classify this. It picks BILLING with 0.85 confidence. The billing handler generates a response, but notice — the Recursive Advisor catches that it didn't mention how to get a refund. Second iteration adds that. Quality went from 2 to 4."

**Query 2** (Route to technical + memory): "My API calls are failing with 429 errors since yesterday"

> "Now it routes to TECHNICAL. But watch the tool calls — it searches Redis memory for this user's recent interactions. It finds the billing dispute from Query 1 and connects the dots: 'Your rate limit may have changed when your plan was modified.' The agent is building context across queries."

**Query 3** (Ambiguous — shows routing reasoning): "Can you help me with my account?"

> "This is intentionally vague. Watch the confidence scores — billing 0.31, technical 0.28, product 0.24. The router picks billing by a slim margin and explains why. In production, you'd add a 'clarify' route for low-confidence cases."

### Build Configuration

```groovy
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.0"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client")
}
```

### Environment Requirements

- Java 21+
- `OPENAI_API_KEY` environment variable
- `redis-spring-ai-memory-server` running on port 8080 (MCP SSE endpoint)
- Redis Stack running on port 6379

## Reference Repos

- `repos/spring-ai-examples/agentic-patterns/routing-workflow/` — routing pattern reference
- `repos/spring-ai-examples/agentic-patterns/evaluator-optimizer/` — evaluator-optimizer reference
- `repos/spring-ai-examples/model-context-protocol/client-starter/` — MCP client reference
- `repos/redis-spring-ai-memory-server/` — the MCP server we connect to
