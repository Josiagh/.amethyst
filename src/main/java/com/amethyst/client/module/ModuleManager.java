package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();
    public static final Freecam FREECAM = new Freecam();
    public static final Freelook FREELOOK = new Freelook();

    public static void init() {
        modules.add(new Sprint());
        modules.add(FREECAM);
        modules.add(FREELOOK);
        modules.add(new Fullbright());
        modules.add(new SusChunk());
        modules.add(new CrystalMacro());
        modules.add(new AnchorMacro());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            for (Module m : modules) if (m.isEnabled()) m.onTick();
        });
    }
}
