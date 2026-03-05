# Demo 2: Doc Insight Pipeline — LangChain4J

## Patterns Demonstrated

1. **Sequential Chain** (#1) — Agents execute in order, output flows via AgenticScope
2. **Parallel Fan-Out** (#3) — Independent analyses run concurrently, results merged
3. **Shared Memory / Blackboard** (#9) — AgenticScope as the typed shared state
4. **Hierarchical Delegation** (#4) — Supervisor dynamically decides which agents to invoke

## The Scenario

A **document analysis pipeline** that takes a technical article and produces a comprehensive analysis report. The demo runs the *same document* through three different orchestration modes, letting the audience see the difference between sequential, parallel, and supervisor-driven execution.

This is a pattern the audience encounters daily: "I need to process this thing through multiple steps." The demo shows three ways to orchestrate those steps.

## What the Audience Sees

The demo runs as a Spring Boot CLI application. The key visual element is the **AgenticScope state table** — a live view of the shared blackboard that updates as agents read and write.

### Visual Design

```
┌─────────────────────────────────────────────────────────┐
│  DOC INSIGHT PIPELINE — LangChain4J Demo                │
│  Patterns: Sequential → Parallel → Supervisor           │
└─────────────────────────────────────────────────────────┘

━━━ INPUT DOCUMENT ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Title: "GOAP Planning for LLM Agent Systems"
Length: 2,400 words | Source: Technical blog post
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


═══ MODE 1: SEQUENTIAL ═══════════════════════════════════

AgenticScope State:
┌──────────────┬───────────┬──────────────────────────────┐
│ Key          │ Written By│ Value (truncated)            │
├──────────────┼───────────┼──────────────────────────────┤
│ document     │ [input]   │ "GOAP Planning for LLM..."   │
│ summary      │ Summarizer│ "This article explores..."   │
│ sentiment    │ Analyst   │ {tone: "technical", ...}     │
│ topics       │ Extractor │ ["GOAP", "A*", "planning"]   │
│ report       │ Reporter  │ "## Analysis Report\n..."    │
└──────────────┴───────────┴──────────────────────────────┘

Pipeline: Summarizer ──→ Sentiment ──→ Topics ──→ Reporter
                2.1s        1.8s        1.4s       2.3s

Total: 7.6s (sequential, each waits for the previous)


═══ MODE 2: PARALLEL + SEQUENTIAL ════════════════════════

AgenticScope State:
┌──────────────┬───────────┬──────────────────────────────┐
│ Key          │ Written By│ Value (truncated)            │
├──────────────┼───────────┼──────────────────────────────┤
│ document     │ [input]   │ "GOAP Planning for LLM..."   │
│ summary      │ Summarizer│ "This article explores..."   │
│ sentiment    │ Analyst   │ {tone: "technical", ...}     │
│ topics       │ Extractor │ ["GOAP", "A*", "planning"]   │
│ report       │ Reporter  │ "## Analysis Report\n..."    │
└──────────────┴───────────┴──────────────────────────────┘

Pipeline:  ┌─ Summarizer ──┐
           │     2.1s       │
   doc ──→ ├─ Sentiment ───┤ ──→ Reporter
           │     1.8s       │      2.3s
           ├─ Topics ──────┤
           │     1.4s       │
           └───────────────┘
              (parallel)

Total: 4.4s (parallel phase: max(2.1, 1.8, 1.4) + reporter: 2.3)
Speedup: 1.7x vs sequential


═══ MODE 3: SUPERVISOR ═══════════════════════════════════

AgenticScope State:
┌──────────────┬───────────┬──────────────────────────────┐
│ Key          │ Written By│ Value (truncated)            │
├──────────────┼───────────┼──────────────────────────────┤
│ document     │ [input]   │ "GOAP Planning for LLM..."   │
│ summary      │ Summarizer│ "This article explores..."   │
│ report       │ Reporter  │ "## Analysis Report\n..."    │
└──────────────┴───────────┴──────────────────────────────┘

Supervisor decisions:
  Step 1: "This is a technical article. I'll summarize first."
          → Invoked: Summarizer
  Step 2: "The summary is sufficient for a report. Sentiment
           and topic extraction aren't needed for this type."
          → Invoked: Reporter
  Step 3: "Report is complete."
          → DONE

Total: 4.8s (supervisor overhead: 0.4s + summarizer: 2.1s + reporter: 2.3s)
Agents used: 2 of 4 (supervisor SKIPPED sentiment + topics)


═══ COMPARISON ═══════════════════════════════════════════

┌──────────────┬───────┬────────────┬───────────────────┐
│ Mode         │ Time  │ Agents Run │ Key Difference     │
├──────────────┼───────┼────────────┼───────────────────┤
│ Sequential   │ 7.6s  │ 4 of 4     │ Predictable, slow  │
│ Parallel     │ 4.4s  │ 4 of 4     │ Fast, all run      │
│ Supervisor   │ 4.8s  │ 2 of 4     │ Smart, skips work  │
└──────────────┴───────┴────────────┴───────────────────┘

"Same agents, same scope, different orchestration."
```

### Why This Visual Design Works

- **The AgenticScope table** is the star — the audience watches keys appear as agents write them. This makes the blackboard pattern tangible. "See how Summarizer wrote to `summary` and Reporter read from it? That's shared state."
- **The timing comparison** makes the parallel speedup visceral. "Same work, 1.7x faster, because we recognized the first three agents are independent."
- **The supervisor skipping agents** is the "aha" — "The supervisor decided sentiment analysis wasn't needed for a technical article. It saved time *and* cost by not doing unnecessary work."

## Architecture

```
                        ┌─────────────────────────┐
    Input Document ───→ │     AgenticScope        │
                        │ (shared blackboard)     │
                        │                         │
                        │  document: "..."        │
                        │  summary: null → "..."  │
                        │  sentiment: null → {...} │
                        │  topics: null → [...]    │
                        │  report: null → "..."    │
                        └─────────────────────────┘
                              ▲    ▲    ▲    ▲
                              │    │    │    │
                  ┌───────────┘    │    │    └──────────┐
                  │                │    │               │
            ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
            │ Summarizer│  │ Sentiment │  │  Topic    │  │  Report   │
            │   Agent   │  │  Analyst  │  │ Extractor │  │  Writer   │
            └───────────┘  └───────────┘  └───────────┘  └───────────┘
                 ↑               ↑              ↑              ↑
                 └───────────────┼──────────────┘              │
                                 │                             │
                     Three orchestration modes:
                     1. Sequential: A → B → C → D
                     2. Parallel: [A,B,C] → D
                     3. Supervisor: LLM picks A → D (skips B,C)
```

## Implementation Plan

### Agent Definitions

Each agent is a LangChain4J `@Agent`-annotated interface:

```java
public interface SummarizerAgent {
    @UserMessage("""
        Summarize this document in 3-4 sentences, focusing on
        the key technical contributions.
        Document: {{document}}
        """)
    @Agent("Summarizes a technical document")
    String summarize(@V("document") String document);
}

public interface SentimentAnalyst {
    @UserMessage("""
        Analyze the tone and sentiment of this document.
        Return a JSON object with: tone, audience_level, objectivity_score.
        Document: {{document}}
        """)
    @Agent("Analyzes document tone and sentiment")
    SentimentResult analyze(@V("document") String document);
}

public interface TopicExtractor {
    @UserMessage("""
        Extract the 5 most important technical topics from this document.
        Return as a JSON array of strings.
        Document: {{document}}
        """)
    @Agent("Extracts key topics from a document")
    List<String> extract(@V("document") String document);
}

public interface ReportWriter {
    @UserMessage("""
        Write a concise analysis report based on these inputs.
        Summary: {{summary}}
        Sentiment: {{sentiment}}
        Topics: {{topics}}
        Use markdown formatting.
        """)
    @Agent("Writes the final analysis report")
    String writeReport(
        @V("summary") String summary,
        @V("sentiment") String sentiment,
        @V("topics") String topics);
}
```

### Three Orchestration Modes

```java
// Mode 1: Sequential
UntypedAgent sequential = AgenticServices.sequenceBuilder()
    .subAgents(summarizer, sentimentAnalyst, topicExtractor, reportWriter)
    .outputKey("report")
    .build();

// Mode 2: Parallel + Sequential
UntypedAgent parallelAnalysis = AgenticServices.parallelBuilder()
    .subAgents(summarizer, sentimentAnalyst, topicExtractor)
    .executor(Executors.newFixedThreadPool(3))
    .build();

UntypedAgent parallelPipeline = AgenticServices.sequenceBuilder()
    .subAgents(parallelAnalysis, reportWriter)
    .outputKey("report")
    .build();

// Mode 3: Supervisor
SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
    .chatModel(plannerModel)
    .subAgents(summarizer, sentimentAnalyst, topicExtractor, reportWriter)
    .responseStrategy(SupervisorResponseStrategy.SUMMARY)
    .build();
```

### Key Classes

| Class | Purpose |
| --- | --- |
| `DocInsightApp` | Spring Boot main + runs all three modes on the same document |
| `SummarizerAgent` | Produces 3-4 sentence summary |
| `SentimentAnalyst` | Returns tone, audience level, objectivity score |
| `TopicExtractor` | Returns top-5 technical topics |
| `ReportWriter` | Synthesizes all inputs into a markdown report |
| `ScopeVisualizer` | Renders the AgenticScope state table in terminal |
| `TimingComparison` | Records and displays the comparison table |

### The AgenticScope Visualizer

The key visual component. After each agent runs, it reads the current scope state and renders the table:

```java
public class ScopeVisualizer {
    public static void render(AgenticScope scope, String agentName) {
        // Print table header
        // For each key in scope:
        //   Print key, who wrote it, value preview
        // Highlight the row that was just written by agentName
    }
}
```

This runs after every agent invocation, so the audience watches the table fill in row by row.

### Demo Script (What You Say While It Runs)

**Mode 1 — Sequential:**

> "Four agents, running one after another. Watch the AgenticScope table — each agent writes its result, and the next agent can read it. The Report Writer reads summary, sentiment, and topics. Total time: 7.6 seconds."

**Mode 2 — Parallel:**

> "Same four agents. But the first three are independent — they all read `document` and write different keys. So we run them in parallel. Watch — all three keys appear at roughly the same time. Then the Reporter synthesizes. 4.4 seconds. Same result, 1.7x faster."

**Mode 3 — Supervisor:**

> "Now the LLM decides. Watch the supervisor's reasoning — it reads the document, decides this is a technical article where sentiment analysis isn't useful. It calls Summarizer, then goes straight to Reporter. Only 2 of 4 agents run. The scope table is smaller. Sometimes the smartest optimization is *not doing work*."

**Transition:**

> "Spring AI gives you full control. LangChain4J gives you explicit workflow composition — sequential, parallel, conditional, supervisor — all sharing a typed blackboard. But notice: I had to *decide* the orchestration. What if the framework could *plan* the workflow for you?"

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
    implementation("dev.langchain4j:langchain4j:1.1.0")
    implementation("dev.langchain4j:langchain4j-agentic:1.1.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.1.0")
    implementation("org.springframework.boot:spring-boot-starter")
}
```

### Environment Requirements

- Java 21+
- `OPENAI_API_KEY` environment variable

### Sample Input Document

Bundled in `src/main/resources/sample-document.md` — a ~2,400-word technical article about GOAP planning for LLM agents. Chosen because:

- It's topical to the presentation
- It's long enough to make summarization meaningful
- It's technical enough that the supervisor might skip sentiment analysis
- The audience has been hearing about GOAP, so they can evaluate the summary quality

## Reference Repos

- `repos/langchain4j-examples/agentic-tutorial/` — official agentic patterns tutorial
- `repos/quarkus-agentic-ai/` — Mario Fusco's pattern-by-pattern demo (routing, parallel, agents-as-tools)
- `repos/langgraph-patterns/` — Embabel's comparison of LangGraph patterns in Java
