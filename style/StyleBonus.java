package ru.stylemod.style;

/**
 * Один бонус, который только что дали.
 * Показывается в HUD несколько секунд.
 */
public class StyleBonus {
    public final String name;
    public final int basePoints;   // сколько ушло в комбо (без коэфф.)
    public final int scorePoints;  // сколько ушло в постоянный стиль (с коэфф.)
    public final float coefficient;
    public final long timestamp;

    public StyleBonus(String name, int basePoints, int scorePoints, float coefficient) {
        this.name = name;
        this.basePoints = basePoints;
        this.scorePoints = scorePoints;
        this.coefficient = coefficient;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired(long now, long lifetimeMs) {
        return now - timestamp > lifetimeMs;
    }
}
