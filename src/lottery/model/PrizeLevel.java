package lottery.model;

/**
 * 双色球奖级及判定规则。
 * redHits = 红球命中个数，blueHit = 蓝球是否命中。
 * 本地模拟器采用固定奖金（一等奖 500 万、二等奖 30 万，三等奖及以下与真实规则一致）。
 */
public enum PrizeLevel {
    FIRST("一等奖", 5_000_000),
    SECOND("二等奖", 300_000),
    THIRD("三等奖", 3_000),
    FOURTH("四等奖", 200),
    FIFTH("五等奖", 10),
    SIXTH("六等奖", 5);

    private final String label;
    private final int amount;

    PrizeLevel(String label, int amount) {
        this.label = label;
        this.amount = amount;
    }

    public String getLabel() { return label; }

    public int getAmount() { return amount; }

    /** 根据命中情况判定奖级，未中奖返回 null */
    public static PrizeLevel judge(int redHits, boolean blueHit) {
        if (redHits == 6 && blueHit) return FIRST;
        if (redHits == 6) return SECOND;
        if (redHits == 5 && blueHit) return THIRD;
        if (redHits == 5 || (redHits == 4 && blueHit)) return FOURTH;
        if (redHits == 4 || (redHits == 3 && blueHit)) return FIFTH;
        if (blueHit) return SIXTH; // 红球命中 0~2 个
        return null;
    }
}
