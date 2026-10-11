<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, watch } from "vue";
import { X, Printer, RefreshCw } from "@lucide/vue";
import { api } from "../api.js";
import { t, money, stateName, dateTime, ask } from "../ui.js";
const props = defineProps({
  id: { type: Number, required: true },
  profile: { type: Object, required: true },
  perform: { type: Function, required: true },
  pending: Boolean,
});
const emit = defineEmits(["book", "close", "changed"]);
const detail = ref(null);
const command = ref(null);
const can = (c) => props.profile.permissions.includes(c);
/** 原单重新读取余额，不在前端猜测可退金额。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function load() {
  await props.perform(async () => {
    detail.value = await api("/enrollments/" + props.id);
    return true;
  });
}
watch(() => props.id, load, { immediate: true });
function open(action) {
  const e = detail.value.enrollment;
  command.value = {
    action,
    requestId: crypto.randomUUID(),
    version: e.version,
    amount:
      action === "COLLECT"
        ? (e.priceMinor - e.paidMinor + e.refundedMinor) / 100
        : detail.value.refundableMinor / 100,
    originalId: detail.value.payments.find((p) => p.kind === "IN")?.id || "",
    reference: "",
    note: "",
  };
}
/** 页面确认后提交冻结请求ID，失败不会自动重放扣款。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function submit() {
  if (
    !(await ask(
      t(
        "请确认金额、凭据和理由已核实。",
        "Confirm the amount, receipt and reason have been verified.",
      ),
    ))
  )
    return;
  await props.perform(async () => {
    const b = { ...command.value };
    const value = Number(b.amount) * 100;
    if (Math.abs(value - Math.round(value)) > 0.000001)
      throw new Error("INVALID_INPUT");
    b.amountMinor = Math.round(value);
    delete b.amount;
    await api(`/enrollments/${props.id}/commands/${b.action}`, {
      method: "POST",
      body: b,
    });
    command.value = null;
    emit("changed");
    return true;
  });
  if (!command.value) await load();
}
function print() {
  window.print();
}
</script>
<template>
  <section v-if="detail" class="panel enrollment-detail">
    <header class="section-title">
      <div>
        <h2>{{ detail.student.name }} · {{ detail.enrollment.planName }}</h2>
        <p class="muted">
          {{ detail.enrollment.reference }} ·
          <span class="badge">{{ stateName(detail.enrollment.status) }}</span>
        </p>
      </div>
      <div class="actions">
        <button
          @click="load"
          :disabled="pending"
          :aria-label="t('刷新原单', 'Refresh enrollment')"
        >
          <RefreshCw :size="16" /></button
        ><button @click="print">
          <Printer :size="16" />{{ t("打印原单", "Print record") }}</button
        ><button
          @click="emit('close')"
          :disabled="pending"
          :aria-label="t('关闭原单', 'Close enrollment')"
        >
          <X :size="18" />
        </button>
      </div>
    </header>
    <div class="stats-strip">
      <div>
        <span>{{ t("总课时", "Purchased") }}</span
        ><strong
          >{{ detail.enrollment.totalMinutes }}
          <small>{{ t("分钟", "min") }}</small></strong
        >
      </div>
      <div>
        <span>{{ t("已预留", "Reserved") }}</span
        ><strong>{{ detail.enrollment.heldMinutes }}</strong>
      </div>
      <div>
        <span>{{ t("已核销", "Used") }}</span
        ><strong>{{ detail.enrollment.spentMinutes }}</strong>
      </div>
      <div>
        <span>{{ t("可预约", "Available") }}</span
        ><strong>{{ detail.availableMinutes }}</strong>
      </div>
    </div>
    <dl class="detail-grid">
      <div>
        <dt>{{ t("有效期", "Valid dates") }}</dt>
        <dd>
          {{ detail.enrollment.startsOn }} → {{ detail.enrollment.endsOn }}
        </dd>
      </div>
      <div>
        <dt>{{ t("套餐应收", "Package price") }}</dt>
        <dd>
          {{ money(detail.enrollment.priceMinor, detail.enrollment.currency) }}
        </dd>
      </div>
      <div>
        <dt>{{ t("已核实收款／已退", "Verified paid / refunded") }}</dt>
        <dd>
          {{ money(detail.enrollment.paidMinor, detail.enrollment.currency) }} /
          {{
            money(detail.enrollment.refundedMinor, detail.enrollment.currency)
          }}
        </dd>
      </div>
      <div>
        <dt>
          {{ t("按已核销计算的可退上限", "Eligible refund by used minutes") }}
        </dt>
        <dd>{{ money(detail.refundableMinor, detail.enrollment.currency) }}</dd>
      </div>
      <div>
        <dt>
          {{ t("取消规则（购买时快照）", "Cancellation rules at purchase") }}
        </dt>
        <dd>
          {{ detail.enrollment.cancelHours }}
          {{
            t(
              "小时前免扣时；逾期取消或缺席",
              "hours notice; late cancel / no-show",
            )
          }}
          {{
            detail.enrollment.lateCharge
              ? t("扣预约分钟", "charges booked minutes")
              : t("不扣时", "does not charge minutes")
          }}
        </dd>
      </div>
      <div>
        <dt>{{ t("备注", "Notes") }}</dt>
        <dd>{{ detail.enrollment.note || "—" }}</dd>
      </div>
    </dl>
    <div class="actions" v-if="detail.enrollment.status === 'OPEN'">
      <button
        class="primary"
        v-if="can('schedule') || can('portal')"
        @click="emit('book', detail.enrollment)"
        :disabled="
          pending ||
          detail.availableMinutes < 30 ||
          detail.enrollment.paidMinor - detail.enrollment.refundedMinor !==
            detail.enrollment.priceMinor
        "
      >
        {{ t("预约课程", "Book lesson") }}</button
      ><button
        v-if="can('finance')"
        @click="open('COLLECT')"
        :disabled="
          pending ||
          detail.enrollment.paidMinor - detail.enrollment.refundedMinor >=
            detail.enrollment.priceMinor
        "
      >
        {{ t("登记已核实收款", "Record verified payment") }}</button
      ><button
        v-if="can('enroll')"
        class="danger"
        @click="open('CLOSE')"
        :disabled="pending || detail.enrollment.heldMinutes > 0"
      >
        {{ t("停止报读", "Close enrollment") }}
      </button>
    </div>
    <div class="actions" v-else-if="can('finance')">
      <button
        class="danger"
        @click="open('REFUND')"
        :disabled="pending || detail.refundableMinor <= 0"
      >
        {{ t("登记原款退款", "Record original-payment refund") }}
      </button>
    </div>
    <p class="hint">
      {{
        t(
          "收退款是人工核实后的记录，不发起银行卡交易。停止报读前必须处理全部未完成课次。",
          "Payments are verified manual records, not card transactions. Handle active lessons before closing enrollment.",
        )
      }}
    </p>
    <h3>{{ t("收退款流水", "Payment ledger") }}</h3>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>{{ t("日期／操作人", "Date / actor") }}</th>
            <th>{{ t("类型与凭据", "Type / receipt") }}</th>
            <th>{{ t("金额", "Amount") }}</th>
            <th>{{ t("理由／原收款", "Reason / original payment") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in detail.payments" :key="p.id">
            <td>
              {{ dateTime(p.createdAt) }}<small>{{ p.actor }}</small>
            </td>
            <td>
              {{ p.kind === "IN" ? t("收款", "Payment") : t("退款", "Refund")
              }}<small>{{ p.reference }} · #{{ p.id }}</small>
            </td>
            <td>{{ money(p.amountMinor, detail.enrollment.currency) }}</td>
            <td>
              {{ p.note
              }}<small v-if="p.originalId"
                >{{ t("原收款", "Original payment") }} #{{
                  p.originalId
                }}</small
              >
            </td>
          </tr>
          <tr v-if="!detail.payments.length">
            <td colspan="4" class="empty">
              {{ t("尚无收退款记录", "No payment records") }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <h3>{{ t("分钟流水", "Minute ledger") }}</h3>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>{{ t("动作／课次", "Action / lesson") }}</th>
            <th>{{ t("预留变动", "Reserved delta") }}</th>
            <th>{{ t("核销变动", "Used delta") }}</th>
            <th>{{ t("日期与说明", "Date / note") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in detail.credits" :key="c.id">
            <td>
              {{
                {
                  RESERVE: t("预留", "Reserve"),
                  RELEASE: t("释放", "Release"),
                  TAUGHT: t("授课核销", "Taught"),
                  LATE_CANCEL: t("逾期取消", "Late cancellation"),
                  NO_SHOW: t("未到课", "No-show"),
                }[c.action] || c.action
              }}<small>#{{ c.lessonId }}</small>
            </td>
            <td>{{ c.heldDelta }}</td>
            <td>{{ c.spentDelta }}</td>
            <td>
              {{ dateTime(c.createdAt) }}<small>{{ c.note }}</small>
            </td>
          </tr>
          <tr v-if="!detail.credits.length">
            <td colspan="4" class="empty">
              {{ t("尚未预约", "No lessons booked") }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <details>
      <summary>{{ t("报读操作历史", "Enrollment history") }}</summary>
      <ol class="history">
        <li v-for="e in detail.events" :key="e.id">
          <strong>{{
            {
              CREATED: t("创建报读", "Enrolled"),
              COLLECT: t("登记收款", "Payment recorded"),
              CLOSE: t("停止报读", "Enrollment closed"),
              REFUND: t("登记原款退款", "Original-payment refund"),
            }[e.action] || e.action
          }}</strong>
          · {{ dateTime(e.createdAt) }} ·
          {{ e.actor }}
          <p>{{ e.note }}</p>
        </li>
      </ol>
    </details>
  </section>
  <div v-if="command" class="modal-backdrop">
    <section
      class="dialog"
      role="dialog"
      aria-modal="true"
      :aria-label="t('收退费操作', 'Enrollment operation')"
    >
      <header>
        <h2>
          {{
            command.action === "COLLECT"
              ? t("登记收款", "Record payment")
              : command.action === "REFUND"
                ? t("原款退款", "Original-payment refund")
                : t("停止报读", "Close enrollment")
          }}
        </h2>
        <button
          @click="command = null"
          :disabled="pending"
          :aria-label="t('关闭', 'Close')"
        >
          <X :size="18" />
        </button>
      </header>
      <form @submit.prevent="submit">
        <div class="form-grid">
          <template v-if="command.action !== 'CLOSE'"
            ><label
              >{{ t("金额", "Amount") }} ({{
                detail.enrollment.currency
              }})<input
                v-model="command.amount"
                type="number"
                required
                min="0.01"
                step="0.01"
                :disabled="pending" /></label
            ><label
              >{{ t("唯一收退款凭据编号", "Unique payment receipt reference")
              }}<input
                v-model="command.reference"
                required
                maxlength="120"
                :disabled="pending" /></label
            ><label v-if="command.action === 'REFUND'" class="span-2"
              >{{ t("原收款", "Original payment")
              }}<select
                v-model="command.originalId"
                required
                :disabled="pending"
              >
                <option
                  v-for="p in detail.payments.filter((p) => p.kind === 'IN')"
                  :key="p.id"
                  :value="p.id"
                >
                  #{{ p.id }} · {{ p.reference }} ·
                  {{ money(p.amountMinor, detail.enrollment.currency) }}
                </option>
              </select></label
            ></template
          ><label class="span-2"
            >{{ t("核实说明／理由", "Verification / reason")
            }}<textarea
              v-model="command.note"
              required
              maxlength="1000"
              rows="3"
              :disabled="pending"
            />
          </label>
        </div>
        <footer class="dialog-footer">
          <button type="button" @click="command = null" :disabled="pending">
            {{ t("取消", "Cancel") }}</button
          ><button class="primary" :disabled="pending">
            {{ t("确认登记", "Confirm record") }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
