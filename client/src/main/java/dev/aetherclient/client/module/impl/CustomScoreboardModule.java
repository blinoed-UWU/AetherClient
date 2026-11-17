package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class CustomScoreboardModule extends Module {
    public final ModeSetting position = addSetting(new ModeSetting("position", "Сторона", "Правый", "Правый", "Левый").withEn("Side"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Тема", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF7C5CFF).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность стекла", 0.62, 0.25, 0.92, 0.02).withEn("Glass Opacity"));
    public final BooleanSetting showTitle = addSetting(new BooleanSetting("show_title", "Заголовок", true).withEn("Show Title"));
    public final BooleanSetting redScores = addSetting(new BooleanSetting("red_scores", "Красные очки", true).withEn("Red Scores"));

    public CustomScoreboardModule() {
        super("scoreboard", "Скорборд", "Стилизованный скорборд в духе Material 3", Category.HUD);
        setEn("Scoreboard", "Material 3 styled scoreboard");
    }

    public String readTitle(MinecraftClient mc) {
        if (mc == null || mc.world == null) return null;
        try {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (obj == null) return null;
            return obj.getDisplayName().getString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public List<String[]> readRows(MinecraftClient mc) {
        List<String[]> rows = new ArrayList<>();
        if (mc == null || mc.world == null) return rows;
        try {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (obj == null) return rows;
            Collection<ScoreboardEntry> entries = sb.getScoreboardEntries(obj);
            if (entries == null || entries.isEmpty()) return rows;

            for (ScoreboardEntry entry : entries) {
                if (entry.hidden()) continue;
                Team team = sb.getScoreHolderTeam(entry.owner());
                String name = Team.decorateName(team, entry.name()).getString();
                rows.add(new String[]{name, Integer.toString(entry.value())});
            }
            rows.sort(Comparator.comparingInt(r -> -Integer.parseInt(r[1])));
            if (rows.size() > 15) {
                return new ArrayList<>(rows.subList(0, 15));
            }
        } catch (Throwable ignored) {
        }
        return rows;
    }
}
