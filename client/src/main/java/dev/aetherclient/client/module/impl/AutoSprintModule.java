package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

public final class AutoSprintModule extends Module {
    public final BooleanSetting keepSprint = addSetting(new BooleanSetting("keep_sprint", "Не сбивать бег", true).withEn("Keep Sprint"));

    public AutoSprintModule() {
        super("auto_sprint", "Авто-спринт", "Автоматически бежит вперёд и не сбивает бег", Category.COMBAT);
        setEn("Auto Sprint", "Automatically sprints forward and keeps sprinting");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null || mc.options == null) return;
        boolean forward = mc.options.forwardKey.isPressed();
        if (forward && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        } else if (!keepSprint.isEnabled()) {
            mc.player.setSprinting(false);
        }
    }
}
