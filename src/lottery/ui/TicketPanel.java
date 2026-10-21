package lottery.ui;

import lottery.model.Ball;
import lottery.model.DrawResult;
import lottery.model.PrizeLevel;
import lottery.model.Ticket;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** 我的彩票列表：机选/自选的票都显示在这里，开奖后高亮命中号码与奖级 */
public class TicketPanel extends JPanel {
    private final DefaultListModel<Ticket> model = new DefaultListModel<Ticket>();
    private final JList<Ticket> list = new JList<Ticket>(model);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final TitledBorder titled;
    private DrawResult lastResult;

    public TicketPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtil.BORDER, 1),
                "我的彩票（0 注）", TitledBorder.LEFT, TitledBorder.TOP,
                UiUtil.font(Font.BOLD, 13), UiUtil.TEXT);
        setBorder(new CompoundBorder(titled, new EmptyBorder(6, 6, 6, 6)));

        list.setCellRenderer(new TicketCellRenderer(this));
        list.setFixedCellHeight(62);
        list.setOpaque(true);
        list.setBackground(new Color(0x14, 0x20, 0x3C));
        list.setSelectionBackground(new Color(0x2C, 0x47, 0x76));
        list.setSelectionForeground(UiUtil.TEXT);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel placeholder = new JLabel(
                "<html><div style='text-align:center;'>还没有彩票<br><br>点击下方【机选一注】<br>或【自选一注】开始</div></html>",
                SwingConstants.CENTER);
        placeholder.setFont(UiUtil.font(Font.PLAIN, 14));
        placeholder.setForeground(UiUtil.TEXT_DIM);

        cardPanel.setOpaque(false);
        cardPanel.add(scroll, "list");
        cardPanel.add(placeholder, "empty");
        add(cardPanel, BorderLayout.CENTER);
        refresh();
    }

    public void addTicket(Ticket t) {
        model.addElement(t);
        refresh();
    }

    /** 开始新一期：只移除上期已开奖的票，保留新买的待开奖票 */
    public void clearEvaluated() {
        for (int i = model.size() - 1; i >= 0; i--) {
            if (model.get(i).isEvaluated()) model.remove(i);
        }
        lastResult = null;
        refresh();
        list.repaint();
    }

    /** 手动清空全部彩票 */
    public void beginNewDraw() {
        model.clear();
        lastResult = null;
        refresh();
        list.repaint();
    }

    public void setLastResult(DrawResult r) {
        this.lastResult = r;
        list.repaint();
    }

    public boolean isEmpty() { return model.isEmpty(); }

    public int ticketCount() { return model.size(); }

    public List<Ticket> getTickets() {
        List<Ticket> r = new ArrayList<Ticket>();
        for (int i = 0; i < model.size(); i++) r.add(model.get(i));
        return r;
    }

    DrawResult lastResult() { return lastResult; }

    private void refresh() {
        titled.setTitle("我的彩票（" + model.size() + " 注）");
        cards.show(cardPanel, model.isEmpty() ? "empty" : "list");
        repaint();
    }

    // ---------------- 单元格渲染 ----------------

    private static class TicketCellRenderer extends JPanel implements ListCellRenderer<Ticket> {
        private final TicketPanel panel;
        private Ticket ticket;
        private int index;
        private boolean selected;

        TicketCellRenderer(TicketPanel panel) {
            this.panel = panel;
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Ticket> list, Ticket value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            this.ticket = value;
            this.index = index;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(selected ? new Color(0x2C, 0x47, 0x76)
                    : (index % 2 == 0 ? new Color(0x17, 0x23, 0x3F) : new Color(0x1C, 0x2B, 0x4B)));
            g2.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 12, 12));

            DrawResult result = panel.lastResult();
            boolean evaluated = ticket.isEvaluated();
            PrizeLevel prize = ticket.getPrize();

            // 左上：注号
            g2.setFont(UiUtil.font(Font.BOLD, 13));
            g2.setColor(UiUtil.TEXT);
            g2.drawString("第 " + (index + 1) + " 注", 14, 22);

            // 右上：奖级 / 中奖金额
            String tag;
            Color tagColor;
            if (!evaluated) {
                tag = "待开奖";
                tagColor = UiUtil.TEXT_DIM;
            } else if (prize != null) {
                tag = prize.getLabel() + "  ¥" + String.format("%,d", prize.getAmount());
                tagColor = prize == PrizeLevel.FIRST ? UiUtil.GOLD
                        : (prize == PrizeLevel.SECOND ? UiUtil.SILVER : UiUtil.GREEN);
            } else {
                tag = "未中奖";
                tagColor = new Color(0x77, 0x84, 0x9C);
            }
            int tw = g2.getFontMetrics().stringWidth(tag);
            g2.setColor(tagColor);
            g2.drawString(tag, w - 14 - tw, 22);

            // 号码行：命中加金圈
            int x = 14;
            int cy = 43;
            int r = 12;
            for (int i = 0; i < ticket.getRedBalls().size(); i++) {
                int n = ticket.getRedBalls().get(i);
                boolean hit = evaluated && result.getRedBalls().contains(n);
                if (hit) BallPainter.paintGlow(g2, UiUtil.GOLD, x + r, cy, r + 6, 0.5f);
                BallPainter.paint(g2, new Ball(Ball.Color.RED, n), x + r, cy, r);
                if (hit) {
                    g2.setColor(UiUtil.GOLD);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.draw(new Ellipse2D.Double(x + 1.5, cy - r + 1.5, 2 * r - 3, 2 * r - 3));
                }
                x += 2 * r + 6;
            }
            g2.setColor(UiUtil.TEXT_DIM);
            g2.setFont(UiUtil.font(Font.BOLD, 14));
            g2.drawString("+", x + 2, cy + 5);
            x += 16;
            boolean blueHit = evaluated && result.getBlueBall() == ticket.getBlueBall();
            if (blueHit) BallPainter.paintGlow(g2, UiUtil.GOLD, x + r, cy, r + 6, 0.5f);
            BallPainter.paint(g2, new Ball(Ball.Color.BLUE, ticket.getBlueBall()), x + r, cy, r);
            if (blueHit) {
                g2.setColor(UiUtil.GOLD);
                g2.setStroke(new BasicStroke(1.8f));
                g2.draw(new Ellipse2D.Double(x + 1.5, cy - r + 1.5, 2 * r - 3, 2 * r - 3));
            }
            g2.dispose();
        }
    }
}
