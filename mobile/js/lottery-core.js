/**
 * 双色球核心逻辑（纯函数，无 DOM 依赖）
 * 浏览器 / Node 双端可用：浏览器挂到 window.LotteryCore，Node 走 module.exports
 * 规则与桌面版 Java 代码保持一致
 */
(function (root, factory) {
  var core = factory();
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = core;
  }
  root.LotteryCore = core;
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  var RED_MIN = 1, RED_MAX = 33;
  var BLUE_MIN = 1, BLUE_MAX = 16;
  var RED_COUNT = 6;
  var TICKET_PRICE = 2;

  // 奖级表（与真实双色球规则一致；一二等奖为本地模拟固定奖金）
  var PRIZE_LEVELS = [
    { key: 'FIRST',  label: '一等奖', amount: 5000000 },
    { key: 'SECOND', label: '二等奖', amount: 300000 },
    { key: 'THIRD',  label: '三等奖', amount: 3000 },
    { key: 'FOURTH', label: '四等奖', amount: 200 },
    { key: 'FIFTH',  label: '五等奖', amount: 10 },
    { key: 'SIXTH',  label: '六等奖', amount: 5 }
  ];

  function shuffleTake(rng) {
    var pool = [];
    for (var i = RED_MIN; i <= RED_MAX; i++) pool.push(i);
    for (var j = pool.length - 1; j > 0; j--) {
      var k = Math.floor(rng() * (j + 1));
      var t = pool[j]; pool[j] = pool[k]; pool[k] = t;
    }
    return pool.slice(0, RED_COUNT);
  }

  function asc(a, b) { return a - b; }

  /** 机选一注（红球升序） */
  function quickPick(rng) {
    rng = rng || Math.random;
    return {
      reds: shuffleTake(rng).sort(asc),
      blue: BLUE_MIN + Math.floor(rng() * (BLUE_MAX - BLUE_MIN + 1))
    };
  }

  /** 开一期奖：drawOrder 为出球顺序（电视开奖顺序，不排序），reds 为排序后用于比对 */
  function draw(period, rng) {
    rng = rng || Math.random;
    var order = shuffleTake(rng);
    return {
      period: period,
      drawOrder: order,
      reds: order.slice().sort(asc),
      blue: BLUE_MIN + Math.floor(rng() * (BLUE_MAX - BLUE_MIN + 1))
    };
  }

  /** 奖级判定：redHits 红球命中数，blueHit 蓝球是否命中；未中奖返回 null */
  function judgePrize(redHits, blueHit) {
    if (redHits === 6 && blueHit) return PRIZE_LEVELS[0];
    if (redHits === 6) return PRIZE_LEVELS[1];
    if (redHits === 5 && blueHit) return PRIZE_LEVELS[2];
    if (redHits === 5 || (redHits === 4 && blueHit)) return PRIZE_LEVELS[3];
    if (redHits === 4 || (redHits === 3 && blueHit)) return PRIZE_LEVELS[4];
    if (blueHit) return PRIZE_LEVELS[5];
    return null;
  }

  /** 比对一注彩票，返回 { redHits, blueHit, prize } */
  function evaluateTicket(ticket, result) {
    var redHits = 0;
    for (var i = 0; i < ticket.reds.length; i++) {
      if (result.reds.indexOf(ticket.reds[i]) >= 0) redHits++;
    }
    var blueHit = ticket.blue === result.blue;
    return { redHits: redHits, blueHit: blueHit, prize: judgePrize(redHits, blueHit) };
  }

  /** 校验自选是否合法：6 个不重复红球 + 1 个蓝球 */
  function validatePick(reds, blue) {
    if (!Array.isArray(reds) || reds.length !== RED_COUNT) return '红球要选 6 个';
    if (typeof blue !== 'number') return '蓝球要选 1 个';
    var seen = {};
    for (var i = 0; i < reds.length; i++) {
      var n = reds[i];
      if (n < RED_MIN || n > RED_MAX || n % 1 !== 0) return '红球号码要在 1-33 之间';
      if (seen[n]) return '红球不能重复';
      seen[n] = true;
    }
    if (blue < BLUE_MIN || blue > BLUE_MAX || blue % 1 !== 0) return '蓝球号码要在 1-16 之间';
    return null;
  }

  /** 新建一注彩票（红球自动排序） */
  function newTicket(reds, blue) {
    return { reds: reds.slice().sort(asc), blue: blue };
  }

  /** 金额格式化：1234567 -> "¥1,234,567" */
  function formatMoney(n) {
    var neg = n < 0;
    var s = String(Math.abs(Math.round(n)));
    var out = '';
    while (s.length > 3) {
      out = ',' + s.slice(-3) + out;
      s = s.slice(0, -3);
    }
    return (neg ? '-¥' : '¥') + s + out;
  }

  /** 带符号金额：净收益显示用 */
  function formatSignedMoney(n) {
    return (n >= 0 ? '+' : '') + formatMoney(n);
  }

  /** 球号两位显示：7 -> "07" */
  function formatBall(n) {
    return (n < 10 ? '0' : '') + n;
  }

  /** 开奖结果展示串：03 07 11 18 25 29 + 09 */
  function formatResult(result) {
    var parts = [];
    for (var i = 0; i < result.reds.length; i++) parts.push(formatBall(result.reds[i]));
    return parts.join(' ') + ' + ' + formatBall(result.blue);
  }

  return {
    RED_MIN: RED_MIN, RED_MAX: RED_MAX,
    BLUE_MIN: BLUE_MIN, BLUE_MAX: BLUE_MAX,
    RED_COUNT: RED_COUNT, TICKET_PRICE: TICKET_PRICE,
    PRIZE_LEVELS: PRIZE_LEVELS,
    quickPick: quickPick,
    draw: draw,
    judgePrize: judgePrize,
    evaluateTicket: evaluateTicket,
    validatePick: validatePick,
    newTicket: newTicket,
    formatMoney: formatMoney,
    formatSignedMoney: formatSignedMoney,
    formatBall: formatBall,
    formatResult: formatResult
  };
});
