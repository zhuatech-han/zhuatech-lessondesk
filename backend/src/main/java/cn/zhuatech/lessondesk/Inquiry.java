// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 公开询盘和工作人员跟进，私有联系人不出现在公开响应。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "inquiry")
public class Inquiry extends RecordBase {
  @Column(nullable = false, length = 40)
  public String reference = "";

  @Column(nullable = false, length = 120)
  public String name = "";

  @Column(nullable = false, length = 120)
  public String contact = "";

  @Column(nullable = false, length = 1000)
  public String note = "";

  @Column(nullable = false, length = 20)
  public String status = "";

  @Column(nullable = false, length = 1000)
  public String followup = "";

  @Column(nullable = true)
  public Long studentId;

  @Column(nullable = false)
  public Instant createdAt;
}
