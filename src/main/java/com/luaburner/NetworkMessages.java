package com.luaburner;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
/**
 * Network payloads for the burner GUI.
 * - list (S->C): the blueprint list + selected blueprint preview
 * - burn (C->S): request to burn the selected blueprint with a given mode
 */
public final class NetworkMessages {
    private NetworkMessages() {}

    public static final ResourceLocation LIST_ID =
            ResourceLocation.fromNamespaceAndPath(LuaBurnerMod.MOD_ID, "bp_list");
    public static final ResourceLocation BURN_ID =
            ResourceLocation.fromNamespaceAndPath(LuaBurnerMod.MOD_ID, "burn_req");
    public static final ResourceLocation STATUS_ID =
            ResourceLocation.fromNamespaceAndPath(LuaBurnerMod.MOD_ID, "burn_status");

    /**
     * S->C: serialized blueprint list (one record per blueprint) + preview content.
     * Sent as UTF-8 bytes, NOT writeUtf: writeUtf has a hard 32767-byte cap and
     * throws EncoderException (client "Connection Lost") when a .lua file is large.
     * writeByteArray uses a VarInt length prefix and has no such cap.
     */
    public record ListPayload(byte[] data) implements CustomPacketPayload {
        public static final Type<ListPayload> TYPE = new Type<>(LIST_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ListPayload> CODEC =
                StreamCodec.of(
                        (buf, msg) -> buf.writeByteArray(msg.data),
                        buf -> new ListPayload(buf.readByteArray()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** C->S: burn request. mode 0 = overwrite, 1 = append(incremental). */
    public record BurnPayload(int selectedIndex, int mode) implements CustomPacketPayload {
        public static final Type<BurnPayload> TYPE = new Type<>(BURN_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, BurnPayload> CODEC =
                StreamCodec.of(
                        (buf, msg) -> { buf.writeInt(msg.selectedIndex); buf.writeInt(msg.mode); },
                        buf -> new BurnPayload(buf.readInt(), buf.readInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S->C: status line (progress + lang key + optional argument). */
    public record StatusPayload(String key, String arg, int progress) implements CustomPacketPayload {
        public static final Type<StatusPayload> TYPE = new Type<>(STATUS_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, StatusPayload> CODEC =
                StreamCodec.of(
                        (buf, msg) -> {
                            buf.writeUtf(msg.key);
                            buf.writeUtf(msg.arg == null ? "" : msg.arg);
                            buf.writeInt(msg.progress);
                        },
                        buf -> new StatusPayload(buf.readUtf(), buf.readUtf(), buf.readInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
