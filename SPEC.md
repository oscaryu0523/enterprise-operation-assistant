# SPEC.md — F01 專案骨架與 CI

> **Feature ID：** F01  
> **版本：** 0.1  
> **狀態：** H1 已核准（2026-11-22）  
> **作者：** Claude Code PM／架構師  
> **建立日期：** 2026-11-22  
> **產品規格來源：** [docs/PROJECT-SPEC.zh-TW.md](docs/PROJECT-SPEC.zh-TW.md)（v1.2）

---

## 1. 背景與目標

### 1.1 背景

本 Feature 是 Enterprise Operation Assistant（EOA）的 **Foundation Phase**，目的是在後續所有功能模組開發前，建立穩固、可重現、CI 自動驗證的專案骨架。  
沒有骨架，後續 F02–F07 無法在相同基準上並行開發、驗收與合併。

### 1.2 目標

1. 本機 `docker compose up` 一鍵啟動應用程式，健康檢查回傳 200。
2. GitHub Actions CI 在每個 PR 自動執行 Build、Unit Test、Lint，任一失敗阻止合併。
3. `main` 設有 Branch Protection：必須通過 CI、必須經 PR，管理員亦不可略過。
4. 新進開發者依 README 能在 30 分鐘內完成本機啟動並執行測試。
5. 為後續 F02 身分驗證與 F03 RAG 預留模組邊界，但 F01 本身不實作業務邏輯。

### 1.3 不在本次範圍

- 任何業務功能（身分、RAG、Agent、工具整合）。
- 正式環境部署或 CD Pipeline。
- 多租戶或外部服務憑證。

---

## 2. 使用情境

### 2.1 Persona

| Persona | 需求 |
|---|---|
| 新進工程師 | clone repo → 看 README → `docker compose up` → 測試通過 |
| Codex（Implementer） | 在 feature branch 實作後，PR CI 自動驗證，Claude Code QA 有明確驗收基準 |
| Claude Code QA | 有可執行的測試命令，能獨立驗收 SPEC.md 與 PR Diff |
| Human（Product Owner） | 能用 `git log` 與 CI 狀態確認 F01 完成，放心進入 F02 |

---

## 3. BDD 驗收條件

### 3.1 健康檢查

```
Given  應用程式已透過 docker compose up 啟動
When   HTTP GET /actuator/health
Then   回傳 HTTP 200，body 包含 {"status": "UP"}
```

### 3.2 CI 阻止合併

```
Given  一個 PR 的 Unit Test 失敗
When   CI 執行完畢
Then   GitHub 顯示 required check 失敗，合併按鈕被停用
```

### 3.3 CI 允許合併

```
Given  PR 的 Build、Unit Test、Lint 均通過
When   CI 執行完畢
Then   required checks 全部綠燈，可由人類合併
```

### 3.4 本機測試

```
Given  開發者在本機執行 ./mvnw test（或等效命令）
When   測試執行完畢
Then   所有測試通過，無 Flaky test，退出碼為 0
```

### 3.5 Branch Protection

```
Given  有人試圖直接 push 到 main
When   push 命令發出
Then   GitHub 拒絕，要求透過 PR
```

### 3.6 環境變數安全

```
Given  專案原始碼被 clone
When   搜尋任何真實憑證或 API Key
Then   找不到；僅有佔位值（如 PLACEHOLDER）或範例值
```

---

## 4. 技術架構與選型

### 4.1 技術基準

| 元件 | 版本 | 說明 |
|---|---|---|
| Java | 21 LTS | 官方支援版本 |
| Spring Boot | 4.x（最新穩定） | 實作前以官方文件核對確切版本 |
| Maven Wrapper | `mvnw` 內建 | 不依賴本機 Maven 安裝 |
| Docker / Docker Compose | 最新穩定 | 容器化本機啟動 |
| PostgreSQL | 16 | F01 僅建立連線設定，不建 Schema |
| GitHub Actions | `ubuntu-latest`，固定 SHA | 見 CI 設計 |

### 4.2 目錄結構（預期）

```
enterprise-operation-assistant/
├── AGENTS.md
├── SPEC.md                      ← 本文件
├── README.md
├── docker-compose.yml
├── .github/
│   ├── workflows/
│   │   └── ci.yml
│   └── PULL_REQUEST_TEMPLATE.md
├── src/
│   ├── main/java/...
│   │   └── EoaApplication.java
│   └── test/java/...
│       └── EoaApplicationTests.java
└── docs/
    ├── PROJECT-SPEC.zh-TW.md
    ├── agent-roles.md
    └── DEVELOPMENT-WORKFLOW.md
```

### 4.3 CI 設計

