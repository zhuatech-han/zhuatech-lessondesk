// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** 公开询盘、岗位操作与目录接口；实时身份与状态均由服务核验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final LessonService service;
  final AdminService admin;
  final Map<String, Window> limits = new LinkedHashMap<>();

  record Window(int count, Instant until) {}

  public ApiController(LessonService service, AdminService admin) {
    this.service = service;
    this.admin = admin;
  }

  /** 无隐私的课程入口。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/public/catalog")
  public Object catalog() {
    return service.publicCatalog();
  }

  /** 私有询盘提交；每连接来源十分钟最多十次，不发送外部消息。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/public/inquiries")
  public synchronized Object inquiry(
      @RequestBody Map<String, Object> b, HttpServletRequest request) {
    var now = Instant.now();
    limits.entrySet().removeIf(x -> x.getValue().until.isBefore(now));
    String key = request.getRemoteAddr();
    var prior = limits.get(key);
    if (prior != null && prior.count >= 10) throw new Problem(429, "INQUIRY_THROTTLED");
    if (limits.size() > 2000) limits.remove(limits.keySet().iterator().next());
    limits.put(key, new Window(prior == null ? 1 : prior.count + 1, now.plusSeconds(600)));
    return service.publicInquiry(b);
  }

  /** 岗位需要的最小目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/options")
  public Object options() {
    return service.options();
  }

  /** 分页、搜索和状态筛选，所有目录先做权限过滤。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/records/{type}")
  public Object list(
      @PathVariable String type,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "newest") String sort) {
    return service.list(type, search, status, page, size, sort);
  }

  /** 新增主数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/masters/{type}")
  public Object create(@PathVariable String type, @RequestBody Map<String, Object> b) {
    return service.master(type, null, b);
  }

  /** 更新版本化主数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/masters/{type}/{id}")
  public Object update(
      @PathVariable String type, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return service.master(type, id, b);
  }

  /** 外键保护历史目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/masters/{type}/{id}")
  public Object delete(
      @PathVariable String type, @PathVariable Long id, @RequestParam long version) {
    service.delete(type, id, version);
    return Map.of("ok", true);
  }

  /** 冻结套餐快照与分钟数。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/enrollments")
  public Object enroll(@RequestBody Map<String, Object> b) {
    return service.enroll(b);
  }

  /** 本人或教务财务查看原单和流水。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/enrollments/{id}")
  public Object enrollment(@PathVariable Long id) {
    return service.enrollmentDetail(id);
  }

  /** 收款、停课、原款退款。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/enrollments/{id}/commands/{action}")
  public Object enrollmentCommand(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> b) {
    return service.enrollmentCommand(id, action, b);
  }

  /** 只返回空闲时间，不公开其他学员。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/slots")
  public Object slots(
      @RequestParam Long enrollmentId,
      @RequestParam Long instructorId,
      @RequestParam Long vehicleId,
      @RequestParam String date,
      @RequestParam int minutes,
      @RequestParam(required = false) Long lessonId) {
    return service.slots(enrollmentId, instructorId, vehicleId, date, minutes, lessonId);
  }

  /** 预留分钟并同时占用教练和车辆。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/lessons")
  public Object book(@RequestBody Map<String, Object> b) {
    return service.book(b);
  }

  /** 本人或岗位授权课次详情。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/lessons/{id}")
  public Object lesson(@PathVariable Long id) {
    return service.lessonDetail(id);
  }

  /** 明确状态命令，版本和UUID避免重复核销。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/lessons/{id}/commands/{action}")
  public Object action(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> b) {
    return service.lessonCommand(id, action, b);
  }

  /** 分币种实收、退款、课时与课次报表。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports")
  public Object reports() {
    return service.reports();
  }

  /** 只读取授权组织审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  public Object audit() {
    return service.audit();
  }

  /** 有权限的CSV导出。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/report.csv")
  public ResponseEntity<String> export() {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lessons.csv")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(service.export());
  }

  /** 管理目录选项。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/options")
  public Object adminOptions() {
    return admin.options();
  }

  /** 管理目录读取。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{type}")
  public Object readAdmin(@PathVariable String type) {
    return admin.read(type);
  }

  /** 创建完整目录记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{type}")
  public Object createAdmin(@PathVariable String type, @RequestBody Map<String, Object> b) {
    return admin.save(type, null, b);
  }

  /** 版本化目录更新，保留最后管理员。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{type}/{id}")
  public Object saveAdmin(
      @PathVariable String type, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return admin.save(type, id, b);
  }
}
