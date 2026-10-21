/* 手机版核心逻辑测试：node mobile/test/core.test.js */
var Core = require('../js/lottery-core.js');

var failures = 0;
function check(name, actual, expected) {
  var ok = (actual === null && expected === null) || String(actual) === String(expected);
  if (!ok) { failures++; console.log('FAIL ' + name + ': actual=' + actual + ' expected=' + expected); }
}

// --- 奖级判定（与桌面版 PrizeLevel.judge 完全一致）---
check('6+1', Core.judgePrize(6, true).key, 'FIRST');
check('6+0', Core.judgePrize(6, false).key, 'SECOND');
check('5+1', Core.judgePrize(5, true).key, 'THIRD');
check('5+0', Core.judgePrize(5, false).key, 'FOURTH');
check('4+1', Core.judgePrize(4, true).key, 'FOURTH');
check('4+0', Core.judgePrize(4, false).key, 'FIFTH');
check('3+1', Core.judgePrize(3, true).key, 'FIFTH');
check('3+0', Core.judgePrize(3, false), null);
check('2+1', Core.judgePrize(2, true).key, 'SIXTH');
check('1+1', Core.judgePrize(1, true).key, 'SIXTH');
check('0+1', Core.judgePrize(0, true).key, 'SIXTH');
check('0+0', Core.judgePrize(0, false), null);
check('2+0', Core.judgePrize(2, false), null);
check('奖金-一等奖', Core.judgePrize(6, true).amount, 5000000);
check('奖金-六等奖', Core.judgePrize(0, true).amount, 5);

// --- 一等奖全中 ---
var winReds = [1, 2, 3, 4, 5, 6];
var result = Core.draw(1, mulberry(42));
result.reds = winReds.slice(); result.drawOrder = winReds.slice(); result.blue = 7;
var t = Core.newTicket(winReds, 7);
var ev = Core.evaluateTicket(t, result);
check('全中 redHits', ev.redHits, 6);
check('全中 blueHit', ev.blueHit, true);
check('全中 奖级', ev.prize.key, 'FIRST');

// --- 随机性：20000 期合法性 ---
var rng = mulberry(12345);
var unique = {};
for (var i = 0; i < 20000; i++) {
  var d = Core.draw(i, rng);
  if (d.reds.length !== 6) { failures++; console.log('FAIL 红球数量'); break; }
  var uniq = {};
  for (var j = 0; j < 6; j++) {
    var n = d.reds[j];
    if (n < 1 || n > 33) { failures++; console.log('FAIL 红球范围 ' + n); break; }
    if (uniq[n]) { failures++; console.log('FAIL 红球重复'); break; }
    uniq[n] = true;
  }
  if (d.blue < 1 || d.blue > 16) { failures++; console.log('FAIL 蓝球范围'); break; }
  // 排序校验
  var sorted = d.reds.slice().sort(function (a, b) { return a - b; });
  if (String(sorted) !== String(d.reds)) { failures++; console.log('FAIL reds 未排序'); break; }
  unique[d.reds.join(',') + '+' + d.blue] = true;
}
var count = Object.keys(unique).length;
if (count < 19900) { failures++; console.log('FAIL 随机性不足 ' + count); }
console.log('20000 期唯一结果数 = ' + count);

// --- 自选校验 ---
check('合法自选', Core.validatePick([1, 2, 3, 4, 5, 6], 7), null);
check('红球不足', Core.validatePick([1, 2, 3], 7), '红球要选 6 个');
check('红球重复', Core.validatePick([1, 1, 2, 3, 4, 5], 7), '红球不能重复');
check('红球越界', Core.validatePick([0, 1, 2, 3, 4, 5], 7), '红球号码要在 1-33 之间');
check('蓝球缺失', Core.validatePick([1, 2, 3, 4, 5, 6], null), '蓝球要选 1 个');
check('蓝球越界', Core.validatePick([1, 2, 3, 4, 5, 6], 17), '蓝球号码要在 1-16 之间');

// --- 格式化 ---
check('formatMoney', Core.formatMoney(1234567), '¥1,234,567');
check('formatMoney 0', Core.formatMoney(0), '¥0');
check('formatSigned +', Core.formatSignedMoney(300), '+¥300');
check('formatSigned -', Core.formatSignedMoney(-120), '-¥120');
check('formatBall', Core.formatBall(7), '07');
check('formatBall 33', Core.formatBall(33), '33');
check('formatResult', Core.formatResult({ reds: [3, 7, 11, 18, 25, 29], blue: 9 }), '03 07 11 18 25 29 + 09');

// --- 机选合法性 ---
var q = Core.quickPick();
check('机选红球数', q.reds.length, 6);
check('机选蓝球范围', (q.blue >= 1 && q.blue <= 16), true);

// 可复现随机数发生器（测试用）
function mulberry(seed) {
  var a = seed >>> 0;
  return function () {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    var t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

console.log(failures === 0 ? 'ALL CORE TESTS PASSED' : 'FAILURES: ' + failures);
process.exit(failures === 0 ? 0 : 1);
