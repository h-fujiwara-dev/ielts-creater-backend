package com.ieltscreator.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.auth.dto.GuestTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

@ExtendWith(MockitoExtension.class)
class GuestAuthServiceTest {

  @Mock private CognitoIdentityProviderClient cognitoClient;

  private GuestAuthService service;

  @BeforeEach
  void setUp() {
    service =
        new GuestAuthService(cognitoClient, "guest-client-id", "guest@example.com", "P@ssw0rd123");
  }

  @Test
  void issuesTokenUsingUserPasswordAuthFlow() {
    when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
        .thenReturn(
            InitiateAuthResponse.builder()
                .authenticationResult(
                    AuthenticationResultType.builder()
                        .accessToken("access-token")
                        .idToken("id-token")
                        .expiresIn(3600)
                        .build())
                .build());

    GuestTokenResponse response = service.issueToken();

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.idToken()).isEqualTo("id-token");
    assertThat(response.expiresIn()).isEqualTo(3600);

    ArgumentCaptor<InitiateAuthRequest> requestCaptor =
        ArgumentCaptor.forClass(InitiateAuthRequest.class);
    org.mockito.Mockito.verify(cognitoClient).initiateAuth(requestCaptor.capture());
    InitiateAuthRequest sentRequest = requestCaptor.getValue();
    assertThat(sentRequest.authFlow()).isEqualTo(AuthFlowType.USER_PASSWORD_AUTH);
    assertThat(sentRequest.clientId()).isEqualTo("guest-client-id");
    assertThat(sentRequest.authParameters())
        .containsEntry("USERNAME", "guest@example.com")
        .containsEntry("PASSWORD", "P@ssw0rd123");
  }

  @Test
  void wrapsCognitoFailureAsGuestAuthFailedException() {
    when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
        .thenThrow(
            CognitoIdentityProviderException.builder()
                .message("Incorrect username or password.")
                .build());

    assertThatThrownBy(() -> service.issueToken()).isInstanceOf(GuestAuthFailedException.class);
  }
}
