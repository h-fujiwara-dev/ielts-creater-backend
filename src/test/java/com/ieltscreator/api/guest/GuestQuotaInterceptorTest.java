package com.ieltscreator.api.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class GuestQuotaInterceptorTest {

  private static final String GUEST_CLIENT_ID = "guest-client-id";

  @Mock private GuestIpQuotaRepository guestIpQuotaRepository;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void skipsNonPostRequests() {
    when(request.getMethod()).thenReturn("GET");
    authenticateAsGuest();

    assertThat(interceptor().preHandle(request, response, new Object())).isTrue();
    verify(guestIpQuotaRepository, never()).incrementAndGetCount(anyString(), any());
  }

  @Test
  void skipsRequestsWithoutGuestAuthentication() {
    when(request.getMethod()).thenReturn("POST");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("someone", "n/a"));

    assertThat(interceptor().preHandle(request, response, new Object())).isTrue();
    verify(guestIpQuotaRepository, never()).incrementAndGetCount(anyString(), any());
  }

  @Test
  void prefersXClientRealIpHeaderSetByApiGateway() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader("X-Client-Real-Ip")).thenReturn("198.51.100.42");
    when(guestIpQuotaRepository.incrementAndGetCount(eq("198.51.100.42"), any())).thenReturn(1);
    authenticateAsGuest();

    assertThat(interceptor().preHandle(request, response, new Object())).isTrue();
  }

  @Test
  void fallsBackToForwardedForFirstAddressWhenRealIpHeaderMissing() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader("X-Client-Real-Ip")).thenReturn(null);
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
    when(guestIpQuotaRepository.incrementAndGetCount(eq("203.0.113.5"), any())).thenReturn(2);
    authenticateAsGuest();

    assertThat(interceptor().preHandle(request, response, new Object())).isTrue();
  }

  @Test
  void rejectsGuestRequestOverLimit() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader("X-Client-Real-Ip")).thenReturn(null);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("198.51.100.9");
    when(guestIpQuotaRepository.incrementAndGetCount(eq("198.51.100.9"), any())).thenReturn(4);
    authenticateAsGuest();

    assertThatThrownBy(() -> interceptor().preHandle(request, response, new Object()))
        .isInstanceOf(RateLimitExceededException.class);
  }

  private GuestQuotaInterceptor interceptor() {
    return new GuestQuotaInterceptor(guestIpQuotaRepository, GUEST_CLIENT_ID, 3);
  }

  private void authenticateAsGuest() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("client_id", GUEST_CLIENT_ID)
            .subject("guest-sub")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
