package ru.stylemod.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import ru.stylemod.config.StyleConfig;
import ru.stylemod.style.StyleManager;

/**
 * Jump reset timer.
 *
 * Детект хита:
 *  1) onCombatHit() из mixin / StyleManager
 *  2) запасной: падение HP каждый тик
 *
 * Детект прыжка:
 *  - rising edge клавиши прыжка
 *  - уход с земли вверх
 *  - прыжок после приземления после KB
 *
 * Оффсет по пингу сдвигает шкалу, ширины зон фиксированы.
 */
public final class JumpResetTracker {
    public enum Grade {
        NONE("", 0xAAAAAA, 0),
        EARLY("EARLY", 0xFF5555, -25),
        PERFECT("PERFECT", 0xFFD700, 80),
        GOOD("GOOD", 0x55FF55, 40),
        LATE("LATE", 0xFFAA00, -15),
        MISSED("MISSED", 0xFF55FF, -35);

        public final String label;
        public final int color;
        public final int basePoints;

        Grade(String label, int color, int basePoints) {
            this.label = label;
            this.color = color;
            this.basePoints = basePoints;
        }
    }

    private static long hitTimeMs = 0;
    private static boolean waiting = false;
    private static boolean scored = false;
    private static boolean wasOnGround = true;
    private static boolean jumpKeyWasDown = false;
    private static boolean landedAfterHit = false;
    private static float lastHealth = -1f;

    private static Grade lastGrade = Grade.NONE;
    private static long lastGradeTimeMs = 0;
    private static int lastDeltaMs = 0;
    private static int lastOffsetMs = 0;

    private static final long DISPLAY_MS = 2000;

    // Ширины зон (фиксированные), шкала начинается с offset
    private static final double W_EARLY = 35;
    private static final double W_PERFECT = 100;
    private static final double W_GOOD = 160;
    private static final double W_LATE = 280;
    private static final double W_MISS = 400; // чуть длиннее, чтобы успеть на пинге

    private static double pingEma = 50;
    private static long lastPingSampleMs = 0;

    private JumpResetTracker() {}

    /** Вызов из mixin / onDamageTaken. */
    public static void onCombatHit(float damage) {
        if (!StyleConfig.get().jumpResetEnabled) return;
        if (damage < 0.1f) return;
        beginWait();
    }

    private static void beginWait() {
        samplePing();
        hitTimeMs = System.currentTimeMillis();
        waiting = true;
        scored = false;
        landedAfterHit = false;
    }

    public static void tick() {
        StyleConfig cfg = StyleConfig.get();
        if (!cfg.jumpResetEnabled) {
            waiting = false;
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            lastHealth = -1f;
            return;
        }

        if (System.currentTimeMillis() - lastPingSampleMs > 400) {
            samplePing();
        }

        // --- запасной детеккт урона по HP ---
        float hp = player.getHealth();
        if (lastHealth > 0f && hp < lastHealth - 0.15f && !player.isDeadOrDying()) {
            // не дублировать, если уже ждём свежий хит
            if (!waiting || System.currentTimeMillis() - hitTimeMs > 80) {
                beginWait();
            }
        }
        lastHealth = hp;

        boolean onGround = player.onGround();
        boolean jumpDown = isJumpDown(client, player);
        double offset = timerOffsetMs();
        double missAt = offset + W_MISS;

        if (waiting && !scored) {
            long delta = System.currentTimeMillis() - hitTimeMs;

            if (onGround) {
                landedAfterHit = true;
            }

            boolean jumpRising = jumpDown && !jumpKeyWasDown;
            double dy = player.getDeltaMovement().y;
            boolean leftGroundUp = wasOnGround && !onGround && dy > 0.03;
            // прыжок после приземления после KB
            boolean jumpAfterLand = landedAfterHit && leftGroundUp;
            boolean anyJump = jumpRising || leftGroundUp || jumpAfterLand;

            if (anyJump && delta < missAt + 50) {
                Grade g = gradeForDelta(delta, offset);
                applyGrade(g, (int) delta, (int) Math.round(offset));
            } else if (delta >= missAt) {
                applyGrade(Grade.MISSED, (int) delta, (int) Math.round(offset));
            }
        }

        wasOnGround = onGround;
        jumpKeyWasDown = jumpDown;
    }

