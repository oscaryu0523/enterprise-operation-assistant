package com.enterprise.operationassistant.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * BDD 驗收條件對應：SPEC.md F02 §3.1–3.8、3.10
 *
 * 測試策略：@SpringBootTest + MockMvc（Integration Test）
 * 資料庫：Testcontainers PostgreSQL（profile: test）
 * Seed data：由 application-test.yml 或 @Sql 載入
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ─── 3.1 登入成功 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("3.1 登入成功：回傳 200 與 JWT token")
    void login_withValidCredentials_returns200AndToken() {
        fail("not implemented");
        // TODO:
        // POST /api/auth/login { email: "user@example.com", password: "correct" }
        // 驗證：HTTP 200
        //       body.access_token 非空
        //       body.expires_in 為正整數
        //       body.token_type == "Bearer"
    }

    @Test
    @DisplayName("3.1 登入成功：寫入 LOGIN_SUCCESS Audit Log")
    void login_withValidCredentials_writesAuditLog() {
        fail("not implemented");
        // TODO:
        // POST /api/auth/login 成功後
        // 查詢 audit_log 資料表，確認有 event=LOGIN_SUCCESS, user_email=user@example.com
    }

    // ─── 3.2 密碼錯誤 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("3.2 密碼錯誤：回傳 401 invalid_credentials")
    void login_withWrongPassword_returns401() {
        fail("not implemented");
        // TODO:
        // POST /api/auth/login { email: "user@example.com", password: "wrong" }
        // 驗證：HTTP 401
        //       body.error == "invalid_credentials"
        //       body 不包含 "exists" 或 "not found"（防帳號列舉）
    }

    @Test
    @DisplayName("3.2 密碼錯誤：寫入 LOGIN_FAILED Audit Log")
    void login_withWrongPassword_writesAuditLog() {
        fail("not implemented");
        // TODO: audit_log 有 event=LOGIN_FAILED, user_email=user@example.com
    }

    // ─── 3.3 帳號不存在 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("3.3 帳號不存在：回傳與密碼錯誤相同的 401 訊息")
    void login_withUnknownEmail_returnsSameErrorAsWrongPassword() {
        fail("not implemented");
        // TODO:
        // POST /api/auth/login { email: "unknown@example.com", password: "any" }
        // 驗證：HTTP 401，body.error == "invalid_credentials"
        //       回應訊息與 3.2 完全相同（防帳號列舉）
    }

    // ─── 3.4 有效 Token 存取受保護端點 ───────────────────────────────────────

    @Test
    @DisplayName("3.4 有效 Token：存取 /api/conversations 回傳 200")
    void getConversations_withValidToken_returns200() {
        fail("not implemented");
        // TODO:
        // 先登入取得 token
        // GET /api/conversations  Authorization: Bearer <token>
        // 驗證：HTTP 200（內容為空陣列即可，F03 實作內容）
    }

    // ─── 3.5 無 Token ────────────────────────────────────────────────────────

    @Test
    @DisplayName("3.5 無 Token：回傳 401 unauthorized")
    void getConversations_withoutToken_returns401() {
        fail("not implemented");
        // TODO:
        // GET /api/conversations（無 Authorization header）
        // 驗證：HTTP 401，body.error == "unauthorized"
    }

    // ─── 3.6 Token 過期 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("3.6 Token 過期：回傳 401 token_expired")
    void getConversations_withExpiredToken_returns401() {
        fail("not implemented");
        // TODO:
        // 產生一個 exp 在過去的 JWT（測試用 JwtService helper）
        // GET /api/conversations  Authorization: Bearer <expired_token>
        // 驗證：HTTP 401，body.error == "token_expired"
    }

    // ─── 3.7 ROLE_USER 存取管理端點 ──────────────────────────────────────────

    @Test
    @DisplayName("3.7 ROLE_USER 存取 /api/admin/users：回傳 403 forbidden")
    void getAdminUsers_withUserRole_returns403() {
        fail("not implemented");
        // TODO:
        // 以 ROLE_USER token 存取 GET /api/admin/users
        // 驗證：HTTP 403，body.error == "forbidden"
        //       audit_log 有 event=ACCESS_DENIED
    }

    // ─── 3.8 ROLE_ADMIN 存取管理端點 ─────────────────────────────────────────

    @Test
    @DisplayName("3.8 ROLE_ADMIN 存取 /api/admin/users：回傳 200")
    void getAdminUsers_withAdminRole_returns200() {
        fail("not implemented");
        // TODO:
        // 以 ROLE_ADMIN token 存取 GET /api/admin/users
        // 驗證：HTTP 200（內容為空陣列即可）
    }

    // ─── 3.10 健康檢查白名單 ──────────────────────────────────────────────────

    @Test
    @DisplayName("3.10 /actuator/health 不需要 Token：回傳 200")
    void actuatorHealth_withoutToken_returns200() {
        fail("not implemented");
        // TODO:
        // GET /actuator/health（無 Authorization header）
        // 驗證：HTTP 200
    }
}
