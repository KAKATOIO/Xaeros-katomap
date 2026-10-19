package com.kaka.xaerokaka;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Tela da maquete 3D. */
public class KakaMapScreen extends Screen {
    private final HeightRenderer renderer = new HeightRenderer();

    private float yaw = 0.7f, pitch = 0.95f, dist = 140f, vs = 1.0f;
    private float tx, tz;
    private int fineCell = 3;           // tamanho (px GUI) de cada célula em repouso
    private long interactUntil = 0;
    private int[] buf = new int[0];
    private int bw, bh, bufCell = -1;
    private boolean dirty = true;
    private int tickCount = 0;

    public KakaMapScreen() {
        super(Component.literal("Xaero kaka"));
    }

    @Override
    protected void init() {
        recenter();
        KakaData.scanAround(this.minecraft, 16);
        rebuildGrid();
    }

    private void recenter() {
        LocalPlayer p = this.minecraft.player;
        if (p != null) { tx = (float) p.getX(); tz = (float) p.getZ(); }
        dirty = true;
    }

    private void rebuildGrid() {
        if (this.minecraft.level == null) return;
        renderer.rebuild(KakaData.current(this.minecraft.level), (int) Math.floor(tx), (int) Math.floor(tz));
        dirty = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCount % 40 == 0) {
            KakaData.scanAround(this.minecraft, 16);
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        boolean interacting = System.currentTimeMillis() < interactUntil;
        int cell = interacting ? fineCell * 2 : fineCell;
        if (cell != bufCell) { bufCell = cell; dirty = true; }
        if (!interacting && !dirty) { /* nada a recalcular */ }

        int w = Math.max(1, this.width / cell), h = Math.max(1, this.height / cell);
        if (buf.length != w * h) { buf = new int[w * h]; bw = w; bh = h; dirty = true; }
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
            if (s != null) {
                int sx = (int) (s[0] * this.width), sy = (int) (s[1] * this.height);
                g.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
                g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFF3030);
            }
        }

        g.text(this.font, "Xaero kaka 3D", 6, 6, 0xFFFFFFFF);
        g.text(this.font, "Esquerdo: girar | Direito/Meio: mover | Scroll: zoom | WASD: mover | Q/E: girar", 6, 18, 0xFFC8C8C8);
        g.text(this.font, "R: centralizar | PgUp/PgDn: altura x" + String.format("%.1f", vs) + " | 1/2/3: qualidade | Esc: sair", 6, 30, 0xFFC8C8C8);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int b = event.button();
        if (b == 0) {
            yaw -= (float) dx * 0.01f;
            pitch = Mth.clamp(pitch + (float) dy * 0.01f, 0.12f, 1.55f);
        } else {
            float k = dist * 0.0022f;
            float sy = (float) Math.sin(yaw), cy = (float) Math.cos(yaw);
            float fwdX = -sy, fwdZ = -cy;          // direção horizontal da câmera
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
            case GLFW.GLFW_KEY_1 -> { fineCell = 4; }
            case GLFW.GLFW_KEY_2 -> { fineCell = 3; }
            case GLFW.GLFW_KEY_3 -> { fineCell = 2; }
            default -> { return super.keyPressed(event); }
        }
        touch();
        return true;
    }
}
