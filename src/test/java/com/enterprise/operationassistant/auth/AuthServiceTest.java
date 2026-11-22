package com.enterprise.operationassistant.auth;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD 驗收條件對應：SPEC.md F02 §3.1–3.3、3.9（業務邏輯層）
 *
 * <p>測試策略：Unit Test，Mockito stub UserRepository 與 JwtService 不啟動 Spring Context，速度快
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private Object userRepository; // TODO: 替換為 UserRepository

  @Mock private Object jwtService; // TODO: 替換為 JwtService

  @Mock private Object auditService; // TODO: 替換為 AuditService

  @InjectMocks private Object authService; // TODO: 替換為 AuthService

  // ─── 登入成功：回傳 token ─────────────────────────────────────────────────

  @Test
  @DisplayName("login：帳號密碼正確 → 回傳含 token 的 LoginResponse")
  void login_withValidCredentials_returnsLoginResponse() {
    fail("not implemented");
    // TODO:
    // stub userRepository.findByEmail("user@example.com") → 有效 User（BCrypt hash）
    // stub jwtService.generateToken(user) → "mock.jwt.token"
    // 呼叫 authService.login(request)
    // 驗證：response.accessToken() == "mock.jwt.token"
    //       response.tokenType() == "Bearer"
    //       response.expiresIn() > 0
    //       verify auditService.log(LOGIN_SUCCESS) 被呼叫一次
  }

  // ─── 密碼錯誤：拋出例外 ───────────────────────────────────────────────────

  @Test
  @DisplayName("login：密碼錯誤 → 拋出 AuthenticationException")
  void login_withWrongPassword_throwsException() {
    fail("not implemented");
    // TODO:
    // stub userRepository.findByEmail(...) → 有效 User
    // 密碼 BCrypt 比對失敗
    // 驗證：拋出 AuthenticationException（或自訂例外）
    //       verify auditService.log(LOGIN_FAILED) 被呼叫一次
    //       verify jwtService.generateToken() 從未被呼叫
  }

  // ─── 帳號不存在：同一例外，不洩漏差異 ───────────────────────────────────

  @Test
  @DisplayName("login：帳號不存在 → 拋出與密碼錯誤相同的例外型別")
  void login_withUnknownEmail_throwsSameExceptionAsWrongPassword() {
    fail("not implemented");
    // TODO:
    // stub userRepository.findByEmail(...) → Optional.empty()
    // 驗證：拋出與密碼錯誤相同的例外型別（不是 UsernameNotFoundException）
    //       例外訊息不包含 "not found" 或 "does not exist"
  }

  // ─── 密碼以 BCrypt 雜湊驗證，不還原明文 ─────────────────────────────────

  @Test
  @DisplayName("3.9 密碼比對：使用 BCrypt，不以明文比較")
  void login_passwordVerification_usesBCrypt() {
    fail("not implemented");
    // TODO:
    // 驗證 authService 使用 BCryptPasswordEncoder.matches()
    // 而非直接比較字串（verify passwordEncoder.matches() 被呼叫）
  }
}
