# SDD → BDD → TDD 操作指南

三層方法論在本專案的具體寫法與流程，作為 `DEVELOPMENT-WORKFLOW.md` 的操作補充。

---

## 為什麼三層缺一不可

```
SDD → 定義「做什麼」與「不做什麼」（規格、邊界、驗收條件）
  ↓
BDD → 把規格轉成「可觀察的行為」（Given/When/Then）
  ↓
TDD → 把行為變成「會失敗的測試」，再驅動最小實作
```

| 缺少哪層 | 後果 |
|---|---|
| 無 SDD | 測試與實作的方向正確，但範圍混亂、反覆改需求 |
| 無 BDD | 有規格但「通過」沒有共識，Agent 自己定義完成標準 |
| 無 TDD | 驗收條件存在但靠手測，回歸問題抓不住、CI 無法自動阻擋 |

三層同時存在，每次 PR 才能自動驗證實作是否真的符合規格。

---

## 第一層：SDD（規格導向開發）

### 誰來寫
Claude Code PM／架構師，同一個規格對話，產出根目錄 `SPEC.md`。

### SPEC.md 必要段落

```
1. 背景與目標（為什麼做、解決什麼問題）
2. 使用情境（Persona 與操作流程）
3. BDD 驗收條件（Given/When/Then，見下方格式）
4. 技術架構與選型（版本、目錄結構、CI 設計）
5. API 與資料模型（Endpoint、欄位、Schema）
6. 錯誤行為（每種失敗情境的預期結果）
7. 安全與品質要求
8. 非功能需求（延遲、啟動時間、CI 時間）
9. 完成定義 DoD（Checkbox 清單）
10. 已知限制與假設
11. 已確認決策（取代「待決問題」）
```

### 規格品質檢查清單（Claude Code PM 自查）

- [ ] 每個驗收條件可以被測試（有輸入、有預期輸出）
- [ ] 涵蓋負向情境（錯誤、權限不足、逾時、空結果）
- [ ] 不做事項明確列出（防止 Codex 偷偷擴大範圍）
- [ ] 技術選型有版本（不寫「最新版」）
- [ ] 無含糊標準（「回答要聰明」不算驗收條件）

---

## 第二層：BDD（行為導向開發）

### Given / When / Then 格式

```
Given  [系統或使用者的前置狀態]
When   [觸發事件或操作]
Then   [可觀察的預期結果]
And    [額外的預期結果，可選]
```

### 必須涵蓋的情境類型

| 類型 | 說明 | 範例關鍵字 |
|---|---|---|
| 正常流程 | 標準輸入、標準輸出 | 使用者有權限、資料存在 |
| 錯誤輸入 | 格式錯誤、缺少欄位 | 空字串、null、超長輸入 |
| 權限邊界 | 有權 vs 無權 | 角色不符、Token 過期 |
| 邊界值 | 最大／最小合法值 | 0 筆結果、最大分頁 |
| 外部失敗 | 下游服務掛掉 | DB 離線、模型 Timeout |
| 安全情境 | 注入、越權嘗試 | Prompt Injection、IDOR |

### 範例（F02 身分驗證）

```
# 正常流程
Given  使用者提供有效帳號與密碼
When   POST /api/auth/login
Then   回傳 HTTP 200，body 包含 access_token 與 expires_in

# 錯誤輸入
Given  使用者提供錯誤密碼
When   POST /api/auth/login
Then   回傳 HTTP 401，body 包含 error: "invalid_credentials"
And    不得洩漏帳號是否存在

# 權限邊界
Given  使用者 Token 已過期
When   GET /api/conversations
Then   回傳 HTTP 401，body 包含 error: "token_expired"

# 安全情境
Given  使用者嘗試存取其他使用者的對話
When   GET /api/conversations/{other_user_id}
Then   回傳 HTTP 403，不得回傳任何對話內容
```

### BDD 條件與測試的對應關係

每條 BDD 驗收條件必須對應到至少一個自動化測試：

