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
 * CLIENT->SERVER request to open the temporary Jenny interaction menu for a
 * regular player. The target must explicitly opt in with the vanilla command:
 * /tag @s add sexmod_player_interaction_opt_in
 * Revoke that permission with:
 * /tag @s remove sexmod_player_interaction_opt_in
 */
public class PlayerInteractRequestPacket implements IMessage {
   static final String OPT_IN_TAG = "sexmod_player_interaction_opt_in";
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
               if (sender.isDead || !sender.isEntityAlive()) {
                  return;
               }

               World world = sender.world;
               EntityPlayer target = world.getPlayerEntityByUUID(packet.targetUUID);
               if (target == null || target == sender || target.isDead || target.isSpectator()
                  || !target.getTags().contains(OPT_IN_TAG)) {
                  sender.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                     "This player has not enabled player interactions."), true);
                  return;
               }

               // The request must correspond to an actual nearby right-click.
               if (sender.getDistanceSq(target) > 20.25D) {
                  return;
               }

               // A transformed player cannot start a second player interaction.
               if (AbstractPlayerGirlEntity.getPlayerGirlByOwner(sender.getPersistentID()) != null) {
                  return;
               }

               AbstractPlayerGirlEntity existing = AbstractPlayerGirlEntity.getPlayerGirlByOwner(packet.targetUUID);
               if (existing != null) {
                  if (existing.isInteractionProxy()
                     && sender.getPersistentID().equals(existing.getProxyViewerUUID())) {
                     existing.setProxyViewerUUID(sender.getPersistentID());
                     PacketHandler.networkWrapper.sendTo(
                        new OpenPlayerGirlMenuPacket(packet.targetUUID, existing.getEntityId()), sender);
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
               PacketHandler.networkWrapper.sendTo(
                  new OpenPlayerGirlMenuPacket(packet.targetUUID, proxy.getEntityId()), sender);
            });
            return null;
         }

         Main.LOGGER.warn("received an invalid message @PlayerInteractRequest :(");
         return null;
      }
   }
}
