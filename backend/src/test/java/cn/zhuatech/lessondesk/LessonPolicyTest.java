// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lessondesk;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

/** 时间边界、货币、取整与CSV安全。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class LessonPolicyTest {
  @Test
  void adjacentIsNotConflict() {
    var a = Instant.parse("2026-10-11T01:00:00Z");
    assertFalse(LessonPolicy.overlap(a, a.plusSeconds(60), a.plusSeconds(60), a.plusSeconds(120)));
    assertTrue(LessonPolicy.overlap(a, a.plusSeconds(61), a.plusSeconds(60), a.plusSeconds(120)));
  }

  @Test
  void minutesDoNotRound() {
    assertThrows(Problem.class, () -> LessonPolicy.instant("2026-10-11T01:00:00.001Z"));
    assertEquals(
        Instant.parse("2026-10-11T01:00:00Z"), LessonPolicy.instant("2026-10-11T01:00:00.000Z"));
  }

  @Test
  void earnedCeilingAndConservation() {
    for (int total = 1; total < 100; total++)
      for (int spent = 0; spent <= total; spent++) {
        long earned = LessonPolicy.earned(1001, spent, total);
        assertTrue(earned >= 0 && earned <= 1001);
        assertTrue(earned * total >= 1001L * spent);
        if (spent > 0) assertTrue((earned - 1) * total < 1001L * spent);
      }
    assertEquals(76, LessonPolicy.earned(1001, 45, 600));
  }

  @Test
  void invalidCreditDenied() {
    assertThrows(Problem.class, () -> LessonPolicy.earned(1000, 601, 600));
    assertThrows(Problem.class, () -> LessonPolicy.earned(1000, 0, 0));
  }

  @Test
  void currencyAndIntegerMoney() {
    assertEquals("GBP", LessonPolicy.currency("GBP"));
    assertThrows(Problem.class, () -> LessonPolicy.currency("JPY"));
    assertThrows(Problem.class, () -> LessonPolicy.minor("1.5"));
    assertThrows(Problem.class, () -> LessonPolicy.minor(0));
  }

  @Test
  void csvBlocksFormulaAndQuotes() {
    assertEquals("\"'  =1+1\"", Rules.csv("  =1+1"));
    assertEquals("\"a\"\"b\"", Rules.csv("a\"b"));
    assertEquals("\"'+123\"", Rules.csv("+123"));
  }
}
