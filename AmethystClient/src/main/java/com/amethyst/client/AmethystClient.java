package com.amethyst.client;

import com.amethyst.client.gui.ClickGuiScreen;
import com.amethyst.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AmethystClient implements ClientModInitializer {
    public static final String ID = "amethystclient";
    private static KeyBinding guiKey, freecamKey, freelookKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(ID, "main"));
        guiKey      = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.amethystclient.gui",      InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        freecamKey  = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.amethystclient.freecam",  InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, cat));
        freelookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.amethystclient.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) client.setScreen(new ClickGuiScreen());
            while (freecamKey.wasPressed()) ModuleManager.FREECAM.toggle();
            while (freelookKey.wasPressed()) ModuleManager.FREELOOK.toggle();
        });
    }
}
