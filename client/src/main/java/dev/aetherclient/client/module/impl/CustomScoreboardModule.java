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
    public final ModeSetting position = addSetting(new ModeSetting("position", "Сторона экрана", "Правый", "Правый", "Левый", "По центру").withEn("Screen Side"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Тема", "Тема", "Статика", "Радуга", "Градиент", "Киберпанк", "Стекло").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Акцентный цвет", 0xFF7C5CFF).withEn("Accent Color"));
    public final ColorSetting textColor = addSetting(new ColorSetting("text_col", "Цвет текста", 0xFFFFFFFF).withEn("Text Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность стекла", 0.62, 0.15, 0.95, 0.02).withEn("Glass Opacity"));
    public final NumberSetting scale = addSetting(new NumberSetting("scale", "Масштаб скорборда", 1.0, 0.6, 1.6, 0.05).withEn("Scoreboard Scale"));
    public final NumberSetting offsetX = addSetting(new NumberSetting("offset_x", "Смещение по X", 0.0, -250.0, 250.0, 5.0).withEn("Offset X"));
    public final NumberSetting offsetY = addSetting(new NumberSetting("offset_y", "Смещение по Y", 0.0, -250.0, 250.0, 5.0).withEn("Offset Y"));
    public final BooleanSetting showTitle = addSetting(new BooleanSetting("show_title", "Показывать заголовок", true).withEn("Show Title"));
    public final BooleanSetting redScores = addSetting(new BooleanSetting("red_scores", "Красные очки справа", true).withEn("Red Scores"));
    public final BooleanSetting roundedCorners = addSetting(new BooleanSetting("rounded", "Скругленные углы", true).withEn("Rounded Corners"));
    public final NumberSetting cornerRadius = addSetting(new NumberSetting("radius", "Радиус скругления", 8.0, 2.0, 18.0, 1.0).withEn("Corner Radius"));
    public final BooleanSetting blur = addSetting(new BooleanSetting("blur", "Размытие фона (Kawase)", true).withEn("Blur Background"));
    public final BooleanSetting hideServerIp = addSetting(new BooleanSetting("hide_ip", "Скрывать IP сервера (Стример)", false).withEn("Hide Server IP"));

    public CustomScoreboardModule() {
        super("scoreboard", "Скорборд", "Стилизованный скорборд в стиле Material 3 Liquid Glass со скруглением и размытием", Category.HUD);
        setEn("Scoreboard", "Material 3 styled scoreboard with custom scale, offsets, blur and streamer mode");
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
                if (hideServerIp.isEnabled() && (name.contains(".ru") || name.contains(".net") || name.contains(".com") || name.contains("mc.") || name.contains("play."))) {
                    name = "AetherClient.dev";
                }
                String score = Integer.toString(entry.value());
                rows.add(new String[]{name, score});
            }

            rows.sort(Comparator.comparingInt((String[] a) -> {
                try {
                    return Integer.parseInt(a[1]);
                } catch (Exception e) {
                    return 0;
                }
            }).reversed());

            if (rows.size() > 15) {
                return rows.subList(0, 15);
            }
            return rows;
        } catch (Throwable ignored) {
            return rows;
        }
    }
}
