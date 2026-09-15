package com.hitboxscaler.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет "клиент -> сервер": игрок выбрал новый масштаб хитбокса.
 */
public record HitboxScalePayload(float scale) implements CustomPayload {

    public static final CustomPayload.Id<HitboxScalePayload> ID =
            new CustomPayload.Id<>(Identifier.of("hitboxscaler", "set_scale"));

    public static final PacketCodec<RegistryByteBuf, HitboxScalePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, HitboxScalePayload::scale,
                    HitboxScalePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
