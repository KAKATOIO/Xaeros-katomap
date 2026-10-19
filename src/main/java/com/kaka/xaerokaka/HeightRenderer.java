package com.kaka.xaerokaka;

import java.util.Map;

/** Renderizador por software (ray-marching de campo de altura) com câmera orbital. */
public final class HeightRenderer {
    public static final int G = 512;           // lado da grade (blocos)
    private static final int NODATA = Integer.MIN_VALUE;

    private final int[] H = new int[G * G];
    private final int[] C = new int[G * G];
    private int x0, z0;                        // canto da grade em coordenadas do mundo
    private int maxH = 64, baseY = 0;
    public boolean hasGrid = false;

    // câmera da última renderização (para projetar marcadores)
    private float camX, camY, camZ, fx, fy, fz, rx, rz, ux, uy, uz, tanH, aspect;

    public int centerX() { return x0 + G / 2; }
    public int centerZ() { return z0 + G / 2; }

    public void rebuild(Map<Long, KakaData.ChunkData> map, int cx, int cz) {
        x0 = cx - G / 2;
        z0 = cz - G / 2;
        java.util.Arrays.fill(H, NODATA);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int ccx = x0 >> 4; ccx <= (x0 + G - 1) >> 4; ccx++) {
            for (int ccz = z0 >> 4; ccz <= (z0 + G - 1) >> 4; ccz++) {
                KakaData.ChunkData d = map.get(KakaData.key(ccx, ccz));
                if (d == null) continue;
                for (int lz = 0; lz < 16; lz++) {
                    int gz = ccz * 16 + lz - z0;
                    if (gz < 0 || gz >= G) continue;
                    for (int lx = 0; lx < 16; lx++) {
                        int gx = ccx * 16 + lx - x0;
                        if (gx < 0 || gx >= G) continue;
                        int h = d.h[lz * 16 + lx];
                        H[gz * G + gx] = h;
                        C[gz * G + gx] = d.c[lz * 16 + lx];
                        if (h < min) min = h;
                        if (h > max) max = h;
                    }
                }
            }
        }
        if (min == Integer.MAX_VALUE) { min = 60; max = 70; }
        maxH = max + 1;
        baseY = min - 6;
        hasGrid = true;
    }

    public int heightAt(int wx, int wz) {
        int gx = wx - x0, gz = wz - z0;
        if (gx < 0 || gz < 0 || gx >= G || gz >= G) return baseY;
        int h = H[gz * G + gx];
        return h == NODATA ? baseY : h;
    }

    private float hs(int idx, float vs, int fallback) {
        int h = H[idx];
        return (h == NODATA ? fallback : h) * vs;
    }

    public void render(int[] out, int w, int h, float yaw, float pitch, float dist,
                       float tx, float tz, float vs) {
        float ty = heightAt((int) Math.floor(tx), (int) Math.floor(tz)) * vs;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float sy = (float) Math.sin(yaw), cy = (float) Math.cos(yaw);
        camX = tx + dist * cp * sy;
        camY = ty + dist * sp;
        camZ = tz + dist * cp * cy;
        fx = (tx - camX) / dist; fy = (ty - camY) / dist; fz = (tz - camZ) / dist;
        float rl = (float) Math.sqrt(fz * fz + fx * fx);
        rx = -fz / rl; rz = fx / rl;
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
        tanH = (float) Math.tan(Math.toRadians(35));
        aspect = (float) w / h;

        float maxY = maxH * vs;
        float maxDist = dist * 3f + 300f;
        float baseV = baseY * vs;
        final float lx = -0.45f, ly = 0.8f, lz = -0.4f;
        float ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);

        for (int py = 0; py < h; py++) {
            float v = (1f - 2f * (py + 0.5f) / h) * tanH;
            float skyT = (float) py / h;
            int sky = lerpColor(0x24385e, 0x8fb0d8, skyT);
            for (int px = 0; px < w; px++) {
                float u = (2f * (px + 0.5f) / w - 1f) * tanH * aspect;
                float dx = fx + u * rx + v * ux;
                float dy = fy + v * uy;
                float dz = fz + u * rz + v * uz;
                float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= dl; dy /= dl; dz /= dl;

                int color = sky;
                float t = 0f;
                if (camY > maxY) {
                    if (dy >= 0f) { out[py * w + px] = sky; continue; }
                    t = (camY - maxY) / -dy;
                }
                float prevT = t;
                for (int i = 0; i < 700 && t < maxDist; i++) {
                    float x = camX + dx * t, y = camY + dy * t, z = camZ + dz * t;
                    int gx = (int) Math.floor(x) - x0, gz = (int) Math.floor(z) - z0;
                    if (gx < 0 || gz < 0 || gx >= G || gz >= G) break;
                    int idx = gz * G + gx;
                    float yh = hs(idx, vs, baseY);
                    if (y < yh) {
                        // refinamento por bisseção
                        float lo = prevT, hi = t;
                        for (int k = 0; k < 5; k++) {
                            float mid = (lo + hi) * 0.5f;
                            float mx = camX + dx * mid, my = camY + dy * mid, mz = camZ + dz * mid;
                            int mgx = (int) Math.floor(mx) - x0, mgz = (int) Math.floor(mz) - z0;
                            if (mgx < 0 || mgz < 0 || mgx >= G || mgz >= G) { hi = mid; continue; }
                            if (my < hs(mgz * G + mgx, vs, baseY)) hi = mid; else lo = mid;
                        }
                        float hx = camX + dx * hi, hy = camY + dy * hi, hz = camZ + dz * hi;
                        gx = (int) Math.floor(hx) - x0; gz = (int) Math.floor(hz) - z0;
                        if (gx < 0 || gz < 0 || gx >= G || gz >= G) break;
                        idx = gz * G + gx;
                        float top = hs(idx, vs, baseY);
                        int base = H[idx] == NODATA ? 0xFF16181d : C[idx];
                        float cl = hs(Math.max(0, idx - 1), vs, baseY), cr = hs(Math.min(G * G - 1, idx + 1), vs, baseY);
                        float cu = hs(Math.max(0, idx - G), vs, baseY), cd = hs(Math.min(G * G - 1, idx + G), vs, baseY);
                        float nx = (cl - cr) * 0.5f, nz = (cu - cd) * 0.5f, ny = 1.4f;
                        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        float ndl = Math.max(0f, (nx * lx + ny * ly + nz * lz) / (nl * ll));
                        float shade = 0.55f + 0.6f * ndl;
                        if (top - hy > 0.45f * vs + 0.3f) shade *= 0.72f; // face lateral da "coluna"
                        float fog = Math.min(1f, t / maxDist);
                        fog = fog * fog * 0.65f;
                        color = shadeAndFog(base, shade, sky, fog);
                        break;
                    }
                    float step = Math.max(0.7f, Math.min(6f, (y - yh) * 0.45f));
                    step = Math.max(step, t * 0.004f);
                    prevT = t;
                    t += step;
                }
                out[py * w + px] = color;
            }
        }
    }

    /** Projeta um ponto do mundo para coordenadas normalizadas 0..1 da tela; null se atrás da câmera. */
    public float[] project(float wx, float wy, float wz) {
        float dx = wx - camX, dy = wy - camY, dz = wz - camZ;
        float zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.1f) return null;
        float sx = (dx * rx + dz * rz) / (zc * tanH * aspect);
        float sy = (dx * ux + dy * uy + dz * uz) / (zc * tanH);
        return new float[] { (sx + 1f) * 0.5f, (1f - sy) * 0.5f };
    }

    private static int lerpColor(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int shadeAndFog(int base, float shade, int sky, float fog) {
        float r = ((base >> 16) & 255) * shade, g = ((base >> 8) & 255) * shade, b = (base & 255) * shade;
        r = r * (1 - fog) + ((sky >> 16) & 255) * fog;
        g = g * (1 - fog) + ((sky >> 8) & 255) * fog;
        b = b * (1 - fog) + (sky & 255) * fog;
        int ri = Math.min(255, (int) r), gi = Math.min(255, (int) g), bi = Math.min(255, (int) b);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }
}
