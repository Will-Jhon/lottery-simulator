package lottery.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** 一期开奖结果 */
public final class DrawResult {
    private final int period;              // 期号
    private final List<Integer> redBalls;  // 升序（用于比对）
    private final List<Integer> drawOrder; // 出球顺序（用于动画展示，电视上为非排序顺序）
    private final int blueBall;
    private final Date drawTime;

    public DrawResult(int period, List<Integer> drawOrder, int blueBall) {
        this.period = period;
        List<Integer> sorted = new ArrayList<>(drawOrder);
        Collections.sort(sorted);
        this.redBalls = Collections.unmodifiableList(sorted);
        this.drawOrder = Collections.unmodifiableList(new ArrayList<>(drawOrder));
        this.blueBall = blueBall;
        this.drawTime = new Date();
    }

    public int getPeriod() { return period; }

    public List<Integer> getRedBalls() { return redBalls; }

    /** 出球顺序（未排序，模拟电视开奖顺序） */
    public List<Integer> getDrawOrder() { return drawOrder; }

    public int getBlueBall() { return blueBall; }

    public Date getDrawTime() { return drawTime; }

    /** 展示用：03 07 11 18 25 29  +  09 */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < redBalls.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02d", redBalls.get(i)));
        }
        sb.append("  +  ").append(String.format("%02d", blueBall));
        return sb.toString();
    }
}
