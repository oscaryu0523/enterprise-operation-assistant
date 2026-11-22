# 企業智慧維運 AI Agent
## 產品與技術規格書

> 版本：1.2  
> 狀態：初版基準規格  
> 主要語言：繁體中文  
> 最後更新：2026-11-21

## 1. 專案概述

**企業智慧維運 AI Agent（Enterprise Operation Assistant，簡稱 EOA）** 是一套面向企業的 AI Agent 平台，協助員工透過自然語言查詢企業知識、理解維運資訊、分析事件，並在權限與政策控管下呼叫核准的業務或 IT 工具。

本專案定位為可在本機執行、適合作品集展示的實作型專案，重點呈現 AI 應用工程能力：模型抽象化、Agent 編排、RAG、工具整合、存取控制、稽核與可觀測性。

### 1.1 問題背景

企業資訊與日常作業流程通常分散在文件、資料庫、監控系統、工單平台及各部門工具中。員工必須在多個系統間切換並手動彙整資訊。單純的一般聊天機器人並不足以滿足企業需求，企業場景還需要根據授權資料提供有依據的回答、受控的工具存取、可追蹤的操作紀錄、妥善處理模型故障與併發，以及支援多家模型供應商。

### 1.2 產品願景

提供統一且具權限感知能力的 AI 助理，讓員工以自然語言查詢企業知識、取得維運資訊，並執行經核准的作業流程。

### 1.3 目標使用者

- 一般員工：查詢內部知識、理解作業程序。
- IT／維運工程師：調查事件、檢視維運資料。
- 部門管理者：管理知識來源、工具與存取政策。
- 平台管理者：設定模型供應商、安全政策及系統健康狀態。

## 2. 產品目標與非目標

### 2.1 產品目標

1. 提供統一的企業對話介面。
2. 透過 Provider 抽象層支援不同 LLM 供應商。
3. 使用 RAG 查詢內部知識，並在檢索前執行權限過濾。
4. 允許 Agent 在後端政策控管下呼叫明確註冊的工具。
5. 記錄對話、工具呼叫、錯誤及安全相關事件。
6. 可使用免費或 Mock 模型方案在本機開發與展示。
7. 提供自動化測試及可重現的容器化啟動方式。
8. 建立安全的長期架構，未來可透過選配 Local Agent 連接每位員工經授權的本機工作區。

### 2.2 初期 MVP 不包含

- 取代企業既有身分系統或資料來源系統。
- 不受限制地存取員工電腦、檔案或企業內網。
- 在初期 MVP 導入 Local Agent 或完整桌面控制能力。
- 自動執行具破壞性的正式環境操作。
- 訓練或微調基礎模型。
- 在沒有實際需求前，將每個模組都拆成微服務。
- 宣稱已取得正式企業合規認證。

## 3. 功能範圍與優先級

| 優先級 | 功能模組 | 初期要求 |
|---|---|---|
| P0 | AI Gateway | Provider 抽象、設定、逾時與錯誤處理 |
| P0 | Agent Runtime | 對話上下文、工具選擇、有限度執行 |
| P0 | RAG | 文件匯入、切塊、Embedding、檢索、來源引用 |
| P0 | 身分與存取控制 | 認證、角色、資源層級授權 |
| P0 | 對話管理 | 對話與訊息持久化 |
| P1 | 工具整合 | 唯讀維運工具與 Connector 介面 |
| P1 | MCP 整合 | 評估並整合 Model Context Protocol Server |
| P1 | Log／事件分析 | 摘要日誌並關聯提供的維運上下文 |
| P1 | 稽核與監控 | 工具稽核紀錄、Metrics、結構化日誌 |
| P1 | Local Agent 基礎能力 | 裝置註冊、安全配對、Heartbeat 與撤銷機制設計 |
| P2 | 本機檔案操作 | 透過 Local Agent 存取使用者授權資料夾及 Allowlist 工具 |
| P2 | 管理後台 | 管理 Provider、工具、知識來源、裝置與政策 |

