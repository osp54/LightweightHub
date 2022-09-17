package inside;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.util.*;
import com.google.gson.*;
import mindustry.content.Fx;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Host;
import mindustry.world.Tile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static inside.Bundle.*;
import static mindustry.Vars.*;
import static mindustry.net.Administration.ActionType.*;

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
        Call.label(player.con, format("server.offline", findLocale(player), host.players, host.mapname), refreshDuration, server.labelX * tilesize, server.labelY * tilesize);
    }

    public static void showOfflineLabel(Player player, Server server) {
        Call.label(player.con, format("server.online", findLocale(player)), refreshDuration, server.labelX * tilesize, server.labelY * tilesize);
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
        var configFile = dataDirectory.child("config-hub.json");
        if (configFile.exists()) {
            config = gson.fromJson(configFile.reader(), Config.class);
            Log.info("[Hub] Конфигурация успешно загружена. (@)", configFile.absolutePath());
        } else {
            configFile.writeString(gson.toJson(config = new Config()));
            Log.info("[Hub] Файл конфигурации сгенерирован. (@)", configFile.absolutePath());
        }

        Bundle.load();

        content.units().each(unit -> unit.payloadCapacity = 0f);

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

        Events.on(WorldLoadEvent.class, event -> {
            state.rules.blockDamageMultiplier = 0f;
            state.rules.unitDamageMultiplier = 0f;
            state.rules.teams.get(state.rules.defaultTeam).cheat = true;
        });

        Timer.schedule(() -> {
            var tasks = config.servers.stream().map(server -> CompletableFuture.runAsync(() -> server.pingHost(host -> {
                counter.addAndGet(host.players);
                Groups.player.each(player -> showOnlineLabel(player, server, host));
            }, e -> Groups.player.each(player -> showOfflineLabel(player, server))))).toArray(CompletableFuture<?>[]::new);

            CompletableFuture.allOf(tasks).thenRun(() -> {
                counter.addAndGet(Groups.player.size());
                Core.settings.put("totalPlayers", counter.getAndSet(0));
            }).join();
        }, delaySeconds, refreshDuration);

        netServer.admins.addActionFilter(action -> action.type != placeBlock && action.type != breakBlock);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("reload-config", "Перезагрузить конфигурацию плагина LightweightHub.", args -> {
            config = gson.fromJson(dataDirectory.child("config-hub.json").readString(), Config.class);
            Log.info("[Hub] Конфигурация успешно перезагружена.");
        });
    }
}
