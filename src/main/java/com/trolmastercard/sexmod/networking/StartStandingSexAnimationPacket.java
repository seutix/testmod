package com.trolmastercard.sexmod.networking;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.entity.AbstractPlayerGirlEntity;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * CLIENT->SERVER owner command for a transformed player-girl.
 * Interaction proxies have additional checks: only the player who opened the
 * menu may submit one of the supported actions, and the represented player
 * must still have the explicit opt-in tag.
 */
public class StartStandingSexAnimationPacket implements IMessage {
   boolean isValid;
   UUID girlUUID;
   UUID playerUUID;
   String animation;

   public StartStandingSexAnimationPacket() {
   }

   public StartStandingSexAnimationPacket(UUID girlUUID, UUID playerUUID, String animation) {
      this.girlUUID = girlUUID;
      this.playerUUID = playerUUID;
      this.animation = animation;
   }

   public void fromBytes(ByteBuf buf) {
      this.girlUUID = UUID.fromString(ByteBufUtils.readUTF8String(buf));
      this.playerUUID = UUID.fromString(ByteBufUtils.readUTF8String(buf));
      this.animation = ByteBufUtils.readUTF8String(buf);
      this.isValid = true;
   }

   public void toBytes(ByteBuf buf) {
      ByteBufUtils.writeUTF8String(buf, this.girlUUID.toString());
      ByteBufUtils.writeUTF8String(buf, this.playerUUID.toString());
      ByteBufUtils.writeUTF8String(buf, this.animation);
   }

   public static class Handler implements IMessageHandler<StartStandingSexAnimationPacket, IMessage> {
      public IMessage onMessage(StartStandingSexAnimationPacket packet, MessageContext ctx) {
         if (packet.isValid && ctx.side == Side.SERVER) {
            final EntityPlayerMP sender = ctx.getServerHandler().player;
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
               AbstractPlayerGirlEntity playerGirl = AbstractPlayerGirlEntity.getPlayerGirlByOwner(packet.girlUUID);
               if (playerGirl == null) {
                  playerGirl = AbstractPlayerGirlEntity.getPlayerGirlByUUID(packet.girlUUID);
               }

               if (playerGirl == null) {
                  return;
               }

               if (playerGirl.isInteractionProxy()) {
                  EntityPlayer owner = playerGirl.world.getPlayerEntityByUUID(playerGirl.getOwnerUserUUID());
                  boolean allowedAction = "action.names.blowjob".equals(packet.animation)
                     || "action.names.boobjob".equals(packet.animation);
                  boolean validViewer = sender.getPersistentID().equals(playerGirl.getProxyViewerUUID());
                  boolean validTarget = owner != null && owner.isEntityAlive()
                     && owner.getPersistentID().equals(packet.playerUUID)
                     && owner.getTags().contains(PlayerInteractRequestPacket.OPT_IN_TAG);
                  if (!allowedAction || !validViewer || !validTarget
                     || sender.world != playerGirl.world || sender.getDistanceSq(owner) > 36.0D) {
                     sender.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        "Player interaction is no longer available."), true);
                     return;
                  }
               }

               playerGirl.handleOwnerCommand(packet.animation, packet.playerUUID);
            });
            return null;
         }

         Main.LOGGER.warn("received an invalid message @StartStandingSexAnimation :(");
         return null;
      }
   }
}