| BDD 情境類型 | 對應測試層級 | Spring Boot 測試工具 |
|---|---|---|
| 正常流程（API 層） | Integration Test | `@SpringBootTest` + `MockMvc` |
| 錯誤輸入（API 層） | Integration Test | `@SpringBootTest` + `MockMvc` |
| 權限邊界 | Integration Test | `@SpringBootTest` + Spring Security Test |
| 業務邏輯 | Unit Test | JUnit 5 + Mockito |
| 外部服務失敗 | Unit Test | Mockito stub |
| 資料庫互動 | Integration Test | Testcontainers（F02 起） |
| Agent 行為 | Eval（EDD） | evals/dataset.jsonl（F04 起） |

---

## 第三層：TDD（測試導向開發）

### Red → Green → Refactor 循環

```
1. RED    寫一個對應 BDD 條件的失敗測試
           → 執行：./mvnw test -Dtest=TargetTest
           → 確認測試因「功能未實作」而失敗，不是因為寫錯測試

2. GREEN  寫最小實作讓測試通過
           → 不多寫、不提前抽象
           → 執行：./mvnw test -Dtest=TargetTest
           → 確認測試通過

3. REFACTOR 在測試保護下整理程式碼
           → 重新命名、抽取方法、消除重複
           → 執行：./mvnw test（全部測試）
           → 確認全部仍通過
```

### 測試檔位置規範

```
src/
├── main/java/com/example/eoa/
│   ├── auth/
│   │   ├── AuthController.java
│   │   └── AuthService.java
│   └── ...
└── test/java/com/example/eoa/
    ├── auth/
    │   ├── AuthControllerTest.java      ← Integration Test（MockMvc）
    │   └── AuthServiceTest.java         ← Unit Test（Mockito）
    └── ...
```

規則：測試檔與被測程式碼同套件路徑，檔名加 `Test` 後綴。

### 測試命名規範

```java
// 格式：methodName_situation_expectedResult
@Test
void login_withValidCredentials_returnsToken() { ... }

@Test
void login_withInvalidPassword_returns401() { ... }

@Test
void getConversation_withOtherUsersId_returns403() { ... }
```

### Codex 每次提交前的自查

```
□ 每條 SPEC.md BDD 條件都有對應測試
□ 測試先失敗後通過（不是一開始就通過）
□ ./mvnw test 本機全部通過
□ ./mvnw spotless:check 格式通過
□ 沒有刪測試或弱化斷言來讓 CI 變綠
□ 沒有修改 SPEC.md 的驗收條件
□ 沒有引入 SPEC.md 範圍外的新功能或依賴
```

---

## 三層整合：一個 Feature 的完整流程

```
[SDD] Claude Code PM 寫 SPEC.md
         ↓ H1 人類核准
[BDD] SPEC.md 第 3 節填入 Given/When/Then
（Claude Code PM 自查：覆蓋率、負向情境）
         ↓
[TDD] Codex 建立 feature branch
      → 寫失敗測試（對應每條 BDD）
      → 最小實作
      → 重構
      → ./mvnw test 全過
         ↓ H2 人類核准
      Commit + Push + PR
         ↓
[QA]  全新 Claude Code 對話
      → SPEC.md + PR Diff + CI 結果
      → 逐條驗收，PR 留 inline comment
         ↓ H3 人類核准
      合併到 main
```

---

## 常見錯誤與修正

| 錯誤 | 修正 |
|---|---|
| BDD 寫「系統應該聰明地回答」 | 改成可量測的預期輸出（HTTP 狀態碼、body 欄位、DB 紀錄） |
| TDD 先寫實作再補測試 | 測試必須先於實作存在且失敗，才算真正 TDD |
| 測試只覆蓋正常流程 | 每個功能至少一個錯誤情境、一個權限邊界測試 |
| Codex 改了驗收條件讓測試通過 | 驗收條件只能由 PM 修改並重新 H1 核准 |
| QA 只說 LGTM | 必須逐條對照 SPEC.md DoD，有問題留 PR inline comment |
