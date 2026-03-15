# ICRS Current Architecture And Orchestration

This document describes the current end-to-end architecture of the ICRS project as implemented in the codebase.

It covers:
- frontend application flow
- authentication and security flow
- grievance submission and lifecycle flow
- agentic AI orchestration flow
- RAG and pgvector flow
- sentiment microservice flow
- persistence layer and supporting services

## 1. System Overview

```mermaid

flowchart TD
    A["Frontend Entry
frontend/src/main.tsx
createRoot()
renders App"] --> B["Frontend Router
frontend/src/App.tsx
App()
Routes + ProtectedRoute"]

    B --> C["Auth UI
frontend/src/components/auth/*
LoginPage / CreateAccount / VerifyAccount"]
    B --> D["Student UI
frontend/src/components/student/*
StudentDashboard / SubmitGrievance / TrackGrievance"]
    B --> E["Faculty UI
frontend/src/components/faculty/FacultyDashboard.tsx
FacultyDashboard()"]

    C --> F["Frontend Auth Service
frontend/src/services/authService.ts
register() / login() / verify() / resend() / getCurrentUser()"]
    D --> G["Frontend Grievance Service
frontend/src/services/grievanceService.ts
submit() / getMyGrievances() / getById() / getAll() / updateStatus() / getComments() / addComment() / getCategories()"]
    E --> G

    F --> H["Axios API Client
frontend/src/services/apiClient.ts
axios.create()
request interceptor
response interceptor"]
    G --> H

    H --> I["Spring Boot App
src/main/java/com/college/icrs/IcrsApplication.java
main()"]

    I --> J["Security Layer
src/main/java/com/college/icrs/config/SecurityConfig.java
securityFilterChain()

src/main/java/com/college/icrs/config/JWTAuthenticationFilter.java
doFilterInternal()"]

    J --> K["Auth API
src/main/java/com/college/icrs/controller/AuthenticationController.java
signup() / login() / verifyUser() / resendVerificationCode()"]
    J --> L["Grievance API
src/main/java/com/college/icrs/controller/GrievanceController.java
createGrievance() / getMyGrievances() / getAllGrievances() / updateStatus() / addComment() / getComments()"]
    J --> M["Category API
src/main/java/com/college/icrs/controller/CategoryController.java"]
    J --> N["User API
src/main/java/com/college/icrs/controller/UserController.java"]

    K --> O["Authentication Service
src/main/java/com/college/icrs/service/AuthenticationService.java
signup() / login() / verifyUser() / resendVerificationCode()"]
    O --> P["JWT Service
src/main/java/com/college/icrs/service/JwtService.java
generateToken() / extractUsername() / isTokenValid()"]
    O --> Q["Email Service
src/main/java/com/college/icrs/service/EmailService.java
sendVerificationEmail() / sendAsync()"]

    L --> R["Grievance Service
src/main/java/com/college/icrs/service/GrievanceService.java
createGrievance() / updateGrievance() / updateGrievanceStatus() / applyAiDecisionMetadata() / updateAiRecommendation() / markResolvedByAi() / addComment()"]
    R --> S["Embedding Service
src/main/java/com/college/icrs/rag/EmbeddingService.java
indexGrievance() / removeGrievance() / buildEmbeddingText()"]
    L --> T["Agentic AI Entry
src/main/java/com/college/icrs/ai/service/AgenticAiService.java
processNewGrievanceAsync() / processNewGrievance()"]

    T --> U["LangGraph Workflow
src/main/java/com/college/icrs/ai/agent/GrievanceWorkflowGraph.java
process() / compileGraph()"]
    U --> V["AI Tools Layer
src/main/java/com/college/icrs/ai/agent/GrievanceAgentTools.java
selectContextTools() / classify() / resolve() / finalizeDecision() / buildPolicyContext() / buildCommentContext() / buildStatusHistoryContext()"]
    V --> W["RAG Service
src/main/java/com/college/icrs/rag/RagService.java
retrieveSimilar() / buildContextSection()"]
    V --> X["Sentiment Service Client
src/main/java/com/college/icrs/ai/service/SentimentAnalysisService.java
analyze()"]

    X --> Y["Python Sentiment Microservice
src/main/python/sentiment_service/app.py
health() / analyze()"]

    S --> Z["Vector Document Builder
src/main/java/com/college/icrs/rag/GrievanceVectorDocumentFactory.java
fromGrievance() / buildContent()"]
    W --> AA["pgvector / Vector Store
PostgreSQL + vector_store
Spring AI VectorStore"]
    R --> AB["Relational Persistence
PostgreSQL tables:
users / grievances / comments / status_history / categories / subcategories / attachments"]
    O --> AB
    W --> AB
    Q --> AC["SMTP / Mail Infrastructure"]
```

