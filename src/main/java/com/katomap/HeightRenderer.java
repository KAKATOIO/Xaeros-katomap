package com.katomap;

import java.util.Arrays;
import java.util.Map;

/**
 * Renderizador por software (ray-marching de campo de altura) com câmera orbital.
 * Não usa nenhuma API do Minecraft: recebe apenas arrays de altura/cor.
 */
public final class HeightRenderer {
    public static final int G = 512;                 // lado da grade, em blocos
    private static final int NODATA = Integer.MIN_VALUE;
    private static final int VOID_COLOR = 0xFF16181D;
    private static final int MAX_STEPS = 1600;

    private final int[] H = new int[G * G];
    private final int[] C = new int[G * G];
    private int x0, z0;                              // canto da grade (coords do mundo)
    private int maxH = 64, baseY = 0;
    public boolean hasGrid = false;
    public boolean hasData = false;

    // câmera da última renderização
    private float camX, camY, camZ, fx, fy, fz, rx, rz, ux, uy, uz, tanH, aspect, lastVs = 1f;

    // resultado do último march()
    private int hitIdx;
    private float hitT, hitX, hitY, hitZ, hitTop;

    // estado do cálculo de slab
    private float sMin, sMax;

    public int centerX() { return x0 + G / 2; }
    public int centerZ() { return z0 + G / 2; }

