# SPEC.md — F02 身分驗證與 RBAC

> **Feature ID：** F02  
> **版本：** 0.1  
> **狀態：** H1 已核准（2026-11-22）  
> **作者：** Claude Code PM／架構師  
> **建立日期：** 2026-11-22  
> **前置 Feature：** F01（已合併，Spring Boot 4.2.0 / Java 21 骨架就緒）  
> **產品規格來源：** [docs/PROJECT-SPEC.zh-TW.md](docs/PROJECT-SPEC.zh-TW.md) §4.4 身分與存取控制

---

## 1. 背景與目標

### 1.1 背景

F01 建立了可運行的骨架，但任何端點目前都沒有保護。F02 的目標是在 F03（RAG）與 F04（Agent）之前，讓每個 API 都知道「誰在呼叫、有沒有權限呼叫」。

沒有身分基礎，後續模組無法做權限過濾（RAG 文件隔離）、工具控管（Agent 只能呼叫有授權的工具）及稽核紀錄（誰做了什麼操作）。

### 1.2 目標

1. 使用者可以用帳號密碼登入，取得 JWT Access Token。
2. 受保護端點驗證 Token，無效或缺少 Token 回傳 401。
3. 角色（`ROLE_USER`、`ROLE_ADMIN`）控制端點存取，權限不足回傳 403。
4. 每次登入與存取拒絕都寫入 Audit Log（DB 資料表）。
5. 密碼使用 BCrypt 雜湊，不以明文儲存或記錄。

### 1.3 不在本次範圍

- OAuth2 / SSO / 第三方登入。
- Refresh Token 機制（F02 只做 Access Token）。
- 使用者自助註冊（帳號由管理員或 seed data 建立）。
- 細緻的資源層級權限（Resource-level ABAC），留給後續 Feature。
- 前端 UI。

---

## 2. 使用情境

| Persona | 情境 |
|---|---|
| 一般員工（`ROLE_USER`）| 登入 → 取得 Token → 呼叫受保護 API |
| 管理員（`ROLE_ADMIN`）| 登入 → 存取管理端點（`/api/admin/**`） |
| 未登入者 | 呼叫受保護 API → 401 |
| 一般員工存取管理端點 | → 403 |

---

## 3. BDD 驗收條件

### 3.1 登入成功

```
Given  資料庫中有帳號 user@example.com，角色 ROLE_USER
When   POST /api/auth/login  { "email": "user@example.com", "password": "correct" }
Then   HTTP 200
And    body 包含 access_token（非空字串）
And    body 包含 expires_in（正整數，秒）
And    body 包含 token_type: "Bearer"
And    Audit Log 新增一筆 event: LOGIN_SUCCESS, user: user@example.com
```

### 3.2 密碼錯誤

```
Given  資料庫中有帳號 user@example.com
When   POST /api/auth/login  { "email": "user@example.com", "password": "wrong" }
Then   HTTP 401
And    body 包含 error: "invalid_credentials"
And    body 不得包含帳號是否存在的資訊（防帳號列舉）
And    Audit Log 新增一筆 event: LOGIN_FAILED, user: user@example.com
```

### 3.3 帳號不存在

```
Given  資料庫中沒有 unknown@example.com
When   POST /api/auth/login  { "email": "unknown@example.com", "password": "any" }
Then   HTTP 401
And    body 與 3.2 相同錯誤訊息（不得洩漏帳號不存在）
And    回應時間與帳號存在時的差異 < 50ms（防時序攻擊）
```

### 3.4 有效 Token 存取受保護端點

```
Given  使用者持有有效 JWT Token（ROLE_USER）
When   GET /api/conversations  Authorization: Bearer <token>
Then   HTTP 200（或 204 若無資料）
And    不得回傳其他使用者的資料
```

### 3.5 無 Token 存取受保護端點

```
Given  請求未帶 Authorization header
When   GET /api/conversations
Then   HTTP 401
And    body 包含 error: "unauthorized"
```

