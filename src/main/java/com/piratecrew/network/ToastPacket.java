package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A pirate-styled toast: an icon, a headline and a line of detail, in an accent colour. */
public class ToastPacket {
    public final ItemStack icon;
    public final String title, detail;
    public final int color;

    public ToastPacket(ItemStack icon, String title, String detail, int color) {
        this.icon = icon;
        this.title = title;
        this.detail = detail;
        this.color = color;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(icon);
        buf.writeUtf(title, 128);
        buf.writeUtf(detail, 256);
        buf.writeInt(color);
    }

    public static ToastPacket decode(FriendlyByteBuf buf) {
        return new ToastPacket(buf.readItem(), buf.readUtf(128), buf.readUtf(256), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.PirateToast.show(icon, title, detail, color));
        ctx.get().setPacketHandled(true);
    }
}
