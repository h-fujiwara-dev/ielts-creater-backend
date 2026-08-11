package com.ieltscreator.api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * {@code app.auth.mode=cognito}固定の結合テスト共通基盤。JWKSはローカルのMockWebServerで提供し、 実際のAmazon
 * Cognitoへは通信しない。GetUser API呼び出し（CognitoUserAttributesClient）は
 * 実際に通信すると失敗するため、これを使う個別テストで{@code @MockitoBean}により置き換えること。
 *
 * <p>Postgresコンテナはno-auth固定のAbstractIntegrationTestとは別の静的コンテナを持つ
 * （app.auth.modeが異なりSpringコンテキストが別になるため、同一パターンで独立させる）。
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractCognitoIntegrationTest {

  protected static final String CLIENT_ID = "test-client-id";
  protected static final String OTHER_CLIENT_ID = "other-client-id";

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final MockWebServer JWKS_SERVER = new MockWebServer();
  private static final RSAKey RSA_JWK = generateRsaJwk();

  static {
    POSTGRES.start();
    startJwksServer();
  }

  @DynamicPropertySource
  static void cognitoProperties(DynamicPropertyRegistry registry) {
    registry.add("app.auth.mode", () -> "cognito");
    registry.add("app.auth.cognito.issuer-uri", AbstractCognitoIntegrationTest::issuerUri);
    registry.add("app.auth.cognito.client-id", () -> CLIENT_ID);
    registry.add("app.auth.cognito.region", () -> "ap-northeast-1");
  }

  protected static String issuerUri() {
    return "http://localhost:" + JWKS_SERVER.getPort();
  }

  /** 指定のsubject/client_id/有効期限でRS256署名済みのCognitoアクセストークン風JWTを生成する。 */
  protected static String signedAccessToken(String subject, String clientId, Instant expiresAt) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(subject)
              .issuer(issuerUri())
              .claim("token_use", "access")
              .claim("client_id", clientId)
              .issueTime(Date.from(Instant.now()))
              .expirationTime(Date.from(expiresAt))
              .build();
      SignedJWT signedJwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_JWK.getKeyID()).build(), claims);
      signedJwt.sign(new RSASSASigner(RSA_JWK));
      return signedJwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("failed to sign test JWT", e);
    }
  }

  private static void startJwksServer() {
    try {
      JWKS_SERVER.start();
      String jwksBody =
          new ObjectMapper()
              .writeValueAsString(Map.of("keys", List.of(RSA_JWK.toPublicJWK().toJSONObject())));
      JWKS_SERVER.setDispatcher(
          new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
              return new MockResponse()
                  .setResponseCode(200)
                  .setHeader("Content-Type", "application/json")
                  .setBody(jwksBody);
            }
          });
    } catch (Exception e) {
      throw new IllegalStateException("failed to start mock JWKS server", e);
    }
  }

  private static RSAKey generateRsaJwk() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
          .privateKey((RSAPrivateKey) keyPair.getPrivate())
          .keyID("test-key")
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("failed to generate RSA key pair", e);
    }
  }
}
