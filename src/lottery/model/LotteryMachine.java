package lottery.model;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 摇奖机：随机产生开奖号码 / 机选彩票 */
public final class LotteryMachine {
    private static final int RED_POOL = 33;
    private static final int BLUE_POOL = 16;

    private final SecureRandom random = new SecureRandom();

    /** 开一期奖；drawOrder 为随机出球顺序（电视开奖顺序） */
    public DrawResult draw(int period) {
        List<Integer> reds = sampleReds();
        int blue = random.nextInt(BLUE_POOL) + 1;
        return new DrawResult(period, reds, blue);
    }

    /** 机选一注彩票 */
    public Ticket quickPick() {
        List<Integer> reds = sampleReds();
        int blue = random.nextInt(BLUE_POOL) + 1;
        return new Ticket(reds, blue);
    }

    private List<Integer> sampleReds() {
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= RED_POOL; i++) pool.add(i);
        Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, Ticket.RED_COUNT));
    }
}
