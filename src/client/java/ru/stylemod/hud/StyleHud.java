package ru.stylemod.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.stylemod.combat.JumpResetTracker;
import ru.stylemod.config.StyleConfig;
import ru.stylemod.style.StyleBonus;
import ru.stylemod.style.StyleManager;
import ru.stylemod.style.StyleRank;

public final class StyleHud {
    private StyleHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        StyleManager style = StyleManager.get();
        StyleConfig cfg = StyleConfig.get();
        Font font = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        if (cfg.showKillBanner && style.isShowingKillScore()) {
            int pts = style.getLastKillStyle();
            String killText = pts > 0 ? ("+" + pts + " STYLE") : "KILL";
            int color = 0xFFFFD700;
            if (pts >= 2000) color = 0xFFFF55FF;
            else if (pts >= 1000) color = 0xFFFF5555;
            else if (pts >= 400) color = 0xFFFFAA00;

            Component killComp = Component.literal(killText).withStyle(ChatFormatting.BOLD);
            float scale = 2.0f;
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            int tw = font.width(killComp);
            int x = (int) ((screenWidth / scale - tw) / 2);
            int y = (int) ((screenHeight / scale) / 2 - 10);
            graphics.drawString(font, killComp, x, y, color, true);
            graphics.pose().popMatrix();
        }

        int score = style.getScore();
        int combo = style.getCombo();
        boolean hasStyle = score > 0 || combo > 0 || !style.getRecentBonuses().isEmpty();
        boolean jrWait = cfg.showJumpReset && JumpResetTracker.isWaiting();
        boolean jrShow = cfg.showJumpReset && JumpResetTracker.isDisplaying();
        boolean showJr = jrWait || jrShow;

        if (!hasStyle && !showJr) return;

        StyleRank rank = style.getRank();
        int rankColor = rank.color | 0xFF000000;

        int right = screenWidth - cfg.hudOffsetX;
        int y = cfg.hudOffsetY;

        if (cfg.showRank && hasStyle) {
            Component rankComp = Component.literal(rank.displayName).withStyle(ChatFormatting.BOLD);
            graphics.drawString(font, rankComp, right - font.width(rankComp), y, rankColor, true);
            y += 12;
        }

        if (cfg.showScore && hasStyle) {
            Component scoreComp = Component.literal(String.valueOf(score)).withStyle(ChatFormatting.BOLD);
            graphics.drawString(font, scoreComp, right - font.width(scoreComp), y, 0xFFFFFFFF, true);
            y += 11;
        }

        if (cfg.showMultiplier && hasStyle) {
            String multStr = String.format("x%.2f (s%.2f)", style.getCoefficient(), style.getSpeedCoefficient());
            graphics.drawString(font, multStr, right - font.width(multStr), y, 0xFFCCCCCC, true);
            y += 12;
        }

        if (cfg.showComboBar && hasStyle) {
            int barW = 80;
            int barH = 4;
            int barX = right - barW;
            float fill = combo / (float) StyleRank.cap();
            if (fill < 0f) fill = 0f;
            if (fill > 1f) fill = 1f;
            graphics.fill(barX, y, barX + barW, y + barH, 0x88000000);
            graphics.fill(barX, y, barX + (int) (barW * fill), y + barH, rankColor);
            y += 8;
        }

        if (showJr) {
            y = renderJumpReset(graphics, font, right, y);
        }

        if (cfg.showBonuses) {
            for (StyleBonus bonus : style.getRecentBonuses()) {
                long age = System.currentTimeMillis() - bonus.timestamp;
                float alpha = 1f - (age / 2500f);
                if (alpha < 0.15f) alpha = 0.15f;
                int a = ((int) (alpha * 255)) << 24;
                int rgb = bonusColor(bonus.name) & 0x00FFFFFF;
                int color = rgb | a;

                String pts = (bonus.scorePoints >= 0 ? "+" : "") + bonus.scorePoints + " ";
                MutableComponent line = Component.literal(pts)
                        .append(Component.literal(bonus.name).withStyle(ChatFormatting.BOLD));
                graphics.drawString(font, line, right - font.width(line), y, color, true);
                y += 10;
            }
        }
    }

    private static int renderJumpReset(GuiGraphics graphics, Font font, int right, int y) {
        int barW = 90;

        if (JumpResetTracker.isWaiting()) {
            float progress = JumpResetTracker.getWaitProgress();
            float[] z = JumpResetTracker.getZoneFractions();
            int barX = right - barW;
            int barY = y;

            graphics.fill(barX, barY, barX + barW, barY + 4, 0xAA000000);

            int x0 = barX;
            int x1 = barX + (int) (barW * z[0]);
            int x2 = barX + (int) (barW * z[1]);
            int x3 = barX + (int) (barW * z[2]);
            int x4 = barX + (int) (barW * z[3]);
            int x5 = barX + (int) (barW * z[4]);
            int x6 = barX + barW;

            if (x1 > x0) graphics.fill(x0, barY, x1, barY + 4, 0x66555555);
            if (x2 > x1) graphics.fill(x1, barY, x2, barY + 4, 0xCCFF5555);
            if (x3 > x2) graphics.fill(x2, barY, x3, barY + 4, 0xCCFFD700);
            if (x4 > x3) graphics.fill(x3, barY, x4, barY + 4, 0xCC55FF55);
            if (x6 > x4) graphics.fill(x4, barY, x6, barY + 4, 0xCCFFAA00);

            int cursor = barX + (int) (barW * progress);
            if (cursor < barX) cursor = barX;
            if (cursor > barX + barW) cursor = barX + barW;
            graphics.fill(cursor - 1, barY - 2, cursor + 1, barY + 6, 0xFFFFFFFF);

            y += 8;
            String hint = "JUMP RESET";
            graphics.drawString(font, hint, right - font.width(hint), y, 0xFFFFFFFF, true);
            y += 11;
        }

        if (JumpResetTracker.isDisplaying()) {
            JumpResetTracker.Grade g = JumpResetTracker.getLastGrade();
            float alpha = JumpResetTracker.getDisplayAlpha();
            int a = ((int) (alpha * 255)) << 24;
            int color = (g.color & 0x00FFFFFF) | a;

            String text = g.label + " " + JumpResetTracker.getLastDeltaMs() + "ms"
                    + "  off" + JumpResetTracker.getLastOffsetMs()
                    + "  p" + JumpResetTracker.getAveragePing();
            Component comp = Component.literal(text).withStyle(ChatFormatting.BOLD);
            graphics.drawString(font, comp, right - font.width(comp), y, color, true);
            y += 11;
        }

        return y;
    }

    private static int bonusColor(String name) {
        if (name == null) return 0xFFFFFF;
        if (name.startsWith("JR ")) {
            return switch (name.substring(3)) {
                case "PERFECT" -> 0xFFD700;
                case "GOOD" -> 0x55FF55;
                case "LATE" -> 0xFFAA00;
                case "EARLY" -> 0xFF5555;
                case "MISSED" -> 0xFF55FF;
                default -> 0xFFFFFF;
            };
        }
        return switch (name) {
            case "HIT" -> 0xC0C0C0;
            case "SPRINT HIT" -> 0x55FF55;
            case "MACE HIT" -> 0xFFAA00;
            case "CRITICAL" -> 0xFF5555;
            case "SHIELD BREAK" -> 0x55FFFF;
            case "RANGED" -> 0x5555FF;
            case "KILL" -> 0xFFD700;
            case "DAMAGED" -> 0xFF55FF;
            case "DEBUG" -> 0xFFFF55;
            default -> 0xFFFFFF;
        };
    }
}
