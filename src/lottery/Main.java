package lottery;

import lottery.ui.LotteryFrame;

import javax.swing.*;
import java.awt.*;

/** 程序入口 */
public class Main {
    public static void main(String[] args) {
        // 统一使用跨平台 LAF（Metal），保证各平台外观一致
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        // 暗色滚动条（尽力而为）
        UIManager.put("ScrollBar.thumb", new Color(0x3A, 0x55, 0x86));
        UIManager.put("ScrollBar.track", new Color(0x12, 0x1D, 0x38));
        UIManager.put("ScrollBar.background", new Color(0x12, 0x1D, 0x38));

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LotteryFrame frame = new LotteryFrame();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
