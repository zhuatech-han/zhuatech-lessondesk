// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 教练车的类别、编号与启停，检修另记时间窗口。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "vehicle")
public class Vehicle extends RecordBase {
  @Column(nullable = false, length = 120)
  public String name = "";

  @Column(nullable = false, length = 24)
  public String plate = "";

  @Column(nullable = false, length = 60)
  public String category = "";

  @Column(nullable = false)
  public boolean enabled = true;
}
