package me.colingrimes.packets.command.blockdestroyer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.colingrimes.midnight.command.Command;
import me.colingrimes.midnight.command.handler.util.ArgumentList;
import me.colingrimes.midnight.command.handler.util.CommandProperties;
import me.colingrimes.midnight.command.handler.util.Sender;
import me.colingrimes.midnight.scheduler.Scheduler;
import me.colingrimes.midnight.util.bukkit.*;
import me.colingrimes.midnight.util.io.Logger;
import me.colingrimes.packets.Packets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Testing Packet 7.1.6 - displayable destroy stages for blocks.
 * @see <a href=https://minecraft.wiki/w/Java_Edition_protocol#Set_Block_Destroy_Stage>Set Block Destroy Stage</a>
 */
public class BlockDestroyer implements Command<Packets> {

	private final ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
	private final Map<Block, DamagedBlock> damagedBlocks = new HashMap<>();

	public BlockDestroyer() {
		Scheduler.sync().runRepeating(() -> Players.forEach(this::tick), 15L, 15L);
		Scheduler.sync().runRepeating(() -> new HashSet<>(damagedBlocks.values()).forEach(this::tick), 15L, 15L);
	}

	@Override
	public void execute(@Nonnull Packets plugin, @Nonnull Sender sender, @Nonnull ArgumentList args) {
		ItemStack blockDestroyer = Items.of(Material.CHAINMAIL_BOOTS)
				.name("&c&lBlock Destroyer")
				.glow()
				.nbt("block_destroyer", true)
				.build();
		Inventories.give(sender.player(), blockDestroyer);
	}

	@Override
	public void configureProperties(@Nonnull CommandProperties properties) {
		properties.setPlayerRequired(true);
	}

	private void tick(@Nonnull Player player) {
		if (!NBT.hasTag(player.getInventory().getBoots(), "block_destroyer", Boolean.class)) {
			return;
		}

		Location ground = player.getLocation().clone().subtract(0, 1, 0);
		for (Location location : Locations.between(ground.clone().add(-2, 0, -2), ground.clone().add(2, 0, 2))) {
			Block block = location.getBlock();
			if (!block.getType().isAir()) {
				damagedBlocks.computeIfAbsent(block, DamagedBlock::new).latestUpdate = Instant.now();
			}
		}
	}

	private void tick(@Nonnull DamagedBlock damagedBlock) {
		if (damagedBlock.block.getType().isAir()) {
			damagedBlocks.remove(damagedBlock.block);
			return;
		}

		// Revert block destroy stage (faster than it gets destroyed).
		if (Instant.now().minusSeconds(2).isAfter(damagedBlock.latestUpdate)) {
			damagedBlock.destroyStage -= 2;
			if (damagedBlock.destroyStage >= 0 && damagedBlock.destroyStage <= 9) {
				Players.forEach(p -> setDestroyStage(p, damagedBlock.block.getLocation(), damagedBlock.destroyStage));
			} else {
				Players.forEach(p -> resetDestroyStage(p, damagedBlock.block.getLocation()));
				damagedBlocks.remove(damagedBlock.block);
			}
			return;
		}

		// Slowly destroy the block.
		damagedBlock.destroyStage += 1;
		if (damagedBlock.destroyStage >= 0 && damagedBlock.destroyStage <= 9) {
			Players.forEach(p -> setDestroyStage(p, damagedBlock.block.getLocation(), damagedBlock.destroyStage));
		} else {
			damagedBlock.block.breakNaturally();
			damagedBlocks.remove(damagedBlock.block);
		}
	}

	/**
	 * Resets the block's destroy stage (so that the animation disappears) for the specified player.
	 *
	 * @param player the player
	 * @param location the location of the block to reset
	 */
	private void resetDestroyStage(@Nonnull Player player, @Nonnull Location location) {
		setDestroyStage(player, location, -1);
	}

	/**
	 * Sets the block's destroy stage for the specified player.
	 *
	 * @param player the player
	 * @param location the location of the block to destroy
	 * @param destroyStage the destroy stage of the block (0-9, if >9, the block will break)
	 */
	private void setDestroyStage(@Nonnull Player player, @Nonnull Location location, int destroyStage) {
		PacketContainer packet = protocol.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
		packet.getIntegers().write(0, location.hashCode());
		packet.getBlockPositionModifier().write(0, new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
		packet.getIntegers().write(1, destroyStage);

		try {
			protocol.sendServerPacket(player, packet);
		} catch (Exception e) {
			Logger.warn("setDestroyStage() packet did not get sent properly.");
		}
	}

	private static class DamagedBlock {
		private final Block block;
		private int destroyStage = 0;
		private Instant latestUpdate;

		public DamagedBlock(@Nonnull Block block) {
			this.block = block;
		}
	}
}
