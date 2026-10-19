package com.katomap;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Cache em memória (altura + cor do topo) dos chunks já vistos nesta sessão, separado por dimensão.
 * É limpo ao sair do mundo/servidor, para não misturar mundos diferentes.
 */
public final class KatomapData {
    public static final class ChunkData {
        public final int[] h = new int[256];
        public final int[] c = new int[256];
    }

    private static final Map<String, Map<Long, ChunkData>> CACHE = new HashMap<>();

    public static void clear() {
        CACHE.clear();
    }

    public static Map<Long, ChunkData> current(ClientLevel level) {
        return CACHE.computeIfAbsent(level.dimension().toString(), k -> new HashMap<>());
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
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = pcx - radiusChunks; cx <= pcx + radiusChunks; cx++) {
            for (int cz = pcz - radiusChunks; cz <= pcz + radiusChunks; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                ChunkData d = map.computeIfAbsent(key(cx, cz), k -> new ChunkData());
                scanChunk(level, chunk, cx, cz, d, pos);
            }
        }
    }

    private static void scanChunk(ClientLevel level, LevelChunk chunk, int cx, int cz, ChunkData d,
                                  BlockPos.MutableBlockPos pos) {
        int minY = level.getMinY();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz) - 1;
                if (top < minY) top = minY;
                pos.set(cx * 16 + lx, top, cz * 16 + lz);
                BlockState st = chunk.getBlockState(pos);
                int color;
                if (st.getFluidState().is(FluidTags.WATER)) {
                    int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, lx, lz) - 1;
                    float f = Math.max(0.35f, 1f - Math.max(0, top - floor) / 40f);
                    int w = MapColor.WATER.col;
                    int r = (int) (((w >> 16) & 255) * f);
                    int g = (int) (((w >> 8) & 255) * f);
                    int b = (int) ((w & 255) * f);
                    color = 0xFF000000 | (r << 16) | (g << 8) | b;
                } else {
                    int col = st.getMapColor(level, pos).col;
                    if (col == 0) col = 0x808080;       // MapColor.NONE
                    color = 0xFF000000 | (col & 0xFFFFFF);
                }
                d.h[lz * 16 + lx] = top;
                d.c[lz * 16 + lx] = color;
            }
        }
    }

    private KatomapData() {}
}
