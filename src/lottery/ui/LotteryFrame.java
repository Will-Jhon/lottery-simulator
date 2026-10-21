package lottery.ui;

import lottery.model.Ball;
import lottery.model.DrawResult;
import lottery.model.LotteryMachine;
import lottery.model.Ticket;
import lottery.util.Sound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 主窗口：标题栏统计 + 摇奖机 + 我的彩票 + 操作按钮 + 状态栏。
 */
public class LotteryFrame extends JFrame implements MachinePanel.DrawListener {
    private static final int START_BALANCE = 100;

    private final LotteryMachine machine = new LotteryMachine();
    private final MachinePanel machinePanel = new MachinePanel();
    private final TicketPanel ticketPanel = new TicketPanel();

    private final JLabel periodValue = new JLabel();
    private final JLabel balanceValue = new JLabel();
    private final JLabel spentValue = new JLabel();
    private final JLabel wonValue = new JLabel();
    private final JLabel netValue = new JLabel();
    private final JLabel bestValue = new JLabel();
    private final JLabel statusLabel = new JLabel(" 欢迎来到本地双色球模拟器，先买几注彩票吧");
    private FancyButton drawButton;
    private FancyButton soundButton;
    private final List<FancyButton> buyButtons = new ArrayList<FancyButton>();

    private int balance = START_BALANCE;
    private int totalSpent = 0;
    private int totalWon = 0;
    private int bestWin = 0;
    private int period = 2026001;
    private boolean drawing = false;

    public LotteryFrame() {
        super("本地双色球模拟器");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1120, 800);
        setMinimumSize(new Dimension(1000, 720));

