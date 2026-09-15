package com.hitboxscaler.networking;

import java.util.UUID;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет "сервер -> клиент": сообщает всем клиентам, какой масштаб хитбокса
 * сейчас у игрока с данным UUID. Рассылается:
 *  - всем игрокам сразу после того, как кто-то поменял свой масштаб (включая
 *    самого отправителя, чтобы его собственный клиент тоже обновил локальную
 *    карту HitboxScaleManager);
 *  - только что подключившемуся игроку — по одному пакету на каждого игрока,
 *    у которого масштаб отличается от 1.0x, чтобы он сразу увидел актуальную
 *    картину, а не только события, произошедшие после его входа.
 */
public record HitboxScaleSyncPayload(UUID playerUuid, float scale) implements CustomPayload {

    public static final CustomPayload.Id<HitboxScaleSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("hitboxscaler", "sync_scale"));

    public static final PacketCodec<RegistryByteBuf, HitboxScaleSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.UUID, HitboxScaleSyncPayload::playerUuid,
                    PacketCodecs.FLOAT, HitboxScaleSyncPayload::scale,
                    HitboxScaleSyncPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
