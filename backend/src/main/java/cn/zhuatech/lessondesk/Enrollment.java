// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 购买套餐的不可变价格快照、预留核销分钟和原款余额。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "enrollment")
public class Enrollment extends RecordBase {
  @Column(nullable = false)
  public Long studentId;

  @Column(nullable = false)
  public Long planId;

  @Column(nullable = false, length = 40)
  public String reference = "";

  @Column(nullable = false, length = 120)
  public String planName = "";

  @Column(nullable = false, length = 60)
  public String category = "";

  @Column(nullable = false)
  public int totalMinutes;

  @Column(nullable = false)
  public int heldMinutes;

  @Column(nullable = false)
  public int spentMinutes;

  @Column(nullable = false)
  public long priceMinor;

  @Column(nullable = false)
  public long paidMinor;

  @Column(nullable = false)
  public long refundedMinor;

  @Column(nullable = false, length = 3)
  public String currency = "";

  @Column(nullable = false)
  public int cancelHours;

  @Column(nullable = false)
  public boolean lateCharge;

  @Column(nullable = false)
  public LocalDate startsOn;

  @Column(nullable = false)
  public LocalDate endsOn;

  @Column(nullable = false, length = 20)
  public String status = "";

  @Column(nullable = false, length = 1000)
  public String note = "";

  @Column(nullable = false)
  public Instant createdAt;
}
