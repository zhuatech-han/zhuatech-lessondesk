// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 空库初始化岗位与目录，不插入课程、学员或虚构收费。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  public static final Set<String> CODES =
      Set.of(
          "catalog",
          "masters",
          "students",
          "inquiries",
          "enroll",
          "finance",
          "lessons",
          "schedule",
          "coach",
          "portal",
          "reports",
          "export",
          "users",
          "roles",
          "settings",
          "audit");
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String password;
  final String username;

  public Bootstrap(
      Store db,
      BCryptPasswordEncoder encoder,
      @Value("${lessondesk.admin-password}") String password,
      @Value("${lessondesk.admin-username}") String username) {
    this.db = db;
    this.encoder = encoder;
    this.password = password;
    this.username = username;
  }

  /** 初次管理员采用部署者强密码，后续重启不覆盖。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    var d = new Department();
    d.name = "驾校分校";
    d.zone = "Asia/Shanghai";
    db.save(d);
    var names =
        Map.ofEntries(
            Map.entry("catalog", "业务目录"),
            Map.entry("masters", "教练车辆套餐"),
            Map.entry("students", "学员档案"),
            Map.entry("inquiries", "询盘跟进"),
            Map.entry("enroll", "报读停课"),
            Map.entry("finance", "收退款台账"),
            Map.entry("lessons", "课次查阅"),
            Map.entry("schedule", "前台约课"),
            Map.entry("coach", "本人教学"),
            Map.entry("portal", "本人约课"),
            Map.entry("reports", "经营统计"),
            Map.entry("export", "业务导出"),
            Map.entry("users", "账号管理"),
            Map.entry("roles", "角色权限"),
            Map.entry("settings", "组织设置"),
            Map.entry("audit", "操作审计"));
    new TreeMap<>(names)
        .forEach(
            (k, v) -> {
              var p = new Permission();
              p.code = k;
              p.name = v;
              db.save(p);
            });
    role("管理员", "ALL", CODES);
    role(
        "前台教务",
        "DEPARTMENT",
        Set.of(
            "catalog",
            "masters",
            "students",
            "inquiries",
            "enroll",
            "lessons",
            "schedule",
            "reports",
            "export",
            "audit"));
    role(
        "财务",
        "DEPARTMENT",
        Set.of("catalog", "students", "enroll", "finance", "lessons", "reports", "export"));
    role("教练", "ASSIGNED", Set.of("catalog", "coach"));
    role("学员", "STUDENT", Set.of("catalog", "portal"));
    var a = new Account();
    a.username = "admin";
    a.displayName = "管理员";
    a.passwordHash = encoder.encode(password);
    a.departmentId = d.id;
    a.roleId = db.all(AccessRole.class).getFirst().id;
    db.save(a);
    String[][] menus = {
      {"lessons", "预约与课表", "Lessons & schedule", "lessons"},
      {"portal", "我的课时与约课", "My lessons", "portal"},
      {"coach", "教练工作台", "Instructor desk", "coach"},
      {"students", "学员", "Students", "students"},
      {"enrollments", "报读与收退费", "Enrollment & fees", "enroll"},
      {"masters", "教练、车辆与套餐", "Instructors, vehicles & plans", "masters"},
      {"inquiries", "询盘跟进", "Inquiries", "inquiries"},
      {"reports", "经营报表", "Reports", "reports"},
      {"audit", "操作审计", "Audit", "audit"},
      {"users", "登录账号", "Accounts", "users"},
      {"roles", "角色权限", "Roles", "roles"},
      {"settings", "组织与设置", "Settings", "settings"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    for (var k : Map.of("bookingLeadMinutes", "30", "bookingWindowDays", "60").entrySet()) {
      var s = new SystemSetting();
      s.code = k.getKey();
      s.value = k.getValue();
      db.save(s);
    }
    String[][] categories = {{"MANUAL", "手动挡", "Manual"}, {"AUTO", "自动挡", "Automatic"}};
    for (var v : categories) {
      var c = new DictionaryEntry();
      c.type = "CATEGORY";
      c.code = v[0];
      c.name = v[1];
      c.nameEn = v[2];
      c.enabled = true;
      db.save(c);
    }
  }

  private void role(String n, String scope, Set<String> codes) {
    var r = new AccessRole();
    r.name = n;
    r.scope = scope;
    r.permissions.addAll(codes);
    db.save(r);
  }
}