P0 是第一個可展示 MVP 的必要範圍。P1、P2 必須在 P0 驗收標準達成後再逐步實作。

## 4. 核心功能需求

### 4.1 AI Gateway 與模型供應商

- 定義統一的 Provider 介面，避免 Agent Runtime 直接依賴特定模型廠商。
- 可設定 Provider、模型名稱、Endpoint、憑證、Temperature、Token 上限及 Timeout。
- 初期先提供 Mock Provider，再加入可在本機使用的 Ollama 相容 Provider。
- 支援 Provider 健康檢查及明確錯誤回報。
- 僅針對可重試錯誤進行有限次重試，避免工具被重複執行。
- 機密資訊不得提交至版控，也不得出現在日誌中。

**初期 Provider 策略：**先實作可用於確定性測試的 Mock Provider，以及供本機實驗使用的 Ollama 相容 Provider。雲端模型 Adapter 為選配，且必須可透過設定啟用。

### 4.2 Agent Runtime

- 接收使用者訊息及已認證的使用者上下文。
- 判斷需求應使用知識檢索、工具呼叫或直接由模型回答。
- 僅提供目前使用者有權限呼叫的工具。
- 工具執行前由後端驗證參數。
- 設定最大工具呼叫次數、Timeout、Payload 大小及迭代次數。
- 工具失敗或模型無法完成任務時，回傳清楚的錯誤資訊。
- 保存對話訊息及執行中繼資料。

模型可以提出操作建議，但授權判斷與實際執行政策必須由後端負責。

### 4.3 RAG 知識檢索

- 匯入支援的文字型文件及其來源中繼資料。
- 擷取文字、正規化內容，並依可調整的參數切分 Chunk。
- 透過可替換的 Embedding Provider 產生向量。
- 使用 PostgreSQL 搭配 pgvector 儲存向量與中繼資料。
- 檢索相關 Chunk，並在回答中附上來源參考。
- **在檢索內容送入模型上下文之前，先依使用者權限過濾文件。**
- 支援來源文件重新索引及刪除。
- 追蹤匯入狀態與失敗原因。

MVP 先使用少量且可控的範例文件，後續再擴充檔案格式。未來若透過 Local Agent 處理本機文件，必須先在裝置端檢查授權範圍，且僅將任務所需、已獲授權的內容或檢索片段傳至中央平台。

### 4.4 工具整合

- 每個工具都要註冊固定名稱、描述、JSON 輸入 Schema、風險等級及所需權限。
- 初期從唯讀工具開始，例如服務健康狀態查詢、部署歷史查詢、Mock 工單搜尋。
- 在工具邊界驗證輸入與輸出。
- 套用 Allowlist、Timeout、Rate Limit 及稽核紀錄。
- 未來若加入高影響的寫入操作，必須要求人工明確核准。
- MVP 不開放任意 Shell 執行或不受限制的檔案系統存取。

### 4.5 Local Agent 與員工裝置存取

產品長期架構支援在員工電腦安裝選配的 Local Agent。Local Agent 負責在該裝置上執行經核准的本機操作；中央平台不會直接瀏覽或掛載員工的檔案系統。

- 每個安裝實例都必須註冊為裝置，並關聯已認證的使用者與企業組織。
- 裝置配對必須經使用者明確同意，並支援裝置撤銷及憑證輪替。
- 使用者必須明確選擇並授權特定本機資料夾；存取範圍不得超出授權資料夾，且使用者可隨時撤銷。
- Local Agent 透過 TLS 主動連線中央平台，使用短效 Access Token 及／或裝置憑證。需定義 Heartbeat、重新連線、憑證過期及離線行為。
- 僅開放明確註冊的 Local Tool，初期以 list、read、search 為主。create、update、move 僅能在細緻權限控管下加入，必要時要求確認。
- delete、覆寫、大量異動及其他高風險操作必須視為高風險行為，先提供操作預覽並要求人工明確確認。不得提供任意 Shell 執行或不受限制的桌面控制。
- 落實使用者、企業組織及裝置隔離。稽核操作者、裝置、資料夾範圍、請求操作、政策判斷及執行結果。
- 遵守資料最小化原則。本機檔案預設不會整批上傳；僅傳輸完成任務所需且已授權的內容。資料進入模型前仍須執行 RAG 權限過濾。
- 企業管理者可停用 Local Agent、限制允許使用的工具，或設定允許的資料夾政策。

