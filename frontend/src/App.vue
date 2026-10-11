<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import {
  CalendarDays,
  Users,
  Car,
  GraduationCap,
  CreditCard,
  BarChart3,
  Shield,
  Settings,
  LogOut,
  Plus,
  Search,
  X,
  ChevronLeft,
  Download,
} from "@lucide/vue";
import { api, resetApi, download } from "./api.js";
import {
  language,
  t,
  stateName,
  money,
  categoryLabel,
  dateTime,
  errorMessage,
  ask,
  confirmation,
  answerConfirmation,
} from "./ui.js";
import MasterPanel from "./components/MasterPanel.vue";
import EnrollmentPanel from "./components/EnrollmentPanel.vue";
import AdminPanel from "./components/AdminPanel.vue";
const profile = ref(null),
  loading = ref(true),
  publicMode = ref(location.hash === "#public"),
  busy = ref(0),
  message = ref(""),
  section = ref("lessons"),
  masterKind = ref("instructors"),
  portalKind = ref("enrollments"),
  ownWindows = ref(false),
  panel = ref(null),
  about = ref(false),
  credentials = ref({ username: "", password: "" });
const pending = computed(() => busy.value > 0),
  options = ref({
    departments: [],
    students: [],
    instructors: [],
    vehicles: [],
    plans: [],
    accounts: [],
    categories: [],
  }),
  rows = ref({ items: [], total: 0 }),
  page = ref(0),
  search = ref(""),
  status = ref(""),
  sort = ref("time"),
  enrollmentId = ref(null),
  detail = ref(null),
  report = ref(null),
  audit = ref([]),
  dialog = ref(null),
  slots = ref([]),
  slotChecked = ref(false);
const publicData = ref({ schools: [], plans: [] }),
  inquiry = ref({
    departmentId: "",
    name: "",
    contact: "",
    note: "",
    website: "",
  }),
  receipt = ref("");