### 3.6 Token 過期

```
Given  使用者持有已過期的 JWT Token
When   GET /api/conversations  Authorization: Bearer <expired_token>
Then   HTTP 401
And    body 包含 error: "token_expired"
```

### 3.7 ROLE_USER 存取管理端點

```
Given  使用者持有有效 JWT Token（ROLE_USER）
When   GET /api/admin/users  Authorization: Bearer <token>
Then   HTTP 403
And    body 包含 error: "forbidden"
And    Audit Log 新增一筆 event: ACCESS_DENIED
```

### 3.8 ROLE_ADMIN 存取管理端點

```
Given  使用者持有有效 JWT Token（ROLE_ADMIN）
When   GET /api/admin/users  Authorization: Bearer <token>
Then   HTTP 200
```

### 3.9 密碼不以明文儲存

```
Given  管理員透過 seed data 建立帳號
When   查詢資料庫 users 資料表的 password_hash 欄位
Then   值以 "$2a$" 開頭（BCrypt 格式）
And    原始密碼無法從 password_hash 直接還原
```

### 3.10 健康檢查不需要驗證

```
Given  任何請求（無 Token）
When   GET /actuator/health
Then   HTTP 200（白名單端點，不受 Spring Security 保護）
```

---

## 4. 技術架構與選型

### 4.1 新增依賴（需加入 pom.xml）

| 依賴 | 用途 | 版本策略 |
|---|---|---|
| `spring-boot-starter-security` | Spring Security 核心 | 由 Spring Boot 4.2.0 BOM 管理 |
| `spring-boot-starter-data-jpa` | JPA / Hibernate | 由 BOM 管理 |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | JWT 產生與驗證 | 實作前確認與 Spring Boot 4.2.0 相容的穩定版本 |
| `flyway-core` | DB Migration | 由 BOM 管理 |
| `spring-security-test` | 測試用 | 由 BOM 管理 |
| `testcontainers` / `testcontainers-postgresql` | 整合測試用真實 DB | 實作前確認版本 |

### 4.2 資料模型

```sql
-- Migration: V1__create_users_and_audit_log.sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    event       VARCHAR(100) NOT NULL,  -- LOGIN_SUCCESS, LOGIN_FAILED, ACCESS_DENIED
    user_email  VARCHAR(255),
    ip_address  VARCHAR(45),
    details     TEXT,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### 4.3 API 設計

| Method | Path | 保護 | 說明 |
|---|---|---|---|
| POST | `/api/auth/login` | 公開 | 登入，回傳 JWT |
| GET | `/api/admin/users` | `ROLE_ADMIN` | 管理用（F02 只做骨架，回傳空陣列即可） |
| GET | `/api/conversations` | `ROLE_USER` | 對話列表（F03 實作內容，F02 只驗證授權） |
| GET | `/actuator/health` | 公開（白名單） | F01 既有端點 |

### 4.4 JWT 設定

- 演算法：HS256（MVP 階段，Production 建議改 RS256）
- 有效期：1 小時（可透過 `application.yml` 設定）
- Payload：`sub`（email）、`role`、`iat`、`exp`
- Secret：從環境變數 `JWT_SECRET` 讀取，不寫死在程式碼，測試用 `application-test.yml` 設定固定值

### 4.5 目錄結構（新增部分）

```
src/
├── main/java/com/enterprise/operationassistant/
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       └── LoginResponse.java
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthFilter.java
│   │   └── UserDetailsServiceImpl.java
│   ├── user/
│   │   ├── User.java           (JPA Entity)
│   │   └── UserRepository.java
│   └── audit/
│       ├── AuditLog.java       (JPA Entity)
│       ├── AuditLogRepository.java
│       └── AuditService.java
├── main/resources/
│   ├── application.yml         (新增 JWT / Security 設定)
│   └── db/migration/
│       └── V1__create_users_and_audit_log.sql
└── test/java/com/enterprise/operationassistant/
    ├── auth/
    │   ├── AuthControllerTest.java   ← 測試骨架已建立
    │   └── AuthServiceTest.java      ← 測試骨架已建立
    └── security/
        └── JwtServiceTest.java       ← 測試骨架已建立
