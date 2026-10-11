// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.persistence.*;
import java.time.*;

/** 一名学员、一名教练、一辆车；冻结预约时间、时长和缓冲。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "lesson")
public class Lesson extends RecordBase {
  @Column(nullable = false)
  public Long enrollmentId;

  @Column(nullable = false)
  public Long studentId;

  @Column(nullable = false)
  public Long instructorId;

  @Column(nullable = false)
  public Long vehicleId;

  @Column(nullable = false, length = 40)
  public String reference = "";

  @Column(nullable = false, length = 120)
  public String studentName = "";

  @Column(nullable = false, length = 120)
  public String instructorName = "";

  @Column(nullable = false, length = 120)
  public String vehicleName = "";

  @Column(nullable = false, length = 60)
  public String category = "";

  @Column(nullable = false)
  public Instant startsAt;

  @Column(nullable = false)
  public Instant endsAt;

  @Column(nullable = false)
  public Instant blockedUntil;

  @Column(nullable = false)
  public int minutes;

  @Column(nullable = false)
  public int bufferMinutes;

  @Column(nullable = false, length = 20)
  public String status = "";

  @Column(nullable = false)
  public int actualMinutes;

  @Column(nullable = false, length = 200)
  public String topic = "";

  @Column(nullable = false, length = 1000)
  public String progress = "";

  @Column(nullable = false, length = 1000)
  public String note = "";

  @Column(nullable = false)
  public Instant createdAt;
}
