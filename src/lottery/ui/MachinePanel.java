package lottery.ui;

import lottery.model.Ball;
import lottery.model.DrawResult;
import lottery.util.Sound;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 摇奖机面板（电视风格）：
 * 49 个球（33 红 + 16 蓝）在奖仓内翻滚搅拌，开奖时中奖球依次被吸入出球口、
 * 掉入下方展示槽，全部开完后回调结果。
 */
public class MachinePanel extends JPanel {

    public interface DrawListener {
        void onBallRevealed(Ball ball, int index, boolean blue);

        void onDrawComplete(DrawResult result);
    }

    private enum State { IDLE, STIRRING, REVEALING, PAUSE, DONE }

    private static final class MovingBall {
        final Ball ball;
        double x, y, vx, vy;
        boolean ejecting;

        MovingBall(Ball ball, double x, double y) {
            this.ball = ball;
            this.x = x;
            this.y = y;
        }
    }

    private static final class FallingBall {
        Ball ball;
        double x, y;
        double vy = 1;
        int slot;
        boolean settled;
        int bounces;
    }

    private static final class RevealedBall {
        final Ball ball;
        final int slot;
        final long landFrame;

        RevealedBall(Ball ball, int slot, long landFrame) {
            this.ball = ball;
            this.slot = slot;
            this.landFrame = landFrame;
        }
    }

    private static final int BALL_R = 14;
    private static final int SLOT_D = 54;
    private static final int SLOT_GAP = 14;
    private static final int SLOT_BLUE_GAP = 28;
    private static final int CHAMBER_BOTTOM_MARGIN = 240;
    private static final int FRAME_MS = 16;
    private static final double GRAVITY = 0.10;

    private final Timer timer;
    private final Random rand = new Random();
    private final List<MovingBall> balls = new ArrayList<MovingBall>();
    private final List<FallingBall> falling = new ArrayList<FallingBall>();
    private final List<RevealedBall> revealed = new ArrayList<RevealedBall>();

    private DrawListener listener;
    private State state = State.IDLE;
    private DrawResult pendingResult;
    private int revealIndex;
    private int phaseTimer;
    private int phaseDuration;
    private long frameCount;
    private int airJetCooldown;

    private int chamberLeft, chamberRight, chamberTop, chamberBottom;
    private double holeX, holeY;
    private int slotY;
    private final int[] slotX = new int[7];

    private String toastText;
    private long toastUntil;

