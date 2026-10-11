<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, watch } from "vue";
import { Plus, Pencil, Trash2, Search, X } from "@lucide/vue";
import { api } from "../api.js";
import {
  t,
  stateName,
  dateTime,
  money,
  categoryLabel,
  cloneRecord,
  ask,
  toLocal,
  toUtc,
} from "../ui.js";
const props = defineProps({
  kind: { type: String, required: true },
  options: { type: Object, required: true },
  profile: { type: Object, required: true },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["changed"]);
const result = ref({ items: [], total: 0 });
const page = ref(0);
const search = ref("");
const state = ref("");
const form = ref(null);
const initial = ref("");
const ready = ref(false);
const names = computed(() => ({
  students: t("学员档案", "Students"),
  instructors: t("教练", "Instructors"),
  vehicles: t("教练车", "Vehicles"),
  plans: t("课时套餐", "Lesson packages"),
  windows: t("工作、请假与检修", "Work, leave & maintenance"),
  inquiries: t("询盘跟进", "Inquiries"),
}));
const studentName = (id) =>
  props.options?.students?.find((s) => s.id === id)?.name || "—";
const coachName = (id) =>
  props.options?.instructors?.find((s) => s.id === id)?.name || "—";
const carName = (id) =>
  props.options?.vehicles?.find((s) => s.id === id)?.name || "—";
const fields = computed(() => {
  const name = {
    key: "name",
    label: t("名称", "Name"),
    required: true,
    max: 120,
  };
  const category = {
    key: "category",
    label: t("教学类别", "Training category"),
    type: "select",
    options: props.options?.categories?.map((c) => ({
      id: c.code,
      name: t(c.name, c.nameEn),
    })),
    required: true,
  };
  const enabled = {
    key: "enabled",
    label: t("启用", "Enabled"),
    type: "checkbox",
  };
  const rows = {
    students: [
      name,
      {
        key: "accountId",
        label: t("学员账号", "Student account"),
        type: "select",
        options: (props.options?.accounts || []).filter(
          (a) =>
            a.student && a.departmentId === Number(form.value?.departmentId),
        ),
        required: true,
        locked: true,
      },
      {
        key: "contact",
        label: t("联系邮箱／电话", "Contact email / phone"),
        required: true,
        max: 120,
      },
      {
        key: "note",
        label: t("档案备注", "Notes"),
        type: "textarea",
        max: 1000,
      },
      enabled,
    ],
    instructors: [
      name,
      {
        key: "accountId",
        label: t("教练账号", "Instructor account"),
        type: "select",
        options: (props.options?.accounts || []).filter(
          (a) =>
            a.instructor && a.departmentId === Number(form.value?.departmentId),
        ),
        required: true,
        locked: true,
      },
      { ...category, key: "categoryValues", type: "categories" },
      {
        key: "bufferMinutes",
        label: t("课后缓冲分钟", "After-lesson buffer minutes"),
        type: "number",
        min: 0,
        max: 60,
        required: true,
      },
      enabled,
    ],
    vehicles: [
      name,
      {
        key: "plate",
        label: t("车辆编号／车牌", "Vehicle number / plate"),
        required: true,
        max: 24,
      },
      category,
      enabled,
    ],
    plans: [
      name,
      category,
      {
        key: "totalMinutes",
        label: t("总课时（分钟）", "Total lesson minutes"),
        type: "number",
        min: 30,
        max: 60000,
        required: true,
      },
      {
        key: "price",
        label: t("套餐价格", "Package price"),
        type: "number",
        min: 0,
        max: 1000000,
        step: "0.01",
        required: true,
      },
      {
        key: "currency",
        label: t("ISO币种", "ISO currency"),
        required: true,
        max: 3,
      },
      {
        key: "validDays",
        label: t("有效天数", "Valid days"),
        type: "number",
        min: 1,
        max: 1095,
        required: true,
      },
      {
        key: "cancelHours",
        label: t(
          "自助改期／免扣时取消提前小时",
          "Reschedule / free cancellation notice hours",
        ),
        type: "number",
        min: 0,
        max: 72,
        required: true,
      },
      {
        key: "lateCharge",
        label: t(
          "逾期取消、缺席扣预约分钟",
          "Charge booked minutes for late cancel / no-show",
        ),
        type: "checkbox",
      },
      enabled,
    ],
    windows: [
      {
        key: "kind",
        label: t("时段类型", "Window type"),
        type: "select",
        options: [
          { id: "WORK", name: stateName("WORK") },
          { id: "BLOCK", name: stateName("BLOCK") },
        ],
        required: true,
      },
      {
        key: "instructorId",
        label: t(
          "教练（与车辆二选一）",
          "Instructor (choose instructor OR vehicle)",
        ),
        type: "select",
        options: (props.options?.instructors || []).filter(
          (i) => props.profile.scope !== "ASSIGNED" || i.mine,
        ),
      },
      {
        key: "vehicleId",
        label: t("车辆（仅检修）", "Vehicle (maintenance only)"),
        type: "select",
        options:
          props.profile.scope === "ASSIGNED"
            ? []
            : props.options?.vehicles || [],
      },
      {
        key: "startsAt",
        label: t("开始时间", "Start time"),
        type: "datetime-local",
        required: true,
      },
      {
        key: "endsAt",
        label: t("结束时间", "End time"),
        type: "datetime-local",
        required: true,
      },
      { key: "note", label: t("说明", "Notes"), type: "textarea", max: 1000 },
    ],
    inquiries: [
      name,
      {
        key: "contact",
        label: t("联系方式", "Contact"),
        required: true,
        max: 120,
      },
      {
        key: "note",
        label: t("咨询内容", "Inquiry"),
        type: "textarea",
        max: 1000,
      },
      {
        key: "status",
        label: t("跟进状态", "Status"),
        type: "select",
        options: ["NEW", "CONTACTED", "ENROLLED", "CLOSED"].map((s) => ({
          id: s,
          name: stateName(s),
        })),
        required: true,
      },
      {
        key: "studentId",
        label: t("关联学员", "Linked student"),
        type: "select",
        options: props.options?.students || [],
      },
      {
        key: "followup",
        label: t("跟进记录", "Follow-up notes"),
        type: "textarea",
        max: 1000,
      },
    ],
  };
  return rows[props.kind] || [];
});
/** 列表由后端先授权再分页；请求序列防止旧搜索结果覆盖新页。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
let seq = 0;
async function load() {
  const n = ++seq;
  ready.value = false;
  await props.perform(async () => {
    const r = await api(
      `/records/${props.kind}?page=${page.value}&size=15&search=${encodeURIComponent(search.value)}&status=${state.value}`,
    );
    if (n === seq) result.value = r;
    return true;
  });
  if (n === seq) ready.value = true;
}
watch(
  () => props.kind,
  () => {
    page.value = 0;
    search.value = "";
    state.value = "";
    form.value = null;
    load();
  },
  { immediate: true },
);
watch(page, load);
function find() {
  page.value = 0;
  load();
}
/** 主数据表单以快照为副本，账号及组织身份不允许改换。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function edit(row) {
  form.value = row
    ? cloneRecord(row)
    : {
        departmentId: props.profile.departmentId,
        name: "",
        accountId: "",
        category: props.options?.categories?.[0]?.code || "AUTO",
        enabled: true,
        contact: "",
        note: "",
        plate: "",
        bufferMinutes: 15,
        totalMinutes: 600,
        price: 1000,
        currency: "CNY",
        validDays: 90,
        cancelHours: 24,
        lateCharge: true,
        kind: "WORK",
        instructorId:
          props.profile.scope === "ASSIGNED"
            ? props.options?.instructors?.find((i) => i.mine)?.id || ""
            : "",
        vehicleId: "",
        startsAt: "",
        endsAt: "",
        status: "NEW",
        followup: "",
        studentId: "",
      };
  if (props.kind === "instructors")
    form.value.categoryValues = (form.value.category || "AUTO").split(",");
  if (props.kind === "plans" && row) form.value.price = row.priceMinor / 100;
  if (props.kind === "windows" && row) {
    form.value.startsAt = toLocal(row.startsAt);
    form.value.endsAt = toLocal(row.endsAt);
  }
  initial.value = JSON.stringify(form.value);
}
async function canLeave() {
  if (
    form.value &&
    JSON.stringify(form.value) !== initial.value &&
    !(await ask(t("放弃未保存的修改？", "Discard unsaved changes?")))
  )
    return false;
  form.value = null;
  return true;
}
defineExpose({ canLeave });
/** 保存真实字段，金额不静默舍入，本地时间严格转UTC。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function save() {
  await props.perform(async () => {
    const b = cloneRecord(form.value);
    b.departmentId = Number(b.departmentId);
    if (props.kind === "instructors") {
      b.category = b.categoryValues.join(",");
      delete b.categoryValues;
    }
    if (props.kind === "plans") {
      const n = Number(b.price) * 100;
      if (Math.abs(n - Math.round(n)) > 0.000001)
        throw new Error("INVALID_INPUT");
      b.priceMinor = Math.round(n);
      delete b.price;
    }
    if (props.kind === "windows") {
      b.startsAt = toUtc(b.startsAt);
      b.endsAt = toUtc(b.endsAt);
    }
    await api(`/masters/${props.kind}${b.id ? "/" + b.id : ""}`, {
      method: b.id ? "PUT" : "POST",
      body: b,
    });
    form.value = null;
    emit("changed");
    return true;
  });
  if (!form.value) await load();
}
async function remove(row) {
  if (
    !(await ask(
      t("删除这条未被业务引用的记录？", "Delete this unreferenced record?"),
    ))
  )
    return;
  await props.perform(() =>
    api(`/masters/${props.kind}/${row.id}?version=${row.version}`, {
      method: "DELETE",
    }),
  );
  await load();
}
</script>
<template>
  <section>
    <header class="page-title">
      <div>
        <h1>{{ names[kind] }}</h1>
        <p v-if="kind === 'windows'">
          {{
            t(
              "显式发布工作段；请假或检修不能覆盖未完成预约。",
              "Publish work windows; leave or maintenance must preserve active bookings.",
            )
          }}
        </p>
      </div>
      <div class="actions">
        <button @click="load" :disabled="pending">
          {{ t("刷新", "Refresh") }}</button
        ><button class="primary" @click="edit()" :disabled="pending">
          <Plus :size="16" />{{ t("新增", "Add") }}
        </button>
      </div>
    </header>
    <div class="panel">
      <form class="filter-row" @submit.prevent="find">
        <label class="search-box"
          ><Search :size="16" /><input
            v-model="search"
            :placeholder="t('搜索名称或编号', 'Search name or reference')"
            maxlength="120" /></label
        ><select
          v-if="kind === 'inquiries' || kind === 'windows'"
          v-model="state"
        >
          <option value="">{{ t("全部状态", "All statuses") }}</option>
          <option
            v-for="s in kind === 'inquiries'
              ? ['NEW', 'CONTACTED', 'ENROLLED', 'CLOSED']
              : ['WORK', 'BLOCK']"
            :key="s"
            :value="s"
          >
            {{ stateName(s) }}
          </option></select
        ><button :disabled="pending">{{ t("查询", "Search") }}</button
        ><span class="muted">{{ result.total }} {{ t("条", "records") }}</span>
      </form>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("名称／资源", "Name / resource") }}</th>
              <th>{{ t("业务信息", "Details") }}</th>
              <th>{{ t("状态", "Status") }}</th>
              <th>{{ t("操作", "Actions") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in result.items" :key="r.id">
              <td>
                <strong>{{
                  kind === "windows"
                    ? r.instructorId
                      ? coachName(r.instructorId)
                      : carName(r.vehicleId)
                    : r.name
                }}</strong
                ><small v-if="r.reference">{{ r.reference }}</small
                ><small>{{
                  options?.departments?.find((d) => d.id === r.departmentId)
                    ?.name
                }}</small>
              </td>
              <td>
                <template v-if="kind === 'students'">{{ r.contact }}</template
                ><template v-else-if="kind === 'instructors'"
                  >{{ categoryLabel(r.category, options?.categories) }} ·
                  {{ r.bufferMinutes }}
                  {{ t("分钟缓冲", "minute buffer") }}</template
                ><template v-else-if="kind === 'vehicles'"
                  >{{ r.plate }} ·
                  {{ categoryLabel(r.category, options?.categories) }}</template
                ><template v-else-if="kind === 'plans'"
                  >{{ r.totalMinutes }} {{ t("分钟", "min") }} ·
                  {{ money(r.priceMinor, r.currency)
                  }}<small
                    >{{ r.validDays }} {{ t("天有效", "valid days") }} ·
                    {{ r.cancelHours }}
                    {{ t("小时前取消", "hour cancellation notice") }}</small
                  ></template
                ><template v-else-if="kind === 'windows'"
                  >{{ dateTime(r.startsAt)
                  }}<small>→ {{ dateTime(r.endsAt) }}</small
                  ><small>{{ r.note }}</small></template
                ><template v-else-if="kind === 'inquiries'"
                  >{{ r.contact }}<small>{{ r.note }}</small
                  ><small
                    >{{ r.followup }}
                    {{ r.studentId ? studentName(r.studentId) : "" }}</small
                  ></template
                >
              </td>
              <td>
                <span class="badge">{{
                  kind === "windows"
                    ? stateName(r.kind)
                    : kind === "inquiries"
                      ? stateName(r.status)
                      : r.enabled
                        ? t("启用", "Active")
                        : t("停用", "Disabled")
                }}</span>
              </td>
              <td>
                <div class="actions">
                  <button
                    @click="edit(r)"
                    :disabled="pending"
                    :aria-label="t('编辑', 'Edit') + ' ' + (r.name || r.id)"
                  >
                    <Pencil :size="16" /></button
                  ><button
                    @click="remove(r)"
                    :disabled="pending"
                    :aria-label="t('删除', 'Delete') + ' ' + (r.name || r.id)"
                  >
                    <Trash2 :size="16" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!result.items.length">
              <td colspan="4" class="empty">
                {{
                  ready ? t("暂无记录", "No records") : t("正在读取", "Loading")
                }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="pager">
        <span
          >{{ page + 1 }} /
          {{ Math.max(1, Math.ceil(result.total / 15)) }}</span
        >
        <div>
          <button @click="page--" :disabled="pending || page === 0">
            {{ t("上一页", "Previous") }}</button
          ><button
            @click="page++"
            :disabled="pending || (page + 1) * 15 >= result.total"
          >
            {{ t("下一页", "Next") }}
          </button>
        </div>
      </footer>
    </div>
  </section>
  <div v-if="form" class="modal-backdrop" @click.self="canLeave">
    <section
      class="dialog"
      role="dialog"
      aria-modal="true"
      :aria-label="names[kind]"
    >
      <header>
        <h2>
          {{ form.id ? t("编辑", "Edit") : t("新增", "Add") }} ·
          {{ names[kind] }}
        </h2>
        <button
          @click="canLeave"
          :disabled="pending"
          :aria-label="t('关闭', 'Close')"
        >
          <X :size="18" />
        </button>
      </header>
      <form @submit.prevent="save">
        <div class="form-grid">
          <label
            >{{ t("分校", "School")
            }}<select
              v-model="form.departmentId"
              required
              :disabled="pending || !!form.id"
            >
              <option
                v-for="d in options?.departments"
                :key="d.id"
                :value="d.id"
              >
                {{ d.name }}
              </option>
            </select></label
          ><component
            :is="f.type === 'categories' ? 'fieldset' : 'label'"
            v-for="f in fields"
            :key="f.key"
            :class="
              f.type === 'checkbox'
                ? 'check'
                : ['textarea', 'categories'].includes(f.type)
                  ? 'span-2'
                  : ''
            "
            ><input
              v-if="f.type === 'checkbox'"
              v-model="form[f.key]"
              type="checkbox"
              :disabled="pending" />{{ f.label }}
            <div v-if="f.type === 'categories'" class="category-choices">
              <label v-for="o in f.options" :key="o.id" class="check"
                ><input
                  type="checkbox"
                  v-model="form[f.key]"
                  :value="o.id"
                  :disabled="pending"
                />{{ o.name }}</label
              >
            </div>
            <select
              v-else-if="f.type === 'select'"
              v-model="form[f.key]"
              :required="f.required"
              :disabled="pending || (f.locked && !!form.id)"
            >
              <option value="">{{ t("请选择", "Select") }}</option>
              <option v-for="o in f.options" :key="o.id" :value="o.id">
                {{ o.name }}
              </option></select
            ><textarea
              v-else-if="['textarea', 'categories'].includes(f.type)"
              v-model="form[f.key]"
              :maxlength="f.max"
              :required="f.required"
              :disabled="pending"
              rows="3" /><input
              v-else-if="f.type !== 'checkbox'"
              v-model="form[f.key]"
              :type="f.type || 'text'"
              :required="f.required"
              :maxlength="f.max"
              :min="f.min"
              :max="f.max"
              :step="f.step || 1"
              :disabled="pending"
          /></component>
        </div>
        <p v-if="kind === 'windows'" class="hint">
          {{ t("输入时区", "Input time zone") }}:
          {{ Intl.DateTimeFormat().resolvedOptions().timeZone }}
        </p>
        <footer class="dialog-footer">
          <button type="button" @click="canLeave" :disabled="pending">
            {{ t("取消", "Cancel") }}</button
          ><button class="primary" :disabled="pending">
            {{ pending ? t("保存中", "Saving") : t("保存", "Save") }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