        // 渐变背景的内容面板
        JPanel bg = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x10, 0x1C, 0x38),
                        0, h, new Color(0x08, 0x10, 0x20)));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        bg.setBorder(new EmptyBorder(12, 14, 8, 14));
        bg.setOpaque(false);
        setContentPane(bg);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(machinePanel, BorderLayout.CENTER);
        center.add(buildRightPanel(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        machinePanel.setListener(this);
        setIconImage(buildIcon());
        updateStats();
    }

    // ---------------- UI 构建 ----------------

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("本地双色球模拟器");
        title.setFont(UiUtil.font(Font.BOLD, 24));
        title.setForeground(UiUtil.GOLD);
        JLabel sub = new JLabel("红球 1-33 选 6 · 蓝球 1-16 选 1 · 每注 ¥2 · 每期号码完全随机");
        sub.setFont(UiUtil.font(Font.PLAIN, 12));
        sub.setForeground(UiUtil.TEXT_DIM);
        titleBox.add(title);
        titleBox.add(sub);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        stats.setOpaque(false);
        stats.add(statBox("期号", periodValue));
        stats.add(statBox("余额", balanceValue));
        stats.add(statBox("累计投入", spentValue));
        stats.add(statBox("累计奖金", wonValue));
        stats.add(statBox("净收益", netValue));
        stats.add(statBox("最佳单期", bestValue));

        p.add(titleBox, BorderLayout.WEST);
        p.add(stats, BorderLayout.CENTER);
        return p;
    }

    private JPanel statBox(String title, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(UiUtil.font(Font.PLAIN, 11));
        t.setForeground(UiUtil.TEXT_DIM);
        valueLabel.setFont(UiUtil.font(Font.BOLD, 15));
        valueLabel.setForeground(UiUtil.TEXT);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(t, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiUtil.BORDER, 1), new EmptyBorder(4, 6, 4, 6)));
        p.setPreferredSize(new Dimension(102, 52));
        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(352, 10));
        p.add(ticketPanel, BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 8));
        grid.setOpaque(false);
        FancyButton quick = new FancyButton("机选一注", UiUtil.BLUE);
        quick.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { quickPick(1); }
        });
        FancyButton quick5 = new FancyButton("机选五注", UiUtil.BLUE_DARK);
        quick5.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { quickPick(5); }
        });
        FancyButton self = new FancyButton("自选一注", UiUtil.TEAL);
        self.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { selfPick(); }
        });
        FancyButton clear = new FancyButton("清空彩票", UiUtil.GRAY);
        clear.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { clearTickets(); }
        });
        FancyButton allowance = new FancyButton("领零花钱 +¥50", new Color(0x2E, 0x7D, 0x32));
        allowance.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { allowance(); }
        });
        soundButton = new FancyButton("音效 开", UiUtil.PURPLE);
        soundButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { toggleSound(); }
        });
        grid.add(quick);
        grid.add(quick5);
        grid.add(self);
        grid.add(clear);
        grid.add(allowance);
        grid.add(soundButton);
        buyButtons.add(quick);
        buyButtons.add(quick5);
        buyButtons.add(self);
        buyButtons.add(clear);
        buyButtons.add(allowance);
        buyButtons.add(soundButton);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);
        south.add(grid, BorderLayout.CENTER);

        drawButton = new FancyButton("开 始 摇 奖", UiUtil.RED);
        drawButton.setFont(UiUtil.font(Font.BOLD, 18));
        drawButton.setPreferredSize(new Dimension(352, 52));
        drawButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Sound.click();
                if (drawing) {
                    machinePanel.skip();
                    return;
                }
                if (ticketPanel.isEmpty()) {
                    status("还没有买彩票，先【机选一注】或【自选一注】吧");
                    return;
                }
                startDraw();
            }
        });
        south.add(drawButton, BorderLayout.SOUTH);

        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtil.BORDER), new EmptyBorder(6, 2, 2, 2)));
        statusLabel.setFont(UiUtil.font(Font.PLAIN, 13));
        statusLabel.setForeground(UiUtil.TEXT);
        p.add(statusLabel, BorderLayout.CENTER);
        return p;
    }

    private BufferedImage buildIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BallPainter.paint(g, new Ball(Ball.Color.RED, 6), 32, 32, 28);
        g.dispose();
        return img;
    }

    // ---------------- 业务逻辑 ----------------

    private void quickPick(int n) {
        if (drawing) return;
        int bought = 0;
        for (int i = 0; i < n; i++) {
            if (balance < Ticket.PRICE) break;
            balance -= Ticket.PRICE;
            totalSpent += Ticket.PRICE;
            ticketPanel.addTicket(machine.quickPick());
            bought++;
        }
        Sound.click();
        if (bought == 0) {
            status("余额不足，每注 ¥2，可点击【领零花钱】");
        } else if (bought < n) {
            status("余额不足，只机选了 " + bought + " 注");
        } else {
            status("机选 " + bought + " 注成功（共扣 ¥" + (bought * Ticket.PRICE) + "），点击【开始摇奖】开奖");
        }
        updateStats();
    }

    private void selfPick() {
        if (drawing) return;
        Ticket t = BuyTicketDialog.showDialog(this, machine);
        if (t == null) return;
        if (balance < Ticket.PRICE) {
            status("余额不足，每注 ¥2，可点击【领零花钱】");
            return;
        }
        balance -= Ticket.PRICE;
        totalSpent += Ticket.PRICE;
        ticketPanel.addTicket(t);
        Sound.click();
        status("自选成功：" + ticketText(t) + "（已扣 ¥2），点击【开始摇奖】开奖");
        updateStats();
    }

    private void clearTickets() {
        if (drawing) return;
        int n = ticketPanel.ticketCount();
        if (n == 0) {
            status("当前没有彩票");
            return;
        }
        ticketPanel.beginNewDraw();
        Sound.click();
        status("已清空 " + n + " 注彩票（已支付的 ¥" + (n * Ticket.PRICE) + " 不退还）");
    }

    private void allowance() {
        balance += 50;
        Sound.click();
        status("领取零花钱 ¥50，当前余额 ¥" + balance);
        updateStats();
    }

    private void toggleSound() {
        boolean muted = !Sound.isMuted();
        Sound.setMuted(muted);
        soundButton.setText(muted ? "音效 关" : "音效 开");
        if (!muted) Sound.click();
    }

    private void startDraw() {
        drawing = true;
        ticketPanel.clearEvaluated(); // 移除上期已开奖的票，保留新买的票
        period++;
        setTitle("本地双色球模拟器 - 第 " + period + " 期");
        status("第 " + period + " 期正在开奖，请观察摇奖机 ...（点击【跳过动画】可立即开完）");
        setBuyControlsEnabled(false);
        drawButton.setText("跳过动画");
        DrawResult result = machine.draw(period);
        machinePanel.startDraw(result);
    }

    // ---------------- 开奖回调 ----------------

    @Override
    public void onBallRevealed(Ball ball, int index, boolean blue) {
        status("开奖中 ... 已开出 " + (index + 1) + " / 7 个球"
                + (blue ? "（蓝球已出）" : ""));
    }

    @Override
    public void onDrawComplete(DrawResult result) {
        List<Ticket> tickets = ticketPanel.getTickets();
        int win = 0;
        int winTickets = 0;
        for (int i = 0; i < tickets.size(); i++) {
            Ticket t = tickets.get(i);
            t.evaluate(result);
            if (t.getPrize() != null) {
                win += t.getPrizeAmount();
                winTickets++;
            }
        }
        totalWon += win;
        balance += win;
        if (win > bestWin) bestWin = win;
        ticketPanel.setLastResult(result);

        drawing = false;
        setBuyControlsEnabled(true);
        drawButton.setText("开始摇奖");
        updateStats();

        status("第 " + period + " 期开奖：" + result.toDisplayString()
                + "  ｜  持票 " + tickets.size() + " 注，中奖 " + winTickets + " 注，共 ¥" + String.format("%,d", win));
        machinePanel.showToast(win > 0 ? "恭喜！本期共中奖 ¥" + String.format("%,d", win)
                : "本期未中奖，再接再厉", 4000);
        if (win > 0) Sound.fanfare();
    }

    // ---------------- 辅助 ----------------

    private void setBuyControlsEnabled(boolean en) {
        for (int i = 0; i < buyButtons.size(); i++) {
            buyButtons.get(i).setEnabled(en);
        }
    }

    private void status(String s) {
        statusLabel.setText(" " + s);
    }

    private String ticketText(Ticket t) {
        StringBuilder sb = new StringBuilder("红 ");
        for (int i = 0; i < t.getRedBalls().size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02d", t.getRedBalls().get(i)));
        }
        sb.append(" + 蓝 ").append(String.format("%02d", t.getBlueBall()));
        return sb.toString();
    }

    private void updateStats() {
        periodValue.setText(String.valueOf(period));
        balanceValue.setText("¥" + balance);
        spentValue.setText("¥" + totalSpent);
        wonValue.setText("¥" + totalWon);
        int net = totalWon - totalSpent;
        netValue.setText((net >= 0 ? "+¥" : "-¥") + Math.abs(net));
        netValue.setForeground(net >= 0 ? UiUtil.GREEN : UiUtil.RED_LIGHT);
        bestValue.setText("¥" + bestWin);
    }
}
