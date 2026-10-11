// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 驾校主数据、预约分钟账和原款收退款完整业务，写入统一加锁。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class LessonService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final Clock clock;
  final CommandService commands;

  public LessonService(
      Store db, AccessService access, AdminService admin, Clock clock, CommandService commands) {
    this.db = db;
    this.access = access;
    this.admin = admin;
    this.clock = clock;
    this.commands = commands;
  }

  boolean staff() {
    return Set.of("ALL", "DEPARTMENT").contains(access.role().scope);
  }

  void org(Long id, String permission) {
    access.require(permission);
    Rules.check(db.get(Department.class, id).enabled, "DEPARTMENT_DISABLED");
    if (!staff() || !access.department(id)) throw new Problem(403, "OUT_OF_SCOPE");
  }

  void version(long v, Object given) {
    Rules.check(v == Rules.id(given), "VERSION_CONFLICT");
  }

  ZoneId zone(Long id) {
    return ZoneId.of(db.get(Department.class, id).zone);
  }

  Student own() {
    return db.query(Student.class, "from Student where accountId=?1", access.current().id).stream()
        .findFirst()
        .orElseThrow(() -> new Problem(403, "STUDENT_PROFILE_REQUIRED"));
  }

  Instructor instructor() {
    return db
        .query(Instructor.class, "from Instructor where accountId=?1", access.current().id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new Problem(403, "INSTRUCTOR_PROFILE_REQUIRED"));
  }

  boolean enabledAccount(Long id) {
    var a = db.get(Account.class, id);
    return a.enabled && db.get(Department.class, a.departmentId).enabled;
  }

  boolean own(Enrollment e) {
    return Objects.equals(db.get(Student.class, e.studentId).accountId, access.current().id);
  }

  boolean visible(Enrollment e) {
    if (!access.department(e.departmentId)) return false;
    return staff()
            && (access.has("enroll")
                || access.has("finance")
                || access.has("students")
                || access.has("schedule"))
        || access.has("portal") && own(e);
  }

  Enrollment enrollment(Long id) {
    var e = db.get(Enrollment.class, id);
    if (!visible(e)) throw new Problem(403, "OUT_OF_SCOPE");
    return e;
  }

  boolean visible(Lesson l) {
    if (!access.department(l.departmentId)) return false;
    return staff() && access.has("lessons")
        || access.has("portal")
            && Objects.equals(db.get(Student.class, l.studentId).accountId, access.current().id)
        || access.has("coach")
            && Objects.equals(
                db.get(Instructor.class, l.instructorId).accountId, access.current().id);
  }

  Lesson lesson(Long id) {
    var l = db.get(Lesson.class, id);
    if (!visible(l)) throw new Problem(403, "OUT_OF_SCOPE");
    return l;
  }

  boolean held(Lesson l) {
    return Set.of("BOOKED", "ARRIVED", "LIVE").contains(l.status);
  }

  String category(Object x) {
    String c = Rules.text(x, 60, true);
    Rules.check(
        db.query(
                    DictionaryEntry.class,
                    "from DictionaryEntry where type='CATEGORY' and code=?1 and enabled=true",
                    c)
                .size()
            == 1,
        "CATEGORY_DISABLED");
    return c;
  }

  /** 教练可有1–8个已启用教学类别，规范排序，已有预约类别不能移除。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  String instructorCategories(Object v) {
    String raw = Rules.text(v, 500, true);
    String[] parts = raw.split(",", -1);
    Rules.check(parts.length >= 1 && parts.length <= 8, "INVALID_INPUT");
    var set = new TreeSet<String>();
    for (String c : parts) set.add(category(c));
    Rules.check(set.size() == parts.length, "INVALID_INPUT");
    return String.join(",", set);
  }

  Long optionalId(Object x) {
    return x == null || Objects.toString(x, "").isBlank() ? null : Rules.id(x);
  }

  Map<String, Object> map(Object... xs) {
    var m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < xs.length; i += 2) m.put(xs[i].toString(), xs[i + 1]);
    return m;
  }

  /** 页面目录经过岗位过滤，不返回他人联系人、账号或财务记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object options() {
    access.require("catalog");
    var depts =
        db.all(Department.class).stream()
            .filter(d -> d.enabled && access.department(d.id))
            .toList();
    var ids = depts.stream().map(d -> d.id).toList();
    var students =
        staff() && (access.has("students") || access.has("enroll") || access.has("schedule"))
            ? db.all(Student.class).stream().filter(s -> ids.contains(s.departmentId)).toList()
            : access.has("portal")
                ? db.query(Student.class, "from Student where accountId=?1", access.current().id)
                : List.<Student>of();
    var coaches =
        db.all(Instructor.class).stream()
            .filter(i -> ids.contains(i.departmentId) && i.enabled && enabledAccount(i.accountId))
            .map(
                i ->
                    map(
                        "id",
                        i.id,
                        "name",
                        i.name,
                        "category",
                        i.category,
                        "departmentId",
                        i.departmentId,
                        "bufferMinutes",
                        i.bufferMinutes,
                        "mine",
                        Objects.equals(i.accountId, access.current().id)))
            .toList();
    var vehicles =
        db.all(Vehicle.class).stream()
            .filter(v -> ids.contains(v.departmentId) && v.enabled)
            .toList();
    var plans =
        db.all(Plan.class).stream().filter(p -> ids.contains(p.departmentId) && p.enabled).toList();
    var accounts =
        staff() && access.has("masters")
            ? db.all(Account.class).stream()
                .filter(a -> ids.contains(a.departmentId) && a.enabled)
                .map(
                    a ->
                        map(
                            "id",
                            a.id,
                            "name",
                            a.displayName,
                            "departmentId",
                            a.departmentId,
                            "student",
                            db.get(AccessRole.class, a.roleId).scope.equals("STUDENT"),
                            "instructor",
                            db.get(AccessRole.class, a.roleId).permissions.contains("coach")
                                && !db.get(AccessRole.class, a.roleId).scope.equals("STUDENT")))
                .toList()
            : List.of();
    return map(
        "departments",
        depts,
        "students",
        students,
        "instructors",
        coaches,
        "vehicles",
        vehicles,
        "plans",
        plans,
        "accounts",
        accounts,
        "categories",
        db.all(DictionaryEntry.class).stream().filter(d -> d.enabled).toList(),
        "now",
        clock.instant(),
        "bookingLeadMinutes",
        admin.setting("bookingLeadMinutes"));
  }

  String masterPermission(String k) {
    return switch (k) {
      case "students" -> "students";
      case "inquiries" -> "inquiries";
      case "instructors", "vehicles", "plans", "windows" -> "masters";
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  Class<? extends RecordBase> type(String k) {
    return switch (k) {
      case "students" -> Student.class;
      case "instructors" -> Instructor.class;
      case "vehicles" -> Vehicle.class;
      case "plans" -> Plan.class;
      case "windows" -> TimeWindow.class;
      case "inquiries" -> Inquiry.class;
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  /** 搜索、状态、分页、排序统一有界，身份与组织过滤先于分页。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object list(String k, String search, String state, int page, int size, String sort) {
    if (page < 0 || size < 1 || size > 100 || !Set.of("newest", "oldest", "time").contains(sort))
      throw new Problem(400, "INVALID_INPUT");
    String q = Rules.text(search, 120, false).toLowerCase(Locale.ROOT);
    state = Rules.text(state, 30, false);
    List<? extends RecordBase> rows;
    if (k.equals("lessons")) {
      if (!(access.has("lessons") || access.has("coach") || access.has("portal")))
        throw new Problem(403, "FORBIDDEN");
      rows = db.all(Lesson.class).stream().filter(this::visible).toList();
    } else if (k.equals("enrollments")) {
      if (!(access.has("enroll") || access.has("finance") || access.has("portal")))
        throw new Problem(403, "FORBIDDEN");
      rows = db.all(Enrollment.class).stream().filter(this::visible).toList();
    } else {
      String perm = masterPermission(k);
      if (k.equals("windows") && access.has("coach") && !staff()) {
        var id = instructor().id;
        rows =
            db.all(TimeWindow.class).stream()
                .filter(w -> Objects.equals(w.instructorId, id))
                .toList();
      } else {
        org(access.current().departmentId, perm);
        rows = db.all(type(k)).stream().filter(x -> access.department(x.departmentId)).toList();
      }
    }
    String s = state;
    var sorted =
        rows.stream()
            .filter(r -> matches(r, q, s))
            .sorted(
                (a, b) ->
                    sort.equals("time") && a instanceof Lesson x && b instanceof Lesson y
                        ? x.startsAt.compareTo(y.startsAt)
                        : sort.equals("oldest") ? a.id.compareTo(b.id) : b.id.compareTo(a.id))
            .toList();
    long from = (long) page * size;
    return map(
        "items",
        from >= sorted.size()
            ? List.of()
            : sorted.subList((int) from, (int) Math.min(from + size, sorted.size())),
        "total",
        sorted.size(),
        "page",
        page,
        "size",
        size);
  }

  boolean matches(RecordBase r, String q, String status) {
    String state =
        r instanceof Lesson l
            ? l.status
            : r instanceof Enrollment e
                ? e.status
                : r instanceof Inquiry i ? i.status : r instanceof TimeWindow w ? w.kind : "";
    String text =
        r instanceof Lesson l
            ? l.reference + " " + l.studentName + " " + l.instructorName + " " + l.vehicleName
            : r instanceof Enrollment e
                ? e.reference + " " + e.planName + " " + db.get(Student.class, e.studentId).name
                : r instanceof Student s
                    ? s.name + " " + s.contact
                    : r instanceof Instructor i
                        ? i.name
                        : r instanceof Vehicle v
                            ? v.name + " " + v.plate
                            : r instanceof Plan p
                                ? p.name
                                : r instanceof Inquiry i
                                    ? i.name + " " + i.contact + " " + i.reference
                                    : r instanceof TimeWindow w ? w.note : "";
    return (status.isEmpty() || status.equals(state)) && text.toLowerCase(Locale.ROOT).contains(q);
  }

  /** 主数据完整写入，已使用身份与组织不能变更；资源有未完成课次不能停用。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object master(String k, Long id, Map<String, Object> b) {
    admin.lock();
    Long dept = id == null ? Rules.id(b.get("departmentId")) : db.get(type(k), id).departmentId;
    boolean self = k.equals("windows") && !staff() && access.has("coach");
    if (!self) org(dept, masterPermission(k));
    else if (!access.department(dept)) throw new Problem(403, "OUT_OF_SCOPE");
    RecordBase r =
        id == null
            ? switch (k) {
              case "students" -> new Student();
              case "instructors" -> new Instructor();
              case "vehicles" -> new Vehicle();
              case "plans" -> new Plan();
              case "windows" -> new TimeWindow();
              case "inquiries" -> new Inquiry();
              default -> throw new Problem(404, "NOT_FOUND");
            }
            : db.get(type(k), id);
    if (id != null) {
      version(r.version, b.get("version"));
      Rules.check(dept.equals(Rules.id(b.get("departmentId"))), "IDENTITY_LOCKED");
    }
    r.departmentId = dept;
    switch (k) {
      case "students" -> {
        var s = (Student) r;
        var a = db.get(Account.class, Rules.id(b.get("accountId")));
        Rules.check(
            a.departmentId.equals(dept)
                && enabledAccount(a.id)
                && db.get(AccessRole.class, a.roleId).scope.equals("STUDENT"),
            "STUDENT_INVALID");
        if (id != null) Rules.check(s.accountId.equals(a.id), "IDENTITY_LOCKED");
        s.accountId = a.id;
        s.name = Rules.text(b.get("name"), 120, true);
        s.contact = Rules.text(b.get("contact"), 120, true);
        s.note = Rules.paragraph(b.get("note"), 1000, false);
        s.enabled = Rules.flag(b.get("enabled"));
        if (!s.enabled) Rules.check(activeResource("student", s.id).isEmpty(), "ACTIVE_LESSONS");
      }
      case "instructors" -> {
        var i = (Instructor) r;
        var a = db.get(Account.class, Rules.id(b.get("accountId")));
        Rules.check(
            a.departmentId.equals(dept)
                && enabledAccount(a.id)
                && db.get(AccessRole.class, a.roleId).permissions.contains("coach")
                && !db.get(AccessRole.class, a.roleId).scope.equals("STUDENT"),
            "INSTRUCTOR_INVALID");
        if (id != null) Rules.check(i.accountId.equals(a.id), "IDENTITY_LOCKED");
        String c = instructorCategories(b.get("category"));
        if (id != null)
          for (var active : activeResource("instructor", i.id))
            Rules.check(Arrays.asList(c.split(",")).contains(active.category), "ACTIVE_LESSONS");
        i.accountId = a.id;
        i.name = Rules.text(b.get("name"), 120, true);
        i.category = c;
        i.bufferMinutes = Rules.integer(b.get("bufferMinutes"), 0, 60);
        i.enabled = Rules.flag(b.get("enabled"));
        if (!i.enabled) Rules.check(activeResource("instructor", i.id).isEmpty(), "ACTIVE_LESSONS");
      }
      case "vehicles" -> {
        var v = (Vehicle) r;
        String c = category(b.get("category"));
        if (id != null && !v.category.equals(c))
          Rules.check(activeResource("vehicle", v.id).isEmpty(), "ACTIVE_LESSONS");
        v.name = Rules.text(b.get("name"), 120, true);
        v.plate = Rules.text(b.get("plate"), 24, true).toUpperCase(Locale.ROOT);
        v.category = c;
        v.enabled = Rules.flag(b.get("enabled"));
        if (!v.enabled) Rules.check(activeResource("vehicle", v.id).isEmpty(), "ACTIVE_LESSONS");
      }
      case "plans" -> {
        var p = (Plan) r;
        p.name = Rules.text(b.get("name"), 120, true);
        p.category = category(b.get("category"));
        p.totalMinutes = Rules.integer(b.get("totalMinutes"), 30, 60000);
        p.priceMinor = Rules.integer(b.get("priceMinor"), 0, 100_000_000);
        p.currency = LessonPolicy.currency(b.get("currency"));
        p.validDays = Rules.integer(b.get("validDays"), 1, 1095);
        p.cancelHours = Rules.integer(b.get("cancelHours"), 0, 72);
        p.lateCharge = Rules.flag(b.get("lateCharge"));
        p.enabled = Rules.flag(b.get("enabled"));
      }
      case "windows" -> {
        var w = (TimeWindow) r;
        window(w, id, b, self);
      }
      case "inquiries" -> {
        var q = (Inquiry) r;
        if (id == null) {
          q.reference =
              "Q-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(Locale.ROOT);
          q.createdAt = clock.instant();
        }
        q.name = Rules.text(b.get("name"), 120, true);
        q.contact = Rules.text(b.get("contact"), 120, true);
        q.note = Rules.paragraph(b.get("note"), 1000, false);
        q.status = Rules.text(b.getOrDefault("status", "NEW"), 20, true);
        Rules.check(
            Set.of("NEW", "CONTACTED", "ENROLLED", "CLOSED").contains(q.status), "STATE_INVALID");
        q.followup = Rules.paragraph(b.get("followup"), 1000, false);
        q.studentId = optionalId(b.get("studentId"));
        if (q.status.equals("ENROLLED")) Rules.check(q.studentId != null, "STUDENT_INVALID");
        if (q.studentId != null)
          Rules.check(
              db.get(Student.class, q.studentId).departmentId.equals(dept), "STUDENT_INVALID");
      }
    }
    if (id == null) db.save(r);
    else r.version++;
    access.audit("MASTER_" + k.toUpperCase(Locale.ROOT), r.id, dept);
    return r;
  }

  List<Lesson> activeResource(String k, Long id) {
    if (id == null) return List.of();
    return db.all(Lesson.class).stream()
        .filter(
            l ->
                held(l)
                    && Objects.equals(
                        k.equals("student")
                            ? l.studentId
                            : k.equals("instructor") ? l.instructorId : l.vehicleId,
                        id))
        .toList();
  }

  void window(TimeWindow w, Long id, Map<String, Object> b, boolean self) {
    Long coach = optionalId(b.get("instructorId")), car = optionalId(b.get("vehicleId"));
    String kind = Rules.text(b.get("kind"), 20, true);
    Rules.check((coach == null) != (car == null), "WINDOW_INVALID");
    Rules.check(
        Set.of("WORK", "BLOCK").contains(kind) && (!kind.equals("WORK") || coach != null),
        "WINDOW_INVALID");
    if (self) {
      Rules.check(Objects.equals(coach, instructor().id) && car == null, "INSTRUCTOR_ONLY");
      if (id != null) Rules.check(Objects.equals(w.instructorId, coach), "INSTRUCTOR_ONLY");
    }
    if (coach != null)
      Rules.check(
          db.get(Instructor.class, coach).departmentId.equals(w.departmentId), "OUT_OF_SCOPE");
    if (car != null)
      Rules.check(db.get(Vehicle.class, car).departmentId.equals(w.departmentId), "OUT_OF_SCOPE");
    Instant start = LessonPolicy.instant(b.get("startsAt")),
        end = LessonPolicy.instant(b.get("endsAt"));
    Rules.check(
        end.isAfter(start) && Duration.between(start, end).compareTo(Duration.ofDays(7)) <= 0,
        "TIME_INVALID");
    if (kind.equals("WORK"))
      Rules.check(
          Duration.between(start, end).compareTo(Duration.ofHours(12)) <= 0, "WINDOW_INVALID");
    // Changes must preserve every already booked instructor work segment.
    if (id != null && w.kind.equals("WORK"))
      for (var l : activeResource("instructor", w.instructorId))
        if (!covered(l.instructorId, l.startsAt, l.blockedUntil, id, null))
          Rules.check(
              Objects.equals(coach, l.instructorId)
                  && kind.equals("WORK")
                  && !start.isAfter(l.startsAt)
                  && !end.isBefore(l.blockedUntil),
              "ACTIVE_LESSONS");
    if (kind.equals("BLOCK"))
      for (var l :
          activeResource(coach != null ? "instructor" : "vehicle", coach != null ? coach : car))
        Rules.check(
            !LessonPolicy.overlap(start, end, l.startsAt, l.blockedUntil), "ACTIVE_LESSONS");
    for (var other : db.all(TimeWindow.class))
      if (!Objects.equals(other.id, id)
          && Objects.equals(other.instructorId, coach)
          && Objects.equals(other.vehicleId, car)
          && other.kind.equals(kind))
        Rules.check(
            !LessonPolicy.overlap(start, end, other.startsAt, other.endsAt), "WINDOW_CONFLICT");
    w.instructorId = coach;
    w.vehicleId = car;
    w.kind = kind;
    w.startsAt = start;
    w.endsAt = end;
    w.note = Rules.paragraph(b.get("note"), 1000, false);
  }

  /** 无业务引用才可删除目录；删除工作段仍保护有效预约。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void delete(String k, Long id, long rev) {
    admin.lock();
    var r = db.get(type(k), id);
    boolean self = k.equals("windows") && !staff() && access.has("coach");
    if (self) {
      Rules.check(
          Objects.equals(((TimeWindow) r).instructorId, instructor().id), "INSTRUCTOR_ONLY");
    } else org(r.departmentId, masterPermission(k));
    version(r.version, rev);
    if (r instanceof TimeWindow w && w.kind.equals("WORK"))
      for (var l : activeResource("instructor", w.instructorId))
        Rules.check(
            covered(l.instructorId, l.startsAt, l.blockedUntil, id, null), "ACTIVE_LESSONS");
    db.delete(r);
    access.audit("DELETE_" + k.toUpperCase(Locale.ROOT), id, r.departmentId);
  }

  /** 冻结报读条件；学员由教务绑定，不自选他人账号。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object enroll(Map<String, Object> b) {
    admin.lock();
    var s = db.get(Student.class, Rules.id(b.get("studentId")));
    org(s.departmentId, "enroll");
    var stamp = commands.begin("ENROLL", b);
    if (stamp.id != null) return enrollmentDetail(stamp.resultId);
    var p = db.get(Plan.class, Rules.id(b.get("planId")));
    Rules.check(
        s.enabled
            && enabledAccount(s.accountId)
            && p.enabled
            && p.departmentId.equals(s.departmentId),
        "PLAN_UNAVAILABLE");
    category(p.category);
    LocalDate date;
    try {
      date = LocalDate.parse(Rules.text(b.get("startsOn"), 10, true));
    } catch (Exception e) {
      throw new Problem(400, "TIME_INVALID");
    }
    var today = clock.instant().atZone(zone(s.departmentId)).toLocalDate();
    Rules.check(!date.isBefore(today) && !date.isAfter(today.plusDays(90)), "TIME_INVALID");
    var e = new Enrollment();
    e.departmentId = s.departmentId;
    e.studentId = s.id;
    e.planId = p.id;
    e.reference = "E-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(Locale.ROOT);
    e.planName = p.name;
    e.category = p.category;
    e.totalMinutes = p.totalMinutes;
    e.priceMinor = p.priceMinor;
    e.currency = p.currency;
    e.cancelHours = p.cancelHours;
    e.lateCharge = p.lateCharge;
    e.startsOn = date;
    e.endsOn = date.plusDays(p.validDays - 1);
    e.status = "OPEN";
    e.note = Rules.paragraph(b.get("note"), 1000, false);
    e.createdAt = clock.instant();
    db.save(e);
    event(e, "ENROLLMENT", "CREATED", e.note);
    commands.complete(stamp, "ENROLLMENT", e.id);
    return enrollmentDetail(e.id);
  }

  void event(RecordBase r, String type, String action, String note) {
    var e = new BusinessEvent();
    e.departmentId = r.departmentId;
    e.objectType = type;
    e.objectId = r.id;
    e.action = action;
    e.actor = access.current().username;
    e.note = note;
    e.createdAt = clock.instant();
    db.save(e);
    access.audit(type + "_" + action, r.id, r.departmentId);
  }

  void credit(Enrollment e, Lesson l, int heldDelta, int spentDelta, String action, String note) {
    e.heldMinutes += heldDelta;
    e.spentMinutes += spentDelta;
    Rules.check(
        e.heldMinutes >= 0
            && e.spentMinutes >= 0
            && e.heldMinutes + e.spentMinutes <= e.totalMinutes,
        "CREDIT_INVARIANT");
    e.version++;
    var c = new CreditEntry();
    c.departmentId = e.departmentId;
    c.enrollmentId = e.id;
    c.lessonId = l.id;
    c.heldDelta = heldDelta;
    c.spentDelta = spentDelta;
    c.action = action;
    c.actor = access.current().username;
    c.note = note;
    c.createdAt = clock.instant();
    db.save(c);
  }

  /** 本人或教务查看分钟余额、原收款与退款上限；教练无全校财务权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object enrollmentDetail(Long id) {
    var e = enrollment(id);
    return map(
        "enrollment",
        e,
        "student",
        db.get(Student.class, e.studentId),
        "availableMinutes",
        e.status.equals("OPEN") ? e.totalMinutes - e.heldMinutes - e.spentMinutes : 0,
        "earnedMinor",
        LessonPolicy.earned(e.priceMinor, e.spentMinutes, e.totalMinutes),
        "refundableMinor",
        Math.max(
            0,
            e.paidMinor
                - e.refundedMinor
                - LessonPolicy.earned(e.priceMinor, e.spentMinutes, e.totalMinutes)),
        "payments",
        db.query(MoneyEntry.class, "from MoneyEntry where enrollmentId=?1 order by id", id),
        "credits",
        db.query(CreditEntry.class, "from CreditEntry where enrollmentId=?1 order by id", id),
        "events",
        db.query(
            BusinessEvent.class,
            "from BusinessEvent where objectType='ENROLLMENT' and objectId=?1 order by id",
            id));
  }

  /** 收款、停止报读与原款退款；载荷、版本和金额都在同一事务检查。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object enrollmentCommand(Long id, String action, Map<String, Object> b) {
    admin.lock();
    var e = enrollment(id);
    org(e.departmentId, action.equals("CLOSE") ? "enroll" : "finance");
    var stamp = commands.begin("ENROLLMENT_" + id + "_" + action, b);
    if (stamp.id != null) return enrollmentDetail(id);
    version(e.version, b.get("version"));
    String note = Rules.paragraph(b.get("note"), 1000, true);
    switch (action) {
      case "COLLECT" -> {
        Rules.check(e.status.equals("OPEN"), "STATE_INVALID");
        long amount = LessonPolicy.minor(b.get("amountMinor"));
        Rules.check(e.paidMinor - e.refundedMinor + amount <= e.priceMinor, "PAYMENT_EXCESS");
        payment(e, "IN", amount, null, b, note);
        e.paidMinor += amount;
      }
      case "CLOSE" -> {
        Rules.check(e.status.equals("OPEN"), "STATE_INVALID");
        Rules.check(e.heldMinutes == 0, "ACTIVE_LESSONS");
        e.status = "CLOSED";
      }
      case "REFUND" -> {
        Rules.check(e.status.equals("CLOSED"), "CLOSE_BEFORE_REFUND");
        long amount = LessonPolicy.minor(b.get("amountMinor"));
        var original = db.get(MoneyEntry.class, Rules.id(b.get("originalId")));
        Rules.check(
            original.enrollmentId.equals(e.id) && original.kind.equals("IN"),
            "ORIGINAL_PAYMENT_INVALID");
        long refunded =
            db.query(MoneyEntry.class, "from MoneyEntry where originalId=?1", original.id).stream()
                .mapToLong(x -> x.amountMinor)
                .sum();
        Rules.check(
            amount <= original.amountMinor - refunded
                && amount
                    <= e.paidMinor
                        - e.refundedMinor
                        - LessonPolicy.earned(e.priceMinor, e.spentMinutes, e.totalMinutes),
            "REFUND_EXCESS");
        payment(e, "OUT", amount, original.id, b, note);
        e.refundedMinor += amount;
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    e.version++;
    event(e, "ENROLLMENT", action, note);
    commands.complete(stamp, "ENROLLMENT", e.id);
    return enrollmentDetail(id);
  }

  void payment(
      Enrollment e, String kind, long amount, Long original, Map<String, Object> b, String note) {
    var p = new MoneyEntry();
    p.departmentId = e.departmentId;
    p.enrollmentId = e.id;
    p.kind = kind;
    p.amountMinor = amount;
    p.originalId = original;
    p.reference = Rules.text(b.get("reference"), 120, true);
    p.actor = access.current().username;
    p.note = note;
    p.createdAt = clock.instant();
    Rules.check(
        db.query(
                MoneyEntry.class,
                "from MoneyEntry where departmentId=?1 and reference=?2",
                e.departmentId,
                p.reference)
            .isEmpty(),
        "PAYMENT_REFERENCE_REUSED");
    db.save(p);
  }

  boolean covered(Long coach, Instant start, Instant end, Long skip, TimeWindow alternative) {
    return db.all(TimeWindow.class).stream()
        .anyMatch(
            w ->
                !Objects.equals(w.id, skip)
                    && Objects.equals(w.instructorId, coach)
                    && w.kind.equals("WORK")
                    && !w.startsAt.isAfter(start)
                    && !w.endsAt.isBefore(end));
  }

  void bookingWindow(Instant start) {
    var now = clock.instant();
    Rules.check(
        !start.isBefore(now.plusSeconds(admin.setting("bookingLeadMinutes") * 60L))
            && !start.isAfter(now.plusSeconds(admin.setting("bookingWindowDays") * 86400L)),
        "BOOKING_WINDOW");
  }

  void validateBooking(
      Enrollment e, Long coachId, Long carId, Instant start, int minutes, int buffer, Long skip) {
    var s = db.get(Student.class, e.studentId);
    var i = db.get(Instructor.class, coachId);
    var v = db.get(Vehicle.class, carId);
    Rules.check(
        e.status.equals("OPEN") && s.enabled && enabledAccount(s.accountId), "STUDENT_INVALID");
    Rules.check(e.paidMinor - e.refundedMinor == e.priceMinor, "PAYMENT_REQUIRED");
    Rules.check(
        i.enabled
            && v.enabled
            && enabledAccount(i.accountId)
            && db.get(AccessRole.class, db.get(Account.class, i.accountId).roleId)
                .permissions
                .contains("coach"),
        "RESOURCE_DISABLED");
    Rules.check(
        i.departmentId.equals(e.departmentId) && v.departmentId.equals(e.departmentId),
        "OUT_OF_SCOPE");
    Rules.check(
        Arrays.asList(i.category.split(",")).contains(e.category) && v.category.equals(e.category),
        "CATEGORY_MISMATCH");
    category(e.category);
    Rules.check(!s.accountId.equals(i.accountId), "INSTRUCTOR_STUDENT_CONFLICT");
    var date = start.atZone(zone(e.departmentId)).toLocalDate();
    var finish = start.plusSeconds(minutes * 60L);
    var end = finish.plusSeconds(buffer * 60L);
    Rules.check(
        !date.isBefore(e.startsOn)
            && !finish.atZone(zone(e.departmentId)).toLocalDate().isAfter(e.endsOn),
        "PASS_EXPIRED");
    Rules.check(covered(coachId, start, end, null, null), "OUTSIDE_WORK_WINDOW");
    for (var w : db.all(TimeWindow.class))
      if (w.kind.equals("BLOCK")
          && (Objects.equals(w.instructorId, coachId) || Objects.equals(w.vehicleId, carId)))
        Rules.check(!LessonPolicy.overlap(start, end, w.startsAt, w.endsAt), "RESOURCE_BLOCKED");
    for (var l : db.all(Lesson.class))
      if (!Objects.equals(l.id, skip)
          && (held(l) || l.status.equals("DONE") && l.blockedUntil.isAfter(clock.instant()))) {
        if (Objects.equals(l.studentId, e.studentId))
          Rules.check(
              !LessonPolicy.overlap(
                  start,
                  finish,
                  l.startsAt,
                  l.status.equals("LIVE") && l.endsAt.isBefore(clock.instant())
                      ? Instant.MAX
                      : l.endsAt),
              "STUDENT_CONFLICT");
        if (Objects.equals(l.instructorId, coachId) || Objects.equals(l.vehicleId, carId))
          Rules.check(
              !LessonPolicy.overlap(
                  start,
                  end,
                  l.startsAt,
                  l.status.equals("LIVE") && l.blockedUntil.isBefore(clock.instant())
                      ? Instant.MAX
                      : l.blockedUntil),
              "RESOURCE_CONFLICT");
      }
  }

  /** 可用查询只返回时间和资源，不泄漏他人预约；创建时再次加锁核对。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object slots(
      Long enrollmentId, Long coachId, Long carId, String day, int minutes, Long lessonId) {
    var e = enrollment(enrollmentId);
    bookingPermission(e);
    Rules.check(minutes >= 30 && minutes <= 180 && minutes % 15 == 0, "DURATION_INVALID");
    LocalDate date;
    try {
      date = LocalDate.parse(day);
    } catch (Exception ex) {
      throw new Problem(400, "TIME_INVALID");
    }
    var i = db.get(Instructor.class, coachId);
    int buffer = i.bufferMinutes;
    if (lessonId != null) {
      var l = lesson(lessonId);
      Rules.check(
          l.enrollmentId.equals(e.id) && l.status.equals("BOOKED") && l.minutes == minutes,
          "STATE_INVALID");
      buffer = l.bufferMinutes;
    }
    Rules.check(
        e.totalMinutes
                - e.spentMinutes
                - e.heldMinutes
                + (lessonId == null ? 0 : db.get(Lesson.class, lessonId).minutes)
            >= minutes,
        "CREDITS_INSUFFICIENT");
    var out = new ArrayList<Object>();
    for (int minute = 0; minute < 1440; minute += 15) {
      var local = date.atStartOfDay().plusMinutes(minute);
      var offsets = zone(e.departmentId).getRules().getValidOffsets(local);
      if (offsets.size() != 1) continue;
      var start = local.toInstant(offsets.getFirst());
      try {
        bookingWindow(start);
        validateBooking(e, coachId, carId, start, minutes, buffer, lessonId);
        out.add(
            map(
                "startsAt",
                start,
                "endsAt",
                start.plusSeconds(minutes * 60L),
                "label",
                local.toLocalTime().toString()));
      } catch (Problem ignored) {
        /* Only genuine available slots are returned; booking still checks precisely. */
      }
    }
    return out;
  }

  void bookingPermission(Enrollment e) {
    if (staff()) org(e.departmentId, "schedule");
    else {
      access.require("portal");
      if (!own(e)) throw new Problem(403, "OUT_OF_SCOPE");
    }
  }

  /** 一次约课同时预留分钟、教练和车；重复UUID只返回同一预约。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object book(Map<String, Object> b) {
    admin.lock();
    var e = enrollment(Rules.id(b.get("enrollmentId")));
    bookingPermission(e);
    var stamp = commands.begin("BOOK", b);
    if (stamp.id != null) return lessonDetail(stamp.resultId);
    version(e.version, b.get("enrollmentVersion"));
    int minutes = Rules.integer(b.get("minutes"), 30, 180);
    Rules.check(minutes % 15 == 0, "DURATION_INVALID");
    Rules.check(e.totalMinutes - e.spentMinutes - e.heldMinutes >= minutes, "CREDITS_INSUFFICIENT");
    Long coach = Rules.id(b.get("instructorId")), car = Rules.id(b.get("vehicleId"));
    int buffer = db.get(Instructor.class, coach).bufferMinutes;
    var start = LessonPolicy.instant(b.get("startsAt"));
    bookingWindow(start);
    validateBooking(e, coach, car, start, minutes, buffer, null);
    var l = new Lesson();
    l.departmentId = e.departmentId;
    l.enrollmentId = e.id;
    l.studentId = e.studentId;
    l.instructorId = coach;
    l.vehicleId = car;
    l.reference = "L-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(Locale.ROOT);
    l.studentName = db.get(Student.class, e.studentId).name;
    l.instructorName = db.get(Instructor.class, coach).name;
    l.vehicleName = db.get(Vehicle.class, car).name;
    l.category = e.category;
    l.startsAt = start;
    l.minutes = minutes;
    l.bufferMinutes = buffer;
    l.endsAt = start.plusSeconds(minutes * 60L);
    l.blockedUntil = l.endsAt.plusSeconds(buffer * 60L);
    l.status = "BOOKED";
    l.note = Rules.paragraph(b.get("note"), 1000, false);
    l.createdAt = clock.instant();
    db.save(l);
    credit(e, l, minutes, 0, "RESERVE", l.note);
    event(l, "LESSON", "BOOKED", l.note);
    commands.complete(stamp, "LESSON", l.id);
    return lessonDetail(l.id);
  }

  /** 授课原单与状态历史，财务和他人联系资料不返回教练。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object lessonDetail(Long id) {
    var l = lesson(id);
    return map(
        "lesson",
        l,
        "events",
        db.query(
            BusinessEvent.class,
            "from BusinessEvent where objectType='LESSON' and objectId=?1 order by id",
            id),
        "now",
        clock.instant());
  }

  void teaching(Lesson l) {
    access.require("coach");
    if (staff() && access.has("schedule")) return;
    if (!Objects.equals(db.get(Instructor.class, l.instructorId).accountId, access.current().id))
      throw new Problem(403, "INSTRUCTOR_ONLY");
  }

  /** 预约改期、取消、签到、授课与缺席；冻结规则计算扣时，终态不可回写。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object lessonCommand(Long id, String action, Map<String, Object> b) {
    admin.lock();
    var l = lesson(id);
    var e = db.get(Enrollment.class, l.enrollmentId);
    boolean own =
        access.has("portal")
            && Objects.equals(db.get(Student.class, l.studentId).accountId, access.current().id);
    if (Set.of("CANCEL", "RESCHEDULE").contains(action)) {
      bookingPermission(e);
    } else if (action.equals("ARRIVE")) {
      if (!own) {
        if (staff()) org(l.departmentId, "schedule");
        else teaching(l);
      }
    } else if (action.equals("NOSHOW")) {
      if (staff()) org(l.departmentId, "schedule");
      else teaching(l);
    } else teaching(l);
    var stamp = commands.begin("LESSON_" + id + "_" + action, b);
    if (stamp.id != null) return lessonDetail(id);
    version(l.version, b.get("version"));
    String note = Rules.paragraph(b.get("note"), 1000, true);
    switch (action) {
      case "RESCHEDULE" -> {
        Rules.check(l.status.equals("BOOKED"), "STATE_INVALID");
        if (own)
          Rules.check(
              clock.instant().isBefore(l.startsAt.minusSeconds(e.cancelHours * 3600L)),
              "CANCELLATION_DEADLINE");
        var start = LessonPolicy.instant(b.get("startsAt"));
        bookingWindow(start);
        Long coach = Rules.id(b.get("instructorId")), car = Rules.id(b.get("vehicleId"));
        validateBooking(e, coach, car, start, l.minutes, l.bufferMinutes, l.id);
        String old = l.startsAt + " / " + l.instructorName + " / " + l.vehicleName;
        l.instructorId = coach;
        l.vehicleId = car;
        l.instructorName = db.get(Instructor.class, coach).name;
        l.vehicleName = db.get(Vehicle.class, car).name;
        l.startsAt = start;
        l.endsAt = start.plusSeconds(l.minutes * 60L);
        l.blockedUntil = l.endsAt.plusSeconds(l.bufferMinutes * 60L);
        note = "原时段 " + old + "；" + note;
      }
      case "CANCEL" -> {
        Rules.check(l.status.equals("BOOKED") || l.status.equals("ARRIVED"), "STATE_INVALID");
        boolean late = !clock.instant().isBefore(l.startsAt.minusSeconds(e.cancelHours * 3600L));
        int charge = own && late && e.lateCharge ? l.minutes : 0;
        l.status = charge > 0 ? "LATE_CANCELLED" : "CANCELLED";
        l.actualMinutes = charge;
        credit(e, l, -l.minutes, charge, charge > 0 ? "LATE_CANCEL" : "RELEASE", note);
      }
      case "ARRIVE" -> {
        Rules.check(l.status.equals("BOOKED"), "STATE_INVALID");
        Rules.check(
            !clock.instant().isBefore(l.startsAt.minusSeconds(900))
                && clock.instant().isBefore(l.endsAt),
            "CHECKIN_WINDOW");
        l.status = "ARRIVED";
      }
      case "START" -> {
        Rules.check(l.status.equals("ARRIVED"), "STATE_INVALID");
        Rules.check(!clock.instant().isBefore(l.startsAt.minusSeconds(900)), "CHECKIN_WINDOW");
        validateBooking(
            e, l.instructorId, l.vehicleId, l.startsAt, l.minutes, l.bufferMinutes, l.id);
        l.status = "LIVE";
      }
      case "FINISH" -> {
        Rules.check(l.status.equals("LIVE"), "STATE_INVALID");
        int actual = Rules.integer(b.get("actualMinutes"), 0, l.minutes);
        l.topic = Rules.text(b.get("topic"), 200, true);
        l.progress = Rules.paragraph(b.get("progress"), 1000, true);
        l.actualMinutes = actual;
        l.status = "DONE";
        credit(e, l, -l.minutes, actual, "TAUGHT", note);
        var actualEnd = clock.instant();
        if (actualEnd.isAfter(l.endsAt)) {
          l.endsAt = actualEnd;
          l.blockedUntil = actualEnd.plusSeconds(l.bufferMinutes * 60L);
        }
      }
      case "NOSHOW" -> {
        Rules.check(l.status.equals("BOOKED"), "STATE_INVALID");
        Rules.check(!clock.instant().isBefore(l.startsAt.plusSeconds(900)), "NO_SHOW_TOO_EARLY");
        int charge = e.lateCharge ? l.minutes : 0;
        l.actualMinutes = charge;
        l.status = "NO_SHOW";
        credit(e, l, -l.minutes, charge, "NO_SHOW", note);
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    l.version++;
    event(l, "LESSON", action, note);
    commands.complete(stamp, "LESSON", l.id);
    return lessonDetail(id);
  }

  /** 统计按组织与币种分别汇总实收退款，不把预约金额当营收。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object reports() {
    org(access.current().departmentId, "reports");
    var es =
        db.all(Enrollment.class).stream().filter(e -> access.department(e.departmentId)).toList();
    var ls = db.all(Lesson.class).stream().filter(l -> access.department(l.departmentId)).toList();
    var currencies =
        es.stream()
            .map(e -> e.currency)
            .distinct()
            .sorted()
            .map(
                c ->
                    map(
                        "currency",
                        c,
                        "collectedMinor",
                        es.stream()
                            .filter(e -> e.currency.equals(c))
                            .mapToLong(e -> e.paidMinor)
                            .sum(),
                        "refundedMinor",
                        es.stream()
                            .filter(e -> e.currency.equals(c))
                            .mapToLong(e -> e.refundedMinor)
                            .sum(),
                        "netMinor",
                        es.stream()
                            .filter(e -> e.currency.equals(c))
                            .mapToLong(e -> e.paidMinor - e.refundedMinor)
                            .sum(),
                        "earnedMinor",
                        es.stream()
                            .filter(e -> e.currency.equals(c))
                            .mapToLong(
                                e ->
                                    LessonPolicy.earned(
                                        e.priceMinor, e.spentMinutes, e.totalMinutes))
                            .sum()))
            .toList();
    return map(
        "students",
        db.all(Student.class).stream().filter(s -> access.department(s.departmentId)).count(),
        "inquiries",
        db.all(Inquiry.class).stream()
            .filter(s -> access.department(s.departmentId) && s.status.equals("NEW"))
            .count(),
        "booked",
        ls.stream().filter(this::held).count(),
        "completed",
        ls.stream().filter(l -> l.status.equals("DONE")).count(),
        "heldMinutes",
        es.stream().mapToInt(e -> e.heldMinutes).sum(),
        "spentMinutes",
        es.stream().mapToInt(e -> e.spentMinutes).sum(),
        "currencies",
        currencies,
        "lessons",
        ls);
  }

  /** 授权CSV导出没有密码和他人私有备注，文本做公式前缀保护。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public String export() {
    org(access.current().departmentId, "export");
    var out =
        new StringBuilder(
            "reference,student,instructor,vehicle,status,starts_at,booked_minutes,used_minutes\r\n");
    for (var l : db.all(Lesson.class))
      if (access.department(l.departmentId))
        out.append(
                Stream.of(
                        l.reference,
                        l.studentName,
                        l.instructorName,
                        l.vehicleName,
                        l.status,
                        l.startsAt,
                        l.minutes,
                        l.actualMinutes)
                    .map(Rules::csv)
                    .collect(Collectors.joining(",")))
            .append("\r\n");
    return out.toString();
  }

  /** 审计仅授权组织读取。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object audit() {
    org(access.current().departmentId, "audit");
    return db
        .jpql(AuditEvent.class, "from AuditEvent order by id desc")
        .setMaxResults(1000)
        .getResultList()
        .stream()
        .filter(a -> access.department(a.departmentId))
        .toList();
  }

  /** 公开课程入口只展示启用分校和套餐，不展示学员、教练账号和日程。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object publicCatalog() {
    var depts = db.all(Department.class).stream().filter(d -> d.enabled).toList();
    var ids = depts.stream().map(d -> d.id).toList();
    return map(
        "schools",
        depts.stream().map(d -> map("id", d.id, "name", d.name)).toList(),
        "categories",
        db.all(DictionaryEntry.class).stream()
            .filter(c -> c.enabled)
            .map(c -> map("code", c.code, "name", c.name, "nameEn", c.nameEn))
            .toList(),
        "plans",
        db.all(Plan.class).stream()
            .filter(p -> p.enabled && ids.contains(p.departmentId))
            .map(
                p ->
                    map(
                        "id",
                        p.id,
                        "departmentId",
                        p.departmentId,
                        "name",
                        p.name,
                        "category",
                        p.category,
                        "totalMinutes",
                        p.totalMinutes,
                        "priceMinor",
                        p.priceMinor,
                        "currency",
                        p.currency,
                        "validDays",
                        p.validDays))
            .toList());
  }

  /** 匿名表单仅创建私有询盘，返回随机回执；不发送短信邮件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object publicInquiry(Map<String, Object> b) {
    admin.lock();
    Long dept = Rules.id(b.get("departmentId"));
    Rules.check(db.get(Department.class, dept).enabled, "DEPARTMENT_DISABLED");
    Rules.check(Rules.text(b.get("website"), 100, false).isEmpty(), "INVALID_INPUT");
    var q = new Inquiry();
    q.departmentId = dept;
    q.name = Rules.text(b.get("name"), 120, true);
    q.contact = Rules.text(b.get("contact"), 120, true);
    q.note = Rules.paragraph(b.get("note"), 1000, true);
    q.status = "NEW";
    q.reference = "Q-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(Locale.ROOT);
    q.createdAt = clock.instant();
    db.save(q);
    var audit = new AuditEvent();
    audit.actor = "PUBLIC";
    audit.action = "INQUIRY_RECEIVED";
    audit.objectId = q.id.toString();
    audit.departmentId = dept;
    audit.createdAt = clock.instant();
    db.save(audit);
    return map("reference", q.reference, "received", true);
  }
}
