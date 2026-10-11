// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 真实Security、JPA、Flyway接口验证约课、权限、分钟守恒和原款退款。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import(WorkflowTest.TimeConfig.class)
class WorkflowTest {
  static final String PW = "Aa9" + UUID.randomUUID();
  static final Instant NOW = Instant.parse("2026-10-11T01:00:00Z");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:lessons;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    r.add("lessondesk.admin-password", () -> PW);
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }

  static class MutableClock extends Clock {
    Instant value = NOW;

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return value;
    }
  }

  @Autowired MockMvc mvc;
  @Autowired MutableClock clock;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession admin, student, other, coach, otherCoach;
  long studentId,
      student2Id,
      coachId,
      coach2Id,
      vehicleId,
      vehicle2Id,
      planId,
      enrollId,
      enroll2Id,
      studentAccount,
      coachAccount;
  String suffix;
  Instant at = NOW.plusSeconds(7200);

  JsonNode call(MockHttpSession s, String method, String url, Map<String, Object> b, int expected)
      throws Exception {
    MockHttpServletRequestBuilder r =
        switch (method) {
          case "POST" -> post(url);
          case "PUT" -> put(url);
          case "DELETE" -> delete(url);
          default -> get(url);
        };
    if (s != null) r.session(s);
    if (!method.equals("GET"))
      r.with(csrf()).contentType("application/json").content(json.writeValueAsString(b));
    var out = mvc.perform(r).andReturn().getResponse();
    assertEquals(expected, out.getStatus(), url + " " + out.getContentAsString());
    if (out.getContentType() != null && out.getContentType().startsWith("text/csv"))
      return json.valueToTree(Map.of("csv", out.getContentAsString()));
    return json.readTree(out.getContentAsString());
  }

  MockHttpSession login(String username) throws Exception {
    var s = new MockHttpSession();
    call(s, "POST", "/api/auth/login", Map.of("username", username, "password", PW), 200);
    return s;
  }

  Map<String, Object> body(Object... x) {
    var b = new LinkedHashMap<String, Object>();
    for (int i = 0; i < x.length; i += 2) b.put(x[i].toString(), x[i + 1]);
    return b;
  }

  Map<String, Object> command(Object... x) {
    var b = body(x);
    b.put("requestId", UUID.randomUUID().toString());
    b.putIfAbsent("note", "TEST verification");
    return b;
  }

  long account(String name, int role) throws Exception {
    return call(
            admin,
            "POST",
            "/api/admin/users",
            body(
                "username",
                name,
                "displayName",
                "TEST " + name,
                "roleId",
                role,
                "departmentId",
                1,
                "enabled",
                true,
                "password",
                PW),
            200)
        .get("id")
        .asLong();
  }

  JsonNode master(String k, Object... x) throws Exception {
    return call(admin, "POST", "/api/masters/" + k, body(x), 200);
  }

  JsonNode enrollment(long id) throws Exception {
    return call(admin, "GET", "/api/enrollments/" + id, Map.of(), 200);
  }

  JsonNode lesson(long id) throws Exception {
    return call(admin, "GET", "/api/lessons/" + id, Map.of(), 200);
  }

  JsonNode ec(long id, String action, int expected, Object... x) throws Exception {
    var b = command(x);
    b.put("version", enrollment(id).get("enrollment").get("version").asLong());
    return call(admin, "POST", "/api/enrollments/" + id + "/commands/" + action, b, expected);
  }

  JsonNode act(MockHttpSession actor, long id, String action, int expected, Object... x)
      throws Exception {
    var b = command(x);
    b.put("version", lesson(id).get("lesson").get("version").asLong());
    return call(actor, "POST", "/api/lessons/" + id + "/commands/" + action, b, expected);
  }

  Map<String, Object> booking(long enrollment, Instant time, long instructor, long car)
      throws Exception {
    return command(
        "enrollmentId",
        enrollment,
        "enrollmentVersion",
        enrollment(enrollment).get("enrollment").get("version").asLong(),
        "instructorId",
        instructor,
        "vehicleId",
        car,
        "startsAt",
        time.toString(),
        "minutes",
        60);
  }

  long book() throws Exception {
    return call(student, "POST", "/api/lessons", booking(enrollId, at, coachId, vehicleId), 200)
        .get("lesson")
        .get("id")
        .asLong();
  }

  JsonNode window(
      MockHttpSession actor,
      String kind,
      Long instructor,
      Long car,
      Instant start,
      Instant end,
      int expected)
      throws Exception {
    return call(
        actor,
        "POST",
        "/api/masters/windows",
        body(
            "departmentId",
            1,
            "kind",
            kind,
            "instructorId",
            instructor,
            "vehicleId",
            car,
            "startsAt",
            start.toString(),
            "endsAt",
            end.toString(),
            "note",
            "TEST window"),
        expected);
  }

  @BeforeEach
  void setup() throws Exception {
    clock.value = NOW;
    at = NOW.plusSeconds(7200);
    suffix = UUID.randomUUID().toString().substring(0, 8);
    admin = login("admin");
    studentAccount = account("s" + suffix, 5);
    long otherAccount = account("o" + suffix, 5);
    coachAccount = account("c" + suffix, 4);
    long otherCoachAccount = account("d" + suffix, 4);
    student = login("s" + suffix);
    other = login("o" + suffix);
    coach = login("c" + suffix);
    otherCoach = login("d" + suffix);
    studentId =
        master(
                "students",
                "departmentId",
                1,
                "accountId",
                studentAccount,
                "name",
                "TEST A " + suffix,
                "contact",
                "a@example.invalid",
                "note",
                "private TEST",
                "enabled",
                true)
            .get("id")
            .asLong();
    student2Id =
        master(
                "students",
                "departmentId",
                1,
                "accountId",
                otherAccount,
                "name",
                "TEST B " + suffix,
                "contact",
                "b@example.invalid",
                "note",
                "other private TEST",
                "enabled",
                true)
            .get("id")
            .asLong();
    coachId =
        master(
                "instructors",
                "departmentId",
                1,
                "accountId",
                coachAccount,
                "name",
                "TEST coach " + suffix,
                "category",
                "AUTO",
                "bufferMinutes",
                15,
                "enabled",
                true)
            .get("id")
            .asLong();
    coach2Id =
        master(
                "instructors",
                "departmentId",
                1,
                "accountId",
                otherCoachAccount,
                "name",
                "TEST coach2 " + suffix,
                "category",
                "AUTO",
                "bufferMinutes",
                15,
                "enabled",
                true)
            .get("id")
            .asLong();
    vehicleId =
        master(
                "vehicles",
                "departmentId",
                1,
                "name",
                "TEST car " + suffix,
                "plate",
                "A" + suffix,
                "category",
                "AUTO",
                "enabled",
                true)
            .get("id")
            .asLong();
    vehicle2Id =
        master(
                "vehicles",
                "departmentId",
                1,
                "name",
                "TEST car2 " + suffix,
                "plate",
                "B" + suffix,
                "category",
                "AUTO",
                "enabled",
                true)
            .get("id")
            .asLong();
    planId =
        master(
                "plans",
                "departmentId",
                1,
                "name",
                "TEST plan " + suffix,
                "category",
                "AUTO",
                "totalMinutes",
                600,
                "priceMinor",
                1001,
                "currency",
                "GBP",
                "validDays",
                90,
                "cancelHours",
                24,
                "lateCharge",
                true,
                "enabled",
                true)
            .get("id")
            .asLong();
    window(admin, "WORK", coachId, null, NOW, NOW.plusSeconds(32400), 200);
    window(admin, "WORK", coach2Id, null, NOW, NOW.plusSeconds(32400), 200);
    enrollId =
        call(
                admin,
                "POST",
                "/api/enrollments",
                command("studentId", studentId, "planId", planId, "startsOn", "2026-10-11"),
                200)
            .get("enrollment")
            .get("id")
            .asLong();
    enroll2Id =
        call(
                admin,
                "POST",
                "/api/enrollments",
                command("studentId", student2Id, "planId", planId, "startsOn", "2026-10-11"),
                200)
            .get("enrollment")
            .get("id")
            .asLong();
    ec(enrollId, "COLLECT", 200, "amountMinor", 1001, "reference", "PA" + suffix);
    ec(enroll2Id, "COLLECT", 200, "amountMinor", 1001, "reference", "PB" + suffix);
  }

  @Test
  void completeLessonAndRefundConservesCreditsAndMoney() throws Exception {
    long id = book();
    assertEquals(60, enrollment(enrollId).get("enrollment").get("heldMinutes").asInt());
    clock.value = at.minusSeconds(600);
    act(student, id, "ARRIVE", 200);
    act(coach, id, "START", 200);
    act(
        coach,
        id,
        "FINISH",
        200,
        "actualMinutes",
        45,
        "topic",
        "TEST steering",
        "progress",
        "TEST improvement");
    var e = enrollment(enrollId);
    assertEquals(0, e.get("enrollment").get("heldMinutes").asInt());
    assertEquals(45, e.get("enrollment").get("spentMinutes").asInt());
    assertEquals(925, e.get("refundableMinor").asLong());
    ec(enrollId, "CLOSE", 200);
    long payment = e.get("payments").get(0).get("id").asLong();
    ec(
        enrollId,
        "REFUND",
        200,
        "amountMinor",
        925,
        "originalId",
        payment,
        "reference",
        "RF" + suffix);
    assertEquals(0, enrollment(enrollId).get("refundableMinor").asLong());
    ec(
        enrollId,
        "REFUND",
        409,
        "amountMinor",
        1,
        "originalId",
        payment,
        "reference",
        "RF2" + suffix);
  }

  @Test
  void instructorAndVehicleConflictsAreIndependent() throws Exception {
    book();
    call(other, "POST", "/api/lessons", booking(enroll2Id, at, coachId, vehicle2Id), 409);
    call(other, "POST", "/api/lessons", booking(enroll2Id, at, coach2Id, vehicleId), 409);
    call(other, "POST", "/api/lessons", booking(enroll2Id, at, coach2Id, vehicle2Id), 200);
  }

  @Test
  void studentConflictAcrossEnrollments() throws Exception {
    book();
    long e =
        call(
                admin,
                "POST",
                "/api/enrollments",
                command("studentId", studentId, "planId", planId, "startsOn", "2026-10-11"),
                200)
            .get("enrollment")
            .get("id")
            .asLong();
    ec(e, "COLLECT", 200, "amountMinor", 1001, "reference", "PC" + suffix);
    call(student, "POST", "/api/lessons", booking(e, at, coach2Id, vehicle2Id), 409);
  }

  @Test
  void bufferBlocksOverlapButAdjacentBoundaryWorks() throws Exception {
    book();
    call(
        other,
        "POST",
        "/api/lessons",
        booking(enroll2Id, at.plusSeconds(3600), coachId, vehicleId),
        409);
    call(
        other,
        "POST",
        "/api/lessons",
        booking(enroll2Id, at.plusSeconds(4500), coachId, vehicleId),
        200);
  }

  @Test
  void missingWorkAndMaintenanceBlockBooking() throws Exception {
    call(
        student,
        "POST",
        "/api/lessons",
        booking(enrollId, NOW.plusSeconds(86400), coachId, vehicleId),
        409);
    window(admin, "BLOCK", null, vehicleId, at, at.plusSeconds(3600), 200);
    call(student, "POST", "/api/lessons", booking(enrollId, at, coachId, vehicleId), 409);
  }

  @Test
  void workWindowChangesCannotInvalidateReservations() throws Exception {
    var w =
        window(
            admin,
            "WORK",
            coachId,
            null,
            NOW.plusSeconds(86400),
            NOW.plusSeconds(86400 + 21600),
            200);
    long id = book();
    window(admin, "BLOCK", coachId, null, at, at.plusSeconds(3600), 409);
    var list = call(admin, "GET", "/api/records/windows?size=100", Map.of(), 200).get("items");
    JsonNode used = null;
    for (var x : list)
      if (x.get("instructorId").asLong() == coachId
          && x.get("startsAt").asString().equals(NOW.toString())) used = x;
    assertNotNull(used);
    call(
        admin,
        "DELETE",
        "/api/masters/windows/"
            + used.get("id").asLong()
            + "?version="
            + used.get("version").asLong(),
        Map.of(),
        409);
    call(
        admin,
        "DELETE",
        "/api/masters/windows/" + w.get("id").asLong() + "?version=" + w.get("version").asLong(),
        Map.of(),
        200);
    act(admin, id, "CANCEL", 200);
  }

  @Test
  void rescheduleDoesNotDoubleReserveAndKeepsHistory() throws Exception {
    long id = book();
    act(
        admin,
        id,
        "RESCHEDULE",
        200,
        "startsAt",
        at.plusSeconds(4500).toString(),
        "instructorId",
        coachId,
        "vehicleId",
        vehicleId);
    assertEquals(60, enrollment(enrollId).get("enrollment").get("heldMinutes").asInt());
    assertEquals(2, lesson(id).get("events").size());
    assertTrue(lesson(id).get("events").get(1).get("note").asString().contains(at.toString()));
  }

  @Test
  void lateStudentCancellationChargesAndOfficeWaives() throws Exception {
    long id = book();
    act(
        student,
        id,
        "RESCHEDULE",
        409,
        "startsAt",
        at.plusSeconds(4500).toString(),
        "instructorId",
        coachId,
        "vehicleId",
        vehicleId);
    act(student, id, "CANCEL", 200);
    assertEquals(60, enrollment(enrollId).get("enrollment").get("spentMinutes").asInt());
    long next =
        call(other, "POST", "/api/lessons", booking(enroll2Id, at, coachId, vehicleId), 200)
            .get("lesson")
            .get("id")
            .asLong();
    act(admin, next, "CANCEL", 200);
    assertEquals(0, enrollment(enroll2Id).get("enrollment").get("spentMinutes").asInt());
  }

  @Test
  void noShowRequiresElapsedGraceAndUsesSnapshotRule() throws Exception {
    long id = book();
    act(coach, id, "NOSHOW", 409);
    clock.value = at.plusSeconds(900);
    act(coach, id, "NOSHOW", 200);
    assertEquals(60, enrollment(enrollId).get("enrollment").get("spentMinutes").asInt());
    assertEquals("NO_SHOW", lesson(id).get("lesson").get("status").asString());
  }

  @Test
  void studentAndInstructorPrivacyAndPermissions() throws Exception {
    long id = book();
    call(other, "GET", "/api/lessons/" + id, Map.of(), 403);
    call(other, "GET", "/api/enrollments/" + enrollId, Map.of(), 403);
    call(coach, "GET", "/api/enrollments/" + enrollId, Map.of(), 403);
    call(student, "GET", "/api/admin/users", Map.of(), 403);
    call(student, "GET", "/api/records/students", Map.of(), 403);
    assertFalse(
        call(coach, "GET", "/api/options", Map.of(), 200).toString().contains("a@example.invalid"));
    act(otherCoach, id, "ARRIVE", 403);
  }

  @Test
  void checkinStateAndAssignedInstructorAreEnforced() throws Exception {
    long id = book();
    act(student, id, "START", 403);
    act(coach, id, "START", 409);
    act(student, id, "ARRIVE", 409);
    clock.value = at.minusSeconds(600);
    act(student, id, "ARRIVE", 200);
    act(otherCoach, id, "START", 403);
    act(coach, id, "START", 200);
    act(admin, id, "CANCEL", 409);
    act(coach, id, "FINISH", 400, "actualMinutes", 61, "topic", "TEST", "progress", "TEST");
    act(coach, id, "FINISH", 200, "actualMinutes", 30, "topic", "TEST", "progress", "TEST");
    act(coach, id, "FINISH", 409, "actualMinutes", 30, "topic", "TEST", "progress", "TEST");
  }

  @Test
  void paymentAndRefundGuards() throws Exception {
    ec(enrollId, "COLLECT", 409, "amountMinor", 1, "reference", "PX" + suffix);
    long foreign = enrollment(enroll2Id).get("payments").get(0).get("id").asLong();
    ec(
        enrollId,
        "REFUND",
        409,
        "amountMinor",
        1,
        "originalId",
        foreign,
        "reference",
        "RX" + suffix);
    ec(enrollId, "CLOSE", 200);
    ec(
        enrollId,
        "REFUND",
        409,
        "amountMinor",
        1,
        "originalId",
        foreign,
        "reference",
        "RX" + suffix);
    ec(enrollId, "COLLECT", 409, "amountMinor", 1, "reference", "PY" + suffix);
  }

  @Test
  void enrollmentCannotCloseWithHeldCredits() throws Exception {
    long id = book();
    ec(enrollId, "CLOSE", 409);
    act(admin, id, "CANCEL", 200);
    ec(enrollId, "CLOSE", 200);
    call(student, "POST", "/api/lessons", booking(enrollId, at, coachId, vehicleId), 409);
  }

  @Test
  void identicalCommandIsIdempotentAndChangedPayloadRejected() throws Exception {
    var b = booking(enrollId, at, coachId, vehicleId);
    var a = call(student, "POST", "/api/lessons", b, 200);
    var repeat = call(student, "POST", "/api/lessons", b, 200);
    assertEquals(a.get("lesson").get("id"), repeat.get("lesson").get("id"));
    assertEquals(60, enrollment(enrollId).get("enrollment").get("heldMinutes").asInt());
    b.put("minutes", 30);
    call(student, "POST", "/api/lessons", b, 409);
  }

  @Test
  void concurrentBookingsHaveOneWinner() throws Exception {
    var a = booking(enrollId, at, coachId, vehicleId);
    var b = booking(enroll2Id, at, coachId, vehicleId);
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var f =
          pool.submit(
              () -> {
                gate.await();
                return mvc.perform(
                        post("/api/lessons")
                            .session(student)
                            .with(csrf())
                            .contentType("application/json")
                            .content(json.writeValueAsString(a)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var g =
          pool.submit(
              () -> {
                gate.await();
                return mvc.perform(
                        post("/api/lessons")
                            .session(other)
                            .with(csrf())
                            .contentType("application/json")
                            .content(json.writeValueAsString(b)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      gate.countDown();
      var statuses =
          new ArrayList<>(List.of(f.get(15, TimeUnit.SECONDS), g.get(15, TimeUnit.SECONDS)));
      Collections.sort(statuses);
      assertEquals(List.of(200, 409), statuses);
    }
  }

  @Test
  void planChangesDoNotChangeEnrollmentSnapshot() throws Exception {
    var p =
        call(admin, "GET", "/api/records/plans?search=" + suffix, Map.of(), 200)
            .get("items")
            .get(0);
    var b = json.convertValue(p, Map.class);
    b.put("priceMinor", 99999);
    b.put("totalMinutes", 30);
    call(admin, "PUT", "/api/masters/plans/" + planId, b, 200);
    assertEquals(1001, enrollment(enrollId).get("enrollment").get("priceMinor").asLong());
    assertEquals(600, enrollment(enrollId).get("enrollment").get("totalMinutes").asInt());
  }

  @Test
  void resourceDisableAndMasterVersionGuard() throws Exception {
    book();
    var i =
        call(admin, "GET", "/api/records/instructors?search=" + suffix, Map.of(), 200).get("items");
    JsonNode selected = null;
    for (var x : i) if (x.get("id").asLong() == coachId) selected = x;
    var b = json.convertValue(selected, Map.class);
    b.put("enabled", false);
    call(admin, "PUT", "/api/masters/instructors/" + coachId, b, 409);
    b.put("enabled", true);
    b.put("version", 999);
    call(admin, "PUT", "/api/masters/instructors/" + coachId, b, 409);
    call(admin, "DELETE", "/api/masters/plans/" + planId + "?version=1", Map.of(), 409);
  }

  @Test
  void csrfAndPublicFieldsProtectPrivateData() throws Exception {
    assertEquals(
        403,
        mvc.perform(
                post("/api/lessons").session(student).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
    call(null, "GET", "/api/records/lessons", Map.of(), 401);
    var p = call(null, "GET", "/api/public/catalog", Map.of(), 200);
    assertFalse(p.toString().contains("accountId"));
    var q =
        call(
            null,
            "POST",
            "/api/public/inquiries",
            body(
                "departmentId",
                1,
                "name",
                "TEST public",
                "contact",
                "public@example.invalid",
                "note",
                "TEST package question",
                "website",
                ""),
            200);
    assertEquals(2, q.size());
    assertFalse(q.toString().contains("public@example.invalid"));
    call(student, "GET", "/api/records/inquiries", Map.of(), 403);
  }

  @Test
  void coachOwnWindowAndRevokeExistingSession() throws Exception {
    window(coach, "BLOCK", coach2Id, null, at.plusSeconds(86400), at.plusSeconds(90000), 409);
    window(coach, "WORK", coachId, null, at.plusSeconds(86400), at.plusSeconds(90000), 200);
    var users = call(admin, "GET", "/api/admin/users", Map.of(), 200);
    JsonNode selected = null;
    for (var x : users) if (x.get("id").asLong() == coachAccount) selected = x;
    var b = json.convertValue(selected, Map.class);
    b.put("enabled", false);
    call(admin, "PUT", "/api/admin/users/" + coachAccount, b, 200);
    call(coach, "GET", "/api/options", Map.of(), 401);
  }

  @Test
  void lastAdminAndStudentRoleLimits() throws Exception {
    var roles = call(admin, "GET", "/api/admin/roles", Map.of(), 200);
    JsonNode s = null;
    for (var x : roles) if (x.get("id").asLong() == 5) s = x;
    var b = json.convertValue(s, Map.class);
    b.put("permissions", List.of("catalog", "portal", "finance"));
    call(admin, "PUT", "/api/admin/roles/5", b, 409);
    var users = call(admin, "GET", "/api/admin/users", Map.of(), 200);
    JsonNode a = null;
    for (var x : users) if (x.get("username").asString().equals("admin")) a = x;
    var disabled = json.convertValue(a, Map.class);
    disabled.put("enabled", false);
    call(admin, "PUT", "/api/admin/users/1", disabled, 409);
  }

  @Test
  void instructorCanTeachMultipleCategoriesAndCannotRemoveBookedCategory() throws Exception {
    book();
    var all =
        call(admin, "GET", "/api/records/instructors?search=" + suffix, Map.of(), 200).get("items");
    JsonNode row = null;
    for (var x : all) if (x.get("id").asLong() == coachId) row = x;
    var b = json.convertValue(row, Map.class);
    b.put("category", "MANUAL,AUTO");
    var saved = call(admin, "PUT", "/api/masters/instructors/" + coachId, b, 200);
    assertEquals("AUTO,MANUAL", saved.get("category").asString());
    b = json.convertValue(saved, Map.class);
    b.put("category", "MANUAL");
    call(admin, "PUT", "/api/masters/instructors/" + coachId, b, 409);
  }

  @Test
  void departmentScopeDoesNotExposeOtherSchool() throws Exception {
    var dep =
        call(
            admin,
            "POST",
            "/api/admin/departments",
            body("name", "TEST school " + suffix, "zone", "UTC", "enabled", true),
            200);
    long did = dep.get("id").asLong();
    long acc =
        call(
                admin,
                "POST",
                "/api/admin/users",
                body(
                    "username",
                    "x" + suffix,
                    "displayName",
                    "TEST other school",
                    "roleId",
                    5,
                    "departmentId",
                    did,
                    "password",
                    PW,
                    "enabled",
                    true),
                200)
            .get("id")
            .asLong();
    long sid =
        master(
                "students",
                "departmentId",
                did,
                "accountId",
                acc,
                "name",
                "TEST hidden " + suffix,
                "contact",
                "hidden@example.invalid",
                "note",
                "private",
                "enabled",
                true)
            .get("id")
            .asLong();
    long pid =
        master(
                "plans",
                "departmentId",
                did,
                "name",
                "TEST other plan " + suffix,
                "category",
                "AUTO",
                "totalMinutes",
                60,
                "priceMinor",
                1000,
                "currency",
                "GBP",
                "validDays",
                90,
                "cancelHours",
                0,
                "lateCharge",
                false,
                "enabled",
                true)
            .get("id")
            .asLong();
    long eid =
        call(
                admin,
                "POST",
                "/api/enrollments",
                command("studentId", sid, "planId", pid, "startsOn", "2026-10-11"),
                200)
            .get("enrollment")
            .get("id")
            .asLong();
    account("f" + suffix, 2);
    var office = login("f" + suffix);
    call(office, "GET", "/api/enrollments/" + eid, Map.of(), 403);
    assertEquals(
        0,
        call(office, "GET", "/api/records/students?search=hidden", Map.of(), 200)
            .get("total")
            .asInt());
  }
}
