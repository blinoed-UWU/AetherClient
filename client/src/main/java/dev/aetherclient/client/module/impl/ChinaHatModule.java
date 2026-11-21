package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ChinaHatModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Форма шляпы", "Классический конус",
                    "Классический конус", "Шляпа ведьмы", "Нимб / Ореол", "Многоярусный", "Зонтик")
                    .withEn("Hat Shape")
    );
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус шляпы", 0.65, 0.3, 1.6, 0.05).withEn("Hat Radius"));
    public final NumberSetting height = addSetting(new NumberSetting("height", "Высота конуса", 0.28, 0.05, 0.8, 0.02).withEn("Cone Height"));
    public final NumberSetting offsetY = addSetting(new NumberSetting("offset_y", "Высота над головой", 0.0, -0.3, 0.8, 0.05).withEn("Vertical Offset"));
    public final NumberSetting spinSpeed = addSetting(new NumberSetting("spin_speed", "Скорость вращения", 1.0, 0.0, 4.0, 0.1).withEn("Spin Speed"));
    public final BooleanSetting bobbing = addSetting(new BooleanSetting("bobbing", "Покачивание при ходьбе", true).withEn("Walking Bobbing"));
    public final NumberSetting bobHeight = addSetting(new NumberSetting("bob_height", "Амплитуда покачивания", 0.08, 0.02, 0.2, 0.01).withEn("Bob Amplitude"));
    public final BooleanSetting layers = addSetting(new BooleanSetting("layers", "3D Объём (слои)", true).withEn("Double Layers"));
    public final NumberSetting layerCount = addSetting(new NumberSetting("layer_count", "Количество слоёв", 2.0, 1.0, 4.0, 1.0).withEn("Layer Count"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет обода", 0xCC7C5CFF).withEn("Rim Color"));
    public final ColorSetting colorTip = addSetting(new ColorSetting("color_tip", "Цвет вершины", 0xEE38BDF8).withEn("Tip Color"));
    public final BooleanSetting rainbow = addSetting(new BooleanSetting("rainbow", "Радужный режим", false).withEn("Rainbow"));
    public final NumberSetting rainbowSpeed = addSetting(new NumberSetting("rainbow_speed", "Скорость радуги", 1.0, 0.2, 4.0, 0.1).withEn("Rainbow Speed"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновое свечение", true).withEn("Glow"));
    public final NumberSetting segments = addSetting(new NumberSetting("segments", "Детализация", 32.0, 16.0, 64.0, 4.0).withEn("Segments"));
    public final BooleanSetting firstPerson = addSetting(new BooleanSetting("first_person", "Вид от 1-го лица", false).withEn("First Person"));

    public ChinaHatModule() {
        super("china_hat", "Китайская шляпа", "3D коническая шляпка над головой с градиентом, покачиванием, слоями и неоном", Category.VISUAL);
        setEn("China Hat", "3D conical hat above head with gradient, walking bobbing, multiple layers and glow");
    }
}
