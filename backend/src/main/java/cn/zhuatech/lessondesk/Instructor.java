// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 教练账号、教学类别与静态课间缓冲。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "instructor")
public class Instructor extends RecordBase {
  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false, length = 120)
  public String name = "";

  @Column(nullable = false, length = 500)
  public String category = "";

  @Column(nullable = false)
  public int bufferMinutes;

  @Column(nullable = false)
  public boolean enabled = true;
}
