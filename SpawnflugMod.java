package de.spawnflug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-only Elytra-like glide using vanilla startFallFlying().
 * No elytra is placed into the player's inventory or equipment.
 * Minecraft 26.3 / Java 25 / Fabric.
 *
 * This is a source project. Please compile and test before using on a public server.
 */
public final class SpawnflugMod implements ModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("spawnflug.json");
    private final Map<UUID, Flight> state = new HashMap<>();
    private Config config;

    @Override
    public void onInitialize() {
        config = readConfig();
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("spawnflug")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("radius")
                    .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 1000))
                        .executes(ctx -> {
                            config.radius = IntegerArgumentType.getInteger(ctx, "blocks");
                            writeConfig();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aSpawnflug-Radius: " + config.radius + " Bloecke"), true);
                            return 1;
                        })))
                .then(Commands.literal("boost")
                    .then(Commands.argument("strength", DoubleArgumentType.doubleArg(0.1, 5.0))
                        .executes(ctx -> {
                            config.boost = DoubleArgumentType.getDouble(ctx, "strength");
                            writeConfig();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aSpawnflug-Boost: " + config.boost), true);
                            return 1;
                        })))
                .then(Commands.literal("center")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        BlockPos b = p.blockPosition();
                        config.x = b.getX();
                        config.y = b.getY();
                        config.z = b.getZ();
                        writeConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§aFlug-Spawn gesetzt: " + b.toShortString()), true);
                        return 1;
                    }))
                .then(Commands.literal("reset")
                    .executes(ctx -> {
                        config = new Config();
                        writeConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§aSpawnflug zurueckgesetzt"), true);
                        return 1;
                    }))
                .then(Commands.literal("info")
                    .executes(ctx -> {
                        String center = config.x == null ? "Weltspawn" : config.x + ", " + config.y + ", " + config.z;
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6Spawnflug §7| §eRadius: §f" + config.radius +
                            " §7| §eBoost: §f" + config.boost + " §7| §eMitte: §f" + center), false);
                        return 1;
                    }))
            )
        );
    }

    private void tick(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            Flight flight = state.computeIfAbsent(player.getUUID(), id -> new Flight());
            boolean jump = player.getLastClientInput().jump();
            boolean pressed = jump && !flight.lastJump;
            flight.lastJump = jump;

            // Only Survival, only overworld. Never interfere with Spectator/Creative.
            if (player.gameMode() != GameType.SURVIVAL || player.level().dimension() != Level.OVERWORLD) {
                flight.active = false;
                flight.ticks = 0;
                flight.boostUsed = false;
                continue;
            }

            if (player.onGround() || player.isInWater()) {
                // Vanilla stops fall-flying as soon as ground/water is reached.
                flight.active = false;
                flight.ticks = 0;
                flight.boostUsed = false;
                flight.lastGroundTick = server.getTickCount();
                continue;
            }

            BlockPos spawn = config.x == null
                    ? server.overworld().getRespawnData().pos()
                    : new BlockPos(config.x, config.y, config.z);
            boolean nearSpawn = player.position().distanceToSqr(Vec3.atCenterOf(spawn))
                    <= (double) config.radius * config.radius;

            // First press jumped from ground, second press in the air starts gliding.
            // We require a fresh space press, not a held-down jump key.
            if (!flight.active && pressed && nearSpawn && !player.isFallFlying()) {
                flight.active = true;
                flight.ticks = 0;
                flight.boostUsed = false;
                player.startFallFlying();
                player.sendOverlayMessage(Component.literal("§bUnsichtbare Elytra aktiv! §7Leertaste = Boost"));
            }

            if (flight.active) {
                // startFallFlying is reasserted, because there is no physical chest Elytra.
                player.startFallFlying();
                player.fallDistance = 0.0F;
                flight.ticks++;

                if (pressed && !flight.boostUsed && flight.ticks >= 8) {
                    Vec3 direction = player.getLookAngle();
                    player.setDeltaMovement(direction.scale(config.boost));
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    flight.boostUsed = true;
                    player.sendOverlayMessage(Component.literal("§aBoost! §7Lande, um den Flug zu beenden."));
                }
            }
        }
        state.keySet().removeIf(id -> !online.contains(id));
    }

    private Config readConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Config read = GSON.fromJson(reader, Config.class);
                if (read != null && read.radius > 0 && read.boost > 0) return read;
            } catch (Exception e) {
                System.err.println("[Spawnflug] Konfiguration konnte nicht gelesen werden: " + e.getMessage());
            }
        }
        Config defaults = new Config();
        try { Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer out = Files.newBufferedWriter(CONFIG_PATH)) { GSON.toJson(defaults, out); }
        } catch (IOException e) { System.err.println("[Spawnflug] Konfiguration nicht speicherbar: " + e.getMessage()); }
        return defaults;
    }

    private void writeConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[Spawnflug] Konfiguration nicht speicherbar: " + e.getMessage());
        }
    }

    private static final class Config {
        int radius = 30;
        double boost = 1.6;
        Integer x = null;
        Integer y = null;
        Integer z = null;
    }

    private static final class Flight {
        boolean active;
        boolean lastJump;
        boolean boostUsed;
        int ticks;
        int lastGroundTick;
    }
}