Local Agent 屬於 MVP 之後的能力。第一階段 MVP 以瀏覽器文件上傳為主，不要求員工安裝任何本機程式。

### 4.6 身分與授權

- 使用 Spring Security 處理使用者認證。
- 初期以 RBAC（角色型存取控制）管理基本權限。
- 對對話、文件及工具執行資源層級授權檢查。
- 使用可信任的後端上下文取得身分與權限，不得採信模型自行產生的身分資訊。
- 不允許模型自行提升使用者或自己的權限。

初期角色建議：**USER**、**OPERATOR**、**ADMIN**。

### 4.7 對話管理

- 支援建立、列出、查詢及封存對話。
- 保存使用者與助理訊息、時間及相關中繼資料。
- 每個對話都必須關聯擁有者與存取政策。
- 除非明確授權，使用者不得讀取其他使用者的對話。

### 4.8 稽核與可觀測性

- 記錄使用者身分、Request Correlation ID、選用的 Provider／Model、工具名稱、執行結果、耗時及相關政策判斷。
- 不記錄原始憑證，也避免在日誌中保存不必要的敏感文件內容。
- 提供應用程式 Health 與 Readiness Endpoint。
- 使用結構化日誌及 Micrometer Metrics。
- 讓錯誤可被追蹤，同時避免洩漏敏感資訊。

## 5. 高階系統架構

MVP 採用 **Modular Monolith（模組化單體架構）**，降低部署與本機開發複雜度，同時維持清楚的模組邊界。

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
    UI -. 未來裝置配對 .-> LA[員工 Local Agent]
    LA -->|TLS／裝置身分與授權範圍| LTOOLS[Local Tool Registry]
    LTOOLS --> FS[使用者授權資料夾]
    LA -. 最小必要授權資料 .-> API
    API --> AUDIT[Audit & Observability]
    CHAT --> PG
    AUDIT --> OBS[Logs & Metrics]
~~~

### 5.1 架構設計原則

- Domain 模組透過清楚的介面隔離。
- 各模型供應商專屬程式碼集中在 Provider Adapter。
- 授權與政策控管必須由可信任的後端服務執行。
- 將模型輸出、檢索文件及工具輸入全部視為不可信資料。
- 只有在確實有幫助時才導入非同步處理。
- 避免過早拆分微服務。

## 6. 建議技術選型

| 層級 | 技術 | 說明 |
|---|---|---|
| Backend | JDK 21 LTS／Spring Boot 4.2.x | 主要應用程式；採用該版本線最新穩定修補版 |
| AI Framework | Spring AI 2.1.x | 與 Spring Boot 4.2.x 相容的穩定版本線；整合模型及 Vector Store |
| Frontend | React + TypeScript | 對話與管理介面 |
| Database | PostgreSQL + pgvector | 業務資料與向量檢索 |
| Cache | Redis（選配） | 有明確快取需求再加入 |
| Security | Spring Security | 認證與授權 |
| Local Model | Ollama 相容執行環境 | 選配的本機推論 |
| Testing | JUnit Jupiter（版本由 Spring Boot 管理）、Mockito、Testcontainers | 單元與整合測試 |
| Packaging | Docker／Docker Compose | 可重現的本機環境 |
| CI | GitHub Actions | 建置、測試與品質檢查 |
| Observability | Micrometer、Structured Logging | Metrics 與診斷 |
| Optional AI Service | Python + FastAPI | 僅在 Java 服務不適合實作特定能力時加入 |
| Future Local Agent | Java（初期優先） | 每台裝置安裝的輕量程式，MVP 後獨立交付 |

