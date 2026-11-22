package com.luaburner;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side registration: bind our MenuType to the Screen. */
@EventBusSubscriber(modid = LuaBurnerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistries.BURNER_MENU.get(), BurnerScreen::new);
        if (IntegrationCaps.pullerEnabled()) {
            event.register(com.luaburner.puller.PullerRegistry.PULLER_MENU.get(),
                    com.luaburner.puller.PullerScreen::new);
        }
    }
}
