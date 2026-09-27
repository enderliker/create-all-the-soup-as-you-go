package com.ender.allthesoupasyougo.dev;

import com.ender.allthesoupasyougo.AllTheSoupAsYouGoMod;
import com.ender.allthesoupasyougo.registry.ModFluids;
import com.ender.allthesoupasyougo.registry.ModItems;
import com.ender.allthesoupasyougo.registry.Soup;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/** Opens its own world, builds real Create machines, verifies processing, and uses F2's screenshot API. */
@Mod(value = AllTheSoupAsYouGoMod.MODID, dist = Dist.CLIENT)
public final class ShowcaseAutomation {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int FLOOR = -60;
    private static final BlockPos DRAIN = new BlockPos(3, FLOOR + 2, 4);
    private static final BlockPos DEPOT = new BlockPos(8, FLOOR + 2, 4);
    private static final BlockPos SPOUT = DEPOT.above(2);
    private static final Path REPORT = Path.of("showcase-report.txt");
    private final List<Rig> rigs = new ArrayList<>();
    private List<Soup> availableSoups;
    private final AtomicInteger shots = new AtomicInteger();
    private boolean opened;
    private boolean built;
    private boolean filled;
    private int ticks;
    private int view = -1;
    private int viewTicks;
    private boolean capturePending;
    private volatile boolean captureInProgress;
    private boolean creativeChecked;
    private boolean finished;
    private volatile boolean verified;

    public ShowcaseAutomation() {
        if (!Boolean.getBoolean("soup.showcase")) return;
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener(this::renderFrame);
    }

