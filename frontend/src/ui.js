// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { ref } from "vue";
export const language = ref("zh");
/** 页面双语，不翻译经营者业务名称。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const t = (zh, en) => (language.value === "zh" ? zh : en);
const states = {
  OPEN: ["有效报读", "Active enrollment"],
  CLOSED: ["已停止", "Closed"],
  BOOKED: ["已预约", "Booked"],
  ARRIVED: ["已签到", "Checked in"],
  LIVE: ["授课中", "Teaching"],
  DONE: ["已完成", "Completed"],
  CANCELLED: ["已取消", "Cancelled"],
  LATE_CANCELLED: ["逾期取消扣时", "Late cancel charged"],
  NO_SHOW: ["未到课", "No-show"],
  NEW: ["待跟进", "New"],
  CONTACTED: ["已联系", "Contacted"],
  ENROLLED: ["已报读", "Enrolled"],
  WORK: ["教练工作段", "Instructor work"],
  BLOCK: ["请假／检修", "Leave / maintenance"],
};
/** 状态直接对应持久化记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const stateName = (s) => (states[s] ? t(...states[s]) : s);
const errors = {
  UNAUTHENTICATED: [
    "登录已失效，请重新登录。",
    "Session expired. Sign in again.",
  ],
  LOGIN_FAILED: [
    "账号密码错误或账号停用。",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: [
    "尝试过多，请五分钟后重试。",
    "Too many attempts. Retry in five minutes.",
  ],
  FORBIDDEN: ["没有操作权限。", "Permission denied."],
  OUT_OF_SCOPE: [
    "不在当前账号的业务范围内。",
    "Record outside your account scope.",
  ],
  VERSION_CONFLICT: [
    "记录已变化，请刷新核对。",
    "Record changed. Refresh and verify.",
  ],
  STATE_INVALID: [
    "当前状态不能执行此操作。",
    "Action not permitted in this state.",
  ],
  INVALID_INPUT: [
    "请核对必填字段与格式。",
    "Check required fields and formats.",
  ],
  NOT_FOUND: ["记录不存在。", "Record not found."],
  CONFLICT: ["记录重复或仍被引用。", "Duplicate or referenced record."],
  PASSWORD_WEAK: [
    "密码需12–72字节，含大小写字母及数字。",
    "Password: 12–72 bytes, upper/lowercase and digit.",
  ],
  LAST_ADMIN: [
    "必须保留启用的完整管理员。",
    "Keep an active full administrator.",
  ],
  STUDENT_ROLE_INVALID: [
    "学员角色仅有业务目录和本人约课权限。",
    "Student roles require only catalog and portal permissions.",
  ],
  STUDENT_INVALID: [
    "学员停用、账号不匹配或报读无效。",
    "Student or enrollment unavailable.",
  ],
  INSTRUCTOR_INVALID: [
    "请选择同分校启用且有教学权限的教练账号。",
    "Select an active instructor account in this school.",
  ],
  STUDENT_PROFILE_REQUIRED: [
    "账号尚未绑定学员档案，请联系前台。",
    "Account has no student profile. Contact the office.",
  ],
  INSTRUCTOR_PROFILE_REQUIRED: [
    "账号尚未绑定教练档案，请联系管理员。",
    "Account has no instructor profile.",
  ],
  ACTIVE_LESSONS: [
    "仍有未完成课次，请先处理预约。",
    "Handle active lessons first.",
  ],
  CATEGORY_DISABLED: ["教学类别已停用。", "Training category disabled."],
  CATEGORY_MISMATCH: [
    "套餐、教练和车辆类别必须一致。",
    "Plan, instructor and vehicle category must match.",
  ],
  PLAN_UNAVAILABLE: ["套餐或学员无效。", "Plan or student unavailable."],
  PAYMENT_REQUIRED: [
    "报读须足额核实收款后才能约课。",
    "Full verified payment required before booking.",
  ],
  PAYMENT_EXCESS: ["收款超过套餐应收金额。", "Payment exceeds plan balance."],
  REFUND_EXCESS: [
    "超过可退余额或原收款余额。",
    "Refund exceeds eligible or original-payment balance.",
  ],
  CLOSE_BEFORE_REFUND: [
    "先取消未完成课次并停止报读，再退款。",
    "Cancel active lessons and close enrollment before refund.",
  ],
  ORIGINAL_PAYMENT_INVALID: [
    "请选择此报读的原收款。",
    "Select an original payment from this enrollment.",
  ],
  PAYMENT_REFERENCE_REUSED: [
    "收退款凭据编号已使用。",
    "Receipt reference already used.",
  ],
  CREDITS_INSUFFICIENT: ["可用课时不足。", "Insufficient available minutes."],
  CREDIT_INVARIANT: [
    "课时余额核对失败，请刷新。",
    "Minute balance check failed. Refresh.",
  ],
  RESOURCE_DISABLED: [
    "教练、账号或车辆已停用。",
    "Instructor, account or vehicle disabled.",
  ],
  RESOURCE_BLOCKED: [
    "教练请假或车辆检修占用此时段。",
    "Instructor leave or vehicle maintenance blocks this slot.",
  ],
  RESOURCE_CONFLICT: [
    "教练或车辆时段冲突（含课间缓冲）。",
    "Instructor or vehicle conflict including buffer.",
  ],
  STUDENT_CONFLICT: [
    "学员在此时段已有课程。",
    "Student already has a lesson in this slot.",
  ],
  OUTSIDE_WORK_WINDOW: [
    "不在已发布教练工作时段内（含缓冲）。",
    "Outside instructor work window including buffer.",
  ],
  PASS_EXPIRED: ["超出报读有效日期。", "Outside enrollment validity dates."],
  DURATION_INVALID: [
    "每课30–180分钟，按15分钟递增。",
    "Lessons: 30–180 minutes in 15-minute increments.",
  ],
  BOOKING_WINDOW: [
    "预约须符合提前时间和可预约天数。",
    "Outside booking lead time or advance window.",
  ],
  TIME_INVALID: [
    "请核对开始结束时间，使用整分钟。",
    "Check start/end times; use whole minutes.",
  ],
  TIMEZONE_LOCKED: [
    "已有课次后不能修改分校时区。",
    "School timezone is fixed after its first lesson.",
  ],
  WINDOW_INVALID: [
    "工作段须对应一名教练且最多12小时；请假检修最多7天。",
    "Work: one instructor, max 12h. Blocks: max 7 days.",
  ],
  WINDOW_CONFLICT: [
    "同一资源的同类时段已存在。",
    "Same resource already has an overlapping window.",
  ],
  INSTRUCTOR_ONLY: [
    "只能操作本人教练的课次或工作时段。",
    "Only your assigned lessons or availability.",
  ],
  CHECKIN_WINDOW: [
    "签到和开课须在课前15分钟起；签到不得超过结束时间。",
    "Check in from 15 minutes before start and before lesson end.",
  ],
  NO_SHOW_TOO_EARLY: [
    "开课15分钟后才可登记未到课。",
    "No-show only after 15 minutes past lesson start.",
  ],
  CANCELLATION_DEADLINE: [
    "已过自助改期截止时间，请联系前台。",
    "Reschedule deadline passed. Contact the office.",
  ],
  REQUEST_ID_INVALID: [
    "请求编号无效，请刷新。",
    "Invalid request ID. Refresh.",
  ],
  REQUEST_REUSED: [
    "请求编号已用于其他操作。",
    "Request ID reused for different input.",
  ],
  IDENTITY_LOCKED: [
    "已有记录的账号或组织不能改换。",
    "Account or organization is immutable.",
  ],
  DEPARTMENT_DISABLED: ["分校已停用。", "School disabled."],
  CURRENCY_INVALID: [
    "请选择两位小数的ISO货币代码。",
    "Use an ISO currency with two decimal places.",
  ],
  SYSTEM_DIRECTORY_FIXED: [
    "系统目录仅允许修改已有记录。",
    "Edit existing system directory entries.",
  ],
  RESOURCE_LIMIT: ["达到目录数据上限。", "Directory record limit reached."],
  INQUIRY_THROTTLED: [
    "提交过于频繁，请十分钟后重试。",
    "Too many inquiries; retry in ten minutes.",
  ],
  RESULT_UNKNOWN: [
    "连接中断，结果未知。刷新核对后再操作，勿直接重复提交。",
    "Connection lost; result unknown. Refresh and verify before resubmitting.",
  ],
  NETWORK_ERROR: [
    "无法连接服务器，请检查网络。",
    "Server unavailable. Check the connection.",
  ],
};
/** 清晰业务反馈，不暴露配置、密码或异常堆栈。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const errorMessage = (e) =>
  errors[e.message]
    ? t(...errors[e.message])
    : t("操作未完成", "Action failed") + " (" + e.message + ")";
export const confirmation = ref(null);
/** 页面内确认，避免系统弹窗混入截图。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function ask(message) {
  if (confirmation.value) return Promise.resolve(false);
  return new Promise((resolve) => (confirmation.value = { message, resolve }));
}
/** 完成当前业务确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function answerConfirmation(ok) {
  const p = confirmation.value;
  confirmation.value = null;
  p?.resolve(ok);
}
/** 编辑使用副本，不直接改动列表记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const cloneRecord = (r) => JSON.parse(JSON.stringify(r));
/** 用户浏览器本地时间显示，记录统一存UTC。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const dateTime = (v) =>
  v
    ? new Intl.DateTimeFormat(language.value === "zh" ? "zh-CN" : "en", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(v))
    : "—";
/** 金额使用最小单位并按原单币种显示。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const money = (v, c = "CNY") =>
  new Intl.NumberFormat(language.value === "zh" ? "zh-CN" : "en", {
    style: "currency",
    currency: c,
  }).format(Number(v || 0) / 100);
/** 本地表单转UTC，拒绝夏令时不存在的本地分钟。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function toUtc(v) {
  const d = new Date(v);
  if (!v || Number.isNaN(d.getTime())) throw new Error("TIME_INVALID");
  const parts = [
    d.getFullYear(),
    String(d.getMonth() + 1).padStart(2, "0"),
    String(d.getDate()).padStart(2, "0"),
  ];
  const local =
    parts[0] +
    "-" +
    parts[1] +
    "-" +
    parts[2] +
    "T" +
    String(d.getHours()).padStart(2, "0") +
    ":" +
    String(d.getMinutes()).padStart(2, "0");
  if (local !== v) throw new Error("TIME_INVALID");
  return d.toISOString();
}
/** UTC记录转浏览器分钟精度表单。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function toLocal(v) {
  if (!v) return "";
  const d = new Date(v);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}T${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

/** 教学类别显示经营者名称，界面不要求读者理解内部代码。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function categoryLabel(code, categories = []) {
  return String(code || "")
    .split(",")
    .map((c) => {
      const row = categories.find((x) => x.code === c);
      return row ? t(row.name, row.nameEn) : c;
    })
    .join(" / ");
}
