package com.ieltscreator.api.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.common.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** JWT検証失敗時に他の例外系と共通のErrorResponse形式で401を返す。認証エラー発生パスをWARNでログ出力 する（API一覧.md 4章 ロギング方針）。 */
@Slf4j
@RequiredArgsConstructor
public class CognitoAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    log.warn(
        "Authentication failed for {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        authException.getMessage());
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    ErrorResponse body = ErrorResponse.of("UNAUTHORIZED", "Invalid or missing access token.");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
