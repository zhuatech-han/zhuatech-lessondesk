// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { test } from "node:test";
import assert from "node:assert/strict";
import { toUtc, toLocal, money, language } from "./ui.js";
process.env.TZ = "America/New_York";
/** 防止本地时区和夏令时输入被静默归一化后写入错误课次。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
test("local minute retains its UTC instant", () => {
  assert.equal(toUtc("2026-10-11T09:15"), "2026-10-11T13:15:00.000Z");
  assert.equal(toLocal("2026-10-11T13:15:00Z"), "2026-10-11T09:15");
});
test("nonexistent DST and invalid input are rejected", () => {
  assert.throws(() => toUtc("2026-03-08T02:30"), /TIME_INVALID/);
  assert.throws(() => toUtc("invalid"), /TIME_INVALID/);
});
test("original minor currency is displayed without changing the value", () => {
  language.value = "en";
  assert.match(money(1001, "GBP"), /10\.01/);
  assert.match(money(0, "USD"), /0\.00/);
});