const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
const can = (c) => profile.value?.permissions.includes(c);
const icons = {
  lessons: CalendarDays,
  portal: GraduationCap,
  coach: GraduationCap,
  students: Users,
  enrollments: CreditCard,
  masters: Car,
  inquiries: Users,
  reports: BarChart3,
  audit: Shield,
  users: Users,
  roles: Shield,
  settings: Settings,
};
const title = computed(() => {
  const m = profile.value?.menus.find((m) => m.code === section.value);
  return m ? t(m.name, m.nameEn) : t("预约与课表", "Lessons & schedule");
});
const kind = computed(() =>
  section.value === "enrollments" ||
  (section.value === "portal" && portalKind.value === "enrollments")
    ? "enrollments"
    : "lessons",
);
const actionNames = computed(() => ({
  BOOKED: t("预约建单", "Booking created"),
  CANCEL: t("取消预约", "Cancel lesson"),
  RESCHEDULE: t("改期", "Reschedule"),
  ARRIVE: t("签到到课", "Check in"),
  START: t("开始授课", "Start lesson"),
  FINISH: t("完成授课并核销", "Finish and use minutes"),
  NOSHOW: t("登记未到课", "No-show"),
  BOOK: t("预约课程", "Book lesson"),
  ENROLL: t("新增报读", "Enroll student"),
  PASSWORD: t("修改本人密码", "Change password"),
}));
/** 写操作不重试；失效会话立即清空私有页面，临时密码不写本地存储。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function perform(fn) {
  busy.value++;
  message.value = "";
  try {
    return await fn();
  } catch (e) {
    message.value = errorMessage(e);
    if (e.status === 401) {
      profile.value = null;
      dialog.value = null;
      detail.value = null;
      enrollmentId.value = null;
      rows.value = { items: [], total: 0 };
      resetApi();
    }
    return null;
  } finally {
    busy.value--;
  }
}
async function loadOptions() {
  options.value = await api("/options");
}
let sequence = 0;
/** 真实分页和筛选；旧响应不能覆盖新条件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function load() {
  const n = ++sequence;
  if (["lessons", "portal", "coach", "enrollments"].includes(section.value)) {
    const out = await api(
      `/records/${kind.value}?page=${page.value}&size=15&search=${encodeURIComponent(search.value)}&status=${status.value}&sort=${sort.value}`,
    );
    if (n === sequence) rows.value = out;
  } else if (section.value === "reports") report.value = await api("/reports");
  else if (section.value === "audit") audit.value = await api("/audit");
}
async function refresh() {
  await perform(async () => {
    await loadOptions();
    await load();
    return true;
  });
}
async function navigate(k) {
  if (panel.value?.canLeave && !(await panel.value.canLeave())) return;
  section.value = k;
  page.value = 0;
  search.value = "";
  status.value = "";
  enrollmentId.value = null;
  detail.value = null;
  ownWindows.value = false;
  await perform(load);
}
function find() {
  page.value = 0;
  perform(load);
}
function turn(delta) {
  page.value += delta;
  perform(load);
}
/** 登录表单使用后立即清空临时口令，页面测试复用正常会话。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function login() {
  await perform(async () => {
    profile.value = await api("/auth/login", {
      method: "POST",
      body: credentials.value,
    });
    credentials.value.password = "";
    resetApi();
    section.value = profile.value.menus[0]?.code || "lessons";
    await loadOptions();
    await load();
    return true;
  });
}
async function logout() {
  if (!(await ask(t("退出当前账号？", "Sign out?")))) return;
  await perform(async () => {
    await api("/auth/logout", { method: "POST" });
    profile.value = null;
    detail.value = null;
    enrollmentId.value = null;
    resetApi();
    return true;
  });
}
async function openPublic() {
  publicMode.value = true;
  location.hash = "public";
  await perform(async () => {
    publicData.value = await api("/public/catalog");
    inquiry.value.departmentId = publicData.value.schools[0]?.id || "";
    return true;
  });
}
async function inquirySubmit() {
  await perform(async () => {
    const out = await api("/public/inquiries", {
      method: "POST",
      body: inquiry.value,
    });
    receipt.value = out.reference;
    inquiry.value = { ...inquiry.value, name: "", contact: "", note: "" };
    return true;
  });
}
async function show(id) {
  if (kind.value === "enrollments") enrollmentId.value = id;
  else
    await perform(async () => {
      detail.value = await api("/lessons/" + id);
      return true;
    });
}
async function closeDetail() {
  detail.value = null;
  enrollmentId.value = null;
  await refresh();
}
async function backToCoach() {
  if (!panel.value?.canLeave || (await panel.value.canLeave()))
    ownWindows.value = false;
}
async function changeMaster(k) {
  if (!panel.value?.canLeave || (await panel.value.canLeave()))
    masterKind.value = k;
}
/** 冻结当前报读版本，空档查询与实际落单分别校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function book(e, l = null) {
  await perform(async () => {
    await loadOptions();
    const d = await api("/enrollments/" + e.id);
    const p = d.enrollment,
      school = options.value.departments.find((x) => x.id === p.departmentId);
    dialog.value = {
      action: l ? "RESCHEDULE" : "BOOK",
      id: l?.id,
      version: l?.version,
      enrollmentId: p.id,
      enrollmentVersion: p.version,
      departmentId: p.departmentId,
      category: p.category,
      planName: p.planName,
      cancelHours: p.cancelHours,
      lateCharge: p.lateCharge,
      instructorId:
        l?.instructorId ||
        options.value.instructors.find(
          (i) =>
            i.departmentId === p.departmentId &&
            i.category.split(",").includes(p.category),
        )?.id ||
        "",
      vehicleId:
        l?.vehicleId ||
        options.value.vehicles.find(
          (v) => v.departmentId === p.departmentId && v.category === p.category,
        )?.id ||
        "",
      minutes: l?.minutes || 60,
      date: (l ? new Date(l.startsAt) : new Date()).toLocaleDateString(
        "sv-SE",
        { timeZone: school?.zone || "UTC" },
      ),
      startsAt: "",
      note: "",
      requestId: crypto.randomUUID(),
    };
    slots.value = [];
    slotChecked.value = false;
    return true;
  });
}
async function enroll() {
  await perform(async () => {
    await loadOptions();
    dialog.value = {
      action: "ENROLL",
      studentId: options.value.students.find((s) => s.enabled)?.id || "",
      planId: options.value.plans[0]?.id || "",
      startsOn: new Date().toLocaleDateString("sv-SE"),
      note: "",
      requestId: crypto.randomUUID(),
    };
    return true;
  });
}
function action(a) {
  const l = detail.value?.lesson;
  dialog.value =
    a === "PASSWORD"
      ? { action: a, oldPassword: "", newPassword: "" }
      : {
          action: a,
          id: l.id,
          version: l.version,
          requestId: crypto.randomUUID(),
          actualMinutes: l.minutes,
          topic: "",
          progress: "",
          note: "",
        };
}
function resetSlots() {
  slots.value = [];
  slotChecked.value = false;
  if (dialog.value) dialog.value.startsAt = "";
}
async function querySlots() {
  await perform(async () => {
    const b = dialog.value;
    slots.value = await api(
      `/slots?enrollmentId=${b.enrollmentId}&instructorId=${b.instructorId}&vehicleId=${b.vehicleId}&date=${b.date}&minutes=${b.minutes}${b.action === "RESCHEDULE" ? "&lessonId=" + b.id : ""}`,
    );
    slotChecked.value = true;
    return true;
  });
}
/** 明确确认扣时后提交固定请求ID；不自动重放未知结果。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function submit() {
  const b = { ...dialog.value };
  if (
    ["CANCEL", "NOSHOW"].includes(b.action) &&
    !(await ask(
      t(
        "确认执行？是否扣时按购买时的报读规则处理，可在报读原单查看；前台取消可说明理由豁免。",
        "Confirm? Any minute charge follows the rules frozen on the enrollment. Review the enrollment record; office cancellation may waive with reason.",
      ),
    ))
  )
    return;
  await perform(async () => {
    if (b.action === "PASSWORD") {
      await api("/auth/password", { method: "POST", body: b });
      profile.value = null;
      resetApi();
    } else if (b.action === "ENROLL") {
      const out = await api("/enrollments", { method: "POST", body: b });
      enrollmentId.value = out.enrollment.id;
    } else {
      const path =
        b.action === "BOOK"
          ? "/lessons"
          : `/lessons/${b.id}/commands/${b.action}`;
      detail.value = await api(path, { method: "POST", body: b });
      enrollmentId.value = null;
    }
    dialog.value = null;
    if (profile.value) {
      await loadOptions();
      await load();
    }
    return true;
  });
}
async function closeDialog() {
  if (await ask(t("关闭尚未提交的表单？", "Close this unsubmitted form?")))
    dialog.value = null;
}
const masterMode = computed(
  () =>
    ["masters", "students", "inquiries"].includes(section.value) ||
    (section.value === "coach" && ownWindows.value),
);
const dialogFields = computed(() => {
  const b = dialog.value;
  if (!b) return [];
  const note = {
    key: "note",
    label: t("说明／理由", "Notes / reason"),
    type: "textarea",
    required: !["BOOK", "ENROLL"].includes(b.action),
    max: 1000,
  };
  if (b.action === "PASSWORD")
    return [
      {
        key: "oldPassword",
        label: t("原密码", "Current password"),
        type: "password",
        required: true,
      },
      {
        key: "newPassword",
        label: t(
          "新密码（12–72字节，含大小写和数字）",
          "New password (12–72 bytes, upper/lowercase and digit)",
        ),
        type: "password",
        required: true,
        minlength: 12,
        max: 72,
      },
    ];
  if (b.action === "ENROLL")
    return [
      {
        key: "studentId",
        label: t("学员", "Student"),
        type: "select",
        choices: options.value.students.filter((s) => s.enabled),
        required: true,
      },
      {
        key: "planId",
        label: t("套餐", "Package"),
        type: "select",
        choices: options.value.plans
          .filter(
            (p) =>
              p.departmentId ===
              options.value.students.find((s) => s.id === Number(b.studentId))
                ?.departmentId,
          )
          .map((p) => ({
            ...p,
            name:
              p.name +
              " · " +
              p.totalMinutes +
              " min · " +
              money(p.priceMinor, p.currency),
          })),
        required: true,
      },
      {
        key: "startsOn",
        label: t("生效日期", "Valid from"),
        type: "date",
        required: true,
      },
      note,
    ];
  if (["BOOK", "RESCHEDULE"].includes(b.action))
    return [
      {
        key: "instructorId",
        label: t("教练", "Instructor"),
        type: "select",
        choices: options.value.instructors.filter(
          (i) =>
            i.departmentId === b.departmentId &&
            i.category.split(",").includes(b.category),
        ),
        required: true,
      },
      {
        key: "vehicleId",
        label: t("教练车", "Vehicle"),
        type: "select",
        choices: options.value.vehicles.filter(
          (v) => v.departmentId === b.departmentId && v.category === b.category,
        ),
        required: true,
      },
      {
        key: "date",
        label: t("查询日期（分校时区）", "Query date (school zone)"),
        type: "date",
        required: true,
      },
      {
        key: "minutes",
        label: t("课程分钟", "Lesson minutes"),
        type: "number",
        min: 30,
        max: 180,
        step: 15,
        required: true,
        locked: b.action === "RESCHEDULE",
      },
      note,
    ];
  if (b.action === "FINISH")
    return [
      {
        key: "actualMinutes",
        label: t(
          "实际授课分钟（教练登记）",
          "Actual minutes (instructor record)",
        ),
        type: "number",
        min: 0,
        max: detail.value.lesson.minutes,
        required: true,
      },
      {
        key: "topic",
        label: t("训练项目", "Training topic"),
        required: true,
        max: 200,
      },
      {
        key: "progress",
        label: t("训练进度与反馈", "Progress and feedback"),
        type: "textarea",
        required: true,
        max: 1000,
      },
      note,
    ];
  return [note];
});
const auditFiltered = computed(() =>
  audit.value.filter((x) =>
    `${x.actor} ${x.action} ${x.objectId}`
      .toLowerCase()
      .includes(search.value.toLowerCase()),
  ),
);
const auditVisible = computed(() =>
  auditFiltered.value.slice(page.value * 15, page.value * 15 + 15),
);
onMounted(async () => {
  try {
    profile.value = await api("/auth/me");
    section.value = profile.value.menus[0]?.code || "lessons";
    await loadOptions();
    await load();
  } catch (e) {
    if (e.status !== 401) message.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
  if (publicMode.value) await openPublic();
});
</script>
<template>
  <div v-if="loading" class="loading">
    {{ t("正在读取会话", "Loading session") }}
  </div>
  <div v-else-if="publicMode" class="public-page">
    <header class="public-header">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" /><strong>LessonDesk</strong>
      </div>
      <div class="actions">
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
          t("知华科技", "ZhiHua Technology")
        }}</a
        ><button @click="language = language === 'zh' ? 'en' : 'zh'">
          {{ t("English", "中文") }}</button
        ><button
          @click="
            publicMode = false;
            location.hash = '';
          "
        >
          {{
            profile
              ? t("返回工作台", "Back to desk")
              : t("学员／员工登录", "Student / staff sign in")
          }}
        </button>
      </div>
    </header>
    <main class="public-content">
      <h1>{{ t("课程与报名咨询", "Lesson packages & inquiry") }}</h1>
      <p>
        {{
          t(
            "已有学员账号可登录查看本人课时并预约。",
            "Existing students can sign in to view their credits and book lessons.",
          )
        }}
      </p>
      <p v-if="message" class="notice error" role="alert">{{ message }}</p>
      <div class="public-plan-grid">
        <article class="panel" v-for="p in publicData.plans" :key="p.id">
          <p class="muted">
            {{ publicData.schools.find((s) => s.id === p.departmentId)?.name }}
          </p>
          <h2>{{ p.name }}</h2>
          <p>
            {{ categoryLabel(p.category, publicData.categories) }} ·
            {{ p.totalMinutes }} {{ t("分钟", "min") }} · {{ p.validDays }}
            {{ t("天有效", "valid days") }}
          </p>
          <strong class="package-price">{{
            money(p.priceMinor, p.currency)
          }}</strong
          ><button
            @click="
              inquiry.departmentId = p.departmentId;
              inquiry.note =
                t('我想了解：', 'I would like to ask about: ') + p.name;
            "
          >
            {{ t("咨询课程", "Ask about package") }}
          </button>
        </article>
        <div class="panel empty" v-if="!publicData.plans.length">
          {{
            t(
              "暂无公开套餐，可向分校咨询。",
              "No public packages; contact the school.",
            )
          }}
        </div>
      </div>
      <section class="panel">
        <h2>{{ t("向分校咨询", "Contact the school") }}</h2>
        <p class="notice" role="status" v-if="receipt">
          {{ t("已收到咨询，回执编号", "Inquiry received, reference") }}:
          {{ receipt }}
        </p>
        <form class="form-grid" @submit.prevent="inquirySubmit">
          <label
            >{{ t("分校", "School")
            }}<select
              v-model="inquiry.departmentId"
              required
              :disabled="pending"
            >
              <option v-for="s in publicData.schools" :key="s.id" :value="s.id">
                {{ s.name }}
              </option>
            </select></label
          ><label
            >{{ t("称呼", "Name")
            }}<input
              v-model="inquiry.name"
              required
              maxlength="120"
              :disabled="pending" /></label
          ><label
            >{{ t("联系邮箱／电话", "Contact email / phone")
            }}<input
              v-model="inquiry.contact"
              required
              maxlength="120"
              :disabled="pending" /></label
          ><label class="span-2"
            >{{ t("咨询内容", "Inquiry")
            }}<textarea
              v-model="inquiry.note"
              required
              maxlength="1000"
              rows="3"
              :disabled="pending"
            /></label
          ><label class="trap" aria-hidden="true"
            >Website<input
              v-model="inquiry.website"
              tabindex="-1"
              autocomplete="off"
          /></label>
          <div class="actions span-2">
            <button
              class="primary"
              :disabled="pending || !publicData.schools.length"
            >
              {{ t("提交咨询", "Submit inquiry") }}</button
            ><small>{{
              t(
                "联系方式仅供分校跟进，不公开展示。",
                "Your contact details are private and used for school follow-up.",
              )
            }}</small>
          </div>
        </form>
      </section>
    </main>
    <footer class="public-footer">
      {{
        t(
          "公开源码学习版；商用须书面授权。",
          "Non-commercial source edition; commercial use requires written authorization.",
        )
      }}
    </footer>
  </div>
  <div v-else-if="!profile" class="login-page">
    <div class="login-shell">
      <section class="login-intro">
        <img src="/brand/logo.jpg" alt="知华科技" />
        <h1>LessonDesk</h1>
        <h2>{{ t("驾校约课与教务", "Driving school scheduling") }}</h2>
        <p>
          {{
            t(
              "学员约课 · 教练车辆排课 · 课时与收退费",
              "Student bookings · Instructor and vehicle schedules · Credits and fee records",
            )
          }}
        </p>
        <button @click="openPublic">
          {{ t("查看课程／报名咨询", "Packages / enrollment inquiry") }}</button
        ><a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
          t("知华科技官网", "ZhiHua Technology")
        }}</a>
      </section>
      <section class="login-form">
        <button @click="language = language === 'zh' ? 'en' : 'zh'">
          {{ t("English", "中文") }}
        </button>
        <h2>{{ t("账号登录", "Sign in") }}</h2>
        <p>
          {{
            t(
              "学员、教练和教务使用各自账号。",
              "Students, instructors and office staff use individual accounts.",
            )
          }}
        </p>
        <p v-if="message" class="notice error" role="alert">{{ message }}</p>
        <form @submit.prevent="login" autocomplete="off">
          <label
            >{{ t("账号", "Username")
            }}<input
              v-model="credentials.username"
              required
              maxlength="60"
              autocomplete="off"
              :disabled="pending" /></label
          ><label
            >{{ t("密码", "Password")
            }}<input
              v-model="credentials.password"
              type="password"
              required
              maxlength="128"
              autocomplete="off"
              :disabled="pending" /></label
          ><button class="primary" :disabled="pending">
            {{ pending ? t("登录中", "Signing in") : t("登录", "Sign in") }}
          </button>
        </form>
        <p class="hint">
          {{
            t(
              "无统一默认密码，账号由分校创建。",
              "No shared default password. Accounts are created by the school.",
            )
          }}
        </p>
        <small>{{
          t(
            "知华科技（上海如静知华信息科技有限公司）",
            "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
          )
        }}</small>
        <p class="hint">
          {{
            t(
              "公开源码学习版 · 商用须授权",
              "Non-commercial source edition · Commercial use requires authorization",
            )
          }}
        </p>
      </section>
    </div>
  </div>
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" />
        <div>
          <strong>LessonDesk</strong
          ><small>{{ t("驾校约课与教务", "Driving school desk") }}</small>
        </div>
      </div>
      <nav>
        <button
          v-for="m in profile.menus"
          :key="m.code"
          :class="{ active: section === m.code }"
          @click="navigate(m.code)"
          :disabled="pending"
        >
          <component :is="icons[m.code] || CalendarDays" :size="18" />{{
            t(m.name, m.nameEn)
          }}
        </button>
      </nav>
      <footer class="sidebar-footer">
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
          t("知华科技官网", "ZhiHua Technology")
        }}</a
        ><button @click="about = true">
          {{ t("关于与商业授权", "About & licensing") }}</button
        ><small
          >{{ t("公开源码学习版", "Non-commercial source") }} · 1.0.0</small
        >
      </footer>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <span class="grow">{{ title }}</span>
        <div class="actions">
          <button @click="openPublic">
            {{ t("公开课程", "Public packages") }}</button
          ><button @click="language = language === 'zh' ? 'en' : 'zh'">
            {{ t("English", "中文") }}</button
          ><span>{{ profile.displayName }}</span
          ><button @click="action('PASSWORD')" :disabled="pending">
            {{ t("改密", "Password") }}</button
          ><button
            @click="logout"
            :disabled="pending"
            :aria-label="t('退出', 'Sign out')"
          >
            <LogOut :size="18" />
          </button>
        </div>
      </header>
      <main class="main-content">
        <div v-if="message" class="notice error" role="alert">
          {{ message
          }}<button
            @click="message = ''"
            :aria-label="t('关闭提示', 'Dismiss')"
          >
            <X :size="16" />
          </button>
        </div>
        <AdminPanel
          v-if="['users', 'roles', 'settings'].includes(section)"
          ref="panel"
          :section="section"
          :pending="pending"
          :message="message"
          :perform="perform"
          @error="(e) => (message = errorMessage(e))"
        />
        <template v-else-if="masterMode"
          ><div class="tabs" v-if="section === 'masters'">
            <button
              v-for="k in ['instructors', 'vehicles', 'plans', 'windows']"
              :key="k"
              :class="{ active: masterKind === k }"
              @click="changeMaster(k)"
              :disabled="pending"
            >
              {{
                {
                  instructors: t("教练", "Instructors"),
                  vehicles: t("教练车", "Vehicles"),
                  plans: t("套餐", "Packages"),
                  windows: t("工作／请假／检修", "Work / leave / maintenance"),
                }[k]
              }}
            </button>
          </div>
          <button v-if="section === 'coach'" @click="backToCoach">
            <ChevronLeft :size="16" />{{
              t("返回课表", "Back to lessons")
            }}</button
          ><MasterPanel
            ref="panel"
            :kind="
              section === 'masters'
                ? masterKind
                : section === 'coach'
                  ? 'windows'
                  : section
            "
            :options="options"
            :profile="profile"
            :pending="pending"
            :perform="perform"
            @changed="perform(loadOptions)"
        /></template>
        <section v-else-if="section === 'reports'">
          <header class="page-title">
            <div>
              <h1>{{ t("经营报表", "School reports") }}</h1>
              <p>
                {{
                  t(
                    "实收、退款按原单币种分列，预约不计作收入。",
                    "Cash is grouped by original currency; bookings are not income.",
                  )
                }}
              </p>
            </div>
            <div class="actions">
              <button @click="refresh" :disabled="pending">
                {{ t("刷新", "Refresh") }}</button
              ><button
                v-if="can('export')"
                @click="perform(() => download('/report.csv', 'lessons.csv'))"
                :disabled="pending"
              >
                <Download :size="16" />CSV
              </button>
            </div>
          </header>
          <template v-if="report"
            ><div class="stats-strip">
              <div>
                <span>{{ t("学员", "Students") }}</span
                ><strong>{{ report.students }}</strong>
              </div>
              <div>
                <span>{{ t("未完成预约", "Active lessons") }}</span
                ><strong>{{ report.booked }}</strong>
              </div>
              <div>
                <span>{{ t("完成授课", "Completed lessons") }}</span
                ><strong>{{ report.completed }}</strong>
              </div>
              <div>
                <span>{{ t("预留／核销分钟", "Reserved / used min") }}</span
                ><strong
                  >{{ report.heldMinutes }} / {{ report.spentMinutes }}</strong
                >
              </div>
            </div>
            <section class="panel">
              <h2>{{ t("收退款与课时价值", "Cash and used-minute value") }}</h2>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("币种", "Currency") }}</th>
                      <th>{{ t("累计实收", "Collected") }}</th>
                      <th>{{ t("已退", "Refunded") }}</th>
                      <th>{{ t("净收款", "Net cash") }}</th>
                      <th>
                        {{ t("核销对应套餐价值", "Used-minute package value") }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in report.currencies" :key="c.currency">
                      <td>{{ c.currency }}</td>
                      <td>{{ money(c.collectedMinor, c.currency) }}</td>
                      <td>{{ money(c.refundedMinor, c.currency) }}</td>
                      <td>{{ money(c.netMinor, c.currency) }}</td>
                      <td>{{ money(c.earnedMinor, c.currency) }}</td>
                    </tr>
                    <tr v-if="!report.currencies.length">
                      <td colspan="5" class="empty">
                        {{ t("尚无报读", "No enrollments") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p class="hint">
                {{
                  t(
                    "核销价值含规则扣时，不是税务或会计确认报表。",
                    "Used-minute value includes rule-based charges, not a tax or accounting statement.",
                  )
                }}
              </p>
            </section></template
          >
        </section>
        <section v-else-if="section === 'audit'">
          <header class="page-title">
            <h1>{{ t("操作审计", "Audit log") }}</h1>
            <button @click="refresh" :disabled="pending">
              {{ t("刷新", "Refresh") }}
            </button>
          </header>
          <section class="panel">
            <label class="search-box"
              ><Search :size="16" /><input
                v-model="search"
                @input="page = 0"
                :placeholder="t('搜索账号或动作', 'Search actor or action')"
            /></label>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{{ t("时间", "Time") }}</th>
                    <th>{{ t("账号", "Actor") }}</th>
                    <th>{{ t("动作", "Action") }}</th>
                    <th>{{ t("对象", "Object") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="a in auditVisible" :key="a.id">
                    <td>{{ dateTime(a.createdAt) }}</td>
                    <td>{{ a.actor }}</td>
                    <td>{{ a.action }}</td>
                    <td>{{ a.objectId }}</td>
                  </tr>
                  <tr v-if="!auditVisible.length">
                    <td colspan="4" class="empty">
                      {{ t("暂无记录", "No records") }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <footer class="pager">
              <span
                >{{ page + 1 }} /
                {{ Math.max(1, Math.ceil(auditFiltered.length / 15)) }}</span
              >
              <div>
                <button @click="page--" :disabled="pending || page === 0">
                  {{ t("上一页", "Previous") }}</button
                ><button
                  @click="page++"
                  :disabled="pending || (page + 1) * 15 >= auditFiltered.length"
                >
                  {{ t("下一页", "Next") }}
                </button>
              </div>
            </footer>
          </section>
        </section>
        <template v-else
          ><EnrollmentPanel
            v-if="enrollmentId"
            :id="enrollmentId"
            :profile="profile"
            :pending="pending"
            :perform="perform"
            @book="book"
            @close="closeDetail"
            @changed="refresh"
          />
          <section class="panel" v-else-if="detail">
            <header class="section-title">
              <div>
                <h1>
                  {{ detail.lesson.studentName }} ·
                  {{
                    categoryLabel(detail.lesson.category, options.categories)
                  }}
                </h1>
                <p>
                  {{ detail.lesson.reference }} ·
                  <span class="badge">{{
                    stateName(detail.lesson.status)
                  }}</span>
                </p>
              </div>
              <button @click="closeDetail" :disabled="pending">
                <ChevronLeft :size="16" />{{ t("返回列表", "Back to list") }}
              </button>
            </header>
            <dl class="detail-grid">
              <div>
                <dt>{{ t("课程时段", "Lesson time") }}</dt>
                <dd>
                  {{ dateTime(detail.lesson.startsAt) }} →
                  {{ dateTime(detail.lesson.endsAt) }}<small>{{ zone }}</small>
                </dd>
              </div>
              <div>
                <dt>{{ t("教练与车", "Instructor / vehicle") }}</dt>
                <dd>
                  {{ detail.lesson.instructorName }} ·
                  {{ detail.lesson.vehicleName }}
                </dd>
              </div>
              <div>
                <dt>{{ t("预约／核销分钟", "Booked / used min") }}</dt>
                <dd>
                  {{ detail.lesson.minutes }} /
                  {{ detail.lesson.actualMinutes }}
                </dd>
              </div>
              <div>
                <dt>{{ t("课后缓冲", "After-lesson buffer") }}</dt>
                <dd>
                  {{ detail.lesson.bufferMinutes }} {{ t("分钟", "min") }}
                </dd>
              </div>
              <div>
                <dt>{{ t("说明", "Notes") }}</dt>
                <dd>{{ detail.lesson.note || "—" }}</dd>
              </div>
              <div v-if="detail.lesson.topic">
                <dt>{{ t("训练项目与进度", "Training topic / progress") }}</dt>
                <dd>
                  {{ detail.lesson.topic
                  }}<small>{{ detail.lesson.progress }}</small>
                </dd>
              </div>
            </dl>
            <div class="actions">
              <template
                v-if="
                  (can('schedule') || can('portal')) &&
                  ['BOOKED', 'ARRIVED'].includes(detail.lesson.status)
                "
                ><button
                  v-if="detail.lesson.status === 'BOOKED'"
                  @click="
                    book({ id: detail.lesson.enrollmentId }, detail.lesson)
                  "
                  :disabled="pending"
                >
                  {{ t("改期", "Reschedule") }}</button
                ><button
                  class="danger"
                  @click="action('CANCEL')"
                  :disabled="pending"
                >
                  {{ t("取消预约", "Cancel lesson") }}
                </button></template
              ><button
                v-if="
                  detail.lesson.status === 'BOOKED' &&
                  (can('portal') || can('schedule') || can('coach'))
                "
                @click="action('ARRIVE')"
                :disabled="pending"
              >
                {{ t("签到到课", "Check in") }}</button
              ><button
                v-if="
                  detail.lesson.status === 'BOOKED' &&
                  (can('schedule') || can('coach'))
                "
                @click="action('NOSHOW')"
                :disabled="pending"
              >
                {{ t("登记未到课", "No-show") }}</button
              ><button
                v-if="detail.lesson.status === 'ARRIVED' && can('coach')"
                class="primary"
                @click="action('START')"
                :disabled="pending"
              >
                {{ t("开始授课", "Start lesson") }}</button
              ><button
                v-if="detail.lesson.status === 'LIVE' && can('coach')"
                class="primary"
                @click="action('FINISH')"
                :disabled="pending"
              >
                {{ t("完成授课并核销", "Finish and use minutes") }}</button
              ><button
                v-if="can('enroll') || can('portal')"
                @click="
                  enrollmentId = detail.lesson.enrollmentId;
                  detail = null;
                "
                :disabled="pending"
              >
                {{ t("报读原单", "Enrollment record") }}
              </button>
            </div>
            <h3>{{ t("操作历史", "Lesson history") }}</h3>
            <ol class="history">
              <li v-for="e in detail.events" :key="e.id">
                <strong>{{ actionNames[e.action] || e.action }}</strong> ·
                {{ dateTime(e.createdAt) }} · {{ e.actor }}
                <p>{{ e.note }}</p>
              </li>
            </ol>
          </section>
          <section v-else>
            <header class="page-title">
              <div>
                <h1>{{ title }}</h1>
                <p>{{ t("显示时区", "Display zone") }}: {{ zone }}</p>
              </div>
              <div class="actions">
                <button @click="refresh" :disabled="pending">
                  {{ t("刷新", "Refresh") }}</button
                ><button
                  v-if="section === 'enrollments' && can('enroll')"
                  class="primary"
                  @click="enroll"
                  :disabled="pending"
                >
                  <Plus :size="16" />{{
                    t("新增报读", "Enroll student")
                  }}</button
                ><button
                  v-if="section === 'coach'"
                  @click="ownWindows = true"
                  :disabled="pending"
                >
                  {{ t("工作时段与请假", "Work / leave") }}
                </button>
              </div>
            </header>
            <div class="tabs" v-if="section === 'portal'">
              <button
                v-for="k in ['enrollments', 'lessons']"
                :key="k"
                :class="{ active: portalKind === k }"
                @click="
                  portalKind = k;
                  page = 0;
                  status = '';
                  perform(load);
                "
                :disabled="pending"
              >
                {{
                  k === "enrollments"
                    ? t("我的报读与课时", "My enrollment & credits")
                    : t("我的课表", "My lessons")
                }}
              </button>
            </div>
            <section class="panel">
              <form class="filter-row" @submit.prevent="find">
                <label class="search-box"
                  ><Search :size="16" /><input
                    v-model="search"
                    maxlength="120"
                    :placeholder="
                      t(
                        '搜索编号、学员或教练',
                        'Search reference, student or instructor',
                      )
                    " /></label
                ><select v-model="status">
                  <option value="">{{ t("全部状态", "All statuses") }}</option>
                  <option
                    v-for="s in kind === 'enrollments'
                      ? ['OPEN', 'CLOSED']
                      : [
                          'BOOKED',
                          'ARRIVED',
                          'LIVE',
                          'DONE',
                          'CANCELLED',
                          'LATE_CANCELLED',
                          'NO_SHOW',
                        ]"
                    :key="s"
                    :value="s"
                  >
                    {{ stateName(s) }}
                  </option></select
                ><select v-model="sort">
                  <option value="time">
                    {{ t("时间升序", "Time ascending") }}
                  </option>
                  <option value="newest">
                    {{ t("最新优先", "Newest first") }}
                  </option>
                  <option value="oldest">
                    {{ t("最早创建", "Oldest first") }}
                  </option></select
                ><button :disabled="pending">{{ t("查询", "Search") }}</button
                ><span class="muted"
                  >{{ rows.total }} {{ t("条", "records") }}</span
                >
              </form>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("学员／编号", "Student / reference") }}</th>
                      <th>
                        {{
                          kind === "enrollments"
                            ? t("套餐／有效期", "Package / dates")
                            : t("课程时间", "Lesson time")
                        }}
                      </th>
                      <th>
                        {{
                          kind === "enrollments"
                            ? t("课时与收款", "Credits / payment")
                            : t("教练／车辆", "Instructor / vehicle")
                        }}
                      </th>
                      <th>{{ t("状态", "Status") }}</th>
                      <th>{{ t("操作", "Actions") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="r in rows.items" :key="r.id">
                      <td>
                        <strong>{{
                          kind === "enrollments"
                            ? options.students.find((s) => s.id === r.studentId)
                                ?.name || r.studentId
                            : r.studentName
                        }}</strong
                        ><small>{{ r.reference }}</small>
                      </td>
                      <td v-if="kind === 'enrollments'">
                        {{ r.planName
                        }}<small>{{ r.startsOn }} → {{ r.endsOn }}</small>
                      </td>
                      <td v-else>
                        {{ dateTime(r.startsAt)
                        }}<small>{{ r.minutes }} {{ t("分钟", "min") }}</small>
                      </td>
                      <td v-if="kind === 'enrollments'">
                        {{ r.heldMinutes }} {{ t("预留", "reserved") }} /
                        {{ r.spentMinutes }} {{ t("已用", "used") }} /
                        {{
                          r.status === "OPEN"
                            ? r.totalMinutes - r.heldMinutes - r.spentMinutes
                            : 0
                        }}
                        {{ t("可用", "available")
                        }}<small
                          >{{
                            money(r.paidMinor - r.refundedMinor, r.currency)
                          }}
                          / {{ money(r.priceMinor, r.currency) }}</small
                        >
                      </td>
                      <td v-else>
                        {{ r.instructorName }}<small>{{ r.vehicleName }}</small>
                      </td>
                      <td>
                        <span class="badge" :class="r.status.toLowerCase()">{{
                          stateName(r.status)
                        }}</span>
                      </td>
                      <td>
                        <div class="actions">
                          <button @click="show(r.id)" :disabled="pending">
                            {{ t("查看", "View") }}</button
                          ><button
                            v-if="
                              kind === 'enrollments' &&
                              (can('portal') || can('schedule')) &&
                              r.status === 'OPEN'
                            "
                            class="primary"
                            @click="book(r)"
                            :disabled="
                              pending ||
                              r.totalMinutes - r.heldMinutes - r.spentMinutes <
                                30 ||
                              r.paidMinor - r.refundedMinor !== r.priceMinor
                            "
                          >
                            {{ t("约课", "Book") }}
                          </button>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="!rows.items.length">
                      <td colspan="5" class="empty">
                        {{ t("暂无记录", "No records") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <footer class="pager">
                <span
                  >{{ page + 1 }} /
                  {{ Math.max(1, Math.ceil(rows.total / 15)) }}</span
                >
                <div>
                  <button @click="turn(-1)" :disabled="pending || page === 0">
                    {{ t("上一页", "Previous") }}</button
                  ><button
                    @click="turn(1)"
                    :disabled="pending || (page + 1) * 15 >= rows.total"
                  >
                    {{ t("下一页", "Next") }}
                  </button>
                </div>
              </footer>
            </section>
          </section></template
        >
      </main>
    </div>
  </div>
  <div v-if="dialog" class="modal-backdrop">
    <section
      class="dialog"
      role="dialog"
      aria-modal="true"
      :aria-label="actionNames[dialog.action]"
    >
      <header>
        <h2>{{ actionNames[dialog.action] }}</h2>
        <button
          @click="closeDialog"
          :disabled="pending"
          :aria-label="t('关闭', 'Close')"
        >
          <X :size="18" />
        </button>
      </header>
      <form @submit.prevent="submit" autocomplete="off">
        <div class="form-grid">
          <label
            v-for="f in dialogFields"
            :key="f.key"
            :class="{ 'span-2': f.type === 'textarea' }"
            >{{ f.label
            }}<select
              v-if="f.type === 'select'"
              v-model="dialog[f.key]"
              :required="f.required"
              :disabled="pending"
              @change="
                ['instructorId', 'vehicleId'].includes(f.key) && resetSlots()
              "
            >
              <option value="">{{ t("请选择", "Select") }}</option>
              <option v-for="c in f.choices" :key="c.id" :value="c.id">
                {{ c.name }}
              </option></select
            ><textarea
              v-else-if="f.type === 'textarea'"
              v-model="dialog[f.key]"
              :required="f.required"
              :maxlength="f.max"
              rows="3"
              :disabled="pending" /><input
              v-else
              v-model="dialog[f.key]"
              :type="f.type || 'text'"
              :required="f.required"
              :min="f.min"
              :max="f.type === 'number' ? f.max : undefined"
              :maxlength="f.type !== 'number' ? f.max : undefined"
              :minlength="f.minlength"
              :step="f.step || 1"
              autocomplete="off"
              :disabled="pending || f.locked"
              @change="
                ['instructorId', 'vehicleId', 'date', 'minutes'].includes(
                  f.key,
                ) && resetSlots()
              "
          /></label>
          <template v-if="['BOOK', 'RESCHEDULE'].includes(dialog.action)"
            ><p class="hint span-2">
              {{ dialog.planName }} ·
              {{ categoryLabel(dialog.category, options.categories) }} ·
              {{ t("查询日期按分校时区", "Query date uses school zone") }}
              {{
                options.departments.find((d) => d.id === dialog.departmentId)
                  ?.zone
              }}。{{ t("结果显示时区", "Result display zone") }} {{ zone }}。
            </p>
            <div class="span-2">
              <button
                type="button"
                @click="querySlots"
                :disabled="
                  pending ||
                  !dialog.instructorId ||
                  !dialog.vehicleId ||
                  !dialog.date
                "
              >
                {{ t("查询可约时段", "Find available times") }}
              </button>
            </div>
            <div class="slot-grid span-2">
              <button
                type="button"
                v-for="s in slots"
                :key="s.startsAt"
                :class="{ selected: dialog.startsAt === s.startsAt }"
                @click="dialog.startsAt = s.startsAt"
                :disabled="pending"
              >
                {{ dateTime(s.startsAt) }}
              </button>
            </div>
            <p v-if="slotChecked && !slots.length" class="hint span-2">
              {{
                t(
                  "没有可约时段。核对工作段、检修请假、有效期、足额收款和提前时间。",
                  "No slots. Check work windows, blocks, validity, verified payment and lead time.",
                )
              }}
            </p>
            <p class="hint span-2">
              {{
                t(
                  "自助改期或免扣时取消需提前",
                  "Reschedule / free cancellation notice",
                )
              }}
              {{ dialog.cancelHours }}
              {{ t("小时；逾期取消、缺席", "hours; late cancel / no-show") }}
              {{
                dialog.lateCharge
                  ? t("扣预约分钟。", "charges booked minutes.")
                  : t("不扣时。", "does not charge minutes.")
              }}
            </p></template
          >
        </div>
        <footer class="dialog-footer">
          <button type="button" @click="closeDialog" :disabled="pending">
            {{ t("取消", "Cancel") }}</button
          ><button
            class="primary"
            :disabled="
              pending ||
              (['BOOK', 'RESCHEDULE'].includes(dialog.action) &&
                !dialog.startsAt)
            "
          >
            {{ pending ? t("提交中", "Submitting") : t("确认提交", "Confirm") }}
          </button>
        </footer>
      </form>
    </section>
  </div>
  <div v-if="about" class="modal-backdrop">
    <section
      class="dialog about-dialog"
      role="dialog"
      aria-modal="true"
      :aria-label="t('关于与商业授权', 'About & licensing')"
    >
      <header>
        <h2>LessonDesk · 1.0.0</h2>
        <button @click="about = false" :aria-label="t('关闭', 'Close')">
          <X :size="18" />
        </button>
      </header>
      <p>
        {{
          t(
            "知华科技（上海如静知华信息科技有限公司）",
            "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
          )
        }}
      </p>
      <p>
        {{
          t(
            "仅限个人学习交流，未经书面授权不得商用。",
            "For personal learning and non-commercial exchange only. Commercial use requires written authorization.",
          )
        }}
      </p>
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
        >https://www.zhuatech.cn/</a
      ><template v-if="language === 'zh'"
        ><p>商业授权或深度定制开发请联系知华科技。</p>
        <div class="qr-grid">
          <figure>
            <img src="/brand/wechat-zhuatech.png" alt="微信 zhuatech" />
            <figcaption>微信 zhuatech</figcaption>
          </figure>
          <figure>
            <img src="/brand/wechat-zhuatech2.png" alt="微信 zhuatech2" />
            <figcaption>微信 zhuatech2</figcaption>
          </figure>
        </div></template
      >
      <ul v-else>
        <li><a href="mailto:han@zhuatech.cn">han@zhuatech.cn</a></li>
        <li><a href="mailto:jack@zhuatech.cn">jack@zhuatech.cn</a></li>
        <li>
          WhatsApp:
          <a href="https://wa.me/8617521234993" target="_blank" rel="noopener"
            >+86 17521234993</a
          >
        </li>
      </ul>
    </section>
  </div>
  <div v-if="confirmation" class="modal-backdrop confirmation">
    <section
      class="dialog small"
      role="dialog"
      aria-modal="true"
      :aria-label="t('请确认', 'Please confirm')"
    >
      <h2>{{ t("请确认", "Please confirm") }}</h2>
      <p>{{ confirmation.message }}</p>
      <footer class="dialog-footer">
        <button @click="answerConfirmation(false)">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" @click="answerConfirmation(true)">
          {{ t("确认", "Confirm") }}
        </button>
      </footer>
    </section>
  </div>
</template>
