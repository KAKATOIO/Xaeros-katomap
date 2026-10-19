package com.katomap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Tela da maquete 3D. */
public class KatomapScreen extends Screen {
    private final HeightRenderer renderer = new HeightRenderer();

    private float yaw = 0.7f, pitch = 0.95f, dist = 140f, vs = 1.0f;
    private float tx, tz;
    private int fineCell = 3;           // tamanho (px da GUI) de cada célula em repouso
    private long interactUntil = 0;
    private int[] buf = new int[0];
    private int bufW, bufH, bufCell = -1;
    private boolean dirty = true;
    private boolean centered = false;
    private int tickCount = 0;

    public KatomapScreen() {
        super(Component.translatable("katomap.title"));
    }

    @Override
    protected void init() {
        if (!centered) { recenter(); centered = true; }     // init() também roda ao redimensionar a janela
        KatomapData.scanAround(this.minecraft, 16);
        rebuildGrid();
    }

    private void recenter() {
        LocalPlayer p = this.minecraft.player;
        if (p != null) { tx = (float) p.getX(); tz = (float) p.getZ(); }
        dirty = true;
    }

    private void rebuildGrid() {
        if (this.minecraft.level == null) return;
        renderer.rebuild(KatomapData.current(this.minecraft.level), (int) Math.floor(tx), (int) Math.floor(tz));
        dirty = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCount % 40 == 0) {
            KatomapData.scanAround(this.minecraft, 16);
            rebuildGrid();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void touch() {
        interactUntil = System.currentTimeMillis() + 220;
        dirty = true;
        if (Math.abs(tx - renderer.centerX()) > 100 || Math.abs(tz - renderer.centerZ()) > 100) rebuildGrid();
    }

    private String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        boolean interacting = System.currentTimeMillis() < interactUntil;
        int cell = interacting ? fineCell * 2 : fineCell;
        if (cell != bufCell) { bufCell = cell; dirty = true; }

        int w = Math.max(1, this.width / cell), h = Math.max(1, this.height / cell);
        if (w != bufW || h != bufH) { buf = new int[w * h]; bufW = w; bufH = h; dirty = true; }
        if (dirty && renderer.hasGrid) {
            renderer.render(buf, w, h, yaw, pitch, dist, tx, tz, vs);
            dirty = false;
        }

        g.fill(0, 0, this.width, this.height, 0xFF000000);
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                int c = buf[y * w + x];
                int run = 1;
                while (x + run < w && buf[y * w + x + run] == c) run++;
                g.fill(x * cell, y * cell, (x + run) * cell, (y + 1) * cell, c);
                x += run;
            }
        }

        // marcador do jogador
        LocalPlayer p = this.minecraft.player;
        if (p != null && renderer.hasGrid) {
            float[] s = renderer.project((float) p.getX(), (float) (p.getY() + 1.0) * vs, (float) p.getZ());
            if (s != null && s[0] >= 0 && s[0] <= 1 && s[1] >= 0 && s[1] <= 1) {
                int sx = (int) (s[0] * this.width), sy = (int) (s[1] * this.height);
                g.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
                g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFF3030);
            }
        }

        drawCompass(g);

        // painel de ajuda
        g.fill(0, 0, this.width, 42, 0x99000000);
        g.text(this.font, tr("katomap.title"), 6, 5, 0xFFFFFFFF);
        g.text(this.font, tr("katomap.help1"), 6, 17, 0xFFC8C8C8);
        g.text(this.font, tr("katomap.help2", String.format("%.2f", vs)), 6, 29, 0xFFC8C8C8);

        if (!renderer.hasData) {
            g.centeredText(this.font, tr("katomap.nodata"), this.width / 2, this.height / 2, 0xFFFFFF55);
        } else if (mouseY > 42 && !interacting) {
            int[] hit = renderer.pick((float) mouseX / this.width, (float) mouseY / this.height);
            if (hit != null) {
                String s = tr("katomap.coords", hit[0], hit[1], hit[2]);
                int tw = this.font.width(s);
                int bx = Math.min(mouseX + 10, this.width - tw - 8), by = Math.min(mouseY + 10, this.height - 16);
                g.fill(bx - 3, by - 3, bx + tw + 3, by + 11, 0xCC000000);
                g.text(this.font, s, bx, by, 0xFFFFFFFF);
            }
        }
    }

    /** Bússola no canto: N/L/S/O girando conforme a câmera. */
    private void drawCompass(GuiGraphicsExtractor g) {
        int cx = this.width - 34, cy = 74, r = 20;
        g.fill(cx - 28, cy - 28, cx + 28, cy + 28, 0x66000000);
        float sy = (float) Math.sin(yaw), cyw = (float) Math.cos(yaw);
        // norte = -Z ; leste = +X ; direita da tela = (cos, -sin) ; "frente" horizontal = (-sin, -cos)
        label(g, "N", cx + r * sy, cy - r * cyw, 0xFFFF5555);
        label(g, "S", cx - r * sy, cy + r * cyw, 0xFFFFFFFF);
        label(g, tr("katomap.east"), cx + r * cyw, cy + r * sy, 0xFFFFFFFF);
        label(g, tr("katomap.west"), cx - r * cyw, cy - r * sy, 0xFFFFFFFF);
    }

    private void label(GuiGraphicsExtractor g, String s, float x, float y, int color) {
        g.text(this.font, s, (int) x - this.font.width(s) / 2, (int) y - 4, color);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0) {
            yaw -= (float) dx * 0.01f;
            pitch = Mth.clamp(pitch + (float) dy * 0.01f, 0.12f, 1.55f);
        } else {
            float k = dist * 0.0022f;
            float sy = (float) Math.sin(yaw), cy = (float) Math.cos(yaw);
            float fwdX = -sy, fwdZ = -cy;           // frente horizontal da câmera
            float rightX = cy, rightZ = -sy;
            tx += (-rightX * (float) dx + fwdX * (float) dy) * k;
            tz += (-rightZ * (float) dx + fwdZ * (float) dy) * k;
        }
        touch();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        dist = Mth.clamp(dist * (float) Math.pow(0.88, scrollY), 8f, 450f);
        touch();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        float move = dist * 0.06f;
        float sy = (float) Math.sin(yaw), cy = (float) Math.cos(yaw);
        switch (event.key()) {
            case GLFW.GLFW_KEY_W -> { tx -= sy * move; tz -= cy * move; }
            case GLFW.GLFW_KEY_S -> { tx += sy * move; tz += cy * move; }
            case GLFW.GLFW_KEY_A -> { tx -= cy * move; tz += sy * move; }
            case GLFW.GLFW_KEY_D -> { tx += cy * move; tz -= sy * move; }
            case GLFW.GLFW_KEY_Q -> yaw -= 0.12f;
            case GLFW.GLFW_KEY_E -> yaw += 0.12f;
            case GLFW.GLFW_KEY_R -> { recenter(); rebuildGrid(); }
            case GLFW.GLFW_KEY_PAGE_UP -> vs = Math.min(4f, vs + 0.25f);
            case GLFW.GLFW_KEY_PAGE_DOWN -> vs = Math.max(0.25f, vs - 0.25f);
            case GLFW.GLFW_KEY_1 -> fineCell = 4;
            case GLFW.GLFW_KEY_2 -> fineCell = 3;
            case GLFW.GLFW_KEY_3 -> fineCell = 2;
            default -> { return super.keyPressed(event); }
        }
        touch();
        return true;
    }
}