    private static boolean isJumpDown(Minecraft client, LocalPlayer player) {
        try {
            if (client.options.keyJump.isDown()) return true;
        } catch (Throwable ignored) {
        }
        // 1.21 input fallback
        try {
            if (player.input != null && player.input.keyPresses.jump()) return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void samplePing() {
        lastPingSampleMs = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info == null) return;
        int latency = info.getLatency();
        if (latency < 0) latency = 0;
        if (latency > 1000) latency = 1000;
        pingEma = pingEma * 0.8 + latency * 0.2;
    }

    public static int getAveragePing() {
        return (int) Math.round(pingEma);
    }

    public static double timerOffsetMs() {
        if (!StyleConfig.get().jumpResetPingCompensation) return 0;
        double factor = StyleConfig.get().jumpResetPingFactor;
        double max = StyleConfig.get().jumpResetMaxShiftMs;
        double o = pingEma * factor;
        if (o < 0) o = 0;
        if (o > max) o = max;
        return o;
    }

    /** Совместимость со старым именем. */
    public static double getCompensationShift() {
        return timerOffsetMs();
    }

    private static Grade gradeForDelta(long deltaMs, double offset) {
        double local = deltaMs - offset;
        if (local < 0) return Grade.EARLY;
        if (local < W_EARLY) return Grade.EARLY;
        if (local <= W_PERFECT) return Grade.PERFECT;
        if (local <= W_GOOD) return Grade.GOOD;
        if (local <= W_LATE) return Grade.LATE;
        return Grade.MISSED;
    }

    private static void applyGrade(Grade grade, int rawDelta, int offsetMs) {
        if (scored) return;
        scored = true;
        waiting = false;
        lastGrade = grade;
        lastGradeTimeMs = System.currentTimeMillis();
        lastDeltaMs = rawDelta;
        lastOffsetMs = offsetMs;

        if (grade.basePoints != 0) {
            StyleManager.get().addStyle("JR " + grade.label, grade.basePoints);
        }
    }

    public static boolean isDisplaying() {
        return lastGrade != Grade.NONE
                && System.currentTimeMillis() - lastGradeTimeMs < DISPLAY_MS;
    }

    public static Grade getLastGrade() {
        return lastGrade;
    }

    public static int getLastDeltaMs() {
        return lastDeltaMs;
    }

    public static int getLastOffsetMs() {
        return lastOffsetMs;
    }

    public static int getLastEffectiveDeltaMs() {
        return Math.max(0, lastDeltaMs - lastOffsetMs);
    }

    public static float getDisplayAlpha() {
        long age = System.currentTimeMillis() - lastGradeTimeMs;
        float a = 1f - (age / (float) DISPLAY_MS);
        if (a < 0.2f) a = 0.2f;
        if (a > 1f) a = 1f;
        return a;
    }

    public static boolean isWaiting() {
        return waiting && !scored;
    }

    public static float getWaitProgress() {
        if (!waiting) return 0f;
        double missAt = timerOffsetMs() + W_MISS;
        long delta = System.currentTimeMillis() - hitTimeMs;
        float p = (float) (delta / missAt);
        if (p < 0f) p = 0f;
        if (p > 1f) p = 1f;
        return p;
    }

    /**
     * Зоны полоски: pre-offset, early end, perfect end, good end, late end, 1.
     */
    public static float[] getZoneFractions() {
        double o = timerOffsetMs();
        double total = o + W_MISS;
        if (total < 1) total = 1;
        return new float[] {
                (float) (o / total),
                (float) ((o + W_EARLY) / total),
                (float) ((o + W_PERFECT) / total),
                (float) ((o + W_GOOD) / total),
                (float) ((o + W_LATE) / total),
                1f
        };
    }
}
