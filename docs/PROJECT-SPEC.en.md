# Enterprise Operation Assistant
## Product & Technical Specification

> Version: 1.0  
> Status: Initial baseline  
> Primary language: English  
> Last updated: 2026-11-21

## 1. Project Overview

**Enterprise Operation Assistant (EOA)** is an enterprise-oriented AI agent platform that helps employees understand operational information, search internal knowledge, analyze incidents, and invoke approved business or IT tools through a governed conversational interface.

This portfolio-grade project is designed to run locally and demonstrate practical AI application engineering: model abstraction, agent orchestration, retrieval-augmented generation (RAG), tool integration, access control, auditability, and observability.

### 1.1 Problem Statement

Enterprise information and workflows are distributed across documents, databases, monitoring systems, ticketing platforms, and team-specific tools. Employees must switch between systems and manually correlate information. A general-purpose chatbot alone is insufficient because enterprise usage requires grounded answers from authorized information, controlled tool access, traceable actions, resilience to model failures and concurrency, and support for multiple model providers.

### 1.2 Product Vision

Provide a unified, permission-aware assistant through which employees can ask questions in natural language, retrieve relevant company knowledge, and execute approved operational workflows.

### 1.3 Target Users

- General employees: search knowledge and understand procedures.
- IT / operations engineers: investigate incidents and inspect operational data.
- Team administrators: manage knowledge sources, tools, and access policies.
- Platform administrators: configure model providers, security policies, and system health.

## 2. Product Goals and Non-Goals

### 2.1 Goals

1. Offer a unified chat interface for enterprise questions and operational assistance.
2. Support interchangeable LLM providers through a provider abstraction layer.
3. Retrieve answers from internal documents using RAG with authorization filtering.
4. Allow agents to call explicitly registered tools under server-side policy enforcement.
5. Record conversations, tool invocations, errors, and security-relevant events.
6. Run locally with free or mock model options for development and demonstration.
7. Provide automated tests and reproducible container-based setup.

### 2.2 Non-Goals for the Initial MVP

- Replacing enterprise identity providers or source-of-truth systems.
- Unrestricted access to employee computers, files, or corporate networks.
- Autonomous execution of destructive production operations.
- Training or fine-tuning foundation models.
- Prematurely splitting every module into a microservice.
- Claiming production-grade compliance certification.

## 3. Scope and Priorities

| Priority | Capability | Initial expectation |
|---|---|---|
| P0 | AI Gateway | Provider abstraction, configuration, timeout and error handling |
| P0 | Agent Runtime | Conversation context, tool selection, bounded execution |
| P0 | RAG | Document ingestion, chunking, embeddings, retrieval, citations |
| P0 | Identity & Access Control | Authentication, roles, resource-level authorization |
| P0 | Conversation Management | Conversation and message persistence |
| P1 | Tool Integration | Read-only operational tools and connector interface |
| P1 | MCP Integration | Evaluate and integrate Model Context Protocol servers |
| P1 | Log / Incident Analysis | Summarize logs and correlate supplied operational context |
| P1 | Audit & Monitoring | Tool audit trail, metrics, structured logs |
| P2 | Administration Console | Manage providers, tools, knowledge sources, and policies |

P0 defines the first demonstrable MVP. P1 and P2 follow only after P0 acceptance criteria are met.

## 4. Core Functional Requirements

### 4.1 AI Gateway and Model Providers

- Define a common provider interface so the agent runtime does not depend on a specific model vendor.
- Configure provider, model name, endpoint, credentials, temperature, token limits, and timeout.
- Start with a mock provider; add a local Ollama-compatible provider for experimentation.
- Support provider health checks and clear error reporting.
- Apply bounded retries only to retryable failures; avoid duplicate tool execution.
- Keep secrets outside source control and redact them from logs.

**Initial provider strategy:** implement a deterministic mock provider for tests and a local Ollama-compatible provider. Cloud provider adapters are optional and configurable.

