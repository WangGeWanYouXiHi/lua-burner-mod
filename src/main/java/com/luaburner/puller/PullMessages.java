package com.luaburner.puller;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Network payloads for the checklist puller GUI. Pure MC types only. */
public final class PullMessages {
    private PullMessages() {}

    public static final ResourceLocation CHECKLIST_ID =
            ResourceLocation.fromNamespaceAndPath("luaburner", "puller_checklist");
    public static final ResourceLocation ACTION_ID =
            ResourceLocation.fromNamespaceAndPath("luaburner", "puller_action");

    /** S->C: full checklist state. */
    public record ChecklistPayload(BlockPos pos, java.util.List<ChecklistEntry> entries,
                                   boolean pulling, String status, int stackLimit,
                                   long multiplier, int outputMode) implements CustomPacketPayload {
        public static final Type<ChecklistPayload> TYPE = new Type<>(CHECKLIST_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ChecklistPayload> CODEC =
                StreamCodec.of(
                        (buf, msg) -> {
                            buf.writeBlockPos(msg.pos);
                            buf.writeVarInt(msg.entries.size());
                            for (ChecklistEntry e : msg.entries) e.write(buf);
                            buf.writeBoolean(msg.pulling);
                            buf.writeUtf(msg.status == null ? "" : msg.status);
                            buf.writeVarInt(msg.stackLimit);
                            buf.writeLong(msg.multiplier);
                            buf.writeVarInt(msg.outputMode);
                        },
                        buf -> {
                            BlockPos pos = buf.readBlockPos();
                            int n = buf.readVarInt();
                            java.util.List<ChecklistEntry> list = new java.util.ArrayList<>(n);
                            for (int i = 0; i < n; i++) list.add(ChecklistEntry.read(buf));
                            boolean pulling = buf.readBoolean();
                            String status = buf.readUtf();
                            int stackLimit = buf.readVarInt();
                            long multiplier = buf.readLong();
                            int outputMode = buf.readVarInt();
                            return new ChecklistPayload(pos, list, pulling, status,
                                    stackLimit, multiplier, outputMode);
                        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** C->S: action. action 0 = refresh, 1 = toggle pull. */
    public record ActionPayload(BlockPos pos, int action) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(ACTION_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
                StreamCodec.of(
                        (buf, msg) -> { buf.writeBlockPos(msg.pos); buf.writeVarInt(msg.action); },
                        buf -> new ActionPayload(buf.readBlockPos(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void sendChecklist(ServerPlayer player, PullerBlockEntity be) {
        PacketDistributor.sendToPlayer(player, new ChecklistPayload(
                be.getBlockPos(), new java.util.ArrayList<>(be.checklist),
                be.pulling, be.status, be.stackLimit(), be.capacityMultiplier(), be.outputMode));
    }

    /** Called every few ticks while a player has the GUI open, to refresh counts. */
    public static void sendChecklistToAll(ServerPlayer player, PullerBlockEntity be) {
        sendChecklist(player, be);
    }
}