## 2. Startup And Bootstrap Flow

```mermaid
flowchart TD
    A["Application Bootstrap
src/main/java/com/college/icrs/IcrsApplication.java
main()"] --> B["Sentiment Bootstrap
src/main/java/com/college/icrs/bootstrap/SentimentServiceLauncher.java
startIfConfigured()"]
    A --> C["Spring Context + Config
src/main/java/com/college/icrs/config/ApplicationConfig.java
src/main/java/com/college/icrs/config/IcrsProperties.java"]
    A --> D["Security Setup
src/main/java/com/college/icrs/config/SecurityConfig.java
securityFilterChain()"]
    A --> E["Seed Catalog
src/main/java/com/college/icrs/config/CollegeSeedDataInitializer.java"]
    A --> F["Logging Init
src/main/java/com/college/icrs/logging/LogFolderInitializer.java"]

    B --> G["Python Service Process
src/main/python/sentiment_service/app.py
/health /analyze"]
    C --> H["Database / Flyway
src/main/resources/db/migration/*
V1 / V2 / V3__grievance_embeddings.sql"]
```

## 3. Student Submission To Final Outcome

```mermaid
flowchart TD
    A["Student submits grievance
frontend/src/components/student/SubmitGrievance.tsx"] --> B["frontend/src/services/grievanceService.ts
submit()"]
    B --> C["frontend/src/services/apiClient.ts
axios POST /grievances"]
    C --> D["src/main/java/com/college/icrs/controller/GrievanceController.java
createGrievance()"]

    D --> E["src/main/java/com/college/icrs/utils/GrievanceMapper.java
toEntity()"]
    D --> F["src/main/java/com/college/icrs/service/CategoryCatalogService.java
category/subcategory lookup"]
    D --> G["src/main/java/com/college/icrs/service/GrievanceService.java
createGrievance()"]

    G --> H["Set ownership and initial status
GrievanceService.createGrievance()
initializeAiFieldsForNewGrievance()"]
    H --> I["Auto-assignment
GrievanceService.createGrievance()
assign default faculty from category/subcategory"]
    I --> J["Persist grievance row
GrievanceRepository.save()
src/main/java/com/college/icrs/repository/GrievanceRepository.java"]
    J --> K["Index one-vector-per-grievance
src/main/java/com/college/icrs/rag/EmbeddingService.java
indexGrievance()"]
    K --> L["Build canonical grievance text
src/main/java/com/college/icrs/rag/GrievanceVectorDocumentFactory.java
fromGrievance() / buildContent()"]
    L --> M["Write embedding document
Spring AI VectorStore.add()
vector_store table"]
    J --> N["Send submission email
GrievanceService.trySendSubmissionEmail()
EmailService.sendAsync()"]

    G --> O["Return created grievance DTO
GrievanceController.createGrievance()"]
    D --> P["Async AI dispatch
src/main/java/com/college/icrs/ai/service/AgenticAiService.java
processNewGrievanceAsync()"]

    P --> Q["Agentic graph run
src/main/java/com/college/icrs/ai/agent/GrievanceWorkflowGraph.java
process()"]
    Q --> R["Sentiment node
analyzeSentiment()
SentimentAnalysisService.analyze()"]
    R --> S["RAG node
retrieveRagContext()
RagService.retrieveSimilar()"]
    S --> T["Iterative context planner
planContextTools()
GrievanceAgentTools.selectContextTools()"]
    T --> U["Optional policy tool
loadPolicyContext()
buildPolicyContext()"]
    T --> V["Optional comments tool
loadCommentContext()
buildCommentContext()"]
    T --> W["Optional status history tool
loadStatusHistoryContext()
buildStatusHistoryContext()"]
    U --> T
    V --> T
    W --> T

    T --> X["Classification node
classifyGrievance()
GrievanceAgentTools.classify()"]
    X --> Y["Persist AI metadata
persistAiMetadata()
GrievanceService.applyAiDecisionMetadata()"]
    Y --> Z["Resolution node
resolveGrievance()
GrievanceAgentTools.resolve()"]
    Z --> AA["Finalize node
finalizeDecision()
GrievanceAgentTools.finalizeDecision()"]

    AA --> AB["Manual review path
GrievanceService.updateAiRecommendation()
status stays in faculty workflow"]
    AA --> AC["Auto-resolve path
GrievanceService.markResolvedByAi()
+ GrievanceService.addSystemComment()"]

    AB --> AD["Faculty dashboard / tracking UI
frontend/src/components/faculty/FacultyDashboard.tsx
frontend/src/components/student/TrackGrievance.tsx"]
    AC --> AD
```

