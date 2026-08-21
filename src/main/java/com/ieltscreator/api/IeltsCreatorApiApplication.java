package com.ieltscreator.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// GuestDataCleanupService（#00056）の@Scheduledバッチを有効化する
@EnableScheduling
@SpringBootApplication
public class IeltsCreatorApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(IeltsCreatorApiApplication.class, args);
  }
}
