package lottery.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一注双色球彩票：6 个红球（升序）+ 1 个蓝球，每注 2 元 */
public final class Ticket {
    public static final int PRICE = 2;
    public static final int RED_COUNT = 6;

    private final List<Integer> redBalls; // 升序
    private final int blueBall;

    // 开奖后填充
    private int redHits;
    private boolean blueHit;
    private boolean evaluated;
    private PrizeLevel prize;

    public Ticket(List<Integer> redBalls, int blueBall) {
        List<Integer> copy = new ArrayList<>(redBalls);
        Collections.sort(copy);
        this.redBalls = Collections.unmodifiableList(copy);
        this.blueBall = blueBall;
    }

    public List<Integer> getRedBalls() { return redBalls; }

    public int getBlueBall() { return blueBall; }

    /** 对照开奖结果计算命中情况 */
    public void evaluate(DrawResult result) {
        redHits = 0;
        for (int n : redBalls) {
            if (result.getRedBalls().contains(n)) redHits++;
        }
        blueHit = (blueBall == result.getBlueBall());
        prize = PrizeLevel.judge(redHits, blueHit);
        evaluated = true;
    }

    public int getRedHits() { return redHits; }

    public boolean isBlueHit() { return blueHit; }

    /** 是否已开奖比对过（区分"待开奖"与"未中奖"） */
    public boolean isEvaluated() { return evaluated; }

    public PrizeLevel getPrize() { return prize; }

    public int getPrizeAmount() { return prize == null ? 0 : prize.getAmount(); }
}
