// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 分钟流水；预留、释放和核销均追加，不删除旧流水。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "credit_entry")
public class CreditEntry extends RecordBase {
  @Column(nullable = false)
  public Long enrollmentId;

  @Column(nullable = true)
  public Long lessonId;

  @Column(nullable = false)
  public int heldDelta;

  @Column(nullable = false)
  public int spentDelta;

  @Column(nullable = false, length = 40)
  public String action = "";

  @Column(nullable = false, length = 60)
  public String actor = "";

  @Column(nullable = false, length = 1000)
  public String note = "";

  @Column(nullable = false)
  public Instant createdAt;
}
