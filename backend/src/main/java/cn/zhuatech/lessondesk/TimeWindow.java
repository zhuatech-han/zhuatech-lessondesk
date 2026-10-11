// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 教练工作段、请假和车辆检修的显式时间窗口。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "time_window")
public class TimeWindow extends RecordBase {
  @Column(nullable = true)
  public Long instructorId;

  @Column(nullable = true)
  public Long vehicleId;

  @Column(nullable = false, length = 20)
  public String kind = "";

  @Column(nullable = false)
  public Instant startsAt;

  @Column(nullable = false)
  public Instant endsAt;

  @Column(nullable = false, length = 1000)
  public String note = "";
}
