package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HitSoundModule extends Module {
    public final ModeSetting sound = addSetting(
            new ModeSetting("sound", "Звук хита", "Skeet", "Skeet", "Neverlose", "Bubble", "Bell", "Metallic", "Minecraft", "Pop", "Cod")
                    .withEn("Hit Sound")
    );
    public final NumberSetting volume = addSetting(new NumberSetting("volume", "Громкость", 0.85, 0.1, 1.5, 0.05).withEn("Volume"));
    public final NumberSetting pitch = addSetting(new NumberSetting("pitch", "Высота (Pitch)", 1.0, 0.5, 2.0, 0.05).withEn("Pitch"));

    public HitSoundModule() {
        super("hit_sound", "Звук удара", "Кастомный щелчок или колокольчик при попадании по сущности", Category.COMBAT);
        setEn("Hit Sound", "Custom audio feedback upon landing attacks");
    }
}