```

---

## 5. 錯誤行為

| 情境 | HTTP | body error 欄位 |
|---|---|---|
| 無 Token | 401 | `unauthorized` |
| Token 格式錯誤 | 401 | `invalid_token` |
| Token 過期 | 401 | `token_expired` |
| 密碼錯誤 / 帳號不存在 | 401 | `invalid_credentials` |
| 權限不足 | 403 | `forbidden` |
| 請求 body 格式錯誤 | 400 | `bad_request` |

所有錯誤回應格式統一：
```json
{ "error": "<code>", "message": "<human-readable>" }
```

---

## 6. 安全要求

- 密碼一律 BCrypt（`BCryptPasswordEncoder`，cost factor ≥ 10）。
- JWT Secret 長度 ≥ 256 bit，不得硬編碼，不得出現在 Log。
- 登入失敗與成功的回應時間應一致（防時序攻擊）。
- Audit Log 記錄 IP，但不記錄密碼或 Token 明文。
- `/actuator/**` 除 `/actuator/health` 外，全部需要 `ROLE_ADMIN`。

---

## 7. 非功能需求

| 指標 | 目標 |
|---|---|
| 登入 API P95 延遲 | ≤ 500ms（本機 Docker Compose） |
| CI 執行時間 | ≤ 8 分鐘（含 Testcontainers 整合測試） |
| 測試覆蓋率（auth / security 套件） | ≥ 80%（行覆蓋率） |

---

## 8. 完成定義（DoD）

- [ ] `V1__create_users_and_audit_log.sql` Migration 執行成功
- [ ] Seed data：至少一個 `ROLE_USER`、一個 `ROLE_ADMIN` 帳號（BCrypt 雜湊）
- [ ] BDD 3.1–3.10 每條都有對應自動化測試且通過
- [ ] `./mvnw test` 本機全部通過（含 Testcontainers）
- [ ] `./mvnw spotless:check` 通過
- [ ] GitHub Actions CI 綠燈
- [ ] 資料庫 `password_hash` 欄位無明文密碼
- [ ] `JWT_SECRET` 未出現在任何提交的程式碼或 Log
- [ ] Audit Log 在登入成功、失敗、存取拒絕時均寫入
- [ ] Claude Code QA（全新對話）逐條驗收，無 Critical／Major
- [ ] 人類 H3 核准合併

---

## 9. 已知限制

| 項目 | 說明 |
|---|---|
| Refresh Token | F02 不實作，Access Token 1 小時後需重新登入 |
| 細緻資源權限 | F02 只做角色層級，Resource-level 留後續 Feature |
| `/api/conversations` | F02 只驗授權，回傳空陣列，F03 實作內容 |
| `/api/admin/users` | F02 只驗授權，回傳空陣列，管理功能留後續 |
| Testcontainers 冷啟動 | CI 首次執行下載 image 較慢，後續有 layer cache |

---

## 10. 已確認決策

| 決策 | 選擇 | 理由 |
|---|---|---|
| JWT 演算法 | HS256 | MVP 夠用；Production 升 RS256 時只改 `JwtService` |
| Token 有效期 | 1 小時，可設定 | 平衡安全與體驗；Refresh Token 後續再加 |
| 整合測試 DB | Testcontainers PostgreSQL | 與 Production 環境一致，避免 H2 假陽性 |
| Migration 工具 | Flyway | Spring Boot 官方整合，版本化 SQL |

---

*本 SPEC.md 為 F02 規格，已通過人類 H1 核准（2026-11-22）。Codex 可依此規格開始實作。需求或驗收範圍變更時，請回到本文件修訂並重新核准。*
