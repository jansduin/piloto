# PILOTO F1

**Enterprise-Grade Cognitive Kernel & API Gateway**

Multi-module Java 17 / Spring Boot 3.2 system implementing a cognitive AI orchestration framework with evolutionary prompt optimization.

## Architecture

```
piloto-f1/
├── src/                    # CDI Kernel (core engine)
│   └── main/java/          
│       └── com.piloto.cdi.kernel
│           ├── executive/         # CQRS command processing
│           ├── orchestrator/      # Agent coordination & reasoning
│           ├── governance/        # Domain types & policies
│           ├── store/             # Event sourcing & snapshots
│           ├── tools/             # Tool execution engine
│           │   └── selection/     # UCB1 multi-armed bandit
│           ├── evaluation/        # Command validation
│           └── diagnostics/       # Telemetry & anomaly detection
│
├── cdi-api-gateway/        # Spring Boot API Gateway
│   └── src/main/java/
│       └── com.piloto.cdi.gateway
│           ├── config/            # Bean registration
│           ├── llm/               # LLM providers (Gemini, OpenAI, Ollama)
│           │   └── bridge/        # Cognitive agents
│           ├── governance/
│           │   ├── controller/    # REST endpoints (/gov)
│           │   ├── service/       # Prompt governance engine
│           │   ├── repository/    # Prompt registry
│           │   └── evolution/     # Cognitive Evolution Engine (CEE)
│           │       ├── model/     # BehavioralCell, EvolutionVariant
│           │       ├── engine/    # MAP-Elites, TrafficRouter, OPRO
│           │       ├── store/     # Tenant-segmented JSON persistence
│           │       └── config/    # EvolutionProperties
│           ├── events/            # Conversion signal emitter
│           └── websocket/         # Real-time event channels
│
└── piloto-ui/              # Frontend (React)
```

## Cognitive Evolution Engine (CEE)

Hybrid evolutionary framework for autonomous prompt optimization:

- **MAP-Elites** — Quality-diversity grid with behavioral cells
- **OPRO** — LLM-as-optimizer generating candidates from fitness history
- **PromptBreeder** — Self-referential meta-prompt evolution
- **Wilson Score** — Statistical confidence for variant promotion
- **Epsilon-Greedy** — Traffic routing for challenger evaluation
- **UCB1** — Multi-armed bandit for tool selection

## Tech Stack

- **Java 17** / **Spring Boot 3.2.2**
- **Jackson** with JavaTimeModule
- **Google Gemini** (primary LLM provider)
- **Ollama** (local fallback)
- **Maven** multi-module

## Build

```bash
./mvnw clean install -DskipTests
```

## Run

```bash
./mvnw spring-boot:run -f cdi-api-gateway/pom.xml
```

Requires `GOOGLE_API_KEY` environment variable.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/gov/evolution/variant` | Get optimal prompt variant |
| POST | `/gov/evolution/commands/report-outcome` | Report session outcome |
| POST | `/gov/evolution/commands/trigger-optimization` | Trigger OPRO cycle |
| GET | `/gov/evolution/grid` | MAP-Elites grid state |

## License

Proprietary — All rights reserved.
