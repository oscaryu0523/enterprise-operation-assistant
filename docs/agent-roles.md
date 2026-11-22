# Agent 角色與交接（Agent Roles and Handoff）

## 角色與權責

| 角色 | 預設負責者 | 工作內容 | 權限界線 |
|---|---|---|---|
| 產品負責人（Product Owner） | 使用者 | 設定產品目標與優先級；核准範圍、架構／資安取捨、驗收與最終合併 | 對產品決策及合併有最終決定權 |
| PM／架構師（Product Manager / Architect） | Claude Code，同一個規格對話 | 釐清需求、使用情境與驗收條件；設計流程、介面、資料與失敗行為；維護根目錄 `SPEC.md` | 只產出規格，不實作產品程式碼；不默默擴大使用者核准的範圍 |
| 工程師（Engineer / Implementer） | Codex | 依已核准的 `SPEC.md` 實作、測試、自我審查並交付 Git diff 與檢查證據 | 核准範圍內自行完成修正循環；不得擅改需求或驗收 |
| QA（Quality Assurance） | Claude Code，全新獨立對話 | 只依 `SPEC.md`、PR Diff、測試證據與驗收指令逐條驗收；回報重現方式、檔案／行號、影響與建議 | 不沿用 PM／架構對話判斷、不修改程式碼、不核准或執行合併 |

此分工採用 MetaGPT 的角色化與 SOP 交接概念，但由 Claude Code 和 Codex 以人工交接，不導入 MetaGPT 執行框架。Claude PM／架構師在同一對話維護根目錄 `SPEC.md`；使用者核准後，Codex 以該檔案作為唯一的本功能實作依據。H2 後，將 `SPEC.md`、PR 連結、Diff、測試／CI 證據及驗收指令交給全新 Claude QA 對話。PM／架構對話不擔任 QA。

## 每項功能的開發關卡

1. **規格導向開發（SDD）：**Claude PM／架構師在同一對話建立根目錄 `SPEC.md`，記錄目標、範圍、不做事項、使用情境、BDD 驗收條件、流程／API／資料決策、錯誤與逾時行為、檢查方式及已知限制；有長期影響的決策視需要記錄 ADR（Architecture Decision Record）。使用者在實作前核准一次。
2. **行為導向開發（BDD）：**使用 Given／When／Then 描述主要使用情境與可觀察結果，涵蓋錯誤、權限、逾時及誤用案例；將相關情境放進 Review handoff。
3. **評估導向開發（EDD）：**功能包含 LLM 或 Agent 行為時，實作前定義版本化且每次執行固定的資料集，並設定可量測檢查，例如任務成功、工具呼叫正確性、引用依據、權限、延遲及成本。無關功能不需 EDD。
4. **測試導向開發（TDD）：**程式碼行為先寫失敗測試（Red），完成最小實作（Green），再重構（Refactor）。外部模型／Provider 呼叫使用可重現的模擬替身（Mock）。
5. **風險導向驗證（Risk-driven validation）：**優先檢查授權邊界、Prompt Injection、惡意工具輸出、機密／資料外洩、模型／Provider 故障、重試冪等性及高影響操作。
6. **獨立 QA：**Claude Code 每輪 QA 都開新對話，只讀 `SPEC.md`、PR Diff、測試證據和驗收指令。依 Critical／Major／Minor 回報失敗條件、重現步驟、檔案與行號、證據、影響及建議。能對應變更行的發現必須在 PR 以逐行 Review comment 標註，另以 review summary 彙整。Critical／Major 必須修正並複審，或由使用者明確接受風險並記錄在 PR（不需再複審）；Minor 修正後須複審，若以 PR 回覆處理則記錄即可。

## 三個人工關卡與 PR 審查循環（Review loop）

