# Enterprise Operation Assistant
## Product & Technical Specification

> Version: 1.2  
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
8. Establish a secure architecture that can later connect each employee's approved local workspace through an optional Local Agent.

### 2.2 Non-Goals for the Initial MVP

- Replacing enterprise identity providers or source-of-truth systems.
- Unrestricted access to employee computers, files, or corporate networks.
- A Local Agent or full desktop-control capability in the initial MVP.
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
| P1 | Local Agent Foundation | Device registration, secure pairing, heartbeat, and revocation design |
| P2 | Local File Operations | User-approved folder access and allowlisted file tools through Local Agent |
| P2 | Administration Console | Manage providers, tools, knowledge sources, devices, and policies |

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

The MVP uses a small, controlled set of sample documents; format coverage can expand later. In future Local Agent workflows, local files must be permission-checked on the device, and only the minimum necessary authorized content or retrieval snippets may be sent to the central platform.

### 4.4 Tool Integration

- Register each tool with a stable name, description, JSON input schema, risk classification, and required permission.
- Begin with read-only tools, such as service health lookup, deployment history lookup, and mock ticket search.
- Validate inputs and outputs at the tool boundary.
- Apply allowlists, timeouts, rate limits, and audit logging.
- Require explicit human approval for future high-impact write operations.
- Never expose arbitrary shell execution or unrestricted filesystem access in the MVP.

### 4.5 Local Agent and Employee Device Access

The product's long-term architecture supports an optional Local Agent installed on an employee's computer. The Local Agent performs approved local operations on that device; the central platform does not directly browse or mount employee file systems.

- Register each installation as a device associated with an authenticated user and organization.
- Pair devices through an explicit user-approved flow; support device revocation and credential rotation.
- Require the user to explicitly select and authorize specific local folders. Access is limited to those folder scopes and can be revoked at any time.
- Communicate outbound to the central platform over TLS using short-lived access tokens and/or device credentials. Define heartbeat, reconnect, expiry, and offline behavior.
- Expose only explicitly registered Local Tools, initially limited to operations such as list, read, and search. Add create, update, and move only with narrowly scoped permissions and confirmation where appropriate.
- Treat delete, overwrite, bulk changes, and other high-impact actions as high risk; require a clear preview and explicit human confirmation. Do not provide arbitrary shell execution or unrestricted desktop control.
- Enforce user, organization, and device isolation. Audit the actor, device, folder scope, requested operation, decision, and outcome.
- Minimize data transfer. Local file contents are not uploaded wholesale by default; transmit only the authorized content needed for the requested task. Apply the same RAG authorization filtering before content reaches a model.
- Enterprise administrators may disable Local Agent, restrict permitted tools, or define allowed folder policies.

Local Agent is a post-MVP capability. The first MVP supports browser-based document upload and does not require installing software on employee devices.

### 4.6 Identity and Authorization

- Authenticate users through Spring Security.
- Use role-based access control (RBAC) for baseline permissions.
- Enforce resource-level checks for conversations, documents, and tools.
- Derive identity and permissions from trusted backend context, never from model-generated text.
- Ensure the model cannot grant itself or a user additional privileges.

Suggested initial roles: **USER**, **OPERATOR**, **ADMIN**.

### 4.7 Conversation Management

- Create, list, retrieve, and archive conversations.
- Persist user and assistant messages with timestamps.
- Associate each conversation with its owner and access policy.
- Prevent users from reading other users' conversations unless explicitly authorized.

### 4.8 Audit and Observability

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
    UI -. future device pairing .-> LA[Employee Local Agent]
    LA -->|TLS / scoped device identity| LTOOLS[Local Tool Registry]
    LTOOLS --> FS[User-approved folders]
    LA -. minimal authorized results .-> API
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
| Backend | JDK 21 LTS / Spring Boot 4.2.x | Main application; use the latest stable patch in the selected line |
| AI Framework | Spring AI 2.1.x | Stable line compatible with Spring Boot 4.2.x; model and vector-store integration |
| Frontend | React + TypeScript | Chat and administration UI |
| Database | PostgreSQL + pgvector | Application data and vector search |
| Cache | Redis (optional) | Add when a concrete need exists |
| Security | Spring Security | Authentication and authorization |
| Local model | Ollama-compatible runtime | Optional local inference |
| Testing | JUnit Jupiter (version managed by Spring Boot), Mockito, Testcontainers | Unit and integration tests |
| Packaging | Docker / Docker Compose | Reproducible local environment |
| CI | GitHub Actions | Build, test, and quality checks |
| Observability | Micrometer, structured logging | Metrics and diagnostics |
| Optional AI service | Python + FastAPI | Only for capabilities unsuitable for the Java service |
| Future Local Agent | Java (initial preference) | Lightweight per-device process; separate deliverable after MVP |