MVP 使用模組化單體架構。Python 是選配的輔助服務，不是必須額外維護的第二套 Backend。基線採用 JDK 21 LTS、Spring Boot 4.2.x 與 Spring AI 2.1.x 穩定版本；由各自 BOM 管理相容依賴，並採用該版本線最新穩定修補版。避免使用 Milestone、Release Candidate 與 Snapshot。每個實作階段開始前，重新確認官方相容性與支援狀態。

## 7. 資安需求

1. 所有受保護的 Backend Endpoint 都必須執行認證與授權。
2. 文件與工具採最小權限原則。
3. RAG 必須在建立模型上下文之前，依使用者權限過濾檢索結果。
4. Prompt、文件、模型回應及工具參數都視為不可信輸入。
5. 使用後端 Schema 驗證並限制工具參數。
6. 不允許任意 Shell 指令、不受限制的網路存取或本機檔案存取。
7. 使用環境變數或專用 Secret Manager 保存機密，不得提交真實憑證。
8. 日誌必須遮蔽機密與敏感資料。
9. 設定 Request Size Limit、Rate Limit 及 Execution Timeout。
10. 保留安全相關事件與工具執行的稽核紀錄。
11. 未來高影響或不可逆操作必須經人工確認。
12. 不得將 LLM 當成資安邊界或授權判斷的唯一依據。
13. Local Agent 僅能在使用者授權的資料夾範圍及已註冊工具內執行，預設不得使用系統管理員權限。
14. 每個裝置連線都必須認證與授權，並支援裝置撤銷、短效憑證、TLS 及稽核紀錄。
15. 落實企業、使用者及裝置隔離；本機資料傳輸遵守最小化原則，預設不得整批上傳本機資料夾。
16. 刪除或高影響本機操作必須取得人工明確確認。企業政策必須能停用 Local Agent，或限制可用工具及資料夾範圍。

## 8. 初版 API 設計

以下為初步 API 規劃，實作時可再依需求調整。

| Method | Endpoint | 用途 |
|---|---|---|
| POST | /api/v1/auth/login | 使用者登入（啟用本機認證時） |
| GET | /api/v1/conversations | 列出目前使用者可存取的對話 |
| POST | /api/v1/conversations | 建立對話 |
| GET | /api/v1/conversations/{id} | 查詢對話 |
| POST | /api/v1/conversations/{id}/messages | 傳送訊息並取得 Agent 回應 |
| GET | /api/v1/knowledge/sources | 列出可存取的知識來源 |
| POST | /api/v1/knowledge/sources | 註冊或匯入知識來源 |
| GET | /api/v1/tools | 列出目前使用者可用的工具 |
| POST | /api/v1/devices/pairing | 啟動已認證的 Local Agent 配對流程（未來） |
| POST | /api/v1/devices/{id}/heartbeat | 接收裝置 Heartbeat（未來） |
| GET | /api/v1/devices | 列出目前使用者已註冊裝置（未來） |
| DELETE | /api/v1/devices/{id} | 撤銷已註冊裝置（未來） |
| GET | /actuator/health | 健康檢查 |

所有 Endpoint 都必須驗證資源擁有權及存取權限。認證 API 的細節依選定的本機身分驗證方案決定。

## 9. 初版資料模型

核心 Entity：