## 4. Agentic AI Orchestration Details

```mermaid
flowchart TD
    A["AI Entry Service
src/main/java/com/college/icrs/ai/service/AgenticAiService.java
processNewGrievance()"] --> B["Graph Orchestrator
src/main/java/com/college/icrs/ai/agent/GrievanceWorkflowGraph.java
compileGraph() / process()"]

    B --> C["Graph State
src/main/java/com/college/icrs/ai/agent/GrievanceAgentState.java
grievanceId()
sentiment()
contextToolSelection()
plannerIteration()
plannerTrace()
routeTrace()"]

    B --> D["Node: loadGrievance()
GrievanceWorkflowGraph.java"]
    B --> E["Node: analyzeSentiment()
GrievanceWorkflowGraph.java
GrievanceAgentTools.analyzeSentiment()"]
    B --> F["Node: retrieveRagContext()
GrievanceWorkflowGraph.java
GrievanceAgentTools.retrieveSimilar()
GrievanceAgentTools.buildContextSection()"]
    B --> G["Node: planContextTools()
GrievanceWorkflowGraph.java
GrievanceAgentTools.selectContextTools()"]

    G --> H["Conditional route
routeAfterPlanner()
nextContextRoute()"]
    H --> I["Tool: buildPolicyContext()
src/main/java/com/college/icrs/ai/agent/GrievanceAgentTools.java"]
    H --> J["Tool: buildCommentContext()
src/main/java/com/college/icrs/ai/agent/GrievanceAgentTools.java"]
    H --> K["Tool: buildStatusHistoryContext()
src/main/java/com/college/icrs/ai/agent/GrievanceAgentTools.java"]
    H --> L["Route to classify"]

    I --> G
    J --> G
    K --> G

    L --> M["Node: classifyGrievance()
GrievanceAgentTools.classify()"]
    M --> N["Node: persistAiMetadata()
GrievanceService.applyAiDecisionMetadata()"]
    N --> O["Node: resolveGrievance()
GrievanceAgentTools.resolve()"]
    O --> P["Node: finalizeDecision()
GrievanceAgentTools.finalizeDecision()"]

    P --> Q["Manual-review side effect
GrievanceService.updateAiRecommendation()"]
    P --> R["Auto-resolve side effects
GrievanceService.markResolvedByAi()
GrievanceService.addSystemComment()"]

    G --> S["Telemetry logs
ai.context-planner.decision
ai.context-router.decision
ai.context-tool.fetched
ai.context-telemetry.summary"]
```

## 5. RAG And pgvector Architecture

