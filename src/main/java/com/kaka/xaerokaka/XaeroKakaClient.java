package com.kaka.xaerokaka;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class XaeroKakaClient implements ClientModInitializer {
    public static final String MOD_ID = "xaerokaka";
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = new KeyMapping("key.xaerokaka.open", GLFW.GLFW_KEY_K, category);
        KeyMappingHelper.registerKeyMapping(openKey);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.level != null && mc.player != null && mc.screen == null) {
                    mc.setScreen(new KakaMapScreen());
                }
            }
            // mantém o cache atualizado mesmo com a tela fechada (leve: a cada 3 s)
            if (mc.level != null && mc.player != null && mc.player.tickCount % 60 == 0) {
                KakaData.scanAround(mc, 12);
            }
        });
    }
}