1. **H1 規格核准：**Claude Code PM／架構師在同一對話完成 `SPEC.md`；使用者核准一次後，Codex 才開始實作。這次核准涵蓋規格範圍內的工程實作及 QA 修正循環；改變範圍才重新核准。
2. **工程師實作：**Codex 從 `main` 建立短期分支，讀取已核准 `SPEC.md`，依 TDD 實作第一條可展示流程，執行相關測試與檢查，準備 Diff、設計理由、檔案清單與風險。
3. **H2 PR 發布：**Codex 完成本機 Diff 與檢查後，使用者審查並明確授權初次 Commit、Push 及建立 PR。H2 涵蓋同一核准範圍內後續 QA 修正提交，不再逐輪請使用者核准；超出範圍則重新走 H1。
4. **獨立 QA：**Codex 將已核准 `SPEC.md`、PR 連結／Diff、測試與 CI 證據、驗收指令提供給全新 Claude Code 對話。QA 依 Critical／Major／Minor 逐條驗收，說明失敗條件、重現方式、檔案／行號、證據、影響與建議；逐行問題直接錨定 PR Diff，另留 review summary。若 Claude CLI 未直接發佈，透明標示代貼者與 Reviewer。
5. **修正與複審：**Codex 依 QA findings 在 SPEC 範圍內修正、測試並更新同一 PR；不必每輪請使用者核准。每次 QA 複審使用新 Claude 對話，交付 `SPEC.md`、最新 Diff、檢查證據與前次 findings／處理狀態。Claude 確認已修正後，逐項回覆 inline thread `Resolved after re-review` 並標記 Resolved，另留 PR 摘要。未確認不得標記 Resolved。超出 SPEC 範圍時停下並重新取得使用者核准。
6. **H3 合併：**最新 QA 複審已確認所有修正，且沒有未處理 Critical／Major；例外是使用者已明確接受並在 PR 記錄的風險，這類項目不需再次複審。Minor 若已修正須經複審；若以 PR 回覆處理則記錄即可。QA 確認已修正的 inline thread 必須回覆並標記 Resolved。交付 H3 時，提供最終 QA／CI 狀態、變更理由與剩餘風險。使用者確認 CI／EDD 結果及未解風險後，明確核准合併至 `main`。不可自動合併。F01 CI 建立前，H3 依據已記錄的本機檢查，並將缺少 CI 列為已知限制。

## 審查交接（Review handoff）範本

```text
功能／Issue：
Repository 根目錄 `SPEC.md`（已核准）：
已核准的規格與驗收條件：
Pull Request 連結與分支：
修改檔案：
測試與檢查結果：
BDD 情境與風險導向檢查：
EDD 資料集與結果（適用時）：
前次 Reviewer 發現與處理方式（已修正／使用者接受風險／已回覆）：
已知限制與未解風險：
請審查：規格符合度、正確性、資安、回歸問題及範圍膨脹。
請依 Critical／Major／Minor 回報。每項能對應到變更行的發現，請在 PR 以逐行 Review comment 錨定至最精確的相關行；多行問題只選最小必要行範圍。comment 必須包含問題、證據、可能後果及建議修正。僅無法合理對應 Diff 行的發現才放一般 PR comment，並解釋原因。另以 review summary 彙整結論；不要只留一則沒有行號錨點的總結。
複審時逐項檢視前次發現；Reviewer 確認已修正的項目，請回覆原 inline thread `Resolved after re-review` 並標記 Resolved，再以 PR 摘要列出已解決及尚未解決項目。未確認的項目保持未解決。
請勿修改產品程式碼或核准／執行合併。逐行 findings 請直接留言在 PR；未經 QA 複審確認不得標記 Resolved。
```

## F01 CI 範圍

F01 是 Roadmap Phase 1（Foundation），建立 GitHub Actions PR 持續整合（CI）與可重現的 Docker Compose 本機啟動。初期檢查包括 Java 21 建置／編譯、單元測試、已設定的格式／靜態品質檢查、Markdown／文件檢查，以及依賴與機密掃描。資料庫與應用路徑建立後再加入整合測試；Agent 行為出現後加入 EDD。模型測試須可重現，不得要求付費模型 API 或 Repository secrets。GitHub Actions 必須固定到完整 Commit SHA，不可使用浮動 Tag。

CI（持續整合）使用 `pull_request`（不可使用 `pull_request_target`）及 `permissions: contents: read`。每個必要檢查都必須對每個 PR 回報結果；不可在整個 GitHub Actions 工作流程（Workflow）層級使用路徑篩選，讓必要檢查停留 Pending。設定 `main` Branch Protection／Ruleset 要求必須經 PR、指定檢查通過且管理員不可略過，並驗證失敗檢查會阻擋合併。若平台無法強制，記錄限制。

F01 不部署至 staging 或 production。應用可執行後再評估建置／發布套件。未來部署須使用明確環境；production 需要人工核准，不可自動部署。

## Git 與發布規則

- 維持 `main` 穩定，使用短期 `feature`／`fix`／`docs`／`refactor` 分支；不建立長期 `develop`。
- 初次 Commit、Push 及建立 PR 前須取得 H2 明確授權；該授權涵蓋同一 SPEC 範圍內的 QA 修正更新。超出範圍先重新取得 H1 核准。
- 必要 CI 檢查通過、Reviewer 已複審所有修正且風險接受已記錄後，仍須由使用者核准合併。F01 CI 建立前，改以已記錄的本機檢查作為依據，並將缺少 CI 列為已知限制。
- 不向 Agent 提供正式環境機密；採最小權限及範圍明確的測試憑證。
- 執行刪除、不可逆、影響正式環境或對外可見的操作前，先向使用者說明並取得核准。
