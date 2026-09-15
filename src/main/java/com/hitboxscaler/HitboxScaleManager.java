package com.hitboxscaler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Общий (common) класс — используется и мод-мискином, и сетевым обработчиком.
 * Хранит текущий множитель хитбокса для каждого игрока по UUID.
 *
 * Это статическое хранилище: на выделенном (dedicated) сервере оно живёт
 * в серверном процессе и заполняется через сетевые пакеты от клиентов.
 * В одиночной игре (singleplayer) клиент и встроенный сервер работают
 * в одном и том же JVM/classloader, поэтому пакет всё равно доходит
 * до этого же класса и всё работает одинаково в обоих случаях.
 */
public final class HitboxScaleManager {

    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.0f;

    private static final Map<UUID, Float> SCALES = new ConcurrentHashMap<>();

    private HitboxScaleManager() {
    }

    public static float getScale(UUID playerUuid) {
        return SCALES.getOrDefault(playerUuid, 1.0f);
    }

    public static void setScale(UUID playerUuid, float scale) {
        scale = clamp(scale);
        if (scale == 1.0f) {
            SCALES.remove(playerUuid);
        } else {
            SCALES.put(playerUuid, scale);
        }
    }

    public static float clamp(float scale) {
        if (Float.isNaN(scale)) {
            return 1.0f;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /** Неизменяемый снимок всех сейчас применённых (не равных 1.0) масштабов. */
    public static Map<UUID, Float> snapshot() {
        return Map.copyOf(SCALES);
    }

    /** Убирает запись, когда она больше не нужна (например, игрок отключился). */
    public static void clear(UUID playerUuid) {
        SCALES.remove(playerUuid);
    }
}