- **User：**使用者身分、狀態及角色。
- **Role／Permission：**授權定義。
- **Conversation：**擁有者、標題、狀態及時間。
- **Message：**所屬對話、角色、內容、時間及中繼資料。
- **KnowledgeSource：**來源識別、擁有者／範圍及匯入狀態。
- **DocumentChunk：**來源參考、Chunk 文字、中繼資料及 Embedding。
- **ToolDefinition：**工具 Schema、風險等級、所需權限及啟用狀態。
- **ToolExecution：**呼叫者、工具、參數摘要、結果、耗時及 Correlation ID。
- **AuditEvent：**操作者、動作、目標、時間及結果。
- **Device：**使用者／企業關聯、裝置身分、狀態、最後 Heartbeat 及憑證中繼資料。
- **DeviceFolderGrant：**裝置、已授權資料夾範圍、權限等級及撤銷狀態。
- **LocalToolDefinition：**允許的本機操作、輸入 Schema、風險等級及所需權限。

資料庫結構必須透過版本化 Migration 管理。

## 10. 非功能需求

| 面向 | 需求 |
|---|---|
| Reliability | Provider／Tool 發生錯誤時不得導致應用程式崩潰 |
| Performance | 設定明確 Timeout 與資源上限 |
| Scalability | 支援多請求併發，並控管模型與工具的併發數量 |
| Security | 落實最小權限及後端授權 |
| Maintainability | 模組化程式碼、介面文件及自動化測試 |
| Portability | 透過文件化的 Docker Compose 設定啟動本機環境 |
| Observability | Correlation ID、結構化日誌、Health Check 及關鍵 Metrics |
| Cost Control | 可設定模型、Token 上限，並提供 Mock／Local 方案 |
| Device Security | 裝置身分隔離、資料夾範圍授權、撤銷及本機操作稽核 |
| Data Minimization | 僅在必要且已授權時傳輸本機內容 |

具體效能數值需在開發環境完成基準測試後再訂定。

## 11. 測試策略

- **Unit Test：**Provider Routing、授權政策、工具參數驗證、Chunking 及 Service 邏輯。
- **Integration Test：**REST API、資料持久化、向量檢索及 Provider Adapter。
- **Security Test：**跨使用者對話存取、未授權文件檢索及禁止的工具呼叫。
- **Failure Test：**Provider Timeout、模型輸出格式錯誤、Tool Failure、資料庫不可用。
- **End-to-End Test：**使用者提問、取得有權限的知識、收到附來源的回答，並成功呼叫允許的唯讀工具。
- **Deterministic Test：**使用 Mock 模型／Provider 回應，確保 CI 不需要付費 API。
- **未來 Local Agent 測試：**驗證裝置配對／撤銷、資料夾範圍限制、跨使用者／裝置隔離、離線行為、高風險操作確認及稽核事件。

## 12. MVP 驗收標準

符合以下條件即視為 MVP 完成：

1. 開發者能依照文件完成本機啟動。
2. 已認證使用者能建立並延續對話。
3. 系統能透過 Mock 或 Local Model Provider 回答。
4. 不修改 Agent 業務邏輯即可透過設定切換 Provider。
5. 能匯入範例文件並透過 RAG 搜尋。
6. 檢索型回答能附上來源參考。
7. 使用者無法取得超出自身權限的文件或對話。
8. Agent 至少能呼叫一個已授權的唯讀工具。
9. 後端會拒絕未授權的工具呼叫。
10. 工具執行與關鍵錯誤均可稽核。
11. CI 自動化測試通過，且不需要付費 LLM 訂閱。

Local Agent 安裝、裝置配對及直接操作本機檔案，明確不屬於 MVP 驗收範圍。

## 13. 開發 Roadmap

| 階段 | 交付成果 |
|---|---|
| 0. 規格確認 | 確認範圍、架構、開發規範及驗收標準 |
| 1. 基礎建置 | Repository 結構、Spring Boot、設定、可重現的 Docker Compose 本機啟動、必要的 Pull Request CI；不自動部署 |
| 2. 身分與對話 | 認證、RBAC 基礎、對話及訊息 API |
| 3. AI Gateway | Provider 介面、Mock Provider、本機 Provider Adapter |
| 4. Agent Runtime | Prompt／Context 組裝、有限度編排、錯誤處理 |
| 5. RAG | 文件匯入、Embedding、pgvector 檢索、權限過濾 |
| 6. Tools | Tool Registry、Schema、政策檢查、唯讀範例工具 |
| 7. 可觀測性 | Audit Event、Metrics、結構化日誌、Health Check |
| 8. Frontend | 對話介面及必要的管理畫面 |
| 9. 強化與驗收 | 資安、整合、故障及端對端測試；補齊文件 |
| 10. Local Agent 基礎能力（MVP 後） | 裝置身分、配對、安全主動連線、Heartbeat 及撤銷 |
| 11. 本機檔案工具（MVP 後） | 使用者授權資料夾、唯讀／搜尋工具，再逐步加入受控寫入操作 |

