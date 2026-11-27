package com.amethyst.client.gui;

import com.amethyst.client.module.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static final int W = 120, H = 14;
    private static final Map<Module.Category, int[]> panelPos = new EnumMap<>(Module.Category.class);
    private final List<Hit> hits = new ArrayList<>();
    private Module.Category draggingPanel;
    private int dragOffX, dragOffY;
    private Hit slider;

    private record Hit(int x, int y, int w, int h, Module module, BoolSetting bool, NumSetting num, Module.Category panel) {
        boolean contains(double mx, double my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    }

    public ClickGuiScreen() {
        super(Text.literal("AmethystClient"));
        if (panelPos.isEmpty()) {
            int x = 20;
            for (Module.Category c : Module.Category.values()) {
                panelPos.put(c, new int[]{x, 20});
                x += W + 10;
            }
        }
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x66000000);
        ctx.drawTextWithShadow(textRenderer, "AmethystClient", 4, height - 12, 0xFFB388FF);
        hits.clear();

        for (Module.Category cat : Module.Category.values()) {
            int[] p = panelPos.get(cat);
            int y = p[1];

            ctx.fill(p[0], y, p[0] + W, y + H, 0xFF6A1B9A);
            ctx.drawTextWithShadow(textRenderer, cat.name(), p[0] + 4, y + 3, 0xFFFFFFFF);
            hits.add(new Hit(p[0], y, W, H, null, null, null, cat));
            y += H;

            for (Module m : ModuleManager.modules) {
                if (m.category != cat) continue;
                ctx.fill(p[0], y, p[0] + W, y + H, m.isEnabled() ? 0xFF8E24AA : 0xFF212121);
                ctx.drawTextWithShadow(textRenderer, m.name, p[0] + 4, y + 3, 0xFFFFFFFF);
                hits.add(new Hit(p[0], y, W, H, m, null, null, null));
                y += H;

                if (!m.expanded) continue;

                for (BoolSetting b : m.bools) {
                    ctx.fill(p[0], y, p[0] + W, y + H, 0xFF303030);
                    ctx.drawTextWithShadow(textRenderer, "  " + b.name + ": " + (b.value ? "ON" : "OFF"),
                        p[0] + 4, y + 3, b.value ? 0xFF80FF80 : 0xFFFF8080);
                    hits.add(new Hit(p[0], y, W, H, null, b, null, null));
                    y += H;
                }
                for (NumSetting n : m.nums) {
                    ctx.fill(p[0], y, p[0] + W, y + H, 0xFF303030);
                    double frac = (n.value - n.min) / (n.max - n.min);
                    ctx.fill(p[0], y, p[0] + (int) (W * frac), y + H, 0xFF5E35B1);
                    ctx.drawTextWithShadow(textRenderer, String.format("  %s: %.2f", n.name, n.value),
                        p[0] + 4, y + 3, 0xFFFFFFFF);
                    hits.add(new Hit(p[0], y, W, H, null, null, n, null));
                    y += H;
                }
                if (m instanceof SusChunk) {
                    var top = SusChunk.top(6);
                    if (top.isEmpty()) {
                        ctx.fill(p[0], y, p[0] + W, y + H, 0xFF303030);
                        ctx.drawTextWithShadow(textRenderer, "  no sus chunks", p[0] + 4, y + 3, 0xFFAAAAAA);
                        y += H;
                    }
                    for (var e : top) {
                        ctx.fill(p[0], y, p[0] + W, y + H, 0xFF303030);
                        ctx.drawTextWithShadow(textRenderer,
                            String.format("  %d,%d  [%.0f]", e.getKey().getStartX(), e.getKey().getStartZ(), e.getValue()),
                            p[0] + 4, y + 3, 0xFFFFD54F);
                        y += H;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();

        for (Hit h : hits) {
            if (!h.contains(mx, my)) continue;
            if (h.panel != null && button == 0) {
                draggingPanel = h.panel;
                int[] p = panelPos.get(h.panel);
                dragOffX = (int) mx - p[0];
                dragOffY = (int) my - p[1];
            } else if (h.module != null) {
                if (button == 0) h.module.toggle();
                else if (button == 1) h.module.expanded = !h.module.expanded;
            } else if (h.bool != null) {
                h.bool.value = !h.bool.value;
            } else if (h.num != null) {
                slider = h;
                setSlider(mx);
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingPanel != null) {
            int[] p = panelPos.get(draggingPanel);
            p[0] = (int) click.x() - dragOffX;
            p[1] = (int) click.y() - dragOffY;
            return true;
        }
        if (slider != null) { setSlider(click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        slider = null;
        return super.mouseReleased(click);
    }

    private void setSlider(double mx) {
        double frac = MathHelper.clamp((mx - slider.x()) / slider.w(), 0, 1);
        NumSetting n = slider.num();
        n.value = n.min + frac * (n.max - n.min);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(input);
    }
}
