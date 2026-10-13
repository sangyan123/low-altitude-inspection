package com.drone.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 单元测试：令牌签发、解析、校验与异常场景。
 */
class JwtTokenProviderTest {

    private static final String SECRET = "UnitTestDataSecretKeyForJwtSigningMustBeLongEnough256Bits!!";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", SECRET);
        ReflectionTestUtils.setField(provider, "expiration", 60_000L);
    }

    @Test
    @DisplayName("签发后可解析出正确的用户ID与用户名")
    void generate_then_parse_userId_and_username() {
        String token = provider.generateToken(42L, "alice");

        assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("有效令牌校验通过")
    void valid_token_passes_validation() {
        String token = provider.generateToken(1L, "bob");

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("篡改签名的令牌校验失败")
    void tampered_token_fails_validation() {
        String token = provider.generateToken(1L, "bob");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("垃圾字符串校验失败且不抛异常")
    void garbage_token_fails_validation() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("过期令牌校验失败")
    void expired_token_fails_validation() {
        ReflectionTestUtils.setField(provider, "expiration", -1000L);
        String token = provider.generateToken(1L, "bob");

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("密钥不一致的令牌校验失败")
    void token_signed_with_other_key_fails_validation() {
        String token = provider.generateToken(1L, "bob");
        JwtTokenProvider other = new JwtTokenProvider();
        ReflectionTestUtils.setField(other, "secret", "AnotherSecretKeyForJwtSigningMustBeLongEnough256Bits!!");
        ReflectionTestUtils.setField(other, "expiration", 60_000L);

        assertThat(other.validateToken(token)).isFalse();
    }
}