### 4.2 Agent Runtime

- Accept a user message and authenticated user context.
- Determine whether the request requires knowledge retrieval, a tool, or a direct model response.
- Provide only tools the current user is authorized to invoke.
- Validate tool arguments on the server before execution.
- Enforce maximum tool-call count, timeout, payload size, and iteration count.
- Return clear errors when a tool fails or the model cannot complete the request.
- Persist messages and execution metadata.

The model may propose an action, but the backend is responsible for authorization and execution policy.

### 4.3 Retrieval-Augmented Generation (RAG)

- Ingest supported text-based documents with source metadata.
- Extract and normalize text, then split it into configurable chunks.
- Generate embeddings through a replaceable embedding provider.
- Store vectors and metadata in PostgreSQL with pgvector.
- Retrieve relevant chunks and include source references in answers.
- Apply document-level authorization filters **before** retrieved content is supplied to the model.
- Support re-indexing and deletion of source documents.
- Track ingestion status and failures.

The MVP uses a small, controlled set of sample documents; format coverage can expand later.

### 4.4 Tool Integration

- Register each tool with a stable name, description, JSON input schema, risk classification, and required permission.
- Begin with read-only tools, such as service health lookup, deployment history lookup, and mock ticket search.
- Validate inputs and outputs at the tool boundary.
- Apply allowlists, timeouts, rate limits, and audit logging.
- Require explicit human approval for future high-impact write operations.
- Never expose arbitrary shell execution or unrestricted filesystem access in the MVP.

### 4.5 Identity and Authorization

- Authenticate users through Spring Security.
- Use role-based access control (RBAC) for baseline permissions.
- Enforce resource-level checks for conversations, documents, and tools.
- Derive identity and permissions from trusted backend context, never from model-generated text.
- Ensure the model cannot grant itself or a user additional privileges.

Suggested initial roles: **USER**, **OPERATOR**, **ADMIN**.

### 4.6 Conversation Management

- Create, list, retrieve, and archive conversations.
- Persist user and assistant messages with timestamps.
- Associate each conversation with its owner and access policy.
- Prevent users from reading other users' conversations unless explicitly authorized.

### 4.7 Audit and Observability

- Record user identity, request correlation ID, selected provider/model, tool name, execution outcome, duration, and relevant policy decisions.
- Do not log raw credentials or unnecessarily sensitive document contents.
- Expose application health and readiness endpoints.
- Use structured logs and Micrometer metrics.
- Make failures diagnosable without exposing sensitive data.

## 5. High-Level Architecture

The MVP uses a **modular monolith** to keep deployment and local development simple while maintaining clear module boundaries.

~~~mermaid
flowchart TD
    U[Employee / Operator] --> UI[Web UI]
    UI --> API[Spring Boot API]
    API --> SEC[Authentication & Authorization]
    API --> CHAT[Conversation Service]
    CHAT --> AGENT[Agent Runtime]
    AGENT --> GW[AI Gateway]
    GW --> LLM[Mock / Local / Cloud LLM]
    AGENT --> RAG[RAG Service]
    RAG --> PG[(PostgreSQL + pgvector)]
    AGENT --> TOOLS[Tool Registry & Policy]
    TOOLS --> SYS[Approved Tools / MCP Servers]
    API --> AUDIT[Audit & Observability]
    CHAT --> PG
    AUDIT --> OBS[Logs & Metrics]
~~~

### 5.1 Architectural Principles

- Keep domain modules separated behind clear interfaces.
- Keep provider-specific code inside provider adapters.
- Keep authorization and policy enforcement in trusted backend services.
- Treat model output, retrieved content, and tool input as untrusted.
- Use asynchronous processing only where it provides a clear benefit.
- Avoid premature microservice decomposition.