    public MachinePanel() {
        setOpaque(true);
        setBackground(new Color(0x0D, 0x17, 0x30));
        setPreferredSize(new Dimension(680, 540));
        computeLayout();
        buildBalls();
        timer = new Timer(FRAME_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
                repaint();
            }
        });
        timer.start();
    }

    public void setListener(DrawListener l) {
        this.listener = l;
    }

    public boolean isDrawing() {
        return state == State.STIRRING || state == State.REVEALING || state == State.PAUSE;
    }

    /** 开始一期开奖动画（结果已由 LotteryMachine 随机产生，动画负责"演"出来） */
    public void startDraw(DrawResult result) {
        this.pendingResult = result;
        this.revealIndex = 0;
        this.falling.clear();
        this.revealed.clear();
        buildBalls();
        this.state = State.STIRRING;
        this.phaseTimer = 0;
        this.phaseDuration = 90; // 搅拌约 1.5 秒
        Sound.rumble();
    }

    /** 跳过动画，直接开完 */
    public void skip() {
        if (!isDrawing()) return;
        while (revealIndex < 7) {
            Ball b = nextBall(revealIndex);
            revealed.add(new RevealedBall(b, revealIndex, frameCount));
            if (listener != null) listener.onBallRevealed(b, revealIndex, !b.isRed());
            revealIndex++;
        }
        falling.clear();
        Sound.pop();
        complete();
    }

    public void showToast(String text, int durationMs) {
        this.toastText = text;
        this.toastUntil = System.currentTimeMillis() + durationMs;
    }

    private Ball nextBall(int index) {
        if (index < 6) return new Ball(Ball.Color.RED, pendingResult.getDrawOrder().get(index));
        return new Ball(Ball.Color.BLUE, pendingResult.getBlueBall());
    }

    private void complete() {
        state = State.DONE;
        DrawResult r = pendingResult;
        if (listener != null) listener.onDrawComplete(r);
    }

    private void beginReveal() {
        Ball target = nextBall(revealIndex);
        MovingBall mb = null;
        for (int i = 0; i < balls.size(); i++) {
            MovingBall b = balls.get(i);
            if (b.ball.getNumber() == target.getNumber() && b.ball.getColor() == target.getColor()) {
                mb = b;
                break;
            }
        }
        if (mb == null) {
            complete();
            return;
        }
        mb.ejecting = true;
        state = State.REVEALING;
    }

    private void buildBalls() {
        balls.clear();
        for (int i = 1; i <= 33; i++) balls.add(new MovingBall(new Ball(Ball.Color.RED, i), 0, 0));
        for (int i = 1; i <= 16; i++) balls.add(new MovingBall(new Ball(Ball.Color.BLUE, i), 0, 0));
        for (int i = 0; i < balls.size(); i++) {
            MovingBall b = balls.get(i);
            b.x = chamberLeft + BALL_R + rand.nextDouble() * (chamberRight - chamberLeft - 2 * BALL_R);
            b.y = chamberTop + BALL_R + rand.nextDouble() * (chamberBottom - chamberTop - 2 * BALL_R);
            b.vx = (rand.nextDouble() - 0.5) * 3;
            b.vy = -rand.nextDouble() * 2;
            b.ejecting = false;
        }
    }

    private void computeLayout() {
        int w = Math.max(getWidth(), getPreferredSize().width);
        int h = Math.max(getHeight(), getPreferredSize().height);
        chamberLeft = 28;
        chamberRight = w - 28;
        chamberTop = 64;
        chamberBottom = h - CHAMBER_BOTTOM_MARGIN;
        holeX = w / 2.0;
        holeY = chamberBottom;
        slotY = h - 95;
        int total = SLOT_D * 7 + SLOT_GAP * 5 + SLOT_BLUE_GAP;
        int startX = (w - total) / 2;
        for (int i = 0; i < 6; i++) slotX[i] = startX + i * (SLOT_D + SLOT_GAP);
        slotX[6] = slotX[5] + SLOT_D + SLOT_BLUE_GAP;
    }

    private int trayLeft() { return slotX[0] - SLOT_D / 2 - 16; }

    private int trayRight() { return slotX[6] + SLOT_D / 2 + 16; }

    // ---------------- 动画逻辑 ----------------

    private void update() {
        frameCount++;
        computeLayout();
        boolean stirring = state == State.STIRRING;
        double speedCap = stirring ? 5.0 : 3.0;

        // 气流喷射，让球持续翻滚（模拟真实摇奖机的鼓风）
        if (--airJetCooldown <= 0) {
            airJetCooldown = stirring ? 7 : 26;
            if (!balls.isEmpty()) {
                MovingBall b = balls.get(rand.nextInt(balls.size()));
                b.vy -= stirring ? 4.2 : 2.4;
                b.vx += (rand.nextDouble() - 0.5) * 3.0;
            }
        }

        for (int i = 0; i < balls.size(); i++) {
            MovingBall b = balls.get(i);
            if (b.ejecting) {
                // 被抽向出球口
                double tx = holeX;
                double ty = chamberBottom - BALL_R - 2;
                double dx = tx - b.x;
                double dy = ty - b.y;
                b.x += dx * 0.16;
                b.y += dy * 0.16;
                if (Math.abs(dx) < 3 && Math.abs(dy) < 3) {
                    balls.remove(i);
                    i--;
                    FallingBall fb = new FallingBall();
                    fb.ball = b.ball;
                    fb.x = holeX;
                    fb.y = holeY + 4;
                    fb.slot = revealed.size() + falling.size();
                    falling.add(fb);
                    Sound.whoosh();
                }
                continue;
            }
            b.vy += GRAVITY;
            b.vx += (rand.nextDouble() - 0.5) * 0.12;
            b.vy += (rand.nextDouble() - 0.5) * 0.12;
            double sp2 = b.vx * b.vx + b.vy * b.vy;
            if (sp2 > speedCap * speedCap) {
                double k = speedCap / Math.sqrt(sp2);
                b.vx *= k;
                b.vy *= k;
            } else if (sp2 < 0.03) {
                // 防静止
                b.vx = (rand.nextDouble() - 0.5) * 2.4;
                b.vy = -rand.nextDouble() * 2.0;
            }
            b.x += b.vx;
            b.y += b.vy;
            if (b.x < chamberLeft + BALL_R) {
                b.x = chamberLeft + BALL_R;
                b.vx = Math.abs(b.vx) * 0.92;
            }
            if (b.x > chamberRight - BALL_R) {
                b.x = chamberRight - BALL_R;
                b.vx = -Math.abs(b.vx) * 0.92;
            }
            if (b.y < chamberTop + BALL_R) {
                b.y = chamberTop + BALL_R;
                b.vy = Math.abs(b.vy) * 0.92;
            }
            if (b.y > chamberBottom - BALL_R) {
                b.y = chamberBottom - BALL_R;
                b.vy = -Math.abs(b.vy) * 0.88;
            }
        }

        // 球球碰撞（等质量弹性碰撞，营造真实搅拌感）
        for (int i = 0; i < balls.size(); i++) {
            MovingBall a = balls.get(i);
            if (a.ejecting) continue;
            for (int j = i + 1; j < balls.size(); j++) {
                MovingBall b = balls.get(j);
                if (b.ejecting) continue;
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double d2 = dx * dx + dy * dy;
                double min = BALL_R * 2;
                if (d2 > 0.01 && d2 < min * min) {
                    double d = Math.sqrt(d2);
                    double nx = dx / d;
                    double ny = dy / d;
                    double overlap = (min - d) * 0.5;
                    a.x -= nx * overlap;
                    a.y -= ny * overlap;
                    b.x += nx * overlap;
                    b.y += ny * overlap;
                    double p = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny;
                    if (p > 0) {
                        a.vx -= p * nx;
                        a.vy -= p * ny;
                        b.vx += p * nx;
                        b.vy += p * ny;
                    }
                }
            }
        }

        // 掉落中的球
        for (int i = 0; i < falling.size(); i++) {
            FallingBall fb = falling.get(i);
            if (fb.settled) continue;
            fb.vy += 0.5;
            fb.y += fb.vy;
            fb.x += (slotX[fb.slot] - fb.x) * 0.06; // 滑向目标展示槽
            if (fb.y >= slotY) {
                if (fb.bounces == 0 && fb.vy > 2.2) {
                    fb.y = slotY;
                    fb.vy = -fb.vy * 0.32;
                    fb.bounces++;
                } else {
                    fb.y = slotY;
                    fb.settled = true;
                    falling.remove(i);
                    i--;
                    onLanded(fb);
                }
            }
        }

        // 阶段推进
        if (state == State.STIRRING) {
            if (++phaseTimer >= phaseDuration) beginReveal();
        } else if (state == State.PAUSE) {
            if (++phaseTimer >= phaseDuration) {
                if (revealIndex >= 7) complete();
                else beginReveal();
            }
        }
    }

    private void onLanded(FallingBall fb) {
        revealed.add(new RevealedBall(fb.ball, fb.slot, frameCount));
        Sound.pop();
        boolean blue = fb.ball.getColor() == Ball.Color.BLUE;
        if (listener != null) listener.onBallRevealed(fb.ball, fb.slot, blue);
        revealIndex++;
        if (revealIndex >= 7) phaseDuration = 25;        // 蓝球落定后短暂停留
        else if (revealIndex == 6) phaseDuration = 75;   // 蓝球前悬念停顿
        else phaseDuration = 34;                         // 红球间隔
        phaseTimer = 0;
        state = State.PAUSE;
    }

    // ---------------- 绘制 ----------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        computeLayout();
        int w = getWidth();
        int h = getHeight();

        drawBackground(g2, w, h);
        drawTitle(g2, w);
        drawMachineBody(g2);

        for (int i = 0; i < balls.size(); i++) {
            MovingBall b = balls.get(i);
            BallPainter.paint(g2, b.ball, b.x, b.y, BALL_R);
        }
        for (int i = 0; i < falling.size(); i++) {
            FallingBall fb = falling.get(i);
            BallPainter.paint(g2, fb.ball, fb.x, fb.y, BALL_R);
        }

        drawFunnel(g2);
        drawTrayAndSlots(g2);

        if (toastText != null && System.currentTimeMillis() < toastUntil) {
            drawToast(g2, w);
        }
        g2.dispose();
    }

    private void drawBackground(Graphics2D g2, int w, int h) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0x11, 0x1E, 0x3C), 0, h, new Color(0x0A, 0x12, 0x26)));
        g2.fillRect(0, 0, w, h);
    }

    private void drawTitle(Graphics2D g2, int w) {
        String t = "双 色 球 · 摇 奖 机";
        g2.setFont(UiUtil.font(Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(t)) / 2;
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(t, x + 2, 44);
        g2.setColor(UiUtil.GOLD);
        g2.drawString(t, x, 42);

        String left = "奖仓余球 " + balls.size();
        g2.setFont(UiUtil.font(Font.PLAIN, 12));
        int lw = g2.getFontMetrics().stringWidth(left);
        g2.setColor(UiUtil.TEXT_DIM);
        g2.drawString(left, chamberRight - lw, chamberTop - 10);
    }

    private void drawMachineBody(Graphics2D g2) {
        // 外框
        RoundRectangle2D body = new RoundRectangle2D.Double(
                chamberLeft - 14, chamberTop - 14,
                (chamberRight - chamberLeft) + 28, (chamberBottom - chamberTop) + 28, 30, 30);
        g2.setPaint(new GradientPaint(0, chamberTop - 14, new Color(0x33, 0x4A, 0x75),
                0, chamberBottom + 14, new Color(0x14, 0x20, 0x3C)));
        g2.fill(body);
        g2.setColor(new Color(0x5A, 0x74, 0xA8));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(body);

        // 四角螺栓
        g2.setColor(new Color(0x8C, 0xA4, 0xCF));
        int bx = chamberLeft - 6, by = chamberTop - 6;
        int bx2 = chamberRight + 6, by2 = chamberBottom + 6;
        int[][] corners = { { bx, by }, { bx2, by }, { bx, by2 }, { bx2, by2 } };
        for (int i = 0; i < corners.length; i++) {
            g2.fill(new Ellipse2D.Double(corners[i][0] - 3, corners[i][1] - 3, 6, 6));
        }

        // 玻璃奖仓
        RoundRectangle2D glass = new RoundRectangle2D.Double(chamberLeft, chamberTop,
                chamberRight - chamberLeft, chamberBottom - chamberTop, 22, 22);
        g2.setPaint(new GradientPaint(0, chamberTop, new Color(255, 255, 255, 34),
                0, chamberBottom, new Color(255, 255, 255, 10)));
        g2.fill(glass);
        g2.setColor(new Color(0x9F, 0xB6, 0xDA, 150));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(glass);

        // 顶部聚光
        RadialGradientPaint spot = new RadialGradientPaint(
                new Point2D.Float((chamberLeft + chamberRight) / 2f, chamberTop + 20f),
                (chamberBottom - chamberTop) * 0.7f,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 255, 255, 40), new Color(255, 255, 255, 0) });
        g2.setPaint(spot);
        g2.fill(glass);

        // 出球口
        g2.setColor(new Color(0x05, 0x0A, 0x16));
        g2.fill(new Ellipse2D.Double(holeX - 18, holeY - 14, 36, 28));
        g2.setColor(UiUtil.GOLD_DARK);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Ellipse2D.Double(holeX - 18, holeY - 14, 36, 28));
    }

    private void drawFunnel(Graphics2D g2) {
        int tx = trayLeft();
        int tw = trayRight() - trayLeft();
        int ty = slotY - SLOT_D / 2 - 16;
        Polygon funnel = new Polygon();
        funnel.addPoint((int) holeX - 26, chamberBottom - 2);
        funnel.addPoint((int) holeX + 26, chamberBottom - 2);
        funnel.addPoint(tx + tw, ty - 4);
        funnel.addPoint(tx, ty - 4);
        g2.setColor(new Color(180, 205, 245, 26));
        g2.fill(funnel);
        g2.setColor(new Color(160, 190, 235, 60));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(funnel);
    }

    private void drawTrayAndSlots(Graphics2D g2) {
        int tx = trayLeft();
        int tw = trayRight() - trayLeft();
        int ty = slotY - SLOT_D / 2 - 16;
        int th = SLOT_D + 34;
        RoundRectangle2D tray = new RoundRectangle2D.Double(tx, ty, tw, th, 16, 16);
        g2.setPaint(new GradientPaint(0, ty, new Color(0x24, 0x38, 0x60), 0, ty + th, new Color(0x12, 0x1E, 0x3A)));
        g2.fill(tray);
        g2.setColor(new Color(0x8A, 0xA4, 0xD0, 140));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(tray);

        for (int i = 0; i < 7; i++) {
            int cx = slotX[i];
            int cy = slotY;
            boolean blue = i == 6;
            g2.setColor(new Color(0x08, 0x10, 0x22));
            g2.fill(new Ellipse2D.Double(cx - SLOT_D / 2, cy - SLOT_D / 2, SLOT_D, SLOT_D));
            g2.setColor(blue ? UiUtil.GOLD_DARK : new Color(0x6E, 0x88, 0xB8));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new Ellipse2D.Double(cx - SLOT_D / 2, cy - SLOT_D / 2, SLOT_D, SLOT_D));
            if (blue) {
                g2.setFont(UiUtil.font(Font.BOLD, 12));
                g2.setColor(UiUtil.GOLD);
                String s = "蓝球";
                int lw = g2.getFontMetrics().stringWidth(s);
                g2.drawString(s, cx - lw / 2, cy + SLOT_D / 2 + 17);
            }
        }

        // 已开出的球（带落定弹跳动画 + 金光）
        for (int i = 0; i < revealed.size(); i++) {
            RevealedBall rb = revealed.get(i);
            long age = frameCount - rb.landFrame;
            double scale = 1.0;
            if (age < 14) scale = 1 + 0.38 * (1 - age / 14.0);
            double cx = slotX[rb.slot];
            double cy = slotY;
            if (age < 22) {
                float a = (float) (0.55 * (1 - age / 22.0));
                BallPainter.paintGlow(g2, UiUtil.GOLD, cx, cy, BALL_R + 10 * (1 - age / 22.0), a);
            }
            BallPainter.paint(g2, rb.ball, cx, cy, BALL_R * scale);
        }
    }

    private void drawToast(Graphics2D g2, int w) {
        String t = toastText;
        g2.setFont(UiUtil.font(Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(t);
        int bw = tw + 48;
        int bh = 44;
        int x = (w - bw) / 2;
        int y = chamberTop + 8;
        g2.setColor(new Color(0x0A, 0x14, 0x28, 230));
        g2.fill(new RoundRectangle2D.Double(x, y, bw, bh, 22, 22));
        g2.setColor(UiUtil.GOLD);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(x, y, bw, bh, 22, 22));
        g2.drawString(t, x + 24, y + bh / 2 + (fm.getAscent() - fm.getDescent()) / 2);
    }
}
