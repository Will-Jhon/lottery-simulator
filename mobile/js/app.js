/**
 * 手机版 UI + 摇奖机动画
 * 依赖 js/lottery-core.js（纯逻辑）
 * 动画算法与桌面版 MachinePanel 一致：49 球翻滚搅拌 → 中奖球吸入出球口 → 掉入展示槽
 */
(function () {
  'use strict';

  // 非浏览器环境（语法检查 / Node 单测）直接退出
  if (typeof document === 'undefined' || !document.getElementById) return;

  var Core = window.LotteryCore;

  function $(id) { return document.getElementById(id); }

  // ---------------- 持久化状态 ----------------

  var State = {
    KEY: 'lottery-mobile-v1',
    data: { balance: 100, spent: 0, won: 0, best: 0, period: 2026001, muted: false, tipShown: false },
    load: function () {
      try {
        var raw = localStorage.getItem(this.KEY);
        if (raw) {
          var d = JSON.parse(raw);
          for (var k in this.data) {
            if (d[k] !== undefined) this.data[k] = d[k];
          }
        }
      } catch (e) { /* 隐私模式等场景下静默失败 */ }
    },
    save: function () {
      try { localStorage.setItem(this.KEY, JSON.stringify(this.data)); } catch (e) {}
    }
  };
  State.load();

  // ---------------- 音效（WebAudio 合成，零素材） ----------------

  var Sound = {
    ctx: null,
    ensure: function () {
      if (State.data.muted) return false;
      if (!this.ctx) {
        var AC = window.AudioContext || window.webkitAudioContext;
        if (!AC) return false;
        try { this.ctx = new AC(); } catch (e) { return false; }
      }
      if (this.ctx.state === 'suspended' && typeof this.ctx.resume === 'function') {
        this.ctx.resume();
      }
      return true;
    },
    toneAt: function (freq, ms, amp, delaySec) {
      if (!this.ensure()) return;
      try {
        var ctx = this.ctx;
        var t0 = ctx.currentTime + (delaySec || 0);
        var dur = ms / 1000;
        var osc = ctx.createOscillator();
        var gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, t0);
        gain.gain.setValueAtTime(0.0001, t0);
        gain.gain.exponentialRampToValueAtTime(Math.max(amp, 0.0002), t0 + 0.005);
        gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(t0);
        osc.stop(t0 + dur + 0.05);
      } catch (e) {}
    },
    sweep: function (f0, f1, ms, amp) {
      if (!this.ensure()) return;
      try {
        var ctx = this.ctx;
        var t0 = ctx.currentTime;
        var dur = ms / 1000;
        var osc = ctx.createOscillator();
        var gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(f0, t0);
        osc.frequency.linearRampToValueAtTime(f1, t0 + dur);
        gain.gain.setValueAtTime(0.0001, t0);
        gain.gain.exponentialRampToValueAtTime(Math.max(amp, 0.0002), t0 + 0.012);
        gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(t0);
        osc.stop(t0 + dur + 0.05);
      } catch (e) {}
    },
    click: function () { this.toneAt(520, 45, 0.25); },
    pop: function () { this.toneAt(760, 90, 0.50); },
    whoosh: function () { this.sweep(950, 170, 230, 0.30); },
    rumble: function () { this.sweep(130, 55, 1100, 0.25); },
    fanfare: function () {
      var notes = [[523, 0], [659, 150], [784, 300], [1047, 450], [784, 800], [1047, 950]];
      for (var i = 0; i < notes.length; i++) {
        this.toneAt(notes[i][0], (i === 3 || i === 5) ? 320 : 150, 0.45, notes[i][1] / 1000);
      }
    }
  };

  // ---------------- 摇奖机（Canvas 动画） ----------------

  function roundRectPath(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  }

  function drawBall(ctx, ball, x, y, r) {
    var red = ball.color === 'red';
    var light = red ? '#ff9e80' : '#90caf9';
    var mid = red ? '#f05545' : '#3d8bef';
    var dark = red ? '#b71c1c' : '#0d47a1';
    var g = ctx.createRadialGradient(x - r * 0.38, y - r * 0.38, r * 0.08, x, y, r);
    g.addColorStop(0, light);
    g.addColorStop(0.5, mid);
    g.addColorStop(1, dark);
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();
    // 高光
    ctx.fillStyle = 'rgba(255,255,255,0.35)';
    ctx.beginPath();
    ctx.ellipse(x - r * 0.28, y - r * 0.36, r * 0.28, r * 0.17, -0.5, 0, Math.PI * 2);
    ctx.fill();
    // 号码
    ctx.font = 'bold ' + Math.max(9, Math.round(r * 1.05)) + 'px Arial, Helvetica, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = 'rgba(0,0,0,0.45)';
    ctx.fillText(Core.formatBall(ball.number), x + 1, y + 1);
    ctx.fillStyle = '#ffffff';
    ctx.fillText(Core.formatBall(ball.number), x, y);
  }

  function drawGlow(ctx, x, y, r, alpha) {
    if (alpha <= 0.01 || r <= 1) return;
    var g = ctx.createRadialGradient(x, y, r * 0.3, x, y, r);
    g.addColorStop(0, 'rgba(255,213,79,' + (0.9 * alpha).toFixed(3) + ')');
    g.addColorStop(0.7, 'rgba(255,213,79,' + (0.35 * alpha).toFixed(3) + ')');
    g.addColorStop(1, 'rgba(255,213,79,0)');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();
  }

  var Machine = {
    canvas: null, ctx: null, w: 0, h: 0,
    balls: [], falling: [], revealed: [],
    state: 'idle', // idle | stirring | revealing | pause | done
    pendingResult: null, revealIndex: 0,
    phaseTimer: 0, phaseDuration: 0,
    frame: 0, airJet: 0,
    ballR: 13, slotD: 42, slotGap: 8, slotBlueGap: 16,
    chamber: { left: 0, right: 0, top: 0, bottom: 0 },
    holeX: 0, holeY: 0, slotY: 0, slotX: [],
    listener: null, lastTs: 0,

    init: function (canvas) {
      this.canvas = canvas;
      this.ctx = canvas.getContext('2d');
      var self = this;
      this.resize();
      window.addEventListener('resize', function () { self.resize(); });
      window.addEventListener('orientationchange', function () {
        setTimeout(function () { self.resize(); }, 300);
      });
      this.buildBalls();
      var loop = function (ts) {
        var dt = 1;
        if (self.lastTs) dt = Math.min(2.5, (ts - self.lastTs) / 16.667);
        self.lastTs = ts;
        self.update(dt);
        self.draw();
        requestAnimationFrame(loop);
      };
      requestAnimationFrame(loop);
    },

    resize: function () {
      var rect = this.canvas.getBoundingClientRect();
      if (!rect.width || !rect.height) return;
      var dpr = window.devicePixelRatio || 1;
      this.canvas.width = Math.round(rect.width * dpr);
      this.canvas.height = Math.round(rect.height * dpr);
      this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      this.w = rect.width;
      this.h = rect.height;
      this.computeLayout();
    },

    computeLayout: function () {
      var w = this.w || 320;
      var h = this.h || 260;
      this.ballR = Math.max(10, Math.min(15, Math.floor(w / 27)));
      this.slotD = Math.max(34, Math.min(46, Math.floor((w - 24) / 7.8)));
      this.slotGap = Math.max(5, Math.floor(this.slotD * 0.18));
      this.slotBlueGap = this.slotGap * 2;
      this.chamber.left = 14;
      this.chamber.right = w - 14;
      this.chamber.top = Math.round(h * 0.10);
      this.chamber.bottom = Math.round(h * 0.66);
      this.holeX = w / 2;
      this.holeY = this.chamber.bottom;
      this.slotY = Math.round(h * 0.88);
      var total = this.slotD * 7 + this.slotGap * 5 + this.slotBlueGap;
      var startX = Math.max(8, (w - total) / 2);
      for (var i = 0; i < 6; i++) {
        this.slotX[i] = startX + i * (this.slotD + this.slotGap);
      }
      this.slotX[6] = this.slotX[5] + this.slotD + this.slotBlueGap;
    },

    trayLeft: function () { return this.slotX[0] - this.slotD / 2 - 8; },
    trayRight: function () { return this.slotX[6] + this.slotD / 2 + 8; },

    buildBalls: function () {
      this.balls = [];
      var i;
      for (i = 1; i <= 33; i++) this.balls.push({ color: 'red', number: i, x: 0, y: 0, vx: 0, vy: 0, ejecting: false });
      for (i = 1; i <= 16; i++) this.balls.push({ color: 'blue', number: i, x: 0, y: 0, vx: 0, vy: 0, ejecting: false });
      var c = this.chamber;
      for (i = 0; i < this.balls.length; i++) {
        var b = this.balls[i];
        b.x = c.left + this.ballR + Math.random() * (c.right - c.left - 2 * this.ballR);
        b.y = c.top + this.ballR + Math.random() * (c.bottom - c.top - 2 * this.ballR);
        b.vx = (Math.random() - 0.5) * 3;
        b.vy = -Math.random() * 2;
        b.ejecting = false;
      }
    },

    isDrawing: function () {
      return this.state === 'stirring' || this.state === 'revealing' || this.state === 'pause';
    },

    nextBall: function (index) {
      if (index < 6) {
        return { color: 'red', number: this.pendingResult.drawOrder[index] };
      }
      return { color: 'blue', number: this.pendingResult.blue };
    },

    startDraw: function (result) {
      this.pendingResult = result;
      this.revealIndex = 0;
      this.falling = [];
      this.revealed = [];
      this.buildBalls();
      this.state = 'stirring';
      this.phaseTimer = 0;
      this.phaseDuration = 90; // 搅拌约 1.5 秒
      Sound.rumble();
    },

    skip: function () {
      if (!this.isDrawing()) return;
      while (this.revealIndex < 7) {
        var b = this.nextBall(this.revealIndex);
        this.revealed.push({ ball: b, slot: this.revealIndex, landFrame: this.frame });
        if (this.listener) this.listener.onBallRevealed(this.revealIndex, b.color === 'blue');
        this.revealIndex++;
      }
      this.falling = [];
      Sound.pop();
      this.complete();
    },

    complete: function () {
      this.state = 'done';
      if (this.listener) this.listener.onDrawComplete(this.pendingResult);
    },

    beginReveal: function () {
      var target = this.nextBall(this.revealIndex);
      var mb = null;
      for (var i = 0; i < this.balls.length; i++) {
        var b = this.balls[i];
        if (b.number === target.number && b.color === target.color) { mb = b; break; }
      }
      if (!mb) { this.complete(); return; }
      mb.ejecting = true;
      this.state = 'revealing';
    },

    onLanded: function (fb) {
      this.revealed.push({ ball: fb.ball, slot: fb.slot, landFrame: this.frame });
      Sound.pop();
      if (this.listener) this.listener.onBallRevealed(fb.slot, fb.ball.color === 'blue');
      this.revealIndex++;
      if (this.revealIndex >= 7) this.phaseDuration = 25;      // 蓝球落定后短暂停留
      else if (this.revealIndex === 6) this.phaseDuration = 75; // 蓝球前悬念停顿
      else this.phaseDuration = 34;                            // 红球间隔
      this.phaseTimer = 0;
      this.state = 'pause';
    },

    update: function (dt) {
      this.frame += dt;
      var c = this.chamber;
      var stirring = this.state === 'stirring';
      var speedCap = stirring ? 5 : 3;
      var i, j, b;

      // 气流喷射，保持球翻滚
      this.airJet -= dt;
      if (this.airJet <= 0) {
        this.airJet = stirring ? 7 : 26;
        if (this.balls.length) {
          b = this.balls[Math.floor(Math.random() * this.balls.length)];
          b.vy -= stirring ? 4.2 : 2.4;
          b.vx += (Math.random() - 0.5) * 3;
        }
      }

      for (i = 0; i < this.balls.length; i++) {
        b = this.balls[i];
        if (b.ejecting) {
          // 被抽向出球口
          var dx = this.holeX - b.x;
          var dy = (c.bottom - this.ballR - 2) - b.y;
          var k = Math.min(1, 0.16 * dt);
          b.x += dx * k;
          b.y += dy * k;
          if (Math.abs(dx) < 3 && Math.abs(dy) < 3) {
            this.balls.splice(i, 1);
            i--;
            this.falling.push({
              ball: { color: b.color, number: b.number },
              x: this.holeX, y: this.holeY + 4,
              vy: 1, slot: this.revealed.length + this.falling.length,
              settled: false, bounces: 0
            });
            Sound.whoosh();
          }
          continue;
        }
        b.vy += 0.10 * dt;
        b.vx += (Math.random() - 0.5) * 0.12 * dt;
        b.vy += (Math.random() - 0.5) * 0.12 * dt;
        var sp2 = b.vx * b.vx + b.vy * b.vy;
        if (sp2 > speedCap * speedCap) {
          var sk = speedCap / Math.sqrt(sp2);
          b.vx *= sk; b.vy *= sk;
        } else if (sp2 < 0.03) {
          b.vx = (Math.random() - 0.5) * 2.4;
          b.vy = -Math.random() * 2;
        }
        b.x += b.vx * dt;
        b.y += b.vy * dt;
        if (b.x < c.left + this.ballR) { b.x = c.left + this.ballR; b.vx = Math.abs(b.vx) * 0.92; }
        if (b.x > c.right - this.ballR) { b.x = c.right - this.ballR; b.vx = -Math.abs(b.vx) * 0.92; }
        if (b.y < c.top + this.ballR) { b.y = c.top + this.ballR; b.vy = Math.abs(b.vy) * 0.92; }
        if (b.y > c.bottom - this.ballR) { b.y = c.bottom - this.ballR; b.vy = -Math.abs(b.vy) * 0.88; }
      }

      // 球球碰撞（等质量弹性碰撞）
      for (i = 0; i < this.balls.length; i++) {
        var a = this.balls[i];
        if (a.ejecting) continue;
        for (j = i + 1; j < this.balls.length; j++) {
          var b2 = this.balls[j];
          if (b2.ejecting) continue;
          var ddx = b2.x - a.x;
          var ddy = b2.y - a.y;
          var d2 = ddx * ddx + ddy * ddy;
          var min = this.ballR * 2;
          if (d2 > 0.01 && d2 < min * min) {
            var d = Math.sqrt(d2);
            var nx = ddx / d;
            var ny = ddy / d;
            var overlap = (min - d) * 0.5;
            a.x -= nx * overlap; a.y -= ny * overlap;
            b2.x += nx * overlap; b2.y += ny * overlap;
            var p = (a.vx - b2.vx) * nx + (a.vy - b2.vy) * ny;
            if (p > 0) {
              a.vx -= p * nx; a.vy -= p * ny;
              b2.vx += p * nx; b2.vy += p * ny;
            }
          }
        }
      }

      // 掉落中的球
      for (i = 0; i < this.falling.length; i++) {
        var fb = this.falling[i];
        if (fb.settled) continue;
        fb.vy += 0.5 * dt;
        fb.y += fb.vy * dt;
        fb.x += (this.slotX[fb.slot] - fb.x) * Math.min(1, 0.06 * dt);
        if (fb.y >= this.slotY) {
          if (fb.bounces === 0 && fb.vy > 2.2) {
            fb.y = this.slotY;
            fb.vy = -fb.vy * 0.32;
            fb.bounces++;
          } else {
            fb.y = this.slotY;
            fb.settled = true;
            this.falling.splice(i, 1);
            i--;
            this.onLanded(fb);
          }
        }
      }

      // 阶段推进
      if (this.state === 'stirring') {
        this.phaseTimer += dt;
        if (this.phaseTimer >= this.phaseDuration) this.beginReveal();
      } else if (this.state === 'pause') {
        this.phaseTimer += dt;
        if (this.phaseTimer >= this.phaseDuration) {
          if (this.revealIndex >= 7) this.complete();
          else this.beginReveal();
        }
      }
    },

    draw: function () {
      var ctx = this.ctx;
      var w = this.w, h = this.h;
      if (!w || !h) return;
      var c = this.chamber;
      var i;

      // 背景
      var bg = ctx.createLinearGradient(0, 0, 0, h);
      bg.addColorStop(0, '#111e3c');
      bg.addColorStop(1, '#0a1226');
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, w, h);

      // 标题
      ctx.font = 'bold 15px -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = '#ffd54f';
      ctx.fillText('双 色 球 · 摇 奖 机', w / 2, Math.round(h * 0.06));

      // 机身
      roundRectPath(ctx, c.left - 8, c.top - 8, c.right - c.left + 16, c.bottom - c.top + 16, 16);
      var bodyG = ctx.createLinearGradient(0, c.top - 8, 0, c.bottom + 8);
      bodyG.addColorStop(0, '#334a75');
      bodyG.addColorStop(1, '#14203c');
      ctx.fillStyle = bodyG;
      ctx.fill();
      ctx.strokeStyle = '#5a74a8';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // 玻璃奖仓 + 聚光
      roundRectPath(ctx, c.left, c.top, c.right - c.left, c.bottom - c.top, 12);
      ctx.save();
      ctx.clip();
      var spot = ctx.createRadialGradient(w / 2, c.top + 10, 5, w / 2, c.top + 10, (c.bottom - c.top) * 0.8);
      spot.addColorStop(0, 'rgba(255,255,255,0.13)');
      spot.addColorStop(1, 'rgba(255,255,255,0)');
      ctx.fillStyle = spot;
      ctx.fillRect(c.left, c.top, c.right - c.left, c.bottom - c.top);
      ctx.restore();
      ctx.strokeStyle = 'rgba(159,182,218,0.5)';
      ctx.lineWidth = 1;
      ctx.stroke();

      // 出球口
      ctx.fillStyle = '#050a16';
      ctx.beginPath();
      ctx.ellipse(this.holeX, this.holeY, 11, 8, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffa000';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // 仓内球
      for (i = 0; i < this.balls.length; i++) {
        var b = this.balls[i];
        drawBall(ctx, b, b.x, b.y, this.ballR);
      }
      // 掉落中的球
      for (i = 0; i < this.falling.length; i++) {
        var fb = this.falling[i];
        drawBall(ctx, fb.ball, fb.x, fb.y, this.ballR);
      }

      // 漏斗
      var trayTop = this.slotY - this.slotD / 2 - 8;
      ctx.beginPath();
      ctx.moveTo(this.holeX - 16, c.bottom - 2);
      ctx.lineTo(this.holeX + 16, c.bottom - 2);
      ctx.lineTo(this.trayRight(), trayTop - 3);
      ctx.lineTo(this.trayLeft(), trayTop - 3);
      ctx.closePath();
      ctx.fillStyle = 'rgba(180,205,245,0.10)';
      ctx.fill();

      // 托盘 + 展示槽
      var trayL = this.trayLeft();
      var trayW = this.trayRight() - trayL;
      roundRectPath(ctx, trayL, trayTop, trayW, this.slotD + 18, 10);
      var trayG = ctx.createLinearGradient(0, trayTop, 0, trayTop + this.slotD + 18);
      trayG.addColorStop(0, '#243860');
      trayG.addColorStop(1, '#121e3a');
      ctx.fillStyle = trayG;
      ctx.fill();
      ctx.strokeStyle = 'rgba(138,164,208,0.6)';
      ctx.lineWidth = 1;
      ctx.stroke();

      for (i = 0; i < 7; i++) {
        var cx = this.slotX[i];
        var cy = this.slotY;
        var sr = this.slotD / 2;
        ctx.fillStyle = '#081022';
        ctx.beginPath();
        ctx.arc(cx, cy, sr, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = (i === 6) ? '#ffa000' : '#6e88b8';
        ctx.lineWidth = 1.2;
        ctx.stroke();
      }

      // 已开出的球（落定弹跳 + 金光）
      for (i = 0; i < this.revealed.length; i++) {
        var rb = this.revealed[i];
        var age = this.frame - rb.landFrame;
        var scale = 1;
        if (age < 14) scale = 1 + 0.38 * (1 - age / 14);
        var rcx = this.slotX[rb.slot];
        var rcy = this.slotY;
        if (age < 22) {
          drawGlow(ctx, rcx, rcy, this.ballR + 10 * (1 - age / 22), 0.55 * (1 - age / 22));
        }
        drawBall(ctx, rb.ball, rcx, rcy, this.ballR * scale);
      }

      // 余球数
      ctx.font = '10px Arial, sans-serif';
      ctx.textAlign = 'right';
      ctx.fillStyle = 'rgba(159,176,206,0.85)';
      ctx.fillText('余球 ' + this.balls.length, c.right - 4, c.top + 12);
    }
  };

  // ---------------- 业务逻辑 ----------------

  var tickets = [];
  var drawing = false;

  function status(msg) { $('status').textContent = msg; }

  var toastTimer = null;
  function toast(msg, ms) {
    var el = $('toast');
    el.textContent = msg;
    el.classList.remove('hidden');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.add('hidden'); }, ms || 3500);
  }

  function renderStats() {
    var d = State.data;
    $('stat-period').textContent = d.period;
    $('stat-balance').textContent = Core.formatMoney(d.balance);
    $('stat-spent').textContent = Core.formatMoney(d.spent);
    $('stat-won').textContent = Core.formatMoney(d.won);
    var net = d.won - d.spent;
    var netEl = $('stat-net');
    netEl.textContent = Core.formatSignedMoney(net);
    netEl.className = 'stat-value ' + (net >= 0 ? 'up' : 'down');
    $('stat-best').textContent = Core.formatMoney(d.best);
    $('btn-sound').textContent = d.muted ? '🔇' : '🔊';
  }

  function ballEl(n, color, hit) {
    var s = document.createElement('span');
    s.className = 'ball ball-' + color + (hit ? ' hit' : '');
    s.textContent = Core.formatBall(n);
    return s;
  }

  function buildTicketEl(t, idx) {
    var row = document.createElement('div');
    row.className = 'ticket';

    var head = document.createElement('div');
    head.className = 'ticket-head';
    var left = document.createElement('span');
    left.textContent = '第 ' + (idx + 1) + ' 注';
    head.appendChild(left);
    var right = document.createElement('span');
    if (t.evaluated && t.prize) {
      right.textContent = t.prize.label + ' ' + Core.formatMoney(t.prize.amount);
      right.className = 'ticket-prize win';
    } else if (t.evaluated) {
      right.textContent = '未中奖';
      right.className = 'ticket-prize';
    } else {
      right.textContent = '待开奖';
      right.className = 'ticket-prize';
    }
    head.appendChild(right);
    row.appendChild(head);

    var balls = document.createElement('div');
    balls.className = 'ticket-balls';
    for (var i = 0; i < t.reds.length; i++) {
      balls.appendChild(ballEl(t.reds[i], 'red', t.evaluated && t.hitReds.indexOf(t.reds[i]) >= 0));
    }
    balls.appendChild(ballEl(t.blue, 'blue', t.evaluated && t.blueHit));
    row.appendChild(balls);
    return row;
  }

  function renderTickets() {
    var list = $('ticket-list');
    list.innerHTML = '';
    $('ticket-count').textContent = '(' + tickets.length + ' 注)';
    $('ticket-empty').style.display = tickets.length ? 'none' : '';
    for (var i = 0; i < tickets.length; i++) {
      list.appendChild(buildTicketEl(tickets[i], i));
    }
  }

  function buyTicket(t) {
    if (drawing) return false;
    if (State.data.balance < Core.TICKET_PRICE) {
      status('余额不足，每注 ¥2，点右上角 💰 领零花钱');
      return false;
    }
    State.data.balance -= Core.TICKET_PRICE;
    State.data.spent += Core.TICKET_PRICE;
    tickets.push(t);
    State.save();
    renderStats();
    renderTickets();
    return true;
  }

  function quickPick(n) {
    if (drawing) return;
    var bought = 0;
    for (var i = 0; i < n; i++) {
      if (buyTicket(Core.quickPick())) bought++;
    }
    Sound.click();
    if (!bought) return;
    status(bought < n ? '余额不足，只机选了 ' + bought + ' 注'
        : '机选 ' + bought + ' 注成功，点【开始摇奖】开奖');
  }

  function startDraw() {
    if (drawing || !tickets.length) {
      if (!tickets.length) status('还没有彩票，先机选或自选几注吧');
      return;
    }
    drawing = true;
    // 移除上期已开奖的票，保留新买的
    var kept = [];
    for (var i = 0; i < tickets.length; i++) {
      if (!tickets[i].evaluated) kept.push(tickets[i]);
    }
    tickets = kept;
    renderTickets();

    State.data.period++;
    State.save();
    renderStats();

    setBuyButtons(false);
    $('btn-draw').textContent = '跳过动画';
    var result = Core.draw(State.data.period);
    status('第 ' + State.data.period + ' 期开奖中，盯紧摇奖机...');
    Machine.startDraw(result);
  }

  function onBallRevealed(idx, blue) {
    status('开奖中... 已开出 ' + (idx + 1) + ' / 7 个球' + (blue ? '（蓝球已出）' : ''));
  }

  function onDrawComplete(result) {
    var win = 0;
    var winCount = 0;
    for (var i = 0; i < tickets.length; i++) {
      var t = tickets[i];
      var ev = Core.evaluateTicket(t, result);
      t.evaluated = true;
      t.redHits = ev.redHits;
      t.blueHit = ev.blueHit;
      t.prize = ev.prize;
      t.hitReds = [];
      for (var j = 0; j < t.reds.length; j++) {
        if (result.reds.indexOf(t.reds[j]) >= 0) t.hitReds.push(t.reds[j]);
      }
      if (ev.prize) {
        win += ev.prize.amount;
        winCount++;
      }
    }
    State.data.won += win;
    State.data.balance += win;
    if (win > State.data.best) State.data.best = win;
    State.save();
    renderStats();
    renderTickets();

    drawing = false;
    setBuyButtons(true);
    $('btn-draw').textContent = '开始摇奖';
    status('第 ' + result.period + ' 期 ' + Core.formatResult(result)
        + ' ｜ ' + tickets.length + ' 注中奖 ' + winCount + ' 注，共 ' + Core.formatMoney(win));
    toast(win > 0 ? '恭喜！本期共中奖 ' + Core.formatMoney(win) : '本期未中奖，再接再厉');
    if (win > 0) Sound.fanfare();
  }

  function setBuyButtons(en) {
    $('btn-quick').disabled = !en;
    $('btn-quick5').disabled = !en;
    $('btn-self').disabled = !en;
    $('btn-allowance').disabled = !en;
    $('btn-clear').disabled = !en;
  }

  // ---------------- 自选弹层 ----------------

  var pick = { reds: [], blue: null };

  function openPick() {
    if (drawing) return;
    pick.reds = [];
    pick.blue = null;
    buildPickGrids();
    updatePickHint();
    $('pick-dialog').classList.remove('hidden');
  }

  function closePick() {
    $('pick-dialog').classList.add('hidden');
  }

  function buildPickGrids() {
    var red = $('red-grid');
    red.innerHTML = '';
    for (var i = 1; i <= 33; i++) red.appendChild(pickBallEl(i, 'red'));
    var blue = $('blue-grid');
    blue.innerHTML = '';
    for (var j = 1; j <= 16; j++) blue.appendChild(pickBallEl(j, 'blue'));
  }

  function pickBallEl(n, color) {
    var b = document.createElement('button');
    b.type = 'button';
    b.className = 'pick-ball pick-' + color;
    b.textContent = Core.formatBall(n);
    b.addEventListener('click', function () { togglePick(n, color, b); });
    return b;
  }

  function togglePick(n, color, btn) {
    Sound.click();
    if (color === 'red') {
      var idx = pick.reds.indexOf(n);
      if (idx >= 0) {
        pick.reds.splice(idx, 1);
        btn.classList.remove('picked');
      } else {
        if (pick.reds.length >= 6) {
          flashPickHint('红球最多选 6 个');
          return;
        }
        pick.reds.push(n);
        btn.classList.add('picked');
      }
    } else {
      if (pick.blue === n) {
        pick.blue = null;
        btn.classList.remove('picked');
      } else {
        pick.blue = n;
        var all = $('blue-grid').children;
        for (var i = 0; i < all.length; i++) all[i].classList.remove('picked');
        btn.classList.add('picked');
      }
    }
    updatePickHint();
  }

  function updatePickHint() {
    $('pick-hint').textContent = '已选：红球 ' + pick.reds.length + ' / 6'
        + (pick.blue ? ' · 蓝球 ' + Core.formatBall(pick.blue) : ' · 蓝球 未选');
  }

  var pickHintTimer = null;
  function flashPickHint(msg) {
    var el = $('pick-hint');
    el.textContent = msg;
    el.classList.add('warn');
    if (pickHintTimer) clearTimeout(pickHintTimer);
    pickHintTimer = setTimeout(function () {
      el.classList.remove('warn');
      updatePickHint();
    }, 1600);
  }

  function refreshPickButtons() {
    var reds = $('red-grid').children;
    for (var i = 0; i < reds.length; i++) {
      var n = parseInt(reds[i].textContent, 10);
      if (pick.reds.indexOf(n) >= 0) reds[i].classList.add('picked');
      else reds[i].classList.remove('picked');
    }
    var blues = $('blue-grid').children;
    for (var j = 0; j < blues.length; j++) {
      var m = parseInt(blues[j].textContent, 10);
      if (pick.blue === m) blues[j].classList.add('picked');
      else blues[j].classList.remove('picked');
    }
    updatePickHint();
  }

  function confirmPick() {
    var err = Core.validatePick(pick.reds, pick.blue);
    if (err) {
      flashPickHint(err);
      return;
    }
    closePick();
    if (buyTicket(Core.newTicket(pick.reds, pick.blue))) {
      Sound.click();
      status('自选成功，点【开始摇奖】开奖');
    }
  }

  // ---------------- 事件绑定 ----------------

  function bindEvents() {
    $('btn-quick').addEventListener('click', function () { quickPick(1); });
    $('btn-quick5').addEventListener('click', function () { quickPick(5); });
    $('btn-self').addEventListener('click', openPick);
    $('btn-draw').addEventListener('click', function () {
      Sound.click();
      if (drawing) {
        Machine.skip();
        return;
      }
      startDraw();
    });
    $('btn-allowance').addEventListener('click', function () {
      if (drawing) return;
      State.data.balance += 50;
      State.save();
      renderStats();
      Sound.click();
      status('领取零花钱 ¥50，当前余额 ' + Core.formatMoney(State.data.balance));
    });
    $('btn-clear').addEventListener('click', function () {
      if (drawing) return;
      if (!tickets.length) {
        status('当前没有彩票');
        return;
      }
      var n = tickets.length;
      tickets = [];
      renderTickets();
      Sound.click();
      status('已清空 ' + n + ' 注彩票');
    });
    $('btn-sound').addEventListener('click', function () {
      State.data.muted = !State.data.muted;
      State.save();
      renderStats();
      if (!State.data.muted) Sound.click();
      status(State.data.muted ? '音效已关闭' : '音效已开启');
    });

    // 自选弹层
    $('btn-pick-random').addEventListener('click', function () {
      var q = Core.quickPick();
      pick.reds = q.reds.slice();
      pick.blue = q.blue;
      refreshPickButtons();
      Sound.click();
    });
    $('btn-pick-clear').addEventListener('click', function () {
      pick.reds = [];
      pick.blue = null;
      refreshPickButtons();
      Sound.click();
    });
    $('btn-pick-cancel').addEventListener('click', function () {
      closePick();
      Sound.click();
    });
    $('btn-pick-ok').addEventListener('click', confirmPick);
    $('pick-dialog').addEventListener('click', function (e) {
      if (e.target === this) closePick(); // 点遮罩关闭
    });

    // 添加到主屏幕提示
    $('pwa-tip-close').addEventListener('click', function () {
      State.data.tipShown = true;
      State.save();
      $('pwa-tip').classList.add('hidden');
    });
  }

  // ---------------- 启动 ----------------

  function init() {
    Machine.init($('machine-canvas'));
    Machine.listener = { onBallRevealed: onBallRevealed, onDrawComplete: onDrawComplete };
    renderStats();
    renderTickets();
    bindEvents();

    if (!State.data.tipShown) {
      $('pwa-tip').classList.remove('hidden');
    }

    // Service Worker（离线可用，仅 http(s) 下生效）
    if ('serviceWorker' in navigator && location.protocol.indexOf('http') === 0) {
      window.addEventListener('load', function () {
        navigator.serviceWorker.register('sw.js').catch(function () {});
      });
    }
  }

  init();
})();
