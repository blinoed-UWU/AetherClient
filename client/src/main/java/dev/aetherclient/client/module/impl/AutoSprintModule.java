package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class AutoSprintModule extends Module {
    public final ModeSetting mode = addSetting(
            new ModeSetting("mode", "Режим спринта", "Всенаправленный",
                    "Всенаправленный", "Обычный (Вперед)", "Легитный (Сытость)")
                    .withEn("Sprint Mode")
    );
    public final BooleanSetting keepSprint = addSetting(new BooleanSetting("keep_sprint", "Не сбивать бег при ударах", true).withEn("Keep Sprint"));
    public final BooleanSetting sprintInWater = addSetting(new BooleanSetting("sprint_water", "Спринт в воде / плавание", true).withEn("Sprint In Water"));
    public final BooleanSetting sprintWhileSneaking = addSetting(new BooleanSetting("sprint_sneak", "Спринт при приседании", false).withEn("Sprint While Sneaking"));
    public final BooleanSetting sprintParticles = addSetting(new BooleanSetting("particles", "Частицы ветра под ногами", true).withEn("Wind Particles"));
    public final ColorSetting particleColor = addSetting(new ColorSetting("color", "Цвет частиц ветра", 0xFFE0E7FF).withEn("Particle Color"));

    public AutoSprintModule() {
        super("auto_sprint", "Авто-спринт", "Автоматический спринт во всех направлениях без сбивания при атаках и с частицами", Category.COMBAT);
        setEn("Auto Sprint", "Omni-directional automatic sprint keeping speed on hit with wind particles");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null || mc.options == null) return;
        boolean moving;
        if ("Всенаправленный".equals(mode.getValue())) {
            moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        } else {
            moving = mc.options.forwardKey.isPressed();
        }

        if ("Легитный (Сытость)".equals(mode.getValue()) && mc.player.getHungerManager().getFoodLevel() <= 6) {
            return;
        }

        boolean canSprint = !mc.player.horizontalCollision && (sprintWhileSneaking.isEnabled() || !mc.player.isSneaking());
        if (mc.player.isTouchingWater() && !sprintInWater.isEnabled()) {
            canSprint = false;
        }

        if (moving && canSprint) {
            mc.player.setSprinting(true);
        } else if (!keepSprint.isEnabled()) {
            mc.player.setSprinting(false);
        }
    }
}
