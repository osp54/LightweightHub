package inside;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Timer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.ServerLoadEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static mindustry.Vars.*;

public class LightweightHub extends Plugin {
    public static Config config;

    private final Interval interval = new Interval();
    private final AtomicInteger counter = new AtomicInteger();
    private final Seq<Timer.Task> tasks = new Seq<>();

    public final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    public void teleport(Player player){
        teleport(player, null);
    }

    public void teleport(final Player player, Tile tile) {
        for (HostData data : config.servers) {
            if (data.inDiapason(tile != null ? tile.x : player.tileX(), tile != null ? tile.y : player.tileY())) {
                net.pingHost(data.ip, data.port, host -> Call.connect(player.con, data.ip, data.port), e -> {});
            }
        }
    }

    @Override
    public void init() {

        // Заменяем стандартного юнита на Поли
        ((CoreBlock)Blocks.coreNucleus).unitType = UnitTypes.poly;

        Fi cfg = dataDirectory.child("config-hub.json");
        if (!cfg.exists()) {
            cfg.writeString(gson.toJson(config = new Config()));
            Log.info("Файл конфигурации сгенерирован... (@)", cfg.absolutePath());
        } else {
            try {
                config = gson.fromJson(cfg.reader(), Config.class);
            } catch(Exception e) {
                Log.err("Ошибка загрузки файла конфигурации. Что-то не так с форматом json.");
                Log.err(e);
            }
        }

        Events.on(ServerLoadEvent.class, event -> netServer.admins.addActionFilter(action -> false));

        Events.on(TapEvent.class, event -> teleport(event.player, event.tile));

        Events.run(Trigger.update, () -> {
            if (interval.get(60 * 0.15f)) {
                Groups.player.each(this::teleport);
            }
        });

        Events.on(PlayerJoin.class, event -> {
            for (HostData data : config.servers) {
                Call.label(event.player.con(), data.title, 10f, data.titleX, data.titleY);
                net.pingHost(data.ip, data.port, host -> Call.label(event.player.con(), Bundle.format("onlinePattern", Bundle.findLocale(event.player), host.players, host.mapname), 10f, data.labelX, data.labelY),
                        e -> Call.label(event.player.con(), Bundle.format("offlinePattern", Bundle.findLocale(event.player)), 10f, data.labelX, data.labelY));
            }
        });

        Timer.schedule(() -> {
            CompletableFuture<?>[] tasks = config.servers.stream()
                    .map(data -> CompletableFuture.runAsync(() -> {

                        Core.app.post(() -> Call.label(data.title, 5f, data.titleX, data.titleY));
                        net.pingHost(data.ip, data.port, host -> {
                            counter.addAndGet(host.players);
                            Groups.player.each(player -> Call.label(player.con, Bundle.format("onlinePattern", Bundle.findLocale(player), host.players, host.mapname), 5f, data.labelX, data.labelY));
                        }, e -> Groups.player.each(player -> Call.label(player.con, Bundle.format("offlinePattern", Bundle.findLocale(player)), 5f, data.labelX, data.labelY)));
                    }))
                    .toArray(CompletableFuture<?>[]::new);

            CompletableFuture.allOf(tasks).thenRun(() -> {
                counter.addAndGet(Groups.player.size());
                Core.settings.put("totalPlayers", counter.get());
                counter.set(0);
            }).join();
        }, 1.5f, 5f);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {

        handler.register("reload-hub", "Перезапустить файл конфигурации.", args -> {
            try {
                tasks.each(Timer.Task::cancel);
                config = gson.fromJson(dataDirectory.child("config-hub.json").readString(), Config.class);
                Log.info("Успешно перезагружено.");
            } catch(Exception e) {
                Log.err("Ошибка загрузки файла config-hub.json.");
                Log.err(e);
            }
        });
    }
}
