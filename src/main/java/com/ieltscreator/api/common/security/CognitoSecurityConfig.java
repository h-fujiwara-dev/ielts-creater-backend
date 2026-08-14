package com.ieltscreator.api.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase2（Cognito認証）向け。JWKSによるJWT署名検証に加え、CognitoアクセストークンがOIDC標準の
 * audクレームを持たないことに対応するためtoken_use/client_idの独自検証を追加する（API一覧.md 3章）。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class CognitoSecurityConfig {

  @Bean
  public SecurityFilterChain cognitoFilterChain(
      HttpSecurity http, JwtDecoder cognitoJwtDecoder, CognitoAuthenticationEntryPoint entryPoint)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.decoder(cognitoJwtDecoder))
                    .authenticationEntryPoint(entryPoint));
    return http.build();
  }

  @Bean
  public JwtDecoder cognitoJwtDecoder(
      @Value("${app.auth.cognito.issuer-uri}") String issuerUri,
      @Value("${app.auth.cognito.client-id}") String clientId) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(issuerUri + "/.well-known/jwks.json").build();

    OAuth2TokenValidator<Jwt> validator =
        new DelegatingOAuth2TokenValidator<>(
            List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(issuerUri),
                new CognitoTokenUseValidator(),
                new CognitoClientIdValidator(clientId)));
    decoder.setJwtValidator(validator);
    return decoder;
  }

  @Bean
  public CognitoAuthenticationEntryPoint cognitoAuthenticationEntryPoint(
      ObjectMapper objectMapper) {
    return new CognitoAuthenticationEntryPoint(objectMapper);
  }
}
