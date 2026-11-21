package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ShulkerPreviewModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль окна", "Стекло", "Стекло", "Темная тема", "Компакт").withEn("Preview Style")
    );
    public final BooleanSetting showItemCounts = addSetting(new BooleanSetting("show_counts", "Количество предметов", true).withEn("Show Stack Counts"));
    public final BooleanSetting showDurability = addSetting(new BooleanSetting("durability", "Полоска прочности", true).withEn("Show Durability"));
    public final BooleanSetting showEmptySlots = addSetting(new BooleanSetting("empty_slots", "Сетка пустых слотов", true).withEn("Show Empty Slots"));
    public final NumberSetting scale = addSetting(new NumberSetting("scale", "Масштаб тултипа", 1.0, 0.7, 1.6, 0.05).withEn("Tooltip Scale"));
    public final ColorSetting accentColor = addSetting(new ColorSetting("accent", "Акцентный цвет", 0xFF7C5CFF).withEn("Accent Color"));
    public final BooleanSetting blur = addSetting(new BooleanSetting("blur", "Размытие фона", true).withEn("Blur Background"));

    public ShulkerPreviewModule() {
        super("shulker_preview", "Превью шалкера", "Интерактивный предпросмотр содержимого шалкер-боксов в инвентаре при наведении", Category.VISUAL);
        setEn("Shulker Preview", "Interactive 3x9 inventory preview of hovered shulker box contents");
        setEnabled(true);
    }
}
