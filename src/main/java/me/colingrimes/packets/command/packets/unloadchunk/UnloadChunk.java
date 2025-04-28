package me.colingrimes.packets.command.packets.unloadchunk;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import me.colingrimes.midnight.command.Command;
import me.colingrimes.midnight.command.handler.util.ArgumentList;
import me.colingrimes.midnight.command.handler.util.CommandProperties;
import me.colingrimes.midnight.command.handler.util.Sender;
import me.colingrimes.midnight.util.io.Logger;
import me.colingrimes.packets.Packets;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

/**
 * Testing Packet 7.1.34 - unload target chunk.
 * @see <a href=https://minecraft.wiki/w/Java_Edition_protocol#Unload_Chunk>Unload Chunk</a>
 */
public class UnloadChunk implements Command<Packets> {

	private final ProtocolManager protocol = ProtocolLibrary.getProtocolManager();

	@Override
	public void execute(@Nonnull Packets plugin, @Nonnull Sender sender, @Nonnull ArgumentList args) {
		RayTraceResult result = sender.player().rayTraceBlocks(100);
		if (result == null || result.getHitBlock() == null) {
			return;
		}

		Block block = result.getHitBlock();
		Chunk chunk = block.getChunk();
		unloadChunk(sender.player(), chunk);
	}

	@Override
	public void configureProperties(@Nonnull CommandProperties properties) {
		properties.setPlayerRequired(true);
	}

	/**
	 * Unloads the chunk for the specified player.
	 *
	 * @param player the player
	 * @param chunk the chunk to unload
	 */
	private void unloadChunk(@Nonnull Player player, @Nonnull Chunk chunk) {
		PacketContainer packet = protocol.createPacket(PacketType.Play.Server.UNLOAD_CHUNK);
		packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));

		try {
			protocol.sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			Logger.warn("unloadChunk() packet did not get sent properly.");
		}
	}
}
