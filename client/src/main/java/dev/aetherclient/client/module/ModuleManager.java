package dev.aetherclient.client.module;

import dev.aetherclient.client.module.impl.AmbienceModule;
import dev.aetherclient.client.module.impl.AntiBlindModule;
import dev.aetherclient.client.module.impl.ArrowsModule;
import dev.aetherclient.client.module.impl.AspectRatioModule;
import dev.aetherclient.client.module.impl.AutoSprintModule;
import dev.aetherclient.client.module.impl.BlockOverlayModule;
import dev.aetherclient.client.module.impl.BreadcrumbsModule;
import dev.aetherclient.client.module.impl.ChamsModule;
import dev.aetherclient.client.module.impl.ClickGuiModule;
import dev.aetherclient.client.module.impl.ColorFilterModule;
import dev.aetherclient.client.module.impl.CosmeticsModule;
import dev.aetherclient.client.module.impl.CrosshairModule;
import dev.aetherclient.client.module.impl.CustomScoreboardModule;
import dev.aetherclient.client.module.impl.CustomTabListModule;
import dev.aetherclient.client.module.impl.DamageVignetteModule;
import dev.aetherclient.client.module.impl.DiscordRpcModule;
import dev.aetherclient.client.module.impl.EspModule;
import dev.aetherclient.client.module.impl.FreecamModule;
import dev.aetherclient.client.module.impl.FullbrightModule;
import dev.aetherclient.client.module.impl.HitColorModule;
import dev.aetherclient.client.module.impl.HitParticlesModule;
import dev.aetherclient.client.module.impl.HitboxesModule;
import dev.aetherclient.client.module.impl.HoleEspModule;
import dev.aetherclient.client.module.impl.HpCounterModule;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.module.impl.ItemEspModule;
import dev.aetherclient.client.module.impl.ItemPhysicsModule;
import dev.aetherclient.client.module.impl.ItemRadiusModule;
import dev.aetherclient.client.module.impl.JumpCirclesModule;
import dev.aetherclient.client.module.impl.MotionBlurModule;
import dev.aetherclient.client.module.impl.NameProtectModule;
import dev.aetherclient.client.module.impl.NoRenderModule;
import dev.aetherclient.client.module.impl.NotificationsModule;
import dev.aetherclient.client.module.impl.ParticlesModule;
import dev.aetherclient.client.module.impl.ProjectilesModule;
import dev.aetherclient.client.module.impl.SwingAnimationModule;
import dev.aetherclient.client.module.impl.TargetEspModule;
import dev.aetherclient.client.module.impl.TargetHudModule;
import dev.aetherclient.client.module.impl.TimeChangerModule;
import dev.aetherclient.client.module.impl.TracersModule;
import dev.aetherclient.client.module.impl.TrailsModule;
import dev.aetherclient.client.module.impl.ViewmodelModule;
import dev.aetherclient.client.module.impl.CustomSkyModule;
import dev.aetherclient.client.module.impl.HitSoundModule;
import dev.aetherclient.client.module.impl.ClickPearlModule;
import dev.aetherclient.client.module.impl.FastSwapModule;
import dev.aetherclient.client.module.impl.WeatherModule;
import dev.aetherclient.client.module.impl.ZoomModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry holding all Aether Client modules.
 */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public final ClickGuiModule clickGui = register(new ClickGuiModule());
    public final HudModule hud = register(new HudModule());
    public final TargetHudModule targetHud = register(new TargetHudModule());
    public final DamageVignetteModule damageVignette = register(new DamageVignetteModule());
    public final FullbrightModule fullbright = register(new FullbrightModule());
    public final EspModule esp = register(new EspModule());
    public final TracersModule tracers = register(new TracersModule());
    public final TargetEspModule targetEsp = register(new TargetEspModule());
    public final ArrowsModule arrows = register(new ArrowsModule());
    public final JumpCirclesModule jumpCircles = register(new JumpCirclesModule());
    public final TrailsModule trails = register(new TrailsModule());
    public final HitParticlesModule hitParticles = register(new HitParticlesModule());
    public final CrosshairModule crosshair = register(new CrosshairModule());
    public final AntiBlindModule antiBlind = register(new AntiBlindModule());
    public final AmbienceModule ambience = register(new AmbienceModule());
    public final ZoomModule zoom = register(new ZoomModule());
    public final ParticlesModule particles = register(new ParticlesModule());
    public final NameProtectModule nameProtect = register(new NameProtectModule());
    public final ChamsModule chams = register(new ChamsModule());
    public final MotionBlurModule motionBlur = register(new MotionBlurModule());
    public final FreecamModule freecam = register(new FreecamModule());
    public final BreadcrumbsModule breadcrumbs = register(new BreadcrumbsModule());
    public final ProjectilesModule projectiles = register(new ProjectilesModule());
    public final ItemEspModule itemEsp = register(new ItemEspModule());
    public final ItemPhysicsModule itemPhysics = register(new ItemPhysicsModule());
    public final BlockOverlayModule blockOverlay = register(new BlockOverlayModule());
    public final HoleEspModule holeEsp = register(new HoleEspModule());
    public final HitColorModule hitColor = register(new HitColorModule());
    public final WeatherModule weather = register(new WeatherModule());
    public final TimeChangerModule timeChanger = register(new TimeChangerModule());
    public final NoRenderModule noRender = register(new NoRenderModule());
    public final CustomScoreboardModule scoreboard = register(new CustomScoreboardModule());
    public final CustomTabListModule tabList = register(new CustomTabListModule());
    public final CosmeticsModule cosmetics = register(new CosmeticsModule());
    public final NotificationsModule notifications = register(new NotificationsModule());
    public final AutoSprintModule autoSprint = register(new AutoSprintModule());
    public final ColorFilterModule colorFilter = register(new ColorFilterModule());
    public final HpCounterModule hpCounter = register(new HpCounterModule());
    public final SwingAnimationModule swingAnimation = register(new SwingAnimationModule());
    public final AspectRatioModule aspectRatio = register(new AspectRatioModule());
    public final ItemRadiusModule itemRadius = register(new ItemRadiusModule());
    public final ViewmodelModule viewmodel = register(new ViewmodelModule());
    public final CustomSkyModule customSky = register(new CustomSkyModule());
    public final HitSoundModule hitSound = register(new HitSoundModule());
    public final ClickPearlModule clickPearl = register(new ClickPearlModule());
    public final FastSwapModule fastSwap = register(new FastSwapModule());
    public final DiscordRpcModule discordRpc = register(new DiscordRpcModule());
    public final HitboxesModule hitboxes = register(new HitboxesModule());
    public final dev.aetherclient.client.module.impl.TripleSwapModule tripleSwap = register(new dev.aetherclient.client.module.impl.TripleSwapModule());
    public final dev.aetherclient.client.module.impl.ChinaHatModule chinaHat = register(new dev.aetherclient.client.module.impl.ChinaHatModule());
    public final dev.aetherclient.client.module.impl.PearlPredictModule pearlPredict = register(new dev.aetherclient.client.module.impl.PearlPredictModule());
    public final dev.aetherclient.client.module.impl.AutoToolModule autoTool = register(new dev.aetherclient.client.module.impl.AutoToolModule());
    public final dev.aetherclient.client.module.impl.ChestStealerModule chestStealer = register(new dev.aetherclient.client.module.impl.ChestStealerModule());
    public final dev.aetherclient.client.module.impl.InventoryArrangerModule invArranger = register(new dev.aetherclient.client.module.impl.InventoryArrangerModule());
    public final dev.aetherclient.client.module.impl.MaceHelperModule maceHelper = register(new dev.aetherclient.client.module.impl.MaceHelperModule());
    public final dev.aetherclient.client.module.impl.AutoTpAcceptModule autoTpAccept = register(new dev.aetherclient.client.module.impl.AutoTpAcceptModule());
    public final dev.aetherclient.client.module.impl.ItemScrollerModule itemScroller = register(new dev.aetherclient.client.module.impl.ItemScrollerModule());
    public final dev.aetherclient.client.module.impl.UnHookModule unHook = register(new dev.aetherclient.client.module.impl.UnHookModule());

    private <M extends Module> M register(M module) {
        modules.add(module);
        return module;
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                result.add(m);
            }
        }
        return result;
    }
}
