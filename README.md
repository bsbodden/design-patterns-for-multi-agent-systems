# Design Patterns for Multi-Agent Systems

Companion code for the DevNexus 2026 talk **"Design Patterns for Multi-Agent Systems"** — three Java demos comparing 15 orchestration patterns across Spring AI, LangChain4J, and Embabel.

Speaker: **Brian Sam-Bodden** — Java Champion, Principal Applied AI Engineer at Redis.

[Slides (PDF)](design_patterns_for_multi-agent_systems.pdf)

---

## Architecture

| Demo | Framework | Abstraction | Patterns | Port |
|------|-----------|-------------|----------|------|
| **Demo 1** | Spring AI | Low-level (wire it yourself) | Routing, Tool Use + MCP, Generator-Critic | `18881` |
| **Demo 2** | LangChain4J | Explicit workflows | Sequential Chain, Parallel Fan-Out, Shared Memory, Supervisor | `18882` |
| **Demo 3** | Embabel | Intelligent planning | GOAP Planning, OODA Reflection, Open Mode / Swarm | `18883` |

All three demos share a Redis Stack instance and operate on the same Olist e-commerce dataset. Demo 1 hosts the unified UI and proxies to Demo 2 and Demo 3 backends.

```
┌─────────────────────────────────────────────────────────┐
│  Browser → http://localhost:18881                       │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Demo 1      │  │  Demo 2      │  │  Demo 3      │  │
│  │  Spring AI   │──│  LangChain4J │  │  Embabel     │  │
│  │  :8881       │  │  :8882       │  │  :8883       │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │           │
│  ┌──────┴─────────────────┴─────────────────┴───────┐  │
│  │              Redis Stack :6379                    │  │
│  └──────────────────────────────────────────────────-┘  │
│         │                                               │
│  ┌──────┴───────┐                                       │
│  │ Memory Server│  (MCP — redis-spring-ai-memory)       │
│  │  :8080       │                                       │
│  └──────────────┘                                       │
└─────────────────────────────────────────────────────────┘
```

## Prerequisites

- **Docker** and **Docker Compose** (v2)
- An **OpenAI API key** (GPT-4.1 family)

## Quick Start

### 1. Create the `.env` file

```bash
cp .env.example .env
# or create it manually:
cat > .env <<EOF
OPENAI_API_KEY=sk-proj-your-key-here
EOF
```

### 2. Start everything

```bash
docker compose up --build -d
```

This builds and starts all five services:

| Service | Container | External Port | Internal Port |
|---------|-----------|---------------|---------------|
| Redis Stack | `code-redis-1` | `16379` | `6379` |
| Redis Insight | (bundled with Redis Stack) | `18001` | `8001` |
| Memory Server (MCP) | `code-memory-server-1` | `18080` | `8080` |
| Demo 1 — Spring AI | `code-demo1-spring-ai-1` | `18881` | `8881` |
| Demo 2 — LangChain4J | `code-demo2-langchain4j-1` | `18882` | `8882` |
| Demo 3 — Embabel | `code-demo3-embabel-1` | `18883` | `8883` |

### 3. Open the UI

Once all containers are healthy:

```
http://localhost:18881
```

### 4. Verify services are up

```bash
# All containers running
docker compose ps

# Redis
docker compose exec redis redis-cli ping

# Memory Server health
curl http://localhost:18080/actuator/health

# Demo 1 health
curl http://localhost:18881/actuator/health
```

## Data Seeding

Data loads **automatically** on startup — no manual steps required.

**Olist operational data** (orders, sellers, customers, products) is bundled in each demo's classpath and seeded into Redis via Redis OM Spring repositories on first boot.

**Agent memories** (SOPs, episodic interactions, entity profiles, semantic knowledge) are seeded into the Memory Server via MCP tools on Demo 1 startup. This requires the Memory Server to be healthy first (handled by Docker Compose `depends_on`).

The curated dataset under `shared-data/olist/` contains planted patterns (late deliveries from specific sellers, repeat complainers) that the demos are designed to surface.

## Running Individual Demos

Start only the services you need:

```bash
# Just Demo 1 (pulls in redis + memory-server as dependencies)
docker compose up --build demo1-spring-ai -d

# Just Demo 2 (pulls in redis)
docker compose up --build demo2-langchain4j -d

# Just Demo 3 (pulls in redis)
docker compose up --build demo3-embabel -d
```

## Stopping

```bash
# Stop all services
docker compose down

# Stop and remove volumes (clean slate)
docker compose down -v
```

## Local Development (without Docker)

For iterating on a single demo outside Docker:

```bash
# Start Redis Stack
docker compose up redis -d

# Start Memory Server (needed for Demo 1 MCP tools)
docker compose up memory-server -d

# Demo 1 (Gradle)
cd demo1-spring-ai-smart-triage
./gradlew bootRun

# Demo 2 (Gradle)
cd demo2-langchain4j-doc-insight
./gradlew bootRun

# Demo 3 (Maven)
cd demo3-embabel-research-planner
mvn spring-boot:run
```

When running locally, demos connect to Redis on `localhost:16379` and the Memory Server on `localhost:18080` (see each demo's `application.yml`).

## Project Structure

```
code/
├── docker-compose.yml                     # Full stack orchestration
├── .env                                   # API keys (not committed)
├── shared-data/olist/                     # Curated Olist dataset (JSON)
├── scripts/
│   ├── curate-olist-data.py               # Generate curated dataset
│   └── download-olist.sh                  # Download raw Olist data from Kaggle
├── demo1-spring-ai-smart-triage/          # Gradle — Spring AI
├── demo2-langchain4j-doc-insight/         # Gradle — LangChain4J
└── demo3-embabel-research-planner/        # Maven — Embabel
```

## Framework Versions

| Framework | Version | Build |
|-----------|---------|-------|
| Spring AI | BOM 1.1.0 (Spring Boot 3.4.2) | Gradle 8.11 |
| LangChain4J | 1.11.0 + agentic 1.11.0-beta19 | Gradle 8.11 |
| Embabel | 0.3.5-SNAPSHOT (Spring Boot 3.5.9) | Maven 3.9 |
| Java | 21 (Eclipse Temurin) | |
| Redis Stack | latest | |

## Troubleshooting

**Memory server fails health check** — It needs ~30s to start. Docker Compose retries automatically. Check logs:
```bash
docker compose logs memory-server
```

**Demo 1 shows no memory tools** — The MCP connection to the Memory Server may have timed out. Restart Demo 1:
```bash
docker compose restart demo1-spring-ai
```

**Redis connection refused** — Ensure Redis is healthy before starting demos:
```bash
docker compose up redis -d && docker compose exec redis redis-cli ping
```

**OpenAI rate limits** — The demos use GPT-4.1 models. If you hit rate limits, wait a moment between demo runs.
