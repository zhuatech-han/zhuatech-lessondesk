// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 学员绑定本人账号；联系人仅授权岗位与本人读取。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "student")
public class Student extends RecordBase {
  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false, length = 120)
  public String name = "";

  @Column(nullable = false, length = 120)
  public String contact = "";

  @Column(nullable = false, length = 1000)
  public String note = "";

  @Column(nullable = false)
  public boolean enabled = true;
}
