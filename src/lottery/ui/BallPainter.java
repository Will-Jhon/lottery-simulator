package lottery.ui;

import lottery.model.Ball;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/** 彩票球绘制工具：径向渐变球体 + 高光 + 居中数字 */
public final class BallPainter {
    private BallPainter() {}

    /** 在 (cx, cy) 处绘制半径为 r 的球（含两位数号码） */
    public static void paint(Graphics2D g2, Ball ball, double cx, double cy, double r) {
        if (r <= 0.5) return;
        boolean red = ball.isRed();
        Color light = red ? new Color(0xFF, 0x9E, 0x80) : new Color(0x90, 0xCA, 0xF9);
        Color mid = red ? new Color(0xF0, 0x55, 0x45) : new Color(0x3D, 0x8B, 0xEF);
        Color dark = red ? new Color(0xB7, 0x1C, 0x1C) : new Color(0x0D, 0x47, 0xA1);

        Point2D center = new Point2D.Float((float) (cx - r * 0.38), (float) (cy - r * 0.38));
        RadialGradientPaint paint = new RadialGradientPaint(center, (float) r,
                new float[] { 0f, 0.5f, 1f }, new Color[] { light, mid, dark });
        g2.setPaint(paint);
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

        // 高光
        g2.setColor(new Color(255, 255, 255, 110));
        g2.fill(new Ellipse2D.Double(cx - r * 0.55, cy - r * 0.62, r * 0.5, r * 0.32));

        // 边缘
        g2.setColor(new Color(0, 0, 0, 60));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

        // 号码
        String s = String.format("%02d", ball.getNumber());
        Font f = g2.getFont().deriveFont(Font.BOLD, (float) (r * 1.05));
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(s);
        int tx = (int) Math.round(cx - tw / 2.0);
        int ty = (int) Math.round(cy + (fm.getAscent() - fm.getDescent()) / 2.0);
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawString(s, tx + 1, ty + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(s, tx, ty);
    }

    /** 选中/命中光环 */
    public static void paintGlow(Graphics2D g2, Color color, double cx, double cy, double r, float alpha) {
        if (alpha <= 0.01f || r <= 1) return;
        int cr = color.getRed(), cg = color.getGreen(), cb = color.getBlue();
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float((float) cx, (float) cy), (float) r,
                new float[] { 0f, 0.7f, 1f },
                new Color[] {
                        new Color(cr, cg, cb, Math.min(255, (int) (255 * alpha))),
                        new Color(cr, cg, cb, (int) (120 * alpha)),
                        new Color(cr, cg, cb, 0)
                });
        g2.setPaint(paint);
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }
}
