// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 套餐目录；购买时冻结分钟数、价格和取消规则。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "lesson_plan")
public class Plan extends RecordBase {
  @Column(nullable = false, length = 120)
  public String name = "";

  @Column(nullable = false, length = 60)
  public String category = "";

  @Column(nullable = false)
  public int totalMinutes;

  @Column(nullable = false)
  public long priceMinor;

  @Column(nullable = false, length = 3)
  public String currency = "";

  @Column(nullable = false)
  public int validDays;

  @Column(nullable = false)
  public int cancelHours;

  @Column(nullable = false)
  public boolean lateCharge;

  @Column(nullable = false)
  public boolean enabled = true;
}
