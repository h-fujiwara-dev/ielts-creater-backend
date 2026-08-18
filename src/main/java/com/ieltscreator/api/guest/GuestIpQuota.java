package com.ieltscreator.api.guest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ゲスト（#00056）の問題生成に対するIPアドレス単位・日次のリクエスト数カウンタ。
 * GuestIpQuotaRepository#incrementAndGetCountの原子的なUPSERTでのみ更新され、 アプリケーションコードから直接saveされることはない。
 */
@Entity
@Table(name = "guest_ip_quota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuestIpQuota {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "ip_address", nullable = false, length = 45)
  private String ipAddress;

  @Column(name = "usage_date", nullable = false)
  private LocalDate usageDate;

  @Column(name = "request_count", nullable = false)
  private Integer requestCount;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
