package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class WeatherModule extends Module {
    public final ModeSetting mode = addSetting(
            new ModeSetting("mode", "Тип погоды", "Чисто",
                    "Чисто", "Дождь", "Гроза", "Снег", "Снегопад везде")
                    .withEn("Weather Type")
    );
    public final NumberSetting rainStrength = addSetting(new NumberSetting("rain", "Интенсивность дождя", 1.0, 0.1, 1.0, 0.05).withEn("Rain Strength"));
    public final NumberSetting thunderStrength = addSetting(new NumberSetting("thunder", "Интенсивность грозы", 1.0, 0.1, 1.0, 0.05).withEn("Thunder Strength"));
    public final BooleanSetting disableLightningFlash = addSetting(new BooleanSetting("no_flash", "Убрать ослепление от молний", true).withEn("Disable Lightning Flash"));
    public final BooleanSetting disableThunderSound = addSetting(new BooleanSetting("mute_thunder", "Заглушить раскаты грома", false).withEn("Mute Thunder Sound"));
    public final NumberSetting rainVolume = addSetting(new NumberSetting("rain_vol", "Громкость звуков дождя", 0.75, 0.0, 1.0, 0.05).withEn("Rain Sound Volume"));
    public final BooleanSetting snowflakesEverywhere = addSetting(new BooleanSetting("snow_everywhere", "Снег во всех биомах", false).withEn("Snow in All Biomes"));
    public final NumberSetting snowDensity = addSetting(new NumberSetting("snow_density", "Плотность снежинок", 1.2, 0.5, 4.0, 0.1).withEn("Snow Density"));

    private boolean captured = false;
    private boolean origRaining;
    private boolean origThundering;
    private float origRainGradient;
    private float origThunderGradient;

    public WeatherModule() {
        super("weather", "Погода", "Клиентский контроль погоды: ясное небо, снегопад везде, громкость дождя и анти-ослепление", Category.VISUAL);
        setEn("Weather", "Client-side weather control: clear sky, universal snow, rain volume and no lightning blind");
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
        boolean wantRain = "Дождь".equals(m) || "Гроза".equals(m) || "Снег".equals(m) || "Снегопад везде".equals(m);
        boolean wantThunder = "Гроза".equals(m);
        float rainGrad = wantRain ? rainStrength.asFloat() : 0f;
        float thunderGrad = wantThunder ? thunderStrength.asFloat() : 0f;
        try {
            mc.world.getLevelProperties().setRaining(wantRain);
            setThundering(mc.world.getLevelProperties(), wantThunder);
            mc.world.setRainGradient(rainGrad);
            mc.world.setThunderGradient(thunderGrad);
        } catch (Throwable ignored) {
        }
    }

    private void setThundering(net.minecraft.world.WorldProperties props, boolean thundering) {
        try {
            var m = props.getClass().getMethod("setThundering", boolean.class);
            m.invoke(props, thundering);
            return;
        } catch (Throwable ignored) {}
        try {
            for (var m : props.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("thunder") || name.equals("method_155") || name.equals("b")) {
                        m.invoke(props, thundering);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
