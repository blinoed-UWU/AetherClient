package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ViewmodelModule extends Module {
    public final NumberSetting posX = addSetting(new NumberSetting("pos_x", "Смещение X", 0.0, -1.2, 1.2, 0.05).withEn("Offset X"));
    public final NumberSetting posY = addSetting(new NumberSetting("pos_y", "Смещение Y", 0.0, -1.2, 1.2, 0.05).withEn("Offset Y"));
    public final NumberSetting posZ = addSetting(new NumberSetting("pos_z", "Смещение Z", 0.0, -1.2, 1.2, 0.05).withEn("Offset Z"));

    public final NumberSetting rotPitch = addSetting(new NumberSetting("rot_pitch", "Наклон (Pitch)", 0.0, -90.0, 90.0, 2.0).withEn("Pitch"));
    public final NumberSetting rotYaw = addSetting(new NumberSetting("rot_yaw", "Поворот (Yaw)", 0.0, -90.0, 90.0, 2.0).withEn("Yaw"));
    public final NumberSetting rotRoll = addSetting(new NumberSetting("rot_roll", "Крен (Roll)", 0.0, -90.0, 90.0, 2.0).withEn("Roll"));

    public final NumberSetting scale = addSetting(new NumberSetting("scale", "Размер", 1.0, 0.3, 2.0, 0.05).withEn("Item Scale"));
    public final BooleanSetting mirrorHands = addSetting(new BooleanSetting("mirror_hands", "Зеркалить левую руку", true).withEn("Mirror Left Hand"));
    public final BooleanSetting mainHandOnly = addSetting(new BooleanSetting("main_hand", "Только правая рука", false).withEn("Main Hand Only"));
    public final BooleanSetting separateHands = addSetting(new BooleanSetting("separate_hands", "Раздельная настройка рук", false).withEn("Separate Hands"));

    public final NumberSetting offhandX = addSetting(new NumberSetting("off_x", "Левая рука X", 0.0, -1.2, 1.2, 0.05).withEn("Offhand X"));
    public final NumberSetting offhandY = addSetting(new NumberSetting("off_y", "Левая рука Y", 0.0, -1.2, 1.2, 0.05).withEn("Offhand Y"));
    public final NumberSetting offhandZ = addSetting(new NumberSetting("off_z", "Левая рука Z", 0.0, -1.2, 1.2, 0.05).withEn("Offhand Z"));
    public final NumberSetting offhandScale = addSetting(new NumberSetting("off_scale", "Размер левой руки", 1.0, 0.3, 2.0, 0.05).withEn("Offhand Scale"));
    public final NumberSetting swingMultiplier = addSetting(new NumberSetting("swing_mult", "Множитель взмаха", 1.0, 0.2, 2.0, 0.05).withEn("Swing Multiplier"));

    public ViewmodelModule() {
        super("viewmodel", "Позиция рук", "Кастомизация позиции, угла и масштаба рук и предметов от первого лица с отзеркаливанием и раздельной настройкой", Category.VISUAL);
        setEn("Viewmodel", "Custom 1st-person hand position, rotation and item scale with mirrored and separate hand controls");
    }
}
