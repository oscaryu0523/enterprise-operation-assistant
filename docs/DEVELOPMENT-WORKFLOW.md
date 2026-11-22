# Agent 驅動開發流程

## 開發方式

本專案採用 **AI Agent（人工智慧代理）主導實作，使用者負責審查與學習** 的方式。使用者決定產品目標、優先級及是否接受成果；Agent 負責讀取規格與程式碼、提出計畫、執行已核准的工作、檢查結果，並用清楚的方式解釋異動。

Agent 是開發執行者，不是產品方向的最終決策者。不得把未確認的猜測當成需求，也不得為了方便而擴大任務範圍。

## Agent 角色

- **使用者（Product Owner，產品負責人）：**決定目標與優先級，核准一次功能 `SPEC.md`；最後檢視成果並核准合併。
- **PM／架構師（Product Manager / Architect）：Claude Code，同一個規格對話。**釐清使用情境與驗收條件，設計流程、介面、資料、錯誤處理及限制，並維護根目錄 `SPEC.md`；不實作產品程式碼。
- **工程師（Engineer / Implementer）：Codex。**依核准的 `SPEC.md` 實作、測試、自我審查並交付 Git diff 與證據；只能在核准範圍內修正。
- **QA（Quality Assurance）：Claude Code，新開獨立對話。**只讀核准的 `SPEC.md`、PR Diff、測試／CI 證據與驗收指令；逐條驗收並回報可重現的 findings，不修改程式碼、不核准或執行合併。
- **交接方式：**人工交接，但以 `SPEC.md` 和 Git diff 為共同依據。PM／架構對話不得擔任同一功能的 QA；未實際取得全新 QA 對話的驗收結果，不可宣稱完成獨立 QA。

角色權責、H1／H2／H3 關卡、QA 交接範本與 F01 CI 範圍詳見 [`agent-roles.md`](agent-roles.md)。

## 方法組合

- **規格導向開發（SDD，Specification-Driven Development）：**先記錄目標、範圍、不做事項、架構與驗收條件。
- **行為導向開發（BDD，Behavior-Driven Development）：**用 Given／When／Then（前提／事件／結果）描述使用者行為，涵蓋正常、錯誤及權限情境。
- **評估導向開發（EDD，Evaluation-Driven Development）：**Agent／LLM 功能先建立固定評估資料與指標，評估任務成功、工具呼叫、引用依據、權限、延遲及成本。
- **測試導向開發（TDD，Test-Driven Development）：**程式碼行為先寫失敗測試，再完成最小實作並重構（Red／Green／Refactor，紅燈／綠燈／重構）。
- **風險導向驗證（Risk-driven validation）：**優先驗證授權、Prompt Injection（提示注入）、工具濫用、資料外洩、模型故障、重試與高影響操作。

EDD 只在功能包含模型或 Agent 行為時設為必要關卡；文件或基礎設施變更不需要虛構模型評估。

Agent／LLM 功能開始實作前，先定義版本化、每次執行固定的 EDD 資料集與可量測門檻，避免測試結果因樣本變動而失去可比較性。

本專案的 `AGENTS.md`、`docs/agent-roles.md` 與本指南以繁體中文撰寫，必要英文術語首次出現時附中文解釋；程式識別字、指令、檔名及 CI 設定維持原文。`PROJECT-SPEC.en.md` 與 `PROJECT-SPEC.zh-TW.md` 是分開維護的中英文產品規格。規範優先順序見 `AGENTS.md`；產品需求以產品規格為準，本規格第 15 節只提供非規範性摘要。

## 每項任務的工作循環

### 1. 理解需求

- 閱讀本次任務相關的 `AGENTS.md`、產品規格及程式碼。
- 查看 Git 狀態與相關模組，確認既有設計及未提交變更。
- 整理目標、範圍、假設、邊界情況及可驗收條件。
- 若有會改變範圍、資安或驗收方式的未知事項，先提出來釐清。

### 2. 提出計畫

Claude Code PM／架構師先在同一個對話釐清需求、整理架構並建立 Repository 根目錄 `SPEC.md`，包含：

- 目標、範圍、不做事項、使用者情境與可驗收的 Given／When／Then 條件。
- 流程、API／介面、資料模型、相依性與錯誤／逾時行為。
- 主要設計理由、安全與回歸風險、相容性及尚未確定事項。
- 預計新增或執行的測試與驗收指令。

使用者在 Codex 開始實作前核准 `SPEC.md` 一次。核准涵蓋該範圍內的實作與 QA 修正循環；需求或驗收條件變更時，先更新 `SPEC.md` 並重新取得核准。`SPEC.md` 是目前功能規格，不取代 `docs/PROJECT-SPEC.en.md`／`docs/PROJECT-SPEC.zh-TW.md` 的產品需求。

### 3. 小步實作

- 一次只處理一個範圍清楚的任務。
- 先從 `main` 建立短期分支：`feature/<name>`、`fix/<name>`、`docs/<name>` 或 `refactor/<name>`。
- `main` 維持可用，不建立長期 `develop` 分支；每項工作完成後透過審查合併回 `main`。
- 只修改計畫中必要的檔案，不做無關重構。
- 更新受影響的文件、設定、範例及資料庫 Migration。
- 資料格式、API 或架構有多種合理選擇時，先說明取捨；未核准前不擅自引入大型新依賴或新服務。

### 4. 驗證、PR 與獨立審查

