package inside;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.*;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.Tile;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static inside.Bundle.defaultLocale;
import static inside.Bundle.supportedLocales;
import static mindustry.Vars.*;

public class LightweightHub extends Plugin {

    public static final float refreshDuration = 2.5f;
    public static final float delaySeconds = 1.5f;
    public static final float teleportUpdateInterval = 3f;

    public static final String configFileName = "config-hub.json";

    public static Config config;
    public final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

    public static final Interval interval = new Interval();
    public static final AtomicInteger counter = new AtomicInteger();

    public static Locale findLocale(Player player) {
        Locale locale = Structs.find(supportedLocales, l -> l.toString().equals(player.locale) || player.locale.startsWith(l.toString()));
        return locale != null ? locale : defaultLocale();
    }

    public void teleport(Player player) {
        teleport(player, null);
    }

    public void teleport(Player player, Tile tile) {
        config.servers.forEach(data -> {
            if (data.inDiapason(tile != null ? tile.x : player.tileX(), tile != null ? tile.y : player.tileY())) {
                net.pingHost(data.ip, data.port, host -> Call.connect(player.con, data.ip, data.port), e -> {});
            }
        });
    }

    @Override
    public void init() {
        Fi configFile = dataDirectory.child(configFileName);
        if (configFile.exists()) {
            try {
                config = gson.fromJson(configFile.reader(), Config.class);
                Log.info("[Hub] Конфигурация загружена. (@)", configFile.absolutePath());
            } catch (Exception e) {
                Log.err("[Hub] Файл конфигурации не загружен.");
                Log.err(e);
            }
        } else {
            configFile.writeString(gson.toJson(config = new Config()));
            Log.info("[Hub] Файл конфигурации сгенерирован. (@)", configFile.absolutePath());
        }

        Events.on(TapEvent.class, event -> teleport(event.player, event.tile));

        Events.run(Trigger.update, () -> {
            if (interval.get(teleportUpdateInterval)) Groups.player.each(this::teleport);
        });

        Events.on(PlayerJoin.class, event -> config.servers.forEach(data -> {
            net.pingHost(data.ip, data.port, host -> {
                Call.label(event.player.con, host.name, refreshDuration, data.titleX, data.titleY);
                Call.label(event.player.con, Bundle.format("onlinePattern", findLocale(event.player), host.players, host.mapname), refreshDuration, data.labelX, data.labelY);
            }, e -> Call.label(event.player.con(), Bundle.format("offlinePattern", findLocale(event.player)), refreshDuration, data.labelX, data.labelY));
        }));

        Timer.schedule(() -> {
            CompletableFuture<?>[] tasks = config.servers.stream().map(data -> CompletableFuture.runAsync(() -> {
                net.pingHost(data.ip, data.port, host -> {
                    counter.addAndGet(host.players);
                    Groups.player.each(player -> {
                        Call.label(player.con, host.name, refreshDuration, data.titleX, data.titleY);
                        Call.label(player.con, Bundle.format("onlinePattern", findLocale(player), host.players, host.mapname), refreshDuration, data.labelX, data.labelY);
                    });
                }, e -> Groups.player.each(player -> Call.label(player.con, Bundle.format("offlinePattern", findLocale(player)), refreshDuration, data.labelX, data.labelY)));
            })).toArray(CompletableFuture<?>[]::new);

            CompletableFuture.allOf(tasks).thenRun(() -> {
                counter.addAndGet(Groups.player.size());
                Core.settings.put("totalPlayers", counter.get());
                counter.set(0);
            }).join();
        }, delaySeconds, refreshDuration);

        netServer.admins.addActionFilter(action -> false);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("reload-hub", "Load Hub configuration file.", args -> {
            try {
                config = gson.fromJson(dataDirectory.child(configFileName).readString(), Config.class);
                Log.info("[Hub] Файл конфигурации успешно перезагружен.");
            } catch (Exception e) {
                Log.err("[Hub] Файл конфигурации не загружен.");
                Log.err(e);
            }
        });
    }
}
