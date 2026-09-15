package com.hitboxscaler;

import java.util.Map;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.hitboxscaler.networking.HitboxScalePayload;
import com.hitboxscaler.networking.HitboxScaleSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Общая точка входа мода (выполняется и на клиенте, и на сервере).
 * Здесь регистрируются типы пакетов в обе стороны:
 *  - C2S (HitboxScalePayload): игрок сообщает серверу свой новый масштаб;
 *  - S2C (HitboxScaleSyncPayload): сервер рассылает всем клиентам (включая
 *    самого отправителя), какой масштаб сейчас у конкретного игрока.
 *
 * Без S2C-рассылки мод работал только в синглплеере, потому что там клиент
 * и встроенный сервер — один процесс с одной и той же статической картой
 * HitboxScaleManager. На выделенном сервере это два разных JVM, и без
 * обратного пакета остальные клиенты (и клиент самого игрока) никогда не
 * узнавали о новом значении.
 */
public class HitboxScaler implements ModInitializer {

    public static final String MOD_ID = "hitboxscaler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Регистрируем оба типа payload'ов — и клиент->сервер, и сервер->клиент
        PayloadTypeRegistry.playC2S().register(HitboxScalePayload.ID, HitboxScalePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HitboxScaleSyncPayload.ID, HitboxScaleSyncPayload.CODEC);

        // Игрок прислал новый масштаб — запоминаем его на сервере
        // и тут же рассылаем всем онлайн-игрокам (включая отправителя),
        // чтобы их клиентские EntityDimensionsMixin увидели актуальное значение.
        ServerPlayNetworking.registerGlobalReceiver(HitboxScalePayload.ID, (payload, context) -> {
            float scale = HitboxScaleManager.clamp(payload.scale());
            UUID uuid = context.player().getUuid();
            MinecraftServer server = context.player().getServer();
            server.execute(() -> {
                HitboxScaleManager.setScale(uuid, scale);
                broadcastScale(server, uuid, scale);
            });
        });

        // Игрок зашёл на сервер — отправляем ему состояние всех, у кого масштаб
        // отличается от 1.0x, чтобы он сразу видел актуальную картину.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity joined = handler.getPlayer();
            for (Map.Entry<UUID, Float> entry : HitboxScaleManager.snapshot().entrySet()) {
                ServerPlayNetworking.send(joined, new HitboxScaleSyncPayload(entry.getKey(), entry.getValue()));
            }
        });

        // Игрок отключился — можно забыть его масштаб, чтобы карта не росла бесконечно
        // и новый игрок с переиспользованным UUID (например, на тестовом сервере) не
        // унаследовал чужое значение.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                HitboxScaleManager.clear(handler.getPlayer().getUuid()));

        LOGGER.info("HitboxScaler загружен");
    }

    private static void broadcastScale(MinecraftServer server, UUID uuid, float scale) {
        HitboxScaleSyncPayload syncPayload = new HitboxScaleSyncPayload(uuid, scale);
        for (ServerPlayerEntity recipient : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(recipient, syncPayload);
        }
    }
}
