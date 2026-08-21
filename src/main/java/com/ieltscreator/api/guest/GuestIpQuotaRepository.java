package com.ieltscreator.api.guest;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestIpQuotaRepository extends JpaRepository<GuestIpQuota, UUID> {

  /**
   * (ip_address, usage_date)の行を原子的にUPSERTし、加算後のrequest_countを返す。
   * 複数のゲストリクエストが同一IPから同時に届いても競合なくカウントできるよう、 アプリケーション側でのSELECT→比較→UPDATEではなくDB側のINSERT ... ON
   * CONFLICTに寄せている。
   */
  @Query(
      value =
          """
          INSERT INTO guest_ip_quota (ip_address, usage_date, request_count, updated_at)
          VALUES (:ipAddress, :usageDate, 1, now())
          ON CONFLICT (ip_address, usage_date)
          DO UPDATE SET request_count = guest_ip_quota.request_count + 1, updated_at = now()
          RETURNING request_count
          """,
      nativeQuery = true)
  int incrementAndGetCount(
      @Param("ipAddress") String ipAddress, @Param("usageDate") LocalDate usageDate);

  void deleteByUsageDateBefore(LocalDate cutoff);
}
