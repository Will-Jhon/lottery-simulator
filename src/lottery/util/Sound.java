package lottery.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 极简音效合成器（正弦波 + 包络，零依赖）。
 * 出球"啵"声、滑出"嗖"声、开奖轰鸣、中奖小旋律。
 * 无音频设备时静默降级，不影响主程序。
 */
public final class Sound {
    private static final float SAMPLE_RATE = 44100f;
    private static volatile boolean muted = false;

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "lottery-sound");
            t.setDaemon(true);
            return t;
        }
    });

    private Sound() {}

    public static boolean isMuted() { return muted; }

    public static void setMuted(boolean m) { muted = m; }

    /** 按钮点击 */
    public static void click() { tone(520, 45, 0.30); }

    /** 球落入展示槽 */
    public static void pop() { tone(760, 90, 0.55); }

    /** 球被吸出出球口 */
    public static void whoosh() { sweep(950, 170, 230, 0.32); }

    /** 开奖前搅拌轰鸣 */
    public static void rumble() { sweep(130, 55, 1100, 0.28); }

    /** 中奖小旋律 */
    public static void fanfare() {
        if (muted) return;
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                int[] notes = { 523, 659, 784, 1047, 784, 1047 };
                int[] lens = { 150, 150, 150, 320, 150, 480 };
                for (int i = 0; i < notes.length; i++) {
                    playTone(notes[i], notes[i], lens[i], 0.50);
                }
            }
        });
    }

    public static void tone(double freq, int ms, double amplitude) {
        if (muted) return;
        EXEC.execute(new PlayToneTask(freq, freq, ms, amplitude));
    }

    public static void sweep(double f0, double f1, int ms, double amplitude) {
        if (muted) return;
        EXEC.execute(new PlayToneTask(f0, f1, ms, amplitude));
    }

    private static final class PlayToneTask implements Runnable {
        private final double f0, f1, amp;
        private final int ms;

        PlayToneTask(double f0, double f1, int ms, double amp) {
            this.f0 = f0;
            this.f1 = f1;
            this.ms = ms;
            this.amp = amp;
        }

        @Override
        public void run() { playTone(f0, f1, ms, amp); }
    }

    private static void playTone(double f0, double f1, int ms, double amp) {
        SourceDataLine line = null;
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, 4096);
            line.start();
            int n = (int) (SAMPLE_RATE * ms / 1000f);
            if (n < 1) n = 1;
            byte[] buf = new byte[n];
            double phase = 0;
            double duration = ms / 1000.0;
            for (int i = 0; i < n; i++) {
                double t = i / (double) SAMPLE_RATE;
                double f = f0 + (f1 - f0) * (t / duration);
                phase += 2 * Math.PI * f / SAMPLE_RATE;
                buf[i] = (byte) (Math.sin(phase) * 100 * amp * envelope(i, n));
            }
            line.write(buf, 0, n);
            line.drain();
        } catch (Exception ignored) {
            // 无音频设备时静默失败
        } finally {
            if (line != null) line.close();
        }
    }

    /** 包络：快速起振 + 指数衰减，避免爆音 */
    private static double envelope(int i, int n) {
        double attack = Math.min(1.0, i / 180.0);
        double decay = Math.exp(-3.2 * i / n);
        return attack * decay;
    }
}
