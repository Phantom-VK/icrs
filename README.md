# Intelligent College Redressal System (ICRS)

ICRS is a full-stack grievance redressal platform for colleges. It provides role-based grievance handling for students, faculty, and admins, and adds an agentic AI workflow for sentiment analysis, contextual reasoning, classification, AI-generated resolution guidance, and guarded auto-resolution for routine cases.

The project consists of:
- a Spring Boot backend
- a React + Vite frontend
- PostgreSQL with `pgvector` for semantic retrieval
- a local Python sentiment service

## What It Does

- lets students submit, track, and discuss grievances
- supports faculty/admin review, assignment, comments, and status updates
- uses JWT-based authentication and role-based access control
- runs an agentic AI workflow using LangGraph
- performs ML-based sentiment analysis using a local Python service
- classifies grievances and generates AI-based resolution suggestions
- can auto-resolve narrow routine cases behind policy checks
- plans and uses extra context tools when needed during AI processing
- stores one embedding per grievance in PostgreSQL using `pgvector`
- retrieves similar grievances to support AI decisions
- uses a JSON-backed knowledge base for office/faculty routing guidance
- sends email notifications for account and grievance events

## Tech Stack

### Backend
- Java 25
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Spring AI
- LangChain4j
- LangGraph4j
- JWT (`jjwt`)

### AI / ML
- LangGraph-based agentic workflow orchestration
- LangChain4j for LLM integration and tool calling
- DeepSeek-compatible OpenAI API endpoint for chat generation
- AI-driven classification and resolution generation
- ML-based sentiment analysis through a local Python service
- Spring AI pgvector integration for semantic retrieval
- Open-source embedding model: `sentence-transformers/all-MiniLM-L6-v2`
- Python FastAPI sentiment service
- Hugging Face model: `siebert/sentiment-roberta-large-english`

### Frontend
- React 19
- Vite 7
- TypeScript
- Material UI
- React Router
- Axios

## Project Structure

```text
.
├── frontend/                         # React frontend
├── src/main/java/com/college/icrs/   # Spring Boot backend
├── src/main/resources/               # Flyway migrations, JSON knowledge bases
├── src/main/python/sentiment_service/# Local sentiment microservice
└── tools/grievance-vector-import/    # Manual pgvector import utility
```

## Prerequisites

Make sure these are available locally:

- Java 25
- Node.js 22.20+ and npm
- PostgreSQL 16+ with the `pgvector` extension installed
- Python 3.12+
- `uv` for managing the sentiment service environment

## Local Setup

### 1. Create the database

Create a PostgreSQL database named `icrs` and enable `pgvector`:

```sql
CREATE DATABASE icrs;
\c icrs
CREATE EXTENSION IF NOT EXISTS vector;
```

If `CREATE EXTENSION vector` fails, install the pgvector package for your PostgreSQL version first.

### 2. Create `application-secrets.properties`

The backend loads local secrets from `application-secrets.properties`, which is ignored by git.

Create it in the project root with values like these:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/icrs
spring.datasource.username=postgres
spring.datasource.password=postgres

jwt.secret.key=replace-with-a-long-random-secret

support.email=your-email@example.com
support.email.pass=your-email-app-password

ai.apikey=your-ai-api-key
ai.baseurl=https://api.deepseek.com
ai.modelname=deepseek-chat
ai.temperature=0.0
```

Notes:
- `ai.baseurl` should be the provider base URL. The application appends `/v1` if needed.
- `support.email` and `support.email.pass` are required only if you want real email delivery.

### 3. Install frontend dependencies

```bash
cd frontend
npm install
```

### 4. Install the sentiment service dependencies

```bash
cd src/main/python/sentiment_service
uv venv
uv pip install --python .venv/bin/python -r requirements.txt
```

You can run the sentiment service manually with:

```bash
.venv/bin/python -m uvicorn app:app --host 0.0.0.0 --port 8090
```

In normal local development, the Spring Boot app can auto-start this service if:
- `icrs.ai.sentiment.enabled=true`
- `icrs.ai.sentiment.auto-start=true`

### 5. Start the backend

From the project root:

```bash
./gradlew bootRun
```

On startup:
- Flyway applies database migrations
- the backend connects to PostgreSQL
- the sentiment service may auto-start if it is not already running

The backend runs on `http://localhost:8080` by default.

### 6. Start the frontend

In a separate terminal:

```bash
cd frontend
npm run dev
```

The frontend runs on `http://localhost:5173`.

## AI Workflow

The current AI layer is centered on a bounded agentic workflow rather than a simple one-shot prompt.

It uses:
- LangGraph for workflow orchestration
- LangChain4j for model calls, structured outputs, and tool calling
- a local ML sentiment service for emotional tone analysis
- AI-based classification and resolution generation
- `pgvector` retrieval to supply similar past grievance context

Current high-level flow:
1. a grievance is created in the backend
2. the backend stores one canonical vector document for the grievance
3. an async AI workflow starts
4. the workflow runs sentiment analysis through the Python ML service
5. the LangGraph workflow retrieves similar past grievances and plans extra context calls when needed
6. the AI classifies the grievance, generates a resolution or routing suggestion, and applies policy checks
7. routine low-risk cases may be auto-resolved; other cases remain in manual review with AI-generated guidance attached

RAG is one part of this flow. It supports the AI layer by supplying relevant prior grievance context, but the main behavior of the system comes from the agentic workflow, sentiment analysis, and AI-assisted decision pipeline.

## Useful Commands

### Run tests

```bash
./gradlew test
```

### Import grievances directly into the vector store

```bash
./tools/grievance-vector-import/import-grievances.sh path/to/grievances.json
```

Replace existing imported vector rows:

```bash
./tools/grievance-vector-import/import-grievances.sh path/to/grievances.json --replace
```

### Build the frontend

```bash
cd frontend
npm run build
```

## Configuration Highlights

Important application settings live in `application.properties`:

- `icrs.ai.auto-resolve-confidence-threshold=0.70`
- `icrs.ai.rag.top-k=3`
- `spring.ai.vectorstore.pgvector.table-name=vector_store`
- `spring.ai.vectorstore.pgvector.distance-type=cosine-distance`
- `spring.ai.vectorstore.pgvector.dimensions=384`
- `icrs.ai.sentiment.base-url=http://localhost:8090`

## Development Notes

- The project uses Flyway migrations. Do not rely on ad-hoc schema changes.
- `application-secrets.properties` is local-only and should not be committed.
- The sentiment service downloads model artifacts on first run, so the first startup can be slower.
- Imported vector-only grievances can participate in retrieval even when they do not exist in the relational `grievances` table.

## Current Status

The current implementation includes:
- grievance lifecycle management
- role-based dashboards
- email notifications
- LangGraph-orchestrated agentic AI workflow
- ML-based sentiment analysis
- AI-generated classification and resolution guidance
- guarded auto-resolution for routine cases
- pgvector-backed retrieval support
- knowledge-enriched routing guidance
- manual vector import tooling