package com.kaka.xaerokaka;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.levelgen.Heightmap;

/** Cache em memória (altura + cor do topo) de todos os chunks já vistos nesta sessão, por dimensão. */
public final class KakaData {
    public static final class ChunkData {
        public final int[] h = new int[256];
        public final int[] c = new int[256];
    }

    private static final Map<String, Map<Long, ChunkData>> CACHE = new HashMap<>();
    public static int version = 0;

    public static String dimKey(ClientLevel level) {
        return level.dimension().toString();
    }

    public static Map<Long, ChunkData> current(ClientLevel level) {
        return CACHE.computeIfAbsent(dimKey(level), k -> new HashMap<>());
    }

    public static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    /** Varre os chunks carregados ao redor do jogador e atualiza o cache. */
    public static void scanAround(Minecraft mc, int radiusChunks) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        int pcx = Mth.floor(mc.player.getX()) >> 4;
        int pcz = Mth.floor(mc.player.getZ()) >> 4;
        Map<Long, ChunkData> map = current(level);
        for (int cx = pcx - radiusChunks; cx <= pcx + radiusChunks; cx++) {
            for (int cz = pcz - radiusChunks; cz <= pcz + radiusChunks; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                ChunkData d = map.computeIfAbsent(key(cx, cz), k -> new ChunkData());
                scanChunk(level, chunk, cx, cz, d);
            }
        }
        version++;
    }

    private static void scanChunk(ClientLevel level, LevelChunk chunk, int cx, int cz, ChunkData d) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinY();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz) - 1;
                if (top < minY) top = minY;
                pos.set(cx * 16 + lx, top, cz * 16 + lz);
                BlockState st = chunk.getBlockState(pos);
                int color;
                if (!st.getFluidState().isEmpty()) {
                    int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, lx, lz) - 1;
                    float depth = Math.max(0, top - floor);
                    float f = Math.max(0.35f, 1f - depth / 40f);
                    int w = MapColor.WATER.col;
                    int r = (int) (((w >> 16) & 255) * f), g = (int) (((w >> 8) & 255) * f), b = (int) ((w & 255) * f);
                    color = 0xFF000000 | (r << 16) | (g << 8) | b;
                } else {
                    int col = st.getMapColor(level, pos).col;
                    if (col == 0) col = 0x808080;
                    color = 0xFF000000 | (col & 0xFFFFFF);
                }
                d.h[lz * 16 + lx] = top;
                d.c[lz * 16 + lx] = color;
            }
        }
    }

    private KakaData() {}
}