- Trigger：`pull_request`（禁止用 `pull_request_target`）
- Permissions：`contents: read`（最小權限）
- 不使用 Repository Secrets（F01 不需要外部服務）
- 所有 `uses:` Action 固定完整 Commit SHA，不使用浮動 Tag
- Required checks（Branch Protection 設定）：`build-and-test`
- 步驟：Checkout → Setup Java 21 → Cache Maven → `mvnw verify`

### 4.4 Branch Protection 設定

- 保護分支：`main`
- 要求 PR：是
- Required status checks：`build-and-test`（CI Job 名稱）
- 管理員也必須遵守：是
- 允許 Force Push：否

---

## 5. API 與資料模型

F01 無業務 API。僅暴露：

| Endpoint | 方法 | 說明 |
|---|---|---|
| `/actuator/health` | GET | Spring Boot Actuator 健康檢查 |

資料庫：F01 建立 Docker Compose PostgreSQL 服務及連線設定，不執行 Migration；Migration 策略（Flyway）留給 F02。

---

## 6. 錯誤行為

| 情境 | 預期行為 |
|---|---|
| PostgreSQL 未啟動 | 應用程式啟動失敗，日誌輸出明確錯誤訊息 |
| 測試中有任一失敗 | `mvnw test` 退出碼非 0，CI Job 標記失敗 |
| Lint 格式問題 | CI 失敗並回報違反規則的檔案與行號 |

---

## 7. 安全與品質要求

- **Secrets**：不得提交任何正式環境金鑰；Docker Compose 使用 `PLACEHOLDER` 佔位值。
- **Lint**：統一程式碼格式（Checkstyle 或 Spotless，實作前確認工具版本）。
- **Dependency Scan**：F01 CI 加入 OWASP Dependency Check 或等效工具（若執行時間可接受），否則作為 Known Limitation 記錄，於 F02 補上。
- **測試穩定性**：不允許 Flaky test 進 main。

---

## 8. 非功能需求

| 指標 | 目標 | 說明 |
|---|---|---|
| CI 執行時間 | ≤ 5 分鐘 | F01 無複雜整合測試 |
| 本機啟動時間 | ≤ 2 分鐘（docker compose up） | 含 PostgreSQL 啟動 |
| README 可重現性 | 陌生人 30 分鐘內完成本機啟動 | H1 驗收基準之一 |

---

## 9. 完成定義（DoD）

下列全部達成，F01 才算完成：

- [ ] `docker compose up` 後 `/actuator/health` 回傳 HTTP 200
- [ ] `./mvnw test` 本機執行通過
- [ ] GitHub Actions CI `build-and-test` Job 在 PR 自動觸發並通過
- [ ] `main` Branch Protection 已設定，direct push 被拒絕，CI 為 required check
- [ ] PR Template 存在（`.github/PULL_REQUEST_TEMPLATE.md`）
- [ ] README 包含：環境需求、啟動步驟、測試命令、目錄說明
- [ ] 無任何正式環境憑證提交至版控
- [ ] SPEC.md、AGENTS.md、docs/ 文件與實作一致
- [ ] Claude Code QA 在全新對話逐條驗收，無 Critical／Major 未處理項目
- [ ] 人類（H3）明確核准合併

---

## 10. 已知限制與假設

| 項目 | 說明 |
|---|---|
| Dependency Scan | F01 若 CI 時間不足，Dependency Check 列為 Known Limitation，F02 補上 |
| PostgreSQL Schema | F01 只建連線，Schema Migration（Flyway）於 F02 實作 |
| 業務邏輯 | F01 完全沒有；健康檢查是唯一端點 |
| Spring Boot 版本 | 草稿使用「最新穩定 4.x」，Codex 實作前以官方 release notes 核對確切版本與相容性 |

---

## 11. 已確認決策

| 決策項目 | 選擇 | 理由 |
|---|---|---|
| Lint 工具 | **Spotless** | 支援 `spotless:apply` 自動修正；CI 用 `spotless:check`；Agent 開發流程中格式一致性更易維護 |
| Dependency Scan | **F01 列為 Known Limitation，F02 補上** | OWASP Dependency Check 冷啟動需下載 ~300MB 資料庫，超過 CI ≤5 分鐘目標；F02 引入 Spring Security 後一起設定更有意義 |
| Human Reviewer | **要求 1 位人類核准（`required_approving_review_count: 1`）** | 防止 Agent 自行合併，符合 H3 關卡精神；你本人即可核准 |

---

*本 SPEC.md 為 F01 規格，已通過人類 H1 核准（2026-11-22）。*  
*Codex 可依此規格開始實作。需求或驗收範圍變更時，請回到本文件修訂並重新核准。*
