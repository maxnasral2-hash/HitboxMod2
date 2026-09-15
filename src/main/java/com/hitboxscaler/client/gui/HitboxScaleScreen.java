package com.hitboxscaler.client.gui;

import com.hitboxscaler.HitboxScaleManager;
import com.hitboxscaler.client.HitboxScalerClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class HitboxScaleScreen extends Screen {

    private static final float[] PRESETS = {0.9f, 1.0f, 1.1f, 1.2f};

    private float scale;
    private ScaleSlider slider;

    public HitboxScaleScreen(float initialScale) {
        super(Text.literal("Размер хитбокса"));
        this.scale = HitboxScaleManager.clamp(initialScale);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 50;

        // Кнопки-пресеты в один ряд
        int buttonWidth = 60;
        int spacing = 6;
        int totalWidth = PRESETS.length * buttonWidth + (PRESETS.length - 1) * spacing;
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < PRESETS.length; i++) {
            float value = PRESETS[i];
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(formatScale(value) + "x"),
                    button -> applyScale(value)
            ).dimensions(startX + i * (buttonWidth + spacing), top, buttonWidth, 20).build());
        }

        // Ползунок для точной настройки от 0.5x до 2.0x
        this.slider = new ScaleSlider(centerX - 100, top + 30, 200, 20, this.scale);
        this.addDrawableChild(this.slider);

        // Кнопка подтверждения точного значения со слайдера
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Применить"),
                button -> applyScale(this.slider.getScaleValue())
        ).dimensions(centerX - 50, top + 60, 100, 20).build());

        // Кнопка сброса на 1.0x
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Сбросить (1.0x)"),
                button -> applyScale(1.0f)
        ).dimensions(centerX - 75, top + 90, 150, 20).build());
    }

    private void applyScale(float value) {
        this.scale = HitboxScaleManager.clamp(value);
        HitboxScalerClient.sendScaleToServer(this.scale);
        this.close();
    }

    private static String formatScale(float value) {
        return (value == Math.floor(value)) ? String.valueOf((int) value) + ".0" : String.valueOf(value);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 70, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Простой слайдер от MIN_SCALE до MAX_SCALE с шагом 0.05. */
    private static class ScaleSlider extends SliderWidget {

        ScaleSlider(int x, int y, int width, int height, float initialScale) {
            super(x, y, width, height, Text.empty(),
                    toSliderProgress(initialScale));
            this.updateMessage();
        }

        private static double toSliderProgress(float scale) {
            return (scale - HitboxScaleManager.MIN_SCALE) / (HitboxScaleManager.MAX_SCALE - HitboxScaleManager.MIN_SCALE);
        }

        float getScaleValue() {
            float raw = HitboxScaleManager.MIN_SCALE
                    + (float) this.value * (HitboxScaleManager.MAX_SCALE - HitboxScaleManager.MIN_SCALE);
            // округляем до сотых, чтобы не было 1.0000001x
            return Math.round(raw * 100f) / 100f;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal("Масштаб: " + getScaleValue() + "x"));
        }

        @Override
        protected void applyValue() {
            // значение уже применяется через getScaleValue() при нажатии "Применить"
        }
    }
}
