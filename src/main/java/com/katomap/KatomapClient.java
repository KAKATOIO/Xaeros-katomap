package com.katomap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KatomapClient implements ClientModInitializer {
    public static final String MOD_ID = "katomap";
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = new KeyMapping("key." + MOD_ID + ".open", GLFW.GLFW_KEY_K, category);
        KeyMappingHelper.registerKeyMapping(openKey);

        // Não mistura dados de mundos/servidores diferentes.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KatomapData.clear());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.level != null && mc.player != null && mc.screen == null) {
                    mc.setScreen(new KatomapScreen());
                }
            }
            // Mantém o cache atualizado com a tela fechada (leve: a cada 3 s).
            if (mc.level != null && mc.player != null && mc.player.tickCount % 60 == 0) {
                KatomapData.scanAround(mc, 12);
            }
        });
    }
}
