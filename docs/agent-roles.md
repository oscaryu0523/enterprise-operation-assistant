# Agent 角色與交接（Agent Roles and Handoff）

## 角色與權責

| 角色 | 預設負責者 | 工作內容 | 權限界線 |
|---|---|---|---|
| 產品負責人（Product Owner） | 使用者 | 設定產品目標與優先級；核准範圍、架構／資安取捨、驗收與最終合併 | 對產品決策及合併有最終決定權 |
| 協調者（Supervisor） | Codex | 檢查 Repository、釐清任務、準備 SDD／BDD 驗收條件與檔案範圍計畫、協調實作及審查、解釋結果 | 不可核准自己的工作或合併至 `main` |
| 實作者（Implementer） | 預設 Codex；個別任務可由 Claude Code 擔任 | 實作一項已核准任務、更新測試、執行檢查並準備 Review 資料 | 留在核准範圍與分支內，不可默默變更需求 |
| 獨立審查者（Independent Reviewer） | 預設 Claude Code；Claude 實作時交換角色 | 唯讀審查規格、PR Diff、檢查證據、資安及回歸風險 | 不實作自己審查的變更，也不核准或執行合併 |

Codex 與 Claude Code 之間目前採人工交接：提供已核准規格、PR 連結、Diff、檢查結果及審查問題。Reviewer 只能審查，不能修改檔案。未收到實際審查結果前，不可宣稱完成獨立 Review。「整合」是彙整 Agent 交付成果，不代表有權合併至 `main`。

## 每項功能的開發關卡

1. **規格導向開發（SDD）：**記錄目標、範圍、不做事項、架構／API／資料決策、相依性及驗收條件；有長期影響的設計決策，視需要記錄為 ADR（Architecture Decision Record）。
2. **行為導向開發（BDD）：**使用 Given／When／Then 描述主要使用情境與可觀察結果，涵蓋錯誤、權限、逾時及誤用案例；將相關情境放進 Review handoff。
3. **評估導向開發（EDD）：**功能包含 LLM 或 Agent 行為時，實作前定義版本化且每次執行固定的資料集，並設定可量測檢查，例如任務成功、工具呼叫正確性、引用依據、權限、延遲及成本。無關功能不需 EDD。
4. **測試導向開發（TDD）：**程式碼行為先寫失敗測試（Red），完成最小實作（Green），再重構（Refactor）。外部模型／Provider 呼叫使用可重現的 Mock。
5. **風險導向驗證（Risk-driven validation）：**優先檢查授權邊界、Prompt Injection、惡意工具輸出、機密／資料外洩、模型／Provider 故障、重試冪等性及高影響操作。
6. **獨立審查（Independent review）：**Reviewer 依 Critical／Major／Minor 回報發現，附檔案、行號、證據、影響及建議修正。Critical／Major 必須修正或由使用者明確接受風險，再由 Reviewer 複審；Minor 可修正或回覆。

## 三個人工關卡與 PR Review 循環

1. **H1 規格／計畫：**使用者核准非瑣碎任務的規格與計畫，之後 Codex 才開始實作。
2. **Codex 實作與本機檢查：**Codex 完成實作、自我審查與相關本機檢查，整理本機 Diff 給使用者。
3. **H2 PR 發布：**使用者審查本機 Diff 與檢查結果，授權 Commit、Push 及建立 Pull Request。H2 也涵蓋核准範圍內同一分支／PR 的後續修正提交，但每輪更新仍先提供 Diff 給使用者檢視；範圍變更須重新走 H1。
4. **Claude Code 審查 PR：**PR 建立後，Claude Code 唯讀檢查 PR Diff、規格、測試／CI 證據、資安、回歸風險及範圍膨脹。Review handoff 必須要求 Critical／Major／Minor 分級，並提供檔案、行號、證據及建議修正。
5. **修正循環：**Codex 處理 Reviewer 發現並執行相關檢查，將修正 Diff 提供使用者檢視後更新同一個 PR，再請 Claude Code 複審。Critical／Major 必須修正或由使用者明確接受風險。若修正改變核准範圍，先重新取得 H1 核准。
6. **H3 合併：**使用者確認 Review 意見已處理、CI／EDD 結果及未解風險後，明確核准合併至 `main`。不可自動合併。F01 CI 建立前，H3 依據已記錄的本機檢查，並將缺少 CI 列為已知限制。

## Review handoff 範本

```text
功能／Issue：
已核准的規格與驗收條件：
Pull Request 連結與分支：
修改檔案：
測試與檢查結果：
BDD 情境與風險導向檢查：
EDD 資料集與結果（適用時）：
前次 Reviewer 發現與處理方式（已修正／使用者接受風險／已回覆）：
已知限制與未解風險：
請審查：規格符合度、正確性、資安、回歸問題及範圍膨脹。
請依 Critical／Major／Minor 回報，附檔案、行號、證據及建議修正。
請勿編輯檔案或核准／執行合併。
```

## F01 CI 範圍

F01 是 Roadmap Phase 1（Foundation），建立 GitHub Actions PR CI 與可重現的 Docker Compose 本機啟動。初期檢查包括 Java 21 建置／編譯、單元測試、已設定的格式／靜態品質檢查、Markdown／文件檢查，以及依賴與機密掃描。資料庫與應用路徑建立後再加入整合測試；Agent 行為出現後加入 EDD。模型測試須可重現，不得要求付費模型 API 或 Repository secrets。

CI 使用 `pull_request`（不可使用 `pull_request_target`）及 `permissions: contents: read`。每個必要檢查都必須對每個 PR 回報結果；不可使用 Workflow 層級路徑篩選，讓必要檢查停留 Pending。設定 `main` Branch Protection／Ruleset 要求指定檢查通過，並驗證失敗檢查會阻擋合併。若平台無法強制，記錄限制。

F01 不部署至 staging 或 production。應用可執行後再評估建置／發布套件。未來部署須使用明確環境；production 需要人工核准，不可自動部署。

## Git 與發布規則

- 維持 `main` 穩定，使用短期 `feature`／`fix`／`docs`／`refactor` 分支；不建立長期 `develop`。
- Commit、Push 及建立 PR 必須先取得 H2 授權。
- 必要 CI 檢查通過、Review 發現已處理後，仍須由使用者核准合併。
- 不向 Agent 提供正式環境機密；採最小權限及範圍明確的測試憑證。
- 執行刪除、不可逆、影響正式環境或對外可見的操作前，先向使用者說明並取得核准。
