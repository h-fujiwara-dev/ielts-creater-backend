package com.ieltscreator.api.guest;

import com.ieltscreator.api.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ゲスト（#00056）の問題生成（POST /api/v1/question-sets）に対してのみ、IPアドレス単位・日次のクォータを適用する。
 * 通常ユーザーのユーザーID単位の日次上限（QuestionSetGenerationService）とは独立しており、
 * 共有デモアカウントに対してはそちらはバイパスされるため実質こちらが唯一の生成回数制限になる。
 */
@Component
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class GuestQuotaInterceptor implements HandlerInterceptor {

  private final GuestIpQuotaRepository guestIpQuotaRepository;
  private final String guestClientId;
  private final int dailyIpLimit;

  public GuestQuotaInterceptor(
      GuestIpQuotaRepository guestIpQuotaRepository,
      @Value("${app.guest.cognito.client-id:}") String guestClientId,
      @Value("${app.guest.daily-ip-limit:3}") int dailyIpLimit) {
    this.guestIpQuotaRepository = guestIpQuotaRepository;
    this.guestClientId = guestClientId;
    this.dailyIpLimit = dailyIpLimit;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!"POST".equalsIgnoreCase(request.getMethod()) || !isGuestRequest()) {
      return true;
    }

    String ipAddress = resolveClientIp(request);
    int countAfterThisRequest =
        guestIpQuotaRepository.incrementAndGetCount(ipAddress, LocalDate.now(ZoneOffset.UTC));
    if (countAfterThisRequest > dailyIpLimit) {
      throw new RateLimitExceededException(
          "Daily question set generation limit (%d) for guest access reached for this IP address."
              .formatted(dailyIpLimit));
    }
    return true;
  }

  private boolean isGuestRequest() {
    if (guestClientId.isBlank()) {
      return false;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return false;
    }
    return guestClientId.equals(jwtAuth.getToken().getClaimAsString("client_id"));
  }

  // API Gateway HTTP API(HTTP_PROXY統合) -> VPC Link -> ECSの経路では、request.getRemoteAddr()は
  // VPC LinkのENI/Cloud Map経路のIPを返し、かつX-Forwarded-Forも自動付与されないことを実機確認済み
  // （#00056、dev環境での実測でguest_ip_quotaに記録されたIPがVPC内部アドレスになる不具合を発見）。
  // そのためterraform/modules/api-gateway側でAPI Gateway自身が把握しているクライアントIP
  // （$context.http.sourceIp）をX-Client-Real-Ipヘッダーとして注入させ、これを最優先で使う
  // （overwrite:マッピングのためクライアントからのなりすまし送信は上書きされ信頼できる）。
  // X-Forwarded-Forはローカル開発等、別経路でリバースプロキシを挟む場合のフォールバックとして残す。
  private String resolveClientIp(HttpServletRequest request) {
    String realIp = request.getHeader("X-Client-Real-Ip");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