    private void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!opened && (mc.screen instanceof TitleScreen || mc.screen != null
                && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))) {
            opened = true;
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.fov().set(55);
            mc.options.renderDistance().set(8);
            mc.options.simulationDistance().set(8);
            mc.options.framerateLimit().set(60);
            mc.options.bobView().set(false);
            String world = "all-the-soup-showcase";
            if (Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(world).resolve("level.dat"))) {
                mc.createWorldOpenFlows().openWorld(world, () -> fail("Could not open showcase world"));
            } else {
                mc.createWorldOpenFlows().createFreshLevel(world,
                        new LevelSettings("All the Soup — automated showcase", GameType.CREATIVE, false,
                                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20260926L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            }
        }
        if (mc.player == null || mc.level == null || !built || finished) return;
        mc.options.hideGui = true;
        if (!creativeChecked) {
            CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
            for (Soup soup : availableSoups) {
                if (CreativeModeTabs.searchTab().getDisplayItems().stream().noneMatch(s -> s.is(ModItems.get(soup).get()))) {
                    fail("Bucket missing from creative search: " + soup.id());
                    return;
                }
            }
            creativeChecked = true;
            for (Soup soup : Soup.values()) {
                if (!availableSoups.contains(soup) && CreativeModeTabs.searchTab().getDisplayItems().stream()
                        .anyMatch(s -> s.is(ModItems.get(soup).get()))) {
                    fail("Optional bucket visible without Farmer's Delight: " + soup.id());
                    return;
                }
            }
            LOG.info("SOUP_CREATIVE_CHECK: all {} available buckets are present in creative search", availableSoups.size());
        }
        if (Boolean.getBoolean("soup.testsOnly") && verified) {
            finished = true;
            LOG.info("SOUP_TESTS_COMPLETE");
            mc.stop();
            return;
        }
        if (mc.screen != null) mc.setScreen(null);
        int nextView = Math.min(shots.get(), 3);
        if (view != nextView) { view = nextView; viewTicks = 0; }
        viewTicks++;
        if (view == 0) camera(mc, 3.5, FLOOR + 2.1, 9.5, 3.5, FLOOR + 2.65, 4.5);
        if (view == 1) camera(mc, 8.5, FLOOR + 2.6, 10.5, 8.5, FLOOR + 3.35, 4.5);
        if (view == 2) camera(mc, 13.0, FLOOR + 2.5, 18.5, 13.0, FLOOR + 2.5, 1.0);
        if (viewTicks < 80 || capturePending || captureInProgress) return;
        if (view == 0 && mc.level.getBlockEntity(DRAIN) instanceof ItemDrainBlockEntity drain) {
            int processing = drain.saveWithoutMetadata(mc.level.registryAccess()).getInt("ProcessingTicks");
            if (processing == 10) capturePending = true;
        }
        if (view == 1 && mc.level.getBlockEntity(SPOUT) instanceof SpoutBlockEntity spout
                && spout.processingTicks == 10) capturePending = true;
        if (view == 2 && verified) capturePending = true;
        if (view == 3 && viewTicks > 60 && verified) {
            finished = true;
            LOG.info("SOUP_SHOWCASE_COMPLETE: 3 native screenshots, all machine checks passed");
            mc.stop();
        }
    }

    private void camera(Minecraft mc, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - (y + mc.player.getEyeHeight()), dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        mc.player.setPos(x, y, z);
        mc.player.setDeltaMovement(0, 0, 0);
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        mc.player.xo = x; mc.player.yo = y; mc.player.zo = z;
    }

    private void renderFrame(RenderFrameEvent.Post event) {
        if (!capturePending) return;
        capturePending = false;
        captureInProgress = true;
        Minecraft mc = Minecraft.getInstance();
        String[] names = {"01-draining-soup.png", "02-filling-a-bowl.png", "03-soup-kitchen.png"};
        int index = shots.get();
        if (index >= names.length) return;
        // Exactly the native Minecraft screenshot path used by F2, after HUD-free rendering.
        Screenshot.grab(mc.gameDirectory, names[index], mc.getMainRenderTarget(), message -> {
            LOG.info("SOUP_SCREENSHOT: {}", message.getString());
            shots.incrementAndGet();
            captureInProgress = false;
        });
    }

    private void serverTick(ServerTickEvent.Post event) {
        if (!opened || finished) return;
        var server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerLevel level = server.overworld();
        ticks++;
        if (!built) {
            availableSoups = Arrays.stream(Soup.values()).filter(Soup::available).toList();
            build(level);
            for (var player : server.getPlayerList().getPlayers()) {
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(3.5, FLOOR + 2.1, 9.5);
            }
            built = true;
        }
        if (!filled && ticks >= 40) {
            try { SoupEffectChecks.run(level); }
            catch (Exception e) { fail("Suspicious stew checks: " + e.getMessage()); return; }
            LOG.info("SOUP_EFFECT_CHECKS: nine effects, multiple effects, empty stew, buckets and mixing passed");
            for (int i = 0; i < availableSoups.size(); i++) {
                Soup soup = availableSoups.get(i);
                fill(level, new BlockPos(i * 2 + 1, FLOOR + 1, 0), soup, 6000);
                Rig rig = rigs.get(i);
                if (soup == Soup.SUSPICIOUS_STEW)
                    fluid(level, rig.spout).fill(SoupEffectChecks.fluid(1000, SoupEffectChecks.MACHINE_EFFECTS), IFluidHandler.FluidAction.EXECUTE);
                else fill(level, rig.spout, soup, 1000);
                feed(level, rig.drain, soup);
                ((DepotBlockEntity) level.getBlockEntity(rig.depot)).setHeldItem(new ItemStack(Items.BOWL));
            }
            filled = true;
            fill(level, SPOUT.above(), displaySoup(), 6000);
        }
        if (filled) {
            for (Rig rig : rigs) check(level, rig);
            if (ticks == 160) for (Rig rig : rigs) {
                LOG.info("SOUP_RIG {}: bowl={}, drain={}, fill={}, fluid={}, spout={}, item={}", rig.soup.id(), rig.bowlReturned,
                        rig.drainPassed, rig.fillPassed, fluid(level,rig.drain).getFluidInTank(0),
                        fluid(level,rig.spout).getFluidInTank(0), ((DepotBlockEntity)level.getBlockEntity(rig.depot)).getHeldItem());
            }
            for (ItemEntity loose : level.getEntitiesOfClass(ItemEntity.class, new AABB(-3,FLOOR,0,26,FLOOR+10,17))) loose.discard();
            if (ticks % 100 == 50) {
                var drainCap = fluid(level, DRAIN);
                if (drainCap != null) drainCap.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                feed(level, DRAIN, Soup.BEETROOT_SOUP);
                fill(level, SPOUT, displaySoup(), 1000);
                ((DepotBlockEntity) level.getBlockEntity(DEPOT)).setHeldItem(new ItemStack(Items.BOWL));
            }
        }
        if (!verified && rigs.stream().allMatch(r -> r.drainPassed && r.fillPassed)) {
            verified = true;
            StringBuilder report = new StringBuilder("Create 6.0.10 / NeoForge 21.1.251\n");
            report.append("Suspicious stew: nine effects, multiple effects, empty stew, buckets and mixing: PASS\n");
            report.append("Creative search: ").append(availableSoups.size()).append(" available buckets; optional buckets hidden when absent: PASS\n");
            for (Rig r : rigs) report.append(r.soup.id()).append(": drain -> 250 mB + bowl; spout -> original soup using 250 mB: PASS\n");
            try { Files.writeString(REPORT, report.toString()); } catch (Exception e) { throw new RuntimeException(e); }
            LOG.info("SOUP_MACHINE_CHECKS: all {} soups passed both machines", rigs.size());
        }
        if (ticks > 2400 && !finished) fail("Showcase timed out; inspect machine checks and captures");
    }

    private void check(ServerLevel level, Rig rig) {
        var drain = (ItemDrainBlockEntity) level.getBlockEntity(rig.drain);
        IFluidHandler drained = fluid(level, rig.drain);
        if (drain.getHeldItemStack().is(Items.BOWL)) rig.bowlReturned = true;
        if (rig.bowlReturned && drained.getFluidInTank(0).getAmount() == 250
                && drained.getFluidInTank(0).getFluid() == ModFluids.source(rig.soup).get()
                && (rig.soup != Soup.SUSPICIOUS_STEW || SoupEffectChecks.MACHINE_EFFECTS.equals(
                        drained.getFluidInTank(0).get(DataComponents.SUSPICIOUS_STEW_EFFECTS)))) rig.drainPassed = true;
        var depot = (DepotBlockEntity) level.getBlockEntity(rig.depot);
        IFluidHandler supply = fluid(level, rig.spout);
        if (depot.getHeldItem().is(item(rig.soup)) && supply.getFluidInTank(0).getAmount() == 750
                && (rig.soup != Soup.SUSPICIOUS_STEW || SoupEffectChecks.MACHINE_EFFECTS.equals(
                        depot.getHeldItem().get(DataComponents.SUSPICIOUS_STEW_EFFECTS)))) rig.fillPassed = true;
    }

    private void build(ServerLevel level) {
        level.setDayTime(6000);
        level.setWeatherParameters(100000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        for (int x = -1; x < 6; x++) for (int z = -1; z < 3; z++) level.setChunkForced(x, z, true);
        for (int x = -3; x <= 28; x++) for (int z = -3; z <= 18; z++) {
            for (int y = FLOOR + 1; y <= FLOOR + 9; y++) level.setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x,FLOOR,z), ((x + z) % 2 == 0 ? Blocks.SMOOTH_STONE : Blocks.POLISHED_ANDESITE).defaultBlockState());
        }
        for (int x = -3; x <= 28; x++) for (int y = 1; y <= 6; y++) {
            boolean window = y >= 3 && y <= 5 && x % 6 >= 0 && x % 6 <= 3;
            level.setBlockAndUpdate(new BlockPos(x,FLOOR+y,-2), (window ? Blocks.GLASS : Blocks.BRICKS).defaultBlockState());
        }
        for (int x = 0; x < 27; x++) {
            level.setBlockAndUpdate(new BlockPos(x,FLOOR+1,0), Blocks.SPRUCE_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x,FLOOR+1,4), Blocks.SMOOTH_STONE.defaultBlockState());
        }
        for (int i = 0; i < availableSoups.size(); i++) {
            int x = i * 2 + 1;
            block(level, new BlockPos(x,FLOOR+1,0), "create:fluid_tank");
            block(level, new BlockPos(x,FLOOR+2,0), "create:fluid_tank");
            block(level, new BlockPos(x,FLOOR+1,2), "create:depot");
            ((DepotBlockEntity)level.getBlockEntity(new BlockPos(x,FLOOR+1,2))).setHeldItem(new ItemStack(ModItems.get(availableSoups.get(i)).get()));
            int testX = 35 + i * 4;
            Rig rig = new Rig(availableSoups.get(i), new BlockPos(testX,FLOOR+1,0), new BlockPos(testX+2,FLOOR+1,0));
            block(level, rig.drain, "create:item_drain");
            block(level, rig.depot, "create:depot");
            block(level, rig.spout, "create:spout");
            rigs.add(rig);
        }
        block(level, DRAIN, "create:item_drain");
        block(level, DEPOT, "create:depot");
        block(level, SPOUT, "create:spout");
        for (int z = 1; z <= 3; z++) block(level, new BlockPos(3,FLOOR+2,z), "create:fluid_pipe");
        block(level, SPOUT.above(), "create:fluid_tank");
        for (int x : new int[]{0,6,12,18,24}) level.setBlockAndUpdate(new BlockPos(x,FLOOR+6,-1), Blocks.LANTERN.defaultBlockState());
        LOG.info("SOUP_SHOWCASE: constructed kitchen and {} isolated machine test rigs", rigs.size());
    }

    private static void block(ServerLevel level, BlockPos pos, String id) {
        BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, state);
    }
    private static Item item(Soup soup) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(soup.farmersDelight() ? "farmersdelight" : "minecraft", soup.id()));
    }
    private static IFluidHandler fluid(ServerLevel level, BlockPos pos) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
    }
    private static void fill(ServerLevel level, BlockPos pos, Soup soup, int amount) {
        var tank = fluid(level, pos);
        if (tank == null) throw new IllegalStateException("Missing fluid capability at " + pos);
        tank.fill(new FluidStack(ModFluids.source(soup).get(), amount), IFluidHandler.FluidAction.EXECUTE);
    }
    private static void feed(ServerLevel level, BlockPos pos, Soup soup) {
        var drain = (ItemDrainBlockEntity)level.getBlockEntity(pos);
        ItemStack stack = new ItemStack(item(soup));
        if (soup == Soup.SUSPICIOUS_STEW) stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, SoupEffectChecks.MACHINE_EFFECTS);
        drain.setHeldItem(new TransportedItemStack(stack), Direction.WEST);
        drain.sendData();
    }
    private Soup displaySoup() { return availableSoups.contains(Soup.PUMPKIN_SOUP) ? Soup.PUMPKIN_SOUP : Soup.MUSHROOM_STEW; }
    private static void fail(String text) {
        LOG.error("SOUP_SHOWCASE_FAILED: {}", text);
        try { Files.writeString(REPORT, "FAIL: " + text); } catch (Exception ignored) { }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }
    private static final class Rig {
        final Soup soup;
        final BlockPos drain, depot, spout;
        boolean drainPassed, fillPassed, bowlReturned;
        Rig(Soup soup, BlockPos drain, BlockPos depot) {
            this.soup = soup; this.drain = drain; this.depot = depot; this.spout = depot.above(2);
        }
    }
}
