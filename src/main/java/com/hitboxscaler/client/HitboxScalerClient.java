package com.hitboxscaler.client;

import com.hitboxscaler.HitboxScaleManager;
import com.hitboxscaler.client.gui.HitboxScaleScreen;
import com.hitboxscaler.networking.HitboxScalePayload;
import com.hitboxscaler.networking.HitboxScaleSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HitboxScalerClient implements ClientModInitializer {

    // Текущий выбранный масштаб на клиенте (для мгновенного локального отклика GUI)
    public static float currentScale = 1.0f;

    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        // Типы пакетов уже зарегистрированы в общем классе HitboxScaler (метод
        // onInitialize), который вызывается и на клиенте, и на сервере —
        // повторно регистрировать не нужно.

        // Сервер рассылает актуальный масштаб для UUID (своего, чужого игрока,
        // или при входе на сервер — сразу для всех, у кого он не 1.0x).
        // Именно этого обработчика не хватало раньше: клиентский
        // EntityDimensionsMixin читает значения из HitboxScaleManager, а без
        // этого пакета эта карта на клиенте всегда оставалась пустой в мультиплеере.
        ClientPlayNetworking.registerGlobalReceiver(HitboxScaleSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> HitboxScaleManager.setScale(payload.playerUuid(), payload.scale())));

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitboxscaler.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, // по умолчанию клавиша H, можно переназначить в настройках управления
                "category.hitboxscaler"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new HitboxScaleScreen(currentScale));
                }
            }
        });
    }

    /** Отправляет выбранный масштаб на сервер (и на локальный интегрированный сервер в одиночной игре). */
    public static void sendScaleToServer(float scale) {
        currentScale = scale;
        if (ClientPlayNetworking.canSend(HitboxScalePayload.ID)) {
            ClientPlayNetworking.send(new HitboxScalePayload(scale));
            // Обновляем локально сразу же, не дожидаясь, пока сервер пришлёт
            // пакет обратно — иначе между кликом и ответом сервера был бы
            // заметный лаг в собственной визуализации/расчётах игрока.
            // Сервер всё равно пришлёт HitboxScaleSyncPayload следом и
            // подтвердит то же самое значение (или своё clamp-нутое, если
            // оно отличается).
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                HitboxScaleManager.setScale(client.player.getUuid(), scale);
            }
        }
    }
}
