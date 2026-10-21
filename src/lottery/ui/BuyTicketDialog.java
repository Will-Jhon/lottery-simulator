package lottery.ui;

import lottery.model.Ball;
import lottery.model.LotteryMachine;
import lottery.model.Ticket;
import lottery.util.Sound;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 自选一注对话框：33 个红球选 6，16 个蓝球选 1 */
public class BuyTicketDialog extends JDialog {
    private final Set<Integer> selectedRed = new LinkedHashSet<Integer>();
    private Integer selectedBlue;
    private final List<BallButton> redButtons = new ArrayList<BallButton>();
    private final List<BallButton> blueButtons = new ArrayList<BallButton>();
    private final JLabel hintLabel = new JLabel();
    private Ticket result;

    private BuyTicketDialog(Window owner, final LotteryMachine machine) {
        super(owner, "自选一注", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 680);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(UiUtil.BG_DARK);
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 14, 12, 14));

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 12));
        center.setOpaque(false);
        center.add(buildBallGrid(redButtons, 33, true, 6));
        center.add(buildBallGrid(blueButtons, 16, false, 8));
        add(center, BorderLayout.CENTER);

        // 底部提示 + 按钮
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        hintLabel.setFont(UiUtil.font(Font.PLAIN, 13));
        hintLabel.setForeground(UiUtil.TEXT_DIM);
        south.add(hintLabel, BorderLayout.NORTH);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        FancyButton randomBtn = new FancyButton("机选填充", UiUtil.BLUE);
        randomBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Ticket t = machine.quickPick();
                selectedRed.clear();
                selectedRed.addAll(t.getRedBalls());
                selectedBlue = t.getBlueBall();
                refreshButtons();
                Sound.click();
                updateHint();
            }
        });
        FancyButton clearBtn = new FancyButton("清空", UiUtil.GRAY);
        clearBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                selectedRed.clear();
                selectedBlue = null;
                refreshButtons();
                Sound.click();
                updateHint();
            }
        });
        FancyButton cancelBtn = new FancyButton("取消", UiUtil.GRAY);
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                result = null;
                dispose();
            }
        });
        FancyButton okBtn = new FancyButton("确定", new Color(0x2E, 0x7D, 0x32));
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (selectedRed.size() != 6 || selectedBlue == null) {
                    flashHint("请选满 6 个红球和 1 个蓝球");
                    return;
                }
                result = new Ticket(new ArrayList<Integer>(selectedRed), selectedBlue);
                Sound.click();
                dispose();
            }
        });
        btns.add(randomBtn);
        btns.add(clearBtn);
        btns.add(cancelBtn);
        btns.add(okBtn);
        south.add(btns, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        updateHint();
    }

    /** 弹出对话框，返回选中的彩票；取消返回 null */
    public static Ticket showDialog(Window owner, LotteryMachine machine) {
        BuyTicketDialog d = new BuyTicketDialog(owner, machine);
        d.setVisible(true);
        return d.result;
    }

    private JPanel buildBallGrid(List<BallButton> target, int count, boolean red, int cols) {
        JPanel grid = new JPanel(new GridLayout(0, cols, 8, 8));
        grid.setOpaque(false);
        String title = red ? "红球  1 - 33（选 6 个）" : "蓝球  1 - 16（选 1 个）";
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtil.BORDER, 1), title,
                TitledBorder.LEFT, TitledBorder.TOP, UiUtil.font(Font.BOLD, 13), UiUtil.TEXT);
        grid.setBorder(new CompoundBorder(border, new EmptyBorder(8, 8, 8, 8)));
        for (int i = 1; i <= count; i++) {
            BallButton b = new BallButton(i, red);
            target.add(b);
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            cell.setOpaque(false);
            cell.add(b);
            grid.add(cell);
        }
        return grid;
    }

    private void refreshButtons() {
        for (int i = 0; i < redButtons.size(); i++) {
            BallButton b = redButtons.get(i);
            b.setSelected(selectedRed.contains(b.getNumber()));
        }
        for (int i = 0; i < blueButtons.size(); i++) {
            BallButton b = blueButtons.get(i);
            b.setSelected(selectedBlue != null && selectedBlue == b.getNumber());
        }
    }

    private void updateHint() {
        hintLabel.setText(String.format("已选：红球 %d / 6 %s",
                selectedRed.size(),
                selectedBlue == null ? "· 蓝球 未选"
                        : String.format("· 蓝球 %02d", selectedBlue)));
    }

    private void flashHint(String msg) {
        hintLabel.setText(msg);
        hintLabel.setForeground(UiUtil.GOLD);
        Timer t = new Timer(1800, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                hintLabel.setForeground(UiUtil.TEXT_DIM);
                updateHint();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    // ---------------- 圆形选号按钮 ----------------

    private final class BallButton extends JToggleButton {
        private final int number;
        private final boolean red;
        private boolean hover;

        BallButton(int number, boolean red) {
            this.number = number;
            this.red = red;
            setText(String.format("%02d", number));
            setFont(UiUtil.font(Font.BOLD, 14));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(46, 46));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (BallButton.this.red) {
                        if (isSelected()) {
                            if (selectedRed.size() >= 6) {
                                setSelected(false);
                                flashHint("红球最多只能选 6 个");
                                return;
                            }
                            selectedRed.add(number);
                        } else {
                            selectedRed.remove(number);
                        }
                    } else {
                        if (isSelected()) {
                            for (int i = 0; i < blueButtons.size(); i++) {
                                BallButton b = blueButtons.get(i);
                                if (b != BallButton.this) b.setSelected(false);
                            }
                            selectedBlue = number;
                        } else {
                            selectedBlue = null;
                        }
                    }
                    Sound.click();
                    updateHint();
                }
            });
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

        int getNumber() { return number; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            double cx = w / 2.0;
            double cy = h / 2.0;
            double r = isSelected() ? 17 : (hover ? 16 : 15);
            Ball ball = new Ball(red ? Ball.Color.RED : Ball.Color.BLUE, number);
            if (hover) BallPainter.paintGlow(g2, red ? UiUtil.RED_LIGHT : new Color(0x90, 0xCA, 0xF9), cx, cy, r + 7, 0.45f);
            if (isSelected()) BallPainter.paintGlow(g2, UiUtil.GOLD, cx, cy, r + 8, 0.8f);
            BallPainter.paint(g2, ball, cx, cy, r);
            if (isSelected()) {
                g2.setColor(UiUtil.GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(cx - r - 3, cy - r - 3, 2 * (r + 3), 2 * (r + 3)));
            }
            g2.dispose();
        }
    }
}