- 為程式碼行為新增或更新適當測試。
- 先跑最相關的單元或整合測試，再視變更範圍執行建置與完整測試。
- 文件專用變更檢查 Markdown 結構、連結及 Diff，不必執行應用程式測試。
- 檢查完整 Diff：正確性、資安、錯誤處理、回歸風險、機密資料、意外檔案及範圍膨脹。
- 測試未通過時，修正後重跑；若受環境或外部服務限制，明確標示未驗證部分。
- 本機檢查完成後，實作者先把 Diff、檢查結果、設計理由、檔案清單與風險交給使用者審查。使用者核准 H2 後才 Commit、Push 並建立 PR。
- QA 開始前先核准 H2，建立 PR，然後在全新 Claude Code 對話提供已核准 `SPEC.md`、PR 連結、最新 Diff、測試／CI 證據及驗收指令。每個可錨定變更行的 finding 必須在 PR 以逐行 Review comment 標示，說明嚴重度、問題、證據、影響及修正建議；另用 review summary 彙整。若 CLI 無 GitHub 寫入權限，由 Codex 透明代貼並標明 Reviewer／代貼者。
- Codex 在核准 `SPEC.md` 範圍內修正、執行檢查並更新同一 PR，不必每輪重新請使用者核准。每次複審開新 Claude Code 對話，交付 `SPEC.md`、最新 Diff、檢查證據與前次 findings／處理狀態。QA 確認修正後，逐項回覆原 inline thread `Resolved after re-review` 並標記 Resolved，再留摘要列出已解決及未解決項目；未確認不得標記 Resolved。範圍改變時停下，更新 `SPEC.md` 並重新取得 H1 核准。

### 5. 交付與學習

完成後向使用者說明：

- 改了什麼、為什麼這樣設計。
- 重要檔案與模組的責任。
- 執行了哪些測試／檢查及結果。
- 使用者審查時應特別看的決策、風險或待辦事項。

使用三個人工關卡：H1 Claude Code PM／架構師在同一對話完成根目錄 `SPEC.md`，使用者核准一次後才開始實作；這次核准涵蓋同一範圍內的 Codex 實作與 QA 修正循環，範圍改變才重新核准。H2 Codex 完成本機 Diff 與檢查後，使用者明確授權初次 Commit、Push 及建立 PR；授權涵蓋同一 SPEC 範圍內後續 QA 修正提交。PR 建立後由全新 Claude Code QA 對話按 SPEC、Diff、檢查證據與驗收指令逐條驗收。QA 確認已修正的 finding，須回覆 inline thread `Resolved after re-review` 並標記 Resolved，再在 PR 摘要已解決及未解決項目。H3 必須符合以下條件：最新 QA 複審已確認所有修正；沒有未處理的 Critical／Major，除非使用者已明確接受並在 PR 記錄風險（接受的項目不需再複審）；Minor 若修正須複審，若以 PR 回覆處理則記錄即可；最終 QA／CI 狀態、設計理由與剩餘風險已提供使用者。F01 CI 建立前，H3 依據已記錄的本機檢查，並將缺少 CI 列為已知限制。使用者明確核准後才可合併至 `main`。H2 前不得 Commit、Push 或建立 PR。

## 持續整合與交付（CI/CD）原則

- **F01 即 Roadmap Phase 1 Foundation：**建立 GitHub Actions PR 持續整合（CI）與 Docker Compose 本機可重現啟動。CI 執行 Java 21 建置／編譯、單元測試、已選定的格式與靜態品質檢查、Markdown 文件檢查，以及依賴與機密掃描。整合測試等資料庫與應用路徑建立後再加入；EDD 在 Agent 功能出現後加入。
- CI 僅使用 `pull_request`（不可用 `pull_request_target`）、`permissions: contents: read`，不使用 Repository secrets。GitHub Actions 固定到完整 Commit SHA，不用浮動 Tag。每個指定的必要檢查都必須在每個 PR 回報結果；避免在整個 Workflow 層級使用路徑篩選，造成必要檢查停留在 Pending。設定 `main` Branch Protection／Ruleset，要求必須經 PR、指定 CI checks 全綠且管理員不可略過；驗證失敗檢查確實會阻止合併，若目前方案無法強制則記錄限制。
- 必要 CI 檢查全綠後才能合併；F01 CI 建立前，依據已記錄的本機檢查並將缺少 CI 列為已知限制。不自動合併。
- F01 使用 Docker Compose 確保本機可重現啟動，不設定部署到 staging 或 production。
- 等部署目標明確後再設計 CD。未來可先部署 staging；production 必須使用環境核准，由使用者明確授權，不能讓 Agent 或 CI 自動發布。

## 安全界線

- 不得把正式環境機密貼給 Agent、寫進程式碼或提交到 Git。
- Prompt、文件、模型輸出及工具參數一律視為不可信資料；授權由後端執行。
- 採用最小權限，不讀取無關目錄，不執行任意 Shell 或未核准的工具。
- 刪除、覆寫、批次修改、部署、發布、合併及其他不可逆或對外可見操作，先交代影響並取得使用者明確核准。
- 不跳過測試或隱藏測試失敗；所有未執行的檢查都要如實說明。

## 完成標準

任務只有在以下條件都成立後才算完成：

1. 核准的驗收條件已達成。
2. 相關測試及檢查通過，或清楚記錄未能執行的原因與風險。
3. 行為或架構變更已更新文件。
4. Diff 僅包含核准範圍，沒有機密或無關變更。
5. 使用者收到可審查的成果、驗證結果及必要的學習說明。
