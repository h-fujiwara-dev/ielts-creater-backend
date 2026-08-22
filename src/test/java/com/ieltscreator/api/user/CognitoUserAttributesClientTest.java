package com.ieltscreator.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

@ExtendWith(MockitoExtension.class)
class CognitoUserAttributesClientTest {

  @Mock private CognitoIdentityProviderClient cognitoClient;

  private CognitoUserAttributesClient client() {
    return new CognitoUserAttributesClient(cognitoClient);
  }

  @Test
  void mapsEmailAndNameAttributesToDisplayName() {
    when(cognitoClient.getUser(any(GetUserRequest.class)))
        .thenReturn(
            GetUserResponse.builder()
                .userAttributes(
                    AttributeType.builder().name("email").value("user@example.com").build(),
                    AttributeType.builder().name("name").value("Test User").build())
                .build());

    CognitoUserAttributes attributes = client().fetch("access-token");

    assertThat(attributes.email()).isEqualTo("user@example.com");
    assertThat(attributes.displayName()).isEqualTo("Test User");

    ArgumentCaptor<GetUserRequest> requestCaptor = ArgumentCaptor.forClass(GetUserRequest.class);
    Mockito.verify(cognitoClient).getUser(requestCaptor.capture());
    assertThat(requestCaptor.getValue().accessToken()).isEqualTo("access-token");
  }

  @Test
  void returnsNullDisplayNameWhenNameAttributeIsMissing() {
    when(cognitoClient.getUser(any(GetUserRequest.class)))
        .thenReturn(
            GetUserResponse.builder()
                .userAttributes(
                    AttributeType.builder().name("email").value("nameless@example.com").build())
                .build());

    CognitoUserAttributes attributes = client().fetch("access-token");

    assertThat(attributes.email()).isEqualTo("nameless@example.com");
    assertThat(attributes.displayName()).isNull();
  }
}
