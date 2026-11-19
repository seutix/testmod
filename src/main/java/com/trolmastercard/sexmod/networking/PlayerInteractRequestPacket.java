package com.trolmastercard.sexmod.networking;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.entity.AbstractPlayerGirlEntity;
import com.trolmastercard.sexmod.entity.JennyPlayerEntity;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * <b>Role.</b> CLIENT-&gt;SERVER request to interact with a regular player the
 * same way as with an NPC girl. Sent when the local player right-clicks another
 * player who has no player-girl of his own.
 * <p>
 * <b>Handler.</b> SERVER-side, scheduled on the main thread. Spawns a temporary
 * {@link JennyPlayerEntity} owned by the target player (marked with
 * {@link AbstractPlayerGirlEntity#isInteractionProxy()}), so the NPC-style
 * interaction menu and the owner-command scenes work unchanged, then tells the
 * requester to open that girl's menu ({@link OpenPlayerGirlMenuPacket}).
 * The proxy is never written to chunk NBT and is discarded when the menu closes
 * without an action, when the scene ends ({@link ResetGirlPacket}), or when
 * either player logs out.
 */
public class PlayerInteractRequestPacket implements IMessage {
   boolean isValid;
   UUID targetUUID;

   public PlayerInteractRequestPacket() {
   }

   public PlayerInteractRequestPacket(UUID targetUUID) {
      this.targetUUID = targetUUID;
      this.isValid = true;
   }

   public void fromBytes(ByteBuf buf) {
      this.targetUUID = UUID.fromString(ByteBufUtils.readUTF8String(buf));
      this.isValid = true;
   }

   public void toBytes(ByteBuf buf) {
      ByteBufUtils.writeUTF8String(buf, this.targetUUID.toString());
   }

   public static class Handler implements IMessageHandler<PlayerInteractRequestPacket, IMessage> {
      public IMessage onMessage(PlayerInteractRequestPacket packet, MessageContext ctx) {
         if (packet.isValid && ctx.side == Side.SERVER) {
            final EntityPlayerMP sender = ctx.getServerHandler().player;
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
               World world = sender.world;
               EntityPlayer target = world.getPlayerEntityByUUID(packet.targetUUID);
               if (target == null || target == sender || target.isDead) {
                  return;
               }

               // the requester must not already be a player-girl (that flow has its own menu)
               if (AbstractPlayerGirlEntity.getPlayerGirlByOwner(sender.getPersistentID()) != null) {
                  return;
               }

               AbstractPlayerGirlEntity existing = AbstractPlayerGirlEntity.getPlayerGirlByOwner(packet.targetUUID);
               if (existing != null) {
                  if (existing.isInteractionProxy()) {
                     existing.setProxyViewerUUID(sender.getPersistentID());
                     PacketHandler.networkWrapper
                        .sendTo(new OpenPlayerGirlMenuPacket(packet.targetUUID, existing.getEntityId()), sender);
                  }
                  return;
               }

               JennyPlayerEntity proxy = new JennyPlayerEntity(world, packet.targetUUID);
               proxy.setInteractionProxy(true);
               proxy.setProxyViewerUUID(sender.getPersistentID());
               proxy.setNoGravity(true);
               proxy.noClip = true;
               proxy.motionX = 0.0;
               proxy.motionY = 0.0;
               proxy.motionZ = 0.0;
               proxy.setPosition(target.posX, target.posY, target.posZ);
               world.spawnEntity(proxy);
               PacketHandler.networkWrapper
                  .sendTo(new OpenPlayerGirlMenuPacket(packet.targetUUID, proxy.getEntityId()), sender);
            });
            return null;
         } else {
            Main.LOGGER.warn("received an invalid message @PlayerInteractRequest :(");
            return null;
         }
      }
   }
}
