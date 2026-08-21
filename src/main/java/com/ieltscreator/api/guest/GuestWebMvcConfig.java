package com.ieltscreator.api.guest;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class GuestWebMvcConfig implements WebMvcConfigurer {

  private final GuestQuotaInterceptor guestQuotaInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(guestQuotaInterceptor).addPathPatterns("/api/v1/question-sets");
  }
}