```mermaid
flowchart TD
    A["Grievance write/update/delete
src/main/java/com/college/icrs/service/GrievanceService.java
createGrievance() / updateGrievance() / deleteGrievance()"] --> B["Embedding Service
src/main/java/com/college/icrs/rag/EmbeddingService.java
indexGrievance() / removeGrievance()"]

    B --> C["Document Builder
src/main/java/com/college/icrs/rag/GrievanceVectorDocumentFactory.java
fromGrievance()
buildContent()"]

    C --> D["Canonical text sections
Title
Description
Category
Subcategory
Registration Number"]

    D --> E["Spring AI VectorStore
org.springframework.ai.vectorstore.VectorStore
add() / delete() / similaritySearch()"]
    E --> F["PostgreSQL pgvector
vector_store table
V3__grievance_embeddings.sql"]

    G["Query grievance
src/main/java/com/college/icrs/rag/RagService.java
retrieveSimilar()"] --> H["EmbeddingService.buildEmbeddingText()"]
    H --> E
    E --> I["RagService.map()
resolve metadata / grievance row"]
    I --> J["RagService.buildContextSection()
reference cases with similarity, priority, sentiment, priorResolution"]
    J --> K["Agent prompt context
GrievanceAgentTools.classify()
GrievanceAgentTools.resolve()"]
```

## 6. Authentication And Security Flow

```mermaid
flowchart TD
    A["Frontend login/signup UI
frontend/src/components/auth/*"] --> B["frontend/src/services/authService.ts
register() / login() / verify() / resend()"]
    B --> C["frontend/src/services/apiClient.ts
Authorization header interceptor"]
    C --> D["src/main/java/com/college/icrs/config/SecurityConfig.java
securityFilterChain()"]

    D --> E["Public auth endpoints
src/main/java/com/college/icrs/controller/AuthenticationController.java
signup() / login() / verifyUser() / resendVerificationCode()"]
    D --> F["Protected business endpoints
/users/**
/grievances/**"]

    F --> G["src/main/java/com/college/icrs/config/JWTAuthenticationFilter.java
shouldNotFilter()
doFilterInternal()"]
    G --> H["src/main/java/com/college/icrs/service/JwtService.java
extractUsername()
isTokenValid()"]
    H --> I["UserDetailsService + SecurityContext"]

    E --> J["src/main/java/com/college/icrs/service/AuthenticationService.java
signup() / login() / verifyUser() / resendVerificationCode()"]
    J --> K["UserRepository + users table"]
    J --> L["PasswordEncoder"]
    J --> M["EmailService.sendVerificationEmail()"]
    J --> N["JwtService.generateToken()"]
```

## 7. Sentiment Service Flow

```mermaid
flowchart TD
    A["Spring Boot startup
src/main/java/com/college/icrs/IcrsApplication.java"] --> B["Sentiment bootstrap
src/main/java/com/college/icrs/bootstrap/SentimentServiceLauncher.java
startIfConfigured()"]
    B --> C["Health check
SentimentServiceLauncher.isSentimentServiceHealthy()"]
    C --> D["Start uvicorn process if needed
SentimentServiceLauncher.buildStartCommand()"]
    D --> E["Python FastAPI service
src/main/python/sentiment_service/app.py
health() / analyze()"]

    F["AI graph sentiment node
GrievanceWorkflowGraph.analyzeSentiment()"] --> G["HTTP client
src/main/java/com/college/icrs/ai/service/SentimentAnalysisService.java
analyze()"]
    G --> E
    E --> H["transformers.pipeline()
MODEL_NAME = siebert/sentiment-roberta-large-english"]
    G --> I["SentimentDecision
sentiment + confidence + modelName"]
    I --> J["Graph state + grievance AI metadata"]
```

## 8. Notes About Current Behavior

- The frontend is a Vite + React SPA and talks directly to the Spring Boot backend over HTTP using `axios`.
- Authentication is JWT-based and stateless.
- Each grievance is embedded as exactly one vector document in pgvector.
- The AI workflow is now graph-based and agentic in a bounded sense:
  it uses iterative model-driven tool selection before classification and resolution.
- Planner telemetry is currently logged, not persisted in a database table.
- The sentiment service is a separate Python FastAPI process and can be auto-started by the Java application.
