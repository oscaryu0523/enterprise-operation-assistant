package com.enterprise.operationassistant.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * BDD 驗收條件對應：SPEC.md F02 §3.4、3.6（JWT 產生與驗證）
 *
 * 測試策略：Unit Test，不需要 Spring Context
 * JWT Secret：測試固定值（application-test.yml 設定）
 */
class JwtServiceTest {

    // TODO: 建立 JwtService 實例（注入測試用 secret 與 expiration）
    // private JwtService jwtService = new JwtService("test-secret-256bit-xxxxxxxxxxxxxxxx", 3600);

    // ─── Token 產生 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken：產生非空 JWT，包含正確的 sub 與 role")
    void generateToken_containsCorrectClaims() {
        fail("not implemented");
        // TODO:
        // 給定 email="user@example.com", role="ROLE_USER"
        // 呼叫 jwtService.generateToken(email, role)
        // 解析 JWT，驗證：
        //   claims.subject == "user@example.com"
        //   claims.get("role") == "ROLE_USER"
        //   claims.expiration 在未來
    }

    @Test
    @DisplayName("generateToken：token 不包含密碼或敏感欄位")
    void generateToken_doesNotContainSensitiveData() {
        fail("not implemented");
        // TODO:
        // 產生 token 後解析 payload
        // 驗證 claims 不包含 password、secret 等欄位
    }

    // ─── Token 驗證：有效 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken：有效 token → 回傳 true 並可取出 email")
    void validateToken_withValidToken_returnsTrue() {
        fail("not implemented");
        // TODO:
        // 產生 token → 驗證 → true
        // extractEmail(token) == "user@example.com"
    }

    // ─── Token 驗證：過期 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("3.6 validateToken：過期 token → 拋出 TokenExpiredException")
    void validateToken_withExpiredToken_throwsException() {
        fail("not implemented");
        // TODO:
        // 建立 expiration = -1秒（已過期）的 token
        // 呼叫 jwtService.validateToken(expiredToken)
        // 驗證拋出 TokenExpiredException（或 jjwt 的 ExpiredJwtException）
    }

    // ─── Token 驗證：格式錯誤 ─────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken：格式錯誤的 token → 拋出 InvalidTokenException")
    void validateToken_withMalformedToken_throwsException() {
        fail("not implemented");
        // TODO:
        // 傳入 "not.a.jwt"
        // 驗證拋出 InvalidTokenException（或 jjwt 的 MalformedJwtException）
    }

    // ─── Token 驗證：簽章被竄改 ──────────────────────────────────────────────

    @Test
    @DisplayName("validateToken：簽章被竄改的 token → 拋出例外")
    void validateToken_withTamperedSignature_throwsException() {
        fail("not implemented");
        // TODO:
        // 產生有效 token，修改最後一段 signature
        // 驗證拋出 SignatureException 或自訂例外
    }
}
