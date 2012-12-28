/**
 * 
 */
package net.machinemuse.powersuits.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import net.machinemuse.powersuits.common.Config;
import net.machinemuse.powersuits.item.ItemUtils;
import net.machinemuse.powersuits.tinker.TinkerAction;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;

/**
 * Packet for requesting to purchase an upgrade. Player-to-server. Server
 * decides whether it is a valid upgrade or not and replies with an associated
 * inventoryrefresh packet.
 * 
 * @author MachineMuse
 * 
 */
public class MusePacketTinkerRequest extends MusePacket {
	protected ItemStack stack;
	protected int itemSlot;
	protected String tinkerName;

	/**
	 * Constructor for sending this packet.
	 * 
	 * @param player
	 *            Player making the request
	 * @param itemSlot
	 *            Slot containing the item for which the upgrade is requested
	 * @param tinkerName
	 */
	public MusePacketTinkerRequest(Player player, int itemSlot,
			String tinkerName) {
		super(player);
		writeInt(itemSlot);
		writeString(tinkerName);
	}

	/**
	 * Constructor for receiving this packet.
	 * 
	 * @param player
	 * @param data
	 * @throws IOException
	 * 
	 */
	public MusePacketTinkerRequest(DataInputStream data, Player player) {
		super(player, data);
		itemSlot = readInt();
		tinkerName = readString(64);
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.SERVER) {
			EntityPlayerMP srvplayer = (EntityPlayerMP) player;
			stack = srvplayer.inventory.getStackInSlot(itemSlot);
		}
	}

	@Override
	public void handleServer(EntityPlayerMP playerEntity) {
		if (tinkerName != null) {
			InventoryPlayer inventory = playerEntity.inventory;
			int entityId = playerEntity.entityId;
			TinkerAction tinkerType = Config.getTinkerings().get(tinkerName);
			NBTTagCompound itemTag = ItemUtils
					.getItemModularProperties(stack)
					.getCompoundTag(
							tinkerName);
			if (tinkerType.validate(playerEntity, stack)) {
				List<Integer> slots = ItemUtils.deleteFromInventory(
						tinkerType.getCosts(), inventory);
				tinkerType.apply(stack);
				slots.add(this.itemSlot);
				for (Integer slotiter : slots) {
					MusePacket reply = new MusePacketInventoryRefresh(
							player,
							slotiter, inventory.getStackInSlot(slotiter));
					PacketDispatcher.sendPacketToPlayer(reply.getPacket(),
							player);
				}
			}
		}
	}

	@Override
	public void handleClient(EntityClientPlayerMP player) {
		// TODO Auto-generated method stub

	}

}
