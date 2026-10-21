package lottery.ui;

import java.awt.*;

/** 全局 UI 常量：暗色主题调色板 + 中文字体探测 */
public final class UiUtil {
    private UiUtil() {}

    public static final Color BG_DARK = new Color(0x0B, 0x14, 0x28);
    public static final Color PANEL = new Color(0x16, 0x23, 0x3F);
    public static final Color PANEL2 = new Color(0x1C, 0x2C, 0x4E);
    public static final Color BORDER = new Color(0x33, 0x50, 0x7F);
    public static final Color TEXT = new Color(0xE8, 0xEE, 0xF8);
    public static final Color TEXT_DIM = new Color(0x9F, 0xB0, 0xCE);
    public static final Color GOLD = new Color(0xFF, 0xD5, 0x4F);
    public static final Color GOLD_DARK = new Color(0xFF, 0xA0, 0x00);
    public static final Color RED = new Color(0xD3, 0x2F, 0x2F);
    public static final Color RED_LIGHT = new Color(0xFF, 0x8A, 0x80);
    public static final Color BLUE = new Color(0x2F, 0x6F, 0xED);
    public static final Color BLUE_DARK = new Color(0x24, 0x57, 0xB8);
    public static final Color GREEN = new Color(0x69, 0xF0, 0xAE);
    public static final Color TEAL = new Color(0x00, 0x89, 0x7B);
    public static final Color GRAY = new Color(0x4A, 0x55, 0x68);
    public static final Color PURPLE = new Color(0x5C, 0x6B, 0xC0);
    public static final Color SILVER = new Color(0xCF, 0xD8, 0xE8);

    private static final String FONT_NAME = detectFont();

    private static String detectFont() {
        try {
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                if (name.contains("YaHei") || name.contains("雅黑")) return "Microsoft YaHei";
            }
        } catch (Exception ignored) {
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FONT_NAME, style, Math.round(size));
    }
}
