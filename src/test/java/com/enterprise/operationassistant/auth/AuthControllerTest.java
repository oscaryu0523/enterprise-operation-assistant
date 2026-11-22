package com.enterprise.operationassistant.auth;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// BDD 驗收條件對應：SPEC.md F02 §3.1–3.8、3.10
// 測試策略：@SpringBootTest + MockMvc（Integration Test）
// 資料庫：Testcontainers PostgreSQL（profile: test）
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:eoa-auth-test;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password="
    })
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private WebApplicationContext applicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  // ─── 3.1 登入成功 ────────────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.1 登入成功：回傳 200 與 JWT token")
  void login_withValidCredentials_returns200AndToken() {
    fail("not implemented");
    // TODO:
    // POST /api/auth/login { email: "user@example.com", password: "correct" }
    // 驗證：HTTP 200，body.access_token 非空，body.token_type == "Bearer"
  }

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.1 登入成功：寫入 LOGIN_SUCCESS Audit Log")
  void login_withValidCredentials_writesAuditLog() {
    fail("not implemented");
    // TODO: 查詢 audit_log，確認有 event=LOGIN_SUCCESS, user_email=user@example.com
  }

  // ─── 3.2 密碼錯誤 ────────────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.2 密碼錯誤：回傳 401 invalid_credentials")
  void login_withWrongPassword_returns401() {
    fail("not implemented");
    // TODO: HTTP 401，body.error == "invalid_credentials"，不洩漏帳號是否存在
  }

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.2 密碼錯誤：寫入 LOGIN_FAILED Audit Log")
  void login_withWrongPassword_writesAuditLog() {
    fail("not implemented");
    // TODO: audit_log 有 event=LOGIN_FAILED, user_email=user@example.com
  }

  // ─── 3.3 帳號不存在 ───────────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.3 帳號不存在：回傳與密碼錯誤相同的 401 訊息")
  void login_withUnknownEmail_returnsSameErrorAsWrongPassword() {
    fail("not implemented");
    // TODO: HTTP 401，body.error == "invalid_credentials"（防帳號列舉）
  }

  // ─── 3.4 有效 Token ───────────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.4 有效 Token：存取 /api/conversations 回傳 200")
  void getConversations_withValidToken_returns200() {
    fail("not implemented");
    // TODO: 登入取得 token → GET /api/conversations Authorization: Bearer <token> → 200
  }

  // ─── 3.5 無 Token ────────────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.5 無 Token：回傳 401 unauthorized")
  void getConversations_withoutToken_returns401() {
    fail("not implemented");
    // TODO: GET /api/conversations（無 header）→ HTTP 401，body.error == "unauthorized"
  }

  // ─── 3.6 Token 過期 ───────────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.6 Token 過期：回傳 401 token_expired")
  void getConversations_withExpiredToken_returns401() {
    fail("not implemented");
    // TODO: 過期 JWT → HTTP 401，body.error == "token_expired"
  }

  // ─── 3.7 ROLE_USER 存取管理端點 ──────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.7 ROLE_USER 存取 /api/admin/users：回傳 403 forbidden")
  void getAdminUsers_withUserRole_returns403() {
    fail("not implemented");
    // TODO: ROLE_USER token → GET /api/admin/users → 403，audit_log 有 ACCESS_DENIED
  }

  // ─── 3.8 ROLE_ADMIN 存取管理端點 ─────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.8 ROLE_ADMIN 存取 /api/admin/users：回傳 200")
  void getAdminUsers_withAdminRole_returns200() {
    fail("not implemented");
    // TODO: ROLE_ADMIN token → GET /api/admin/users → 200
  }

  // ─── 3.10 健康檢查白名單 ──────────────────────────────────────────────────

  @Test
  @Disabled("skeleton — implement in F02")
  @DisplayName("3.10 /actuator/health 不需要 Token：回傳 200")
  void actuatorHealth_withoutToken_returns200() {
    fail("not implemented");
    // TODO: GET /actuator/health（無 header）→ 200
  }
}
