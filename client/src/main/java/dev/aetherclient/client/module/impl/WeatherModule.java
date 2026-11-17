package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class WeatherModule extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("mode", "Погода", "Чисто", "Чисто", "Дождь", "Гроза", "Снег").withEn("Weather"));
    public final NumberSetting rainStrength = addSetting(new NumberSetting("rain", "Сила дождя", 1.0, 0.1, 1.0, 0.05).withEn("Rain Strength"));
    public final NumberSetting thunderStrength = addSetting(new NumberSetting("thunder", "Сила грозы", 1.0, 0.1, 1.0, 0.05).withEn("Thunder Strength"));

    private boolean captured = false;
    private boolean origRaining;
    private boolean origThundering;
    private float origRainGradient;
    private float origThunderGradient;

    public WeatherModule() {
        super("weather", "Погода", "Клиентский контроль дождя, снега и грозы", Category.VISUAL);
        setEn("Weather", "Client-side control of rain, snow and thunder");
    }

    @Override
    protected void onEnable() {
        captureOriginal();
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        if (captured) {
            try {
                mc.world.getLevelProperties().setRaining(origRaining);
                setThundering(mc.world.getLevelProperties(), origThundering);
                mc.world.setRainGradient(origRainGradient);
                mc.world.setThunderGradient(origThunderGradient);
            } catch (Throwable ignored) {
            }
            captured = false;
        }
    }

    private void captureOriginal() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        try {
            origRaining = mc.world.getLevelProperties().isRaining();
            origThundering = mc.world.getLevelProperties().isThundering();
            origRainGradient = mc.world.getRainGradient(1f);
            origThunderGradient = mc.world.getThunderGradient(1f);
            captured = true;
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.world == null) return;
        if (!captured) captureOriginal();
        String m = mode.getValue();
        boolean isSnow = "Снег".equals(m);
        boolean raining = isSnow || "Дождь".equals(m) || "Гроза".equals(m);
        boolean thundering = "Гроза".equals(m);
        float rain = isSnow ? 1.0f : (raining ? rainStrength.asFloat() : 0f);
        float thunder = thundering ? thunderStrength.asFloat() : 0f;
        try {
            mc.world.getLevelProperties().setRaining(raining);
            setThundering(mc.world.getLevelProperties(), thundering);
            mc.world.setRainGradient(rain);
            mc.world.setThunderGradient(thunder);
        } catch (Throwable ignored) {
        }
    }

    private static void setThundering(Object properties, boolean value) {
        try {
            for (java.lang.reflect.Method m : properties.getClass().getMethods()) {
                if (m.getName().equals("setThundering") && m.getParameterCount() == 1) {
                    m.invoke(properties, value);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String getDisplaySuffix() {
        return mode.getValue();
    }
}
