package com.ieltscreator.api.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Phase1（認証なし）向け。Cognito JWT検証（Phase2, R-2）が実装されるまでの暫定設定。 */
@Configuration
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "no-auth")
public class NoAuthSecurityConfig {

  @Bean
  public SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
    return http.build();
  }
}
