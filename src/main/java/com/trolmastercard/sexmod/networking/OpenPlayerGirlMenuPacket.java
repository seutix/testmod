package com.trolmastercard.sexmod.networking;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.util.PlayerGirlEvents;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * <b>Role.</b> SERVER-&gt;CLIENT instruction to open the interaction menu of a
 * temporary interaction proxy ({@link PlayerInteractRequestPacket}).
 * <p>
 * <b>Handler.</b> CLIENT-side, scheduled on the client thread. The entity may
 * not have arrived yet when the packet lands, so the lookup is parked in
 * {@link PlayerGirlEvents#setPendingPlayerGirlMenu(UUID, int)} and retried for a
 * few ticks; the entity id is the primary key and the owner UUID the fallback.
 */
public class OpenPlayerGirlMenuPacket implements IMessage {
   boolean isValid;
   UUID ownerUUID;
   int entityId;

   public OpenPlayerGirlMenuPacket() {
   }

   public OpenPlayerGirlMenuPacket(UUID ownerUUID, int entityId) {
      this.ownerUUID = ownerUUID;
      this.entityId = entityId;
      this.isValid = true;
   }

   public void fromBytes(ByteBuf buf) {
      this.ownerUUID = UUID.fromString(ByteBufUtils.readUTF8String(buf));
      this.entityId = buf.readInt();
      this.isValid = true;
   }

   public void toBytes(ByteBuf buf) {
      ByteBufUtils.writeUTF8String(buf, this.ownerUUID.toString());
      buf.writeInt(this.entityId);
   }

   public static class Handler implements IMessageHandler<OpenPlayerGirlMenuPacket, IMessage> {
      public IMessage onMessage(OpenPlayerGirlMenuPacket packet, MessageContext ctx) {
         if (packet.isValid && ctx.side == Side.CLIENT) {
            Minecraft.getMinecraft()
               .addScheduledTask(() -> PlayerGirlEvents.setPendingPlayerGirlMenu(packet.ownerUUID, packet.entityId));
            return null;
         } else {
            Main.LOGGER.warn("received an invalid message @OpenPlayerGirlMenu :(");
            return null;
         }
      }
   }
}