## 6. Proposed Technology Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend | Java 17+ / Spring Boot 3.x | Main application |
| AI Framework | Spring AI | Model and vector-store integration |
| Frontend | React + TypeScript | Chat and administration UI |
| Database | PostgreSQL + pgvector | Application data and vector search |
| Cache | Redis (optional) | Add when a concrete need exists |
| Security | Spring Security | Authentication and authorization |
| Local model | Ollama-compatible runtime | Optional local inference |
| Testing | JUnit 5, Mockito, Testcontainers | Unit and integration tests |
| Packaging | Docker / Docker Compose | Reproducible local environment |
| CI | GitHub Actions | Build, test, and quality checks |
| Observability | Micrometer, structured logging | Metrics and diagnostics |
| Optional AI service | Python + FastAPI | Only for capabilities unsuitable for the Java service |

Use a modular monolith for the MVP. Python is optional, not a mandatory second backend.

## 7. Security Requirements

1. Enforce authentication and authorization on every protected backend endpoint.
2. Apply least-privilege access to documents and tools.
3. Filter RAG results by user permissions before model context construction.
4. Treat prompts, documents, model responses, and tool arguments as untrusted input.
5. Validate and constrain tool arguments using server-side schemas.
6. Do not permit arbitrary shell commands, unrestricted network access, or unrestricted local file access.
7. Keep secrets in environment variables or a secret manager; never commit real credentials.
8. Redact secrets and sensitive data from logs.
9. Apply request size limits, rate limits, and execution timeouts.
10. Record security-relevant events and tool executions for audit.
11. Require human confirmation before future high-impact or irreversible actions.
12. Do not rely on the LLM as the security boundary or source of authorization truth.

## 8. Initial API Design

The API is illustrative and may be refined during implementation.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/v1/auth/login | Authenticate a user (if local auth is enabled) |
| GET | /api/v1/conversations | List accessible conversations |
| POST | /api/v1/conversations | Create a conversation |
| GET | /api/v1/conversations/{id} | Retrieve a conversation |
| POST | /api/v1/conversations/{id}/messages | Send a message and receive an agent response |
| GET | /api/v1/knowledge/sources | List accessible knowledge sources |
| POST | /api/v1/knowledge/sources | Register or ingest a knowledge source |
| GET | /api/v1/tools | List tools available to the current user |
| GET | /actuator/health | Health check |

All endpoints must validate ownership and permissions. Authentication details depend on the chosen local identity implementation.

## 9. Initial Data Model

Core entities:

- **User:** identity, status, and assigned roles.
- **Role / Permission:** authorization definitions.
- **Conversation:** owner, title, status, timestamps.
- **Message:** conversation, role, content, timestamps, and metadata.
- **KnowledgeSource:** source identity, owner/scope, ingestion status.
- **DocumentChunk:** source reference, chunk text, metadata, embedding.
- **ToolDefinition:** tool schema, risk level, required permission, enabled status.
- **ToolExecution:** caller, tool, arguments summary, outcome, duration, correlation ID.
- **AuditEvent:** actor, action, target, timestamp, and outcome.

The schema evolves through versioned database migrations.

## 10. Non-Functional Requirements

| Area | Requirement |
|---|---|
| Reliability | Handle provider/tool errors without crashing the application |
| Performance | Set explicit timeouts and bounded resource limits |
| Scalability | Support concurrent requests with controlled model/tool concurrency |
| Security | Enforce least privilege and server-side authorization |
| Maintainability | Modular code, documented interfaces, automated tests |
| Portability | Local startup through documented Docker Compose configuration |
| Observability | Correlation IDs, structured logs, health checks, key metrics |
| Cost control | Configurable model selection, token limits, mock/local options |

Numerical performance targets will be set after a baseline is measured on the development environment.

## 11. Testing Strategy

- **Unit tests:** provider routing, authorization policies, tool validation, chunking, and service logic.
- **Integration tests:** REST APIs, persistence, vector retrieval, and provider adapters.
- **Security tests:** cross-user conversation access, unauthorized document retrieval, and forbidden tool invocation.
- **Failure tests:** provider timeout, invalid model output, tool failure, database unavailability.
- **End-to-end tests:** a user asks a question, retrieves authorized knowledge, receives cited output, and invokes an allowed read-only tool.
- **Deterministic tests:** use mock model/provider responses so CI does not require paid API access.

