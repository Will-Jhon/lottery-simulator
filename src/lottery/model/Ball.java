package lottery.model;

import java.util.Objects;

/** 一个彩票球：号码 + 颜色（红/蓝） */
public final class Ball {
    public enum Color { RED, BLUE }

    private final Color color;
    private final int number;

    public Ball(Color color, int number) {
        this.color = color;
        this.number = number;
    }

    public Color getColor() { return color; }

    public int getNumber() { return number; }

    public boolean isRed() { return color == Color.RED; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ball)) return false;
        Ball other = (Ball) o;
        return number == other.number && color == other.color;
    }

    @Override
    public int hashCode() { return Objects.hash(color, number); }

    @Override
    public String toString() { return (isRed() ? "红" : "蓝") + number; }
}
