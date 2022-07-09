package inside;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.Color;
import arc.util.*;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mindustry.content.Fx;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.net.Host;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.MessageBlock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static inside.Bundle.findLocale;
import static mindustry.Vars.*;

public class LightweightHub extends Plugin {

    public static final float delaySeconds = 3f;
    public static final float refreshDuration = 6f;
    public static final float teleportUpdateInterval = 3f;

    public static final Interval interval = new Interval();
    public static final AtomicInteger counter = new AtomicInteger();
    public static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

    public static Config config;

    public static void showOnlineLabel(Player player, Server server, Host host) {
        Call.label(player.con, host.name, refreshDuration, server.titleX * tilesize, server.titleY * tilesize);
        Call.label(player.con, Bundle.format("server.offline", findLocale(player.locale), host.players, host.mapname), refreshDuration, server.labelX * tilesize, server.labelY * tilesize);
    }

    public static void showOfflineLabel(Player player, Server server) {
        Call.label(player.con, Bundle.format("server.online", findLocale(player.locale)), refreshDuration, server.labelX * tilesize, server.labelY * tilesize);
    }

    public static void teleport(Player player) {
        teleport(player, null);
    }

    public static void teleport(Player player, Tile tile) {
        config.servers.forEach(data -> {
            if (data.inDiapason(tile != null ? tile.x : player.tileX(), tile != null ? tile.y : player.tileY())) {
                data.pingHost(host -> Call.connect(player.con, data.ip, data.port), e -> {});
            }
        });
    }

    @Override
    public void init() {
        Fi configFile = dataDirectory.child("config-hub.json");
        if (configFile.exists()) {
            config = gson.fromJson(configFile.reader(), Config.class);
            Log.info("[Hub] Конфигурация успешно загружена. (@)", configFile.absolutePath());
        } else {
            configFile.writeString(gson.toJson(config = new Config()));
            Log.info("[Hub] Файл конфигурации сгенерирован. (@)", configFile.absolutePath());
        }

        Bundle.load();

        Events.run(Trigger.update, () -> {
            config.servers.forEach(server -> {
                Call.effect(Fx.hitFlamePlasma, server.tileX() - server.size * tilesize, server.tileY() - server.size * tilesize, 45f, Color.white);
                Call.effect(Fx.hitFlamePlasma, server.tileX() + server.size * tilesize, server.tileY() - server.size * tilesize, 135f, Color.white);
                Call.effect(Fx.hitFlamePlasma, server.tileX() + server.size * tilesize, server.tileY() + server.size * tilesize, 225f, Color.white);
                Call.effect(Fx.hitFlamePlasma, server.tileX() - server.size * tilesize, server.tileY() + server.size * tilesize, 315f, Color.white);
            });

            if (interval.get(teleportUpdateInterval)) {
                Groups.player.each(LightweightHub::teleport);
            }
        });

        Events.on(TapEvent.class, event -> teleport(event.player, event.tile));

        Events.on(PlayerJoin.class, event -> config.servers.forEach(server -> server.pingHost(host -> showOnlineLabel(event.player, server, host), e -> showOfflineLabel(event.player, server))));

        Events.on(WorldLoadEvent.class, event -> state.rules.teams.get(state.rules.defaultTeam).cheat = true);

        Timer.schedule(() -> {
            CompletableFuture<?>[] tasks = config.servers.stream().map(server -> CompletableFuture.runAsync(() -> server.pingHost(host -> {
                counter.addAndGet(host.players);
                Groups.player.each(player -> showOnlineLabel(player, server, host));
            }, e -> Groups.player.each(player -> showOfflineLabel(player, server))))).toArray(CompletableFuture<?>[]::new);

            CompletableFuture.allOf(tasks).thenRun(() -> {
                counter.addAndGet(Groups.player.size());
                Core.settings.put("totalPlayers", counter.getAndSet(0));
            }).join();
        }, delaySeconds, refreshDuration);

        netServer.admins.addActionFilter(action -> action.type != ActionType.placeBlock && action.type != ActionType.breakBlock && !(action.tile.block() instanceof MessageBlock));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("reload-config", "Перезагрузить конфигурацию плагина LightweightHub.", args -> {
            config = gson.fromJson(dataDirectory.child("config-hub.json").readString(), Config.class);
            Log.info("[Hub] Конфигурация успешно перезагружена.");
        });
    }
}
