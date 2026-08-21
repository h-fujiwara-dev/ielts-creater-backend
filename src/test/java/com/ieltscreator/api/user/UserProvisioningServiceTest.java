package com.ieltscreator.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

  private static final String GUEST_CLIENT_ID = "guest-client-id";

  @Mock private AppUserRepository appUserRepository;
  @Mock private CognitoUserAttributesClient cognitoUserAttributesClient;

  private UserProvisioningService userProvisioningService;

  @BeforeEach
  void setUp() {
    userProvisioningService =
        new UserProvisioningService(
            appUserRepository, cognitoUserAttributesClient, GUEST_CLIENT_ID);
  }

  @Test
  void returnsExistingUserWithoutCallingCognitoWhenAlreadyProvisioned() {
    AppUser existing =
        AppUser.builder()
            .id(UUID.randomUUID())
            .cognitoSub("sub-123")
            .email("existing@example.com")
            .build();
    when(appUserRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(existing));

    AppUser result = userProvisioningService.provisionFromToken(jwtWithSubject("sub-123"));

    assertThat(result).isEqualTo(existing);
    verify(cognitoUserAttributesClient, never()).fetch(anyString());
  }

  @Test
  void createsUserFromCognitoAttributesOnFirstAccess() {
    when(appUserRepository.findByCognitoSub("sub-456")).thenReturn(Optional.empty());
    when(cognitoUserAttributesClient.fetch("token-sub-456"))
        .thenReturn(new CognitoUserAttributes("new@example.com", "New User"));
    when(appUserRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppUser result = userProvisioningService.provisionFromToken(jwtWithSubject("sub-456"));

    assertThat(result.getCognitoSub()).isEqualTo("sub-456");
    assertThat(result.getEmail()).isEqualTo("new@example.com");
    assertThat(result.getDisplayName()).isEqualTo("New User");
  }

  @Test
  void fallsBackToEmailLocalPartWhenCognitoNameAttributeIsMissing() {
    when(appUserRepository.findByCognitoSub("sub-noname")).thenReturn(Optional.empty());
    when(cognitoUserAttributesClient.fetch("token-sub-noname"))
        .thenReturn(new CognitoUserAttributes("no-name-user@example.com", null));
    when(appUserRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppUser result = userProvisioningService.provisionFromToken(jwtWithSubject("sub-noname"));

    assertThat(result.getDisplayName()).isEqualTo("no-name-user");
  }

  @Test
  void recoversFromConcurrentFirstAccessRaceByReturningExistingRow() {
    AppUser winner =
        AppUser.builder()
            .id(UUID.randomUUID())
            .cognitoSub("sub-789")
            .email("race@example.com")
            .build();
    when(appUserRepository.findByCognitoSub("sub-789"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(winner));
    when(cognitoUserAttributesClient.fetch("token-sub-789"))
        .thenReturn(new CognitoUserAttributes("race@example.com", "Race User"));
    when(appUserRepository.save(any(AppUser.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate cognito_sub"));

    AppUser result = userProvisioningService.provisionFromToken(jwtWithSubject("sub-789"));

    assertThat(result).isEqualTo(winner);
  }

  @Test
  void marksUserAsGuestWhenClientIdMatchesGuestAppClient() {
    when(appUserRepository.findByCognitoSub("sub-guest")).thenReturn(Optional.empty());
    when(cognitoUserAttributesClient.fetch("token-sub-guest"))
        .thenReturn(new CognitoUserAttributes("guest@example.com", "Guest"));
    when(appUserRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppUser result =
        userProvisioningService.provisionFromToken(
            jwtWithSubjectAndClientId("sub-guest", GUEST_CLIENT_ID));

    assertThat(result.isGuest()).isTrue();
  }

  @Test
  void doesNotMarkUserAsGuestWhenClientIdIsForRegularWebClient() {
    when(appUserRepository.findByCognitoSub("sub-regular")).thenReturn(Optional.empty());
    when(cognitoUserAttributesClient.fetch("token-sub-regular"))
        .thenReturn(new CognitoUserAttributes("regular@example.com", "Regular"));
    when(appUserRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppUser result =
        userProvisioningService.provisionFromToken(
            jwtWithSubjectAndClientId("sub-regular", "web-client-id"));

    assertThat(result.isGuest()).isFalse();
  }

  private Jwt jwtWithSubject(String subject) {
    return Jwt.withTokenValue("token-" + subject)
        .header("alg", "RS256")
        .claim("token_use", "access")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private Jwt jwtWithSubjectAndClientId(String subject, String clientId) {
    return Jwt.withTokenValue("token-" + subject)
        .header("alg", "RS256")
        .claim("token_use", "access")
        .claim("client_id", clientId)
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
