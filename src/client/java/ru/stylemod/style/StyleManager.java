package ru.stylemod.style;

import ru.stylemod.config.StyleConfig;
import ru.stylemod.combat.JumpResetTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * combo  — падающий метр (ранг).
 * score  — постоянный стиль.
 *
 * Итоговый множитель очков: comboCoeff * speedCoeff
 * Урон: −75 * speedCoeff (по score и combo).
 * Смерть: полный сброс.
 */
public class StyleManager {
    private static final StyleManager INSTANCE = new StyleManager();

    public static StyleManager get() {
        return INSTANCE;
    }

    private static final float COMBO_COEFF_MIN = 1.0f;
    private static final float COMBO_COEFF_MAX = 4.0f;
    private static final float SPEED_COEFF_MAX = 2.0f;

    private static final float BASE_DECAY_PER_TICK = 15.0f / 20.0f;
    private static final long BONUS_LIFETIME_MS = 2500;
    private static final long KILL_DISPLAY_MS = 2200;

    private static final int DAMAGE_PENALTY = 75;

    private float combo = 0f;
    private int score = 0;
    private StyleRank rank = StyleRank.NONE;

    /** Подряд sprint-ударов без получения урона. */
    private int sprintStreak = 0;

    private final StyleActionRegistry registry = new StyleActionRegistry();
    private final List<StyleBonus> recentBonuses = new ArrayList<>();

    private int lastKillStyle = 0;
    private long lastKillTimestamp = 0;

    private StyleManager() {}

    public StyleActionRegistry getRegistry() {
        return registry;
    }

    public int getSprintStreak() {
        return sprintStreak;
    }

    public void onSprintHit() {
        sprintStreak++;
    }

    public void resetSprintStreak() {
        sprintStreak = 0;
    }

    /** Коэффициент от комбо-метра (1.0 → 4.0). */
    public float getComboCoefficient() {
        float t = combo / (float) StyleRank.cap();
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return COMBO_COEFF_MIN + t * (COMBO_COEFF_MAX - COMBO_COEFF_MIN);
    }

    /** Коэффициент скорости горизонтального движения (1.0 → 2.0). */
    public float getSpeedCoefficient() {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return 1.0f;

        Vec3 v = player.getDeltaMovement();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        // ~0 при стоянии, ~0.28 спринт — линейно до SPEED_COEFF_MAX
        float coeff = 1.0f + (float) (horizontal * 2.8);
        if (coeff > SPEED_COEFF_MAX) coeff = SPEED_COEFF_MAX;
        if (coeff < 1.0f) coeff = 1.0f;
        return coeff;
    }

    /** Общий множитель для начисления очков. */
    public float getCoefficient() {
        return getComboCoefficient() * getSpeedCoefficient();
    }

    public void trigger(String actionId) {
        StyleAction action = registry.get(actionId);
        if (action == null) return;
        addStyle(action.getDisplayName(), action.getPoints());
    }

    public void addStyle(String displayName, int points, boolean ignored) {
        addStyle(displayName, points);
    }

    public void addStyle(String displayName, int points) {
        if (points == 0) return;

        float coeff = getCoefficient();
        int awarded = Math.round(points * coeff);
        if (points > 0 && awarded < 1) awarded = 1;
        if (points < 0 && awarded > -1) awarded = -1;

        if (awarded > 0) {
            score += awarded;
            combo = Math.min(StyleRank.cap(), combo + Math.abs(points));
        } else {
            score = Math.max(0, score + awarded);
            combo = Math.max(0f, combo + awarded); // awarded negative
        }
        updateRank();

        recentBonuses.add(0, new StyleBonus(displayName, points, awarded, coeff));
        while (recentBonuses.size() > 6) {
            recentBonuses.remove(recentBonuses.size() - 1);
        }
    }

    /**
     * Получение урона: −75 * speedCoeff по score и combo, сброс sprint-streak, падение ранга.
     */
    public void onDamageTaken(float damage) {
        JumpResetTracker.onCombatHit(damage);
        if (damage <= 0.01f) return;

        resetSprintStreak();

        float speed = getSpeedCoefficient();
        int penalty = Math.max(1, Math.round(DAMAGE_PENALTY * speed));

        score = Math.max(0, score - penalty);
        combo = Math.max(0f, combo - penalty);

        StyleRank dropped = rank.previous();
        if (dropped != rank && combo > dropped.threshold) {
            combo = dropped.threshold;
        }
        updateRank();

        recentBonuses.add(0, new StyleBonus("DAMAGED", -DAMAGE_PENALTY, -penalty, speed));
        while (recentBonuses.size() > 6) {
            recentBonuses.remove(recentBonuses.size() - 1);
        }
    }

    /** Своя смерть — тихий сброс (если включено в конфиге). */
    public void onDeath() {
        if (!StyleConfig.get().resetOnDeath) return;
        resetSprintStreak();
        reset();
        lastKillStyle = 0;
        lastKillTimestamp = 0;
    }

    /** Килл врага: показать score; сброс — если resetOnKill. */
    public void onKill() {
        if (score > 0 || combo > 0) {
            lastKillStyle = score;
            lastKillTimestamp = System.currentTimeMillis();
        }
        resetSprintStreak();
        if (StyleConfig.get().resetOnKill) {
            reset();
        }
    }

    public void reset() {
        combo = 0f;
        score = 0;
        rank = StyleRank.NONE;
        recentBonuses.clear();
    }

    public int getCombo() {
        return Math.round(combo);
    }

    public float getComboExact() {
        return combo;
    }

    public int getScore() {
        return score;
    }

    public int getTotalStyle() {
        return score;
    }

    public int getCurrentStyle() {
        return getCombo();
    }

    public int getLockedStyle() {
        return score;
    }

    public StyleRank getRank() {
        return rank;
    }

    public List<StyleBonus> getRecentBonuses() {
        return recentBonuses;
    }

    public boolean isShowingKillScore() {
        return System.currentTimeMillis() - lastKillTimestamp < KILL_DISPLAY_MS;
    }

    public int getLastKillStyle() {
        return lastKillStyle;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        recentBonuses.removeIf(b -> b.isExpired(now, BONUS_LIFETIME_MS));

        if (combo > 0f) {
            float decay = BASE_DECAY_PER_TICK * rank.decayMultiplier();
            combo = Math.max(0f, combo - decay);
            updateRank();
        }
    }

    private void updateRank() {
        rank = StyleRank.fromPoints(Math.round(combo));
    }
}