    public void rebuild(Map<Long, KatomapData.ChunkData> map, int cx, int cz) {
        x0 = cx - G / 2;
        z0 = cz - G / 2;
        Arrays.fill(H, NODATA);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int ccx = x0 >> 4; ccx <= (x0 + G - 1) >> 4; ccx++) {
            for (int ccz = z0 >> 4; ccz <= (z0 + G - 1) >> 4; ccz++) {
                KatomapData.ChunkData d = map.get(KatomapData.key(ccx, ccz));
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
        hasData = min != Integer.MAX_VALUE;
        if (!hasData) { min = 60; max = 70; }
        maxH = max + 2;
        baseY = min - 8;
        hasGrid = true;
    }

    private int idxAt(float x, float z) {
        int gx = (int) Math.floor(x) - x0, gz = (int) Math.floor(z) - z0;
        if (gx < 0) gx = 0; else if (gx >= G) gx = G - 1;
        if (gz < 0) gz = 0; else if (gz >= G) gz = G - 1;
        return gz * G + gx;
    }

    private float hs(int idx, float vs) {
        int h = H[idx];
        return (h == NODATA ? baseY : h) * vs;
    }

    /** Altura (bloco) no mundo, ou baseY se não houver dados. */
    public int heightAt(int wx, int wz) {
        int gx = wx - x0, gz = wz - z0;
        if (gx < 0 || gz < 0 || gx >= G || gz >= G) return baseY;
        int h = H[gz * G + gx];
        return h == NODATA ? baseY : h;
    }

    private boolean slab(float o, float d, float lo, float hi) {
        if (Math.abs(d) < 1e-7f) return o >= lo && o <= hi;
        float t1 = (lo - o) / d, t2 = (hi - o) / d;
        if (t1 > t2) { float s = t1; t1 = t2; t2 = s; }
        if (t1 > sMin) sMin = t1;
        if (t2 < sMax) sMax = t2;
        return sMin <= sMax;
    }

    /** Lança um raio a partir da câmera. Preenche hit* e devolve true se acertou o terreno. */
    private boolean march(float dx, float dy, float dz, float vs) {
        float minY = baseY * vs, maxY = maxH * vs;
        sMin = 0f; sMax = 1e9f;
        if (!slab(camX, dx, x0, x0 + G) || !slab(camY, dy, minY, maxY) || !slab(camZ, dz, z0, z0 + G)) return false;
        float t = sMin + 0.001f, end = sMax, prevT = t;
        for (int i = 0; i < MAX_STEPS && t <= end; i++) {
            float x = camX + dx * t, y = camY + dy * t, z = camZ + dz * t;
            int idx = idxAt(x, z);
            float yh = hs(idx, vs);
            if (y < yh) {
                float lo = prevT, hi = t;
                for (int k = 0; k < 6; k++) {
                    float mid = (lo + hi) * 0.5f;
                    float my = camY + dy * mid;
                    if (my < hs(idxAt(camX + dx * mid, camZ + dz * mid), vs)) hi = mid; else lo = mid;
                }
                hitT = hi;
                hitX = camX + dx * hi; hitY = camY + dy * hi; hitZ = camZ + dz * hi;
                hitIdx = idxAt(hitX, hitZ);
                hitTop = hs(hitIdx, vs);
                return true;
            }
            float step = Math.max(0.7f, Math.min(6f, (y - yh) * 0.45f));
            step = Math.max(step, t * 0.004f);
            prevT = t;
            t += step;
        }
        return false;
    }

    public void render(int[] out, int w, int h, float yaw, float pitch, float dist,
                       float tx, float tz, float vs) {
        lastVs = vs;
        float ty = heightAt((int) Math.floor(tx), (int) Math.floor(tz)) * vs;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float sy = (float) Math.sin(yaw), cy = (float) Math.cos(yaw);
        camX = tx + dist * cp * sy;
        camY = ty + dist * sp;
        camZ = tz + dist * cp * cy;
        fx = (tx - camX) / dist; fy = (ty - camY) / dist; fz = (tz - camZ) / dist;
        float rl = (float) Math.sqrt(fz * fz + fx * fx);
        rx = -fz / rl; rz = fx / rl;                    // direita (horizontal)
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;  // cima
        tanH = (float) Math.tan(Math.toRadians(35));
        aspect = (float) w / h;

        float fogDist = dist * 3f + 300f;
        final float lx = -0.45f, ly = 0.8f, lz = -0.4f;
        final float ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);

        for (int py = 0; py < h; py++) {
            float v = (1f - 2f * (py + 0.5f) / h) * tanH;
            int sky = lerpColor(0x24385E, 0x8FB0D8, (float) py / h);
            for (int px = 0; px < w; px++) {
                float u = (2f * (px + 0.5f) / w - 1f) * tanH * aspect;
                float dx = fx + u * rx + v * ux;
                float dy = fy + v * uy;
                float dz = fz + u * rz + v * uz;
                float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= dl; dy /= dl; dz /= dl;

                int color = sky;
                if (march(dx, dy, dz, vs)) {
                    int idx = hitIdx;
                    int base = H[idx] == NODATA ? VOID_COLOR : C[idx];
                    float cl = hs(Math.max(0, idx - 1), vs), cr = hs(Math.min(G * G - 1, idx + 1), vs);
                    float cu = hs(Math.max(0, idx - G), vs), cd = hs(Math.min(G * G - 1, idx + G), vs);
                    float nx = (cl - cr) * 0.5f, nz = (cu - cd) * 0.5f, ny = 1.4f;
                    float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    float ndl = Math.max(0f, (nx * lx + ny * ly + nz * lz) / (nl * ll));
                    float shade = 0.55f + 0.6f * ndl;
                    if (hitTop - hitY > 0.45f * vs + 0.3f) shade *= 0.72f; // lateral da coluna
                    float fog = Math.min(1f, hitT / fogDist);
                    color = shadeAndFog(base, shade, sky, fog * fog * 0.65f);
                }
                out[py * w + px] = color;
            }
        }
    }

    /**
     * Qual bloco está sob o cursor? nx, ny em 0..1 (fração da tela). Retorna {x, y, z} ou null.
     * Válido após um render().
     */
    public int[] pick(float nx, float ny) {
        if (!hasGrid || !hasData) return null;
        float u = (2f * nx - 1f) * tanH * aspect;
        float v = (1f - 2f * ny) * tanH;
        float dx = fx + u * rx + v * ux, dy = fy + v * uy, dz = fz + u * rz + v * uz;
        float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!march(dx / dl, dy / dl, dz / dl, lastVs)) return null;
        if (H[hitIdx] == NODATA) return null;
        return new int[] { (int) Math.floor(hitX), H[hitIdx], (int) Math.floor(hitZ) };
    }

    /** Projeta um ponto do mundo em coordenadas 0..1 da tela; null se estiver atrás da câmera. */
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
        return 0xFF000000 | (Math.min(255, (int) r) << 16) | (Math.min(255, (int) g) << 8) | Math.min(255, (int) b);
    }
}