每個階段都必須有可審查的交付成果，且測試通過後才能擴大下一階段範圍。

## 14. Repository 目標結構

~~~text
enterprise-operation-assistant/
├── AGENTS.md
├── backend/
│   └── (Spring Boot application)
├── frontend/
│   └── (React + TypeScript application)
├── local-agent/                  # MVP 後；每台裝置安裝的本機程式
│   └── (Java application)
├── docs/
│   ├── DEVELOPMENT-WORKFLOW.md
│   ├── PROJECT-SPEC.en.md
│   ├── PROJECT-SPEC.zh-TW.md
│   ├── agent-roles.md
│   └── adr/                        # Architecture Decision Records
├── evals/                          # 模型／Agent 行為的版本化評估資料集
├── infra/
│   └── (Docker Compose and local infrastructure)
├── scripts/
├── .github/
│   └── workflows/
├── .gitignore
└── README.md
~~~

以上為目標結構，不代表現在就必須建立所有目錄。

## 15. Agent 驅動開發工作流程

本專案採用 Agent 主導實作的方式：使用者負責產品方向、優先級、驗收與最終審查；Coding Agent 負責檢查規格與程式碼、提出計畫、完成已核准的範圍、驗證結果並解釋設計，讓使用者能在審查中學習。

預設由 Codex 擔任 Supervisor 與主要 Implementer，Claude Code 擔任獨立且唯讀的 Reviewer；使用者是 Product Owner 並保有最終合併決策權。H2 核准後建立 PR，再將已核准規格、PR 連結、Diff、檢查證據及明確審查問題交給 Reviewer。若 Claude Code 實作任務，則交換角色。角色責任與 Review handoff 範本見 `docs/agent-roles.md`。

每項非瑣碎的實作、規格或流程變更開始前，Agent 先閱讀相關規格與 Repository 狀態，整理目標、假設、邊界情況、驗收條件、風險及預計修改檔案，再提出精簡計畫。使用者確認計畫後才開始工作。只有不改變需求、範圍或驗收條件的小型明確文件修正，以及明確指定的一行調整，才可直接處理，但仍須說明範圍。

使用 SDD 定義範圍與驗收標準，使用 BDD 以 Given／When／Then 描述可觀察行為，程式碼變更採 TDD（Red → Green → Refactor），並以風險導向方式優先驗證資安路徑。包含模型或 Agent 行為的功能在實作前先定義版本化、每次執行固定的 EDD 資料集與可量測門檻，評估任務成功率、工具呼叫正確性、引用依據、權限、延遲及成本；無關功能不必加入 EDD。

一次只執行一項已核准的明確任務，並從 main 建立短期分支。維持 main 可用，不維護長期 develop 分支。使用聚焦的 feature、fix、docs 或 refactor 分支；完成審查後再合併回 main。

Agent 應採最小且完整的異動，避免無關重構，並同步更新受影響文件。程式碼異動要新增或更新相關測試；執行最相關的檢查，審查完整 Diff 的正確性、資安、意外檔案及範圍膨脹，並回報結果與尚未驗證的部分。純文件變更檢查文件結構與 Diff，不必執行應用程式測試。

