// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 人工核实的原收款和原款退款，不代表网关实际交易。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "money_entry")
public class MoneyEntry extends RecordBase {
  @Column(nullable = false)
  public Long enrollmentId;

  @Column(nullable = true)
  public Long originalId;

  @Column(nullable = false)
  public long amountMinor;

  @Column(nullable = false, length = 20)
  public String kind = "";

  @Column(nullable = false, length = 120)
  public String reference = "";

  @Column(nullable = false, length = 60)
  public String actor = "";

  @Column(nullable = false, length = 1000)
  public String note = "";

  @Column(nullable = false)
  public Instant createdAt;
}
