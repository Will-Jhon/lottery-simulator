package lottery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** 圆角渐变扁平按钮，支持悬停提亮与禁用置灰 */
public class FancyButton extends JButton {
    private final Color base;
    private boolean hover;

    public FancyButton(String text, Color base) {
        super(text);
        this.base = base;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(false);
        setFont(UiUtil.font(Font.BOLD, 14));
        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D rr = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 10, 10);

        Color top;
        Color bottom;
        if (isEnabled()) {
            top = hover ? base.brighter() : base;
            bottom = hover ? base.brighter() : base.darker();
        } else {
            top = new Color(0x33, 0x3D, 0x52);
            bottom = new Color(0x26, 0x2F, 0x42);
        }
        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fill(rr);
        g2.setColor(new Color(255, 255, 255, isEnabled() ? 60 : 25));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(rr);

        FontMetrics fm = g2.getFontMetrics();
        String t = getText();
        int tw = fm.stringWidth(t);
        int tx = (w - tw) / 2;
        int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawString(t, tx, ty + 1);
        g2.setColor(isEnabled() ? Color.WHITE : new Color(0x8A, 0x94, 0xA8));
        g2.drawString(t, tx, ty);
        g2.dispose();
    }
}
