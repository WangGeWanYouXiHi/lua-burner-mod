package com.luaburner;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** Registers network payloads and handles client/server messages. */
@EventBusSubscriber(modid = LuaBurnerMod.MOD_ID)
public final class NetworkHandler {
    private NetworkHandler() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").executesOn(
                net.neoforged.neoforge.network.registration.HandlerThread.MAIN);

        // S->C: blueprint list + preview
        reg.playToClient(NetworkMessages.ListPayload.TYPE, NetworkMessages.ListPayload.CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    var screen = net.minecraft.client.Minecraft.getInstance().screen;
                    if (screen instanceof BurnerScreen bs) {
                        SplitData d = SplitData.from(msg.data());
                        bs.setBlueprintData(d.names, d.contents, d.sizes);
                    }
                }));

        // S->C: burn status
        reg.playToClient(NetworkMessages.StatusPayload.TYPE, NetworkMessages.StatusPayload.CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    var screen = net.minecraft.client.Minecraft.getInstance().screen;
                    if (screen instanceof BurnerScreen bs) {
                        bs.setStatus(msg.message(), msg.progress());
                    }
                }));

        // C->S: burn request (or selection-change = index -2, mode toggle = -1)
        reg.playToServer(NetworkMessages.BurnPayload.TYPE, NetworkMessages.BurnPayload.CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer player = (ServerPlayer) ctx.player();
                    if (!(player.containerMenu instanceof BurnerMenu menu)) return;
                    BurnerBlockEntity be = menu.getBlockEntity();
                    if (be == null || be.getLevel() == null) return;
                    var server = be.getLevel().getServer();

                    if (msg.selectedIndex() == -1) {
                        // Mode toggle
                        be.mode = msg.mode();
                        menu.modeSlot.set(be.mode);
                        be.setChanged();
                    } else if (msg.selectedIndex() == -2) {
                        // Selection changed -> resend list so preview updates
                        be.selectedIndex = msg.mode();
                        menu.selectedSlot.set(be.selectedIndex);
                        sendList(player, be);
                    } else {
                        // Actual burn
                        be.selectedIndex = msg.selectedIndex();
                        be.startBurn(server, msg.selectedIndex(), msg.mode());
                    }
                }));
    }

    /** Serialize blueprint list into a string payload and send to one player. */
    public static void sendList(ServerPlayer player, BurnerBlockEntity be) {
        List<BlueprintManager.Blueprint> bps = BlueprintManager.list(be.getLevel().getServer());
        StringBuilder sb = new StringBuilder();
        for (BlueprintManager.Blueprint bp : bps) {
            // Format: name <US> size <US> content(escape newlines) <RS>
            String content = bp.content().replace("\n", "\\n").replace("\u001e", "");
            sb.append(bp.name()).append('\u001f').append(bp.sizeBytes())
                    .append('\u001f').append(content).append('\u001e');
        }
        PacketDistributor.sendToPlayer(player, new NetworkMessages.ListPayload(sb.toString()));
    }

    /** Decode the serialized blueprint list on the client. */
    private record SplitData(List<String> names, List<String> contents, List<Long> sizes) {
        static SplitData from(String raw) {
            List<String> names = new ArrayList<>(), contents = new ArrayList<>();
            List<Long> sizes = new ArrayList<>();
            if (raw == null || raw.isEmpty()) return new SplitData(names, contents, sizes);
            for (String rec : raw.split("\u001e")) {
                if (rec.isEmpty()) continue;
                String[] parts = rec.split("\u001f", 3);
                if (parts.length < 3) continue;
                names.add(parts[0]);
                try { sizes.add(Long.parseLong(parts[1])); }
                catch (NumberFormatException e) { sizes.add(0L); }
                contents.add(parts[2].replace("\\n", "\n"));
            }
            return new SplitData(names, contents, sizes);
        }
    }
}
