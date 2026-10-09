package ru.stylemod.combat;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import ru.stylemod.config.StyleConfig;
import ru.stylemod.style.StyleManager;

import java.util.UUID;

/**
 * Очки за мили-удары / щит / килл.
 * (Лук/арбалет на клиенте без надёжного API — пока отключены, чтобы не ломать сборку.)
 *
 * HIT +25
 * SPRINT HIT +100  (2+ спринт-удара подряд без урона)
 * MACE HIT +300    (×1…2 от fallDistance)
 * CRITICAL +50
 * SHIELD BREAK +100
 */
public final class CombatStyleHook {
    private static UUID lastTargetId;
    private static long lastHitMs;
    private static boolean killCounted;
    private static boolean deathHandled;
    private static GameType lastGameMode;

    private CombatStyleHook() {}

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || player != client.player) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof LivingEntity target)) {
                return InteractionResult.PASS;
            }
            if (target.getHealth() <= 0.0f) {
                return InteractionResult.PASS;
            }
            if (player.getAttackStrengthScale(0.5f) < 0.85f) {
                return InteractionResult.PASS;
            }

            lastTargetId = target.getUUID();
            lastHitMs = System.currentTimeMillis();
            killCounted = false;

            awardMeleeHit(player, target);
            return InteractionResult.PASS;
        });
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // Смена режима игры
        if (client.gameMode != null) {
            GameType mode = client.gameMode.getPlayerMode();
            if (lastGameMode != null && mode != lastGameMode
                    && StyleConfig.get().resetOnGameModeChange) {
                StyleManager.get().resetSprintStreak();
                StyleManager.get().reset();
            }
            lastGameMode = mode;
        }

        if (client.player.isDeadOrDying() || client.player.getHealth() <= 0.0f) {
            if (!deathHandled) {
                deathHandled = true;
                StyleManager.get().onDeath();
                cleanup();
            }
            return;
        }
        deathHandled = false;

        checkKill(client);
    }

    private static void cleanup() {
        lastTargetId = null;
        killCounted = false;
        StyleManager.get().resetSprintStreak();
    }

    private static void awardMeleeHit(Player player, LivingEntity target) {
        StyleManager sm = StyleManager.get();
        ItemStack main = player.getMainHandItem();

        // 1) Базовый удар
        sm.trigger("hit");

        // 2) Sprint-цепочка
        if (player.isSprinting()) {
            sm.onSprintHit();
            if (sm.getSprintStreak() > 1) {
                sm.trigger("sprint_hit");
            }
        } else {
            sm.resetSprintStreak();
        }

        // 3) Булава
        if (main.is(Items.MACE)) {
            float fall = (float) player.fallDistance;
            float maceMult = 1.0f + Math.min(1.0f, fall / 8.0f);
            int macePts = Math.round(300.0f * maceMult);
            sm.addStyle("MACE HIT", macePts);
        }

        // 4) Крит
        if (isCritical(player)) {
            sm.trigger("critical_hit");
        }

        // 5) Щит
        if (target.isBlocking() && canDisableShield(main)) {
            sm.trigger("shield_break");
        }
    }

    private static boolean canDisableShield(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof AxeItem) {
            return true;
        }
        return stack.is(Items.MACE);
    }

    private static boolean isCritical(Player player) {
        return player.fallDistance > 0.0f
                && !player.onGround()
                && !player.isInWater()
                && !player.onClimbable()
                && !player.isPassenger()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.hasEffect(MobEffects.SLOW_FALLING)
                && !player.isSprinting();
    }

    private static void checkKill(Minecraft client) {
        if (lastTargetId == null || killCounted) return;
        if (System.currentTimeMillis() - lastHitMs > 2500L) return;

        Entity e = findEntity(client, lastTargetId);
        if (e == null) {
            if (System.currentTimeMillis() - lastHitMs < 1500L) {
                doKill();
            }
            return;
        }
        if (e instanceof Player living && (living.isDeadOrDying() || living.getHealth() <= 0.0f)) {
            doKill();
        }
    }

    private static void doKill() {
        if (killCounted) return;
        killCounted = true;
        StyleManager sm = StyleManager.get();
        sm.trigger("kill");
        sm.onKill();
        lastTargetId = null;
    }

    private static Entity findEntity(Minecraft client, UUID id) {
        if (client.level == null) return null;
        for (Entity e : client.level.entitiesForRendering()) {
            if (id.equals(e.getUUID())) {
                return e;
            }
        }
        return null;
    }
}