Use a modular monolith for the MVP. Python is optional, not a mandatory second backend. Pin JDK 21 LTS and stable Spring Boot 4.2.x / Spring AI 2.1.x releases; use the latest stable patch versions managed by their BOMs, and avoid milestone, release-candidate, and snapshot builds. Recheck official compatibility and support status when starting each implementation phase.

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
13. A Local Agent must operate only within user-approved folder scopes and registered tools; it must not run with administrator privileges by default.
14. Authenticate and authorize every device connection; support device revocation, short-lived credentials, TLS, and audit logging.
15. Enforce tenant, user, and device isolation. Minimize local data transfer and never upload entire local folders by default.
16. Require explicit confirmation for destructive or high-impact local operations. Enterprise policy must be able to disable Local Agent or restrict its tools and folder scopes.

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
| POST | /api/v1/devices/pairing | Begin an authenticated Local Agent pairing flow (future) |
| POST | /api/v1/devices/{id}/heartbeat | Receive device heartbeat (future) |
| GET | /api/v1/devices | List the current user's registered devices (future) |
| DELETE | /api/v1/devices/{id} | Revoke a registered device (future) |
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
- **Device:** user / organization association, device identity, status, last heartbeat, and credential metadata.
- **DeviceFolderGrant:** device, approved folder scope, permission level, and revocation state.
- **LocalToolDefinition:** allowlisted local operation, input schema, risk level, and required permission.

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
| Device security | Isolated device identities, scoped folder grants, revocation, and auditable local operations |
| Data minimization | Local content is transferred only when required and authorized |

Numerical performance targets will be set after a baseline is measured on the development environment.

## 11. Testing Strategy

- **Unit tests:** provider routing, authorization policies, tool validation, chunking, and service logic.
- **Integration tests:** REST APIs, persistence, vector retrieval, and provider adapters.
- **Security tests:** cross-user conversation access, unauthorized document retrieval, and forbidden tool invocation.
- **Failure tests:** provider timeout, invalid model output, tool failure, database unavailability.
- **End-to-end tests:** a user asks a question, retrieves authorized knowledge, receives cited output, and invokes an allowed read-only tool.
- **Deterministic tests:** use mock model/provider responses so CI does not require paid API access.
- **Future Local Agent tests:** verify device pairing/revocation, folder-scope enforcement, cross-user/device isolation, offline behavior, confirmation of high-risk actions, and audit events.

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

Local Agent installation, device pairing, and direct local-file operations are explicitly outside MVP acceptance criteria.

## 13. Development Roadmap

| Phase | Deliverable |
|---|---|
| 0. Specification | Confirm scope, architecture, conventions, and acceptance criteria |
| 1. Foundation | Repository structure, Spring Boot app, configuration, reproducible Docker Compose local startup, required pull-request CI; no automatic deployment |
| 2. Identity & Chat | Authentication, RBAC baseline, conversation and message APIs |
| 3. AI Gateway | Provider interface, mock provider, local provider adapter |
| 4. Agent Runtime | Prompt/context assembly, bounded orchestration, error handling |
| 5. RAG | Document ingestion, embeddings, pgvector retrieval, permission filtering |
| 6. Tools | Tool registry, schemas, policy checks, read-only sample tools |
| 7. Observability | Audit events, metrics, structured logs, health checks |
| 8. Frontend | Chat interface and essential administration views |
| 9. Hardening | Security, integration, failure, and end-to-end tests; documentation |
| 10. Local Agent Foundation (post-MVP) | Device identity, pairing, secure outbound connection, heartbeat, and revocation |
| 11. Local File Tools (post-MVP) | User-approved folder scopes, read/search tools, then carefully gated write operations |

Each phase must have a reviewable deliverable and passing tests before the next phase expands scope.

