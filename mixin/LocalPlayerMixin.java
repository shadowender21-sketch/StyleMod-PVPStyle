package ru.stylemod.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.stylemod.combat.JumpResetTracker;
import ru.stylemod.style.StyleManager;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "hurtTo", at = @At("HEAD"))
    private void onHurtTo(float health, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        float oldHealth = self.getHealth();
        float damage = oldHealth - health;

        if (health <= 0f && oldHealth > 0f) {
            StyleManager.get().onDeath();
            return;
        }

        if (damage > 0.01f) {
            StyleManager.get().onDamageTaken(damage);
            JumpResetTracker.onCombatHit(damage);
        }
    }
}