## 12. MVP Acceptance Criteria

The MVP is complete when:

1. A developer can start the application using documented local setup instructions.
2. An authenticated user can create and continue a conversation.
3. The application can respond through a mock or local model provider.
4. The provider can be changed through configuration without changing agent business logic.
5. A sample document can be ingested and searched through RAG.
6. Answers can include source references for retrieved knowledge.
7. A user cannot retrieve documents or conversations outside their permissions.
8. The agent can invoke at least one authorized read-only tool.
9. Unauthorized tool calls are rejected by the backend.
10. Tool executions and key failures are auditable.
11. Automated tests pass in CI without a paid LLM subscription.

## 13. Development Roadmap

| Phase | Deliverable |
|---|---|
| 0. Specification | Confirm scope, architecture, conventions, and acceptance criteria |
| 1. Foundation | Repository structure, Spring Boot app, configuration, Docker Compose, CI |
| 2. Identity & Chat | Authentication, RBAC baseline, conversation and message APIs |
| 3. AI Gateway | Provider interface, mock provider, local provider adapter |
| 4. Agent Runtime | Prompt/context assembly, bounded orchestration, error handling |
| 5. RAG | Document ingestion, embeddings, pgvector retrieval, permission filtering |
| 6. Tools | Tool registry, schemas, policy checks, read-only sample tools |
| 7. Observability | Audit events, metrics, structured logs, health checks |
| 8. Frontend | Chat interface and essential administration views |
| 9. Hardening | Security, integration, failure, and end-to-end tests; documentation |

Each phase must have a reviewable deliverable and passing tests before the next phase expands scope.

## 14. Target Repository Structure

~~~text
enterprise-operation-assistant/
├── backend/
│   └── (Spring Boot application)
├── frontend/
│   └── (React + TypeScript application)
├── docs/
│   ├── PROJECT-SPEC.en.md
│   └── PROJECT-SPEC.zh-TW.md
├── infra/
│   └── (Docker Compose and local infrastructure)
├── scripts/
├── .github/
│   └── workflows/
├── .gitignore
└── README.md
~~~

This is a target structure, not a requirement to create every directory immediately.

## 15. AI-Assisted Development Workflow

AI coding agents are development assistants, not autonomous project owners. Use this workflow for each implementation task:

1. Ask the agent to inspect the current repository and relevant specifications.
2. Ask it to clarify requirements, assumptions, edge cases, and acceptance criteria.
3. Request a concise implementation plan and list of files to change.
4. Review the plan before allowing code changes.
5. Ask the agent to implement one bounded task at a time.
6. Require tests and explanations of design decisions and changed files.
7. Review the diff for correctness, security, unnecessary refactoring, and scope creep.
8. Run tests and inspect logs locally.
9. Commit only after changes and tests are reviewed.
10. Update documentation when behavior or architecture changes.

Never provide production secrets to an AI agent. Do not allow an agent to bypass tests, access unrelated files, or perform irreversible actions without explicit approval.

## 16. Open Decisions

Confirm these during implementation rather than guessing:

- Local authentication approach and whether an external identity provider will be demonstrated.
- Exact LLM providers and embedding models to support first.
- Supported document formats for the first ingestion milestone.
- Whether Redis is necessary after measuring actual needs.
- Frontend authentication/session strategy.
- Whether MCP belongs in the first MVP or as a P1 extension.
- Initial performance and concurrency targets.

## 17. Project Success Criteria

The project succeeds as a portfolio demonstration when it proves the ability to design and implement a secure, observable, model-agnostic enterprise AI application—not merely a chatbot wrapper. Evidence should include working code, meaningful tests, clear architecture, permission-aware RAG, governed tool execution, and reproducible local deployment.