採三個人工關卡：H1 使用者核准規格／計畫後才開始非瑣碎工作；Codex 完成本機檢查後，H2 使用者審查本機 Diff 與結果，再授權 Commit、Push 及建立 PR；PR 建立後由 Claude Code 審查。每輪修正時，Codex 先把修改 Diff 與檢查結果交給使用者檢視，再更新同一個 PR；H2 授權涵蓋核准範圍內的後續提交，若改變範圍則重新走 H1。Critical／Major 發現必須修正或由使用者明確接受風險，並交由 Reviewer 複審。H3 使用者確認 Review 發現已處理，檢視 PR CI／EDD 結果與未解風險後，明確核准合併至 `main`。F01 CI 建立前，H3 依據已記錄的本機檢查，並將缺少 CI 列為已知限制。H2 前不得 Commit、Push 或建立 PR。完成後，Agent 說明異動內容、原因、重要設計選擇、修改檔案、執行的檢查，以及使用者應審查或學習的重點。

獨立 Reviewer 在 PR 上檢查 Diff、測試與風險；Critical／Major 問題必須修正或由使用者明確接受風險，並在修正後重新審查；Minor 可修正或回覆說明。F01 即 Roadmap Phase 1 Foundation：建立 Pull Request CI 與可重現的 Docker Compose 本機啟動。初期 CI 包含 Java 21 建置／編譯、單元測試、已設定的品質檢查、Markdown 驗證、依賴與機密掃描；資料庫路徑建立後再加入整合測試，Agent 行為建立後再加入 EDD。CI 使用 `pull_request`（不可用 `pull_request_target`）、`permissions: contents: read`，且不使用 Repository secrets。每個指定的必要檢查都必須在每個 PR 回報結果；避免在整個 Workflow 層級使用路徑篩選，讓必要檢查停留在 Pending。設定 `main` branch protection 或 ruleset 要求指定 CI checks 全綠，並驗證失敗檢查確實會阻止合併；若平台不支援強制，記錄限制。F01 不做部署；待部署目標確定後再規劃 CD，正式環境部署必須經人工明確核准。

文件規範：操作文件以繁體中文撰寫，保留必要英文技術術語並附中文解釋；程式識別字、指令、檔名及 CI 設定維持原文。產品規格保留獨立的英文及繁體中文版。規則優先順序為 `AGENTS.md`（執行與安全規則）→ `docs/agent-roles.md`（角色與關卡）→ `docs/DEVELOPMENT-WORKFLOW.md`（操作流程）；產品規格是產品需求的權威來源。遇到衝突時依優先順序處理並更新相關文件。

不得向 Agent 提供正式環境機密。Prompt、文件、模型輸出及工具參數一律視為不可信資料。採用最小權限。刪除、不可逆、對外可見或影響正式環境的操作，必須先取得使用者明確核准。不得跳過或隱瞞失敗的測試。

## 16. 待確認事項

以下事項應在實作階段確認，不應自行臆測：

- 本機認證方案，以及是否展示外部 Identity Provider。
- 第一批支援的 LLM Provider 與 Embedding Model。
- 第一階段文件匯入支援的格式。
- 經過實際需求評估後是否需要 Redis。
- Frontend 認證與 Session 管理策略。
- MCP 是納入第一版 MVP，或作為 P1 擴充。
- 初期效能及併發目標。
- Local Agent 的安裝包形式及支援的作業系統。
- 裝置身分機制、配對流程、憑證輪替及離線行為。
- 本機檔案操作範圍，以及寫入／刪除操作的確認政策。
- 企業停用 Local Agent、限制工具及資料夾範圍的政策模型。

## 17. 專案成功標準

本專案作為作品集的成功標準，是證明具備設計及實作安全、可觀測、模型供應商無關的企業 AI 應用能力，而不只是包裝一個聊天機器人。關鍵成果包括可運作的程式碼、有意義的測試、清楚的架構、具權限控管的 RAG、受政策管理的工具執行，以及可重現的本機部署方式。產品長期方向也包含安全且選配的 Local Agent，支援使用者授權的本機檔案工作流程。