## 14. Target Repository Structure

~~~text
enterprise-operation-assistant/
├── AGENTS.md
├── backend/
│   └── (Spring Boot application)
├── frontend/
│   └── (React + TypeScript application)
├── local-agent/                  # Post-MVP; per-device local process
│   └── (Java application)
├── docs/
│   ├── DEVELOPMENT-WORKFLOW.md
│   ├── PROJECT-SPEC.en.md
│   ├── PROJECT-SPEC.zh-TW.md
│   ├── agent-roles.md
│   └── adr/                        # Architecture Decision Records
├── evals/                          # Versioned datasets for model/agent behavior
├── infra/
│   └── (Docker Compose and local infrastructure)
├── scripts/
├── .github/
│   └── workflows/
├── .gitignore
└── README.md
~~~

This is a target structure, not a requirement to create every directory immediately.

## 15. Agent-Driven Development Workflow

This section is a non-normative summary. The user owns product direction, priorities, acceptance, and merge approval. Codex is the default Supervisor and Implementer; Claude Code is the default read-only Reviewer. Swap roles when Claude Code implements a task.

For non-trivial work, agree on the specification, scope, and plan before implementation. Use SDD, BDD, TDD, and risk-driven validation; define EDD before implementing model or agent behavior. Work on one short-lived branch at a time.

The workflow is: Implementer completes the change and local checks; H2, the user reviews the local diff, results, rationale, files, and risks, then explicitly authorizes the initial commit, push, and PR; Reviewer reviews the PR. Each finding that maps to changed lines must be posted as an inline PR review comment anchored to the most relevant line(s), with the issue, evidence, impact, and suggested fix; use a minimal line range for multi-line findings. Use a general PR comment only when no changed line is a reasonable anchor, and explain why. Also provide a review summary. For every fix round, the Implementer shows the new diff, check results, rationale, affected files, and risks and waits for explicit user approval before committing and pushing updates. Scope changes return to H1. After a Reviewer confirms a finding is fixed, reply `Resolved after re-review` to its inline thread and mark that thread resolved; add a PR summary listing resolved and unresolved findings. Do not resolve a finding before Reviewer confirmation. H3 requires a Reviewer re-review of fixes, confirmed inline threads resolved, no open Critical/Major findings except risks the user accepted and recorded in the PR, Minor findings fixed and re-reviewed or answered in the PR, and required CI/EDD results. The user explicitly approves merge after reviewing the final Review/CI status and remaining risks. Until F01 provides CI, missing CI is recorded as a known limitation.

F01 is Roadmap Phase 1 (Foundation): reproducible Docker Compose local startup and required PR CI. See `AGENTS.md` for normative execution and safety rules, `docs/agent-roles.md` for role boundaries and human gates, and `docs/DEVELOPMENT-WORKFLOW.md` for operational steps. These files are written primarily in Traditional Chinese, with key English technical terms explained in Chinese; code identifiers, commands, filenames, and CI settings retain their exact spelling. Product requirements are authoritative in this specification. Section 15 summarizes the workflow and does not override the agent rules. Never expose production secrets, treat prompts/documents/model outputs/tool arguments as untrusted, or skip/hide failed tests.

## 16. Open Decisions

Confirm these during implementation rather than guessing:

- Local authentication approach and whether an external identity provider will be demonstrated.
- Exact LLM providers and embedding models to support first.
- Supported document formats for the first ingestion milestone.
- Whether Redis is necessary after measuring actual needs.
- Frontend authentication/session strategy.
- Whether MCP belongs in the first MVP or as a P1 extension.
- Initial performance and concurrency targets.
- Local Agent packaging and supported operating systems.
- Device identity mechanism, pairing UX, credential rotation, and offline behavior.
- Exact local file operations and confirmation policy for write/delete operations.
- Enterprise policy model for disabling Local Agent and restricting folder scopes.

## 17. Project Success Criteria

The project succeeds as a portfolio demonstration when it proves the ability to design and implement a secure, observable, model-agnostic enterprise AI application—not merely a chatbot wrapper. Evidence should include working code, meaningful tests, clear architecture, permission-aware RAG, governed tool execution, and reproducible local deployment. The longer-term product direction also includes a secure, optional Local Agent for user-approved local file workflows.
