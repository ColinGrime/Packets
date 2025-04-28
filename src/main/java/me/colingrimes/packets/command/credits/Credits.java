package me.colingrimes.packets.command.credits;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import me.colingrimes.midnight.command.Command;
import me.colingrimes.midnight.command.handler.util.ArgumentList;
import me.colingrimes.midnight.command.handler.util.CommandProperties;
import me.colingrimes.midnight.command.handler.util.Sender;
import me.colingrimes.midnight.util.io.Logger;
import me.colingrimes.packets.Packets;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

/**
 * Testing Packet 7.1.35 - rolling credits for no reason.
 * @see <a href=https://minecraft.wiki/w/Java_Edition_protocol#Game_Event>Game Event</a>
 */
public class Credits implements Command<Packets> {

	private final ProtocolManager protocol = ProtocolLibrary.getProtocolManager();

	@Override
	public void execute(@Nonnull Packets plugin, @Nonnull Sender sender, @Nonnull ArgumentList args) {
		rollCredits(sender.player());
	}

	@Override
	public void configureProperties(@Nonnull CommandProperties properties) {
		properties.setPlayerRequired(true);
	}

	/**
	 * Rolls the credits for the specified player.
	 *
	 * @param player the player
	 */
	private void rollCredits(@Nonnull Player player) {
		PacketContainer packet = protocol.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
		packet.getGameStateIDs().write(0, 4);
		packet.getFloat().write(0, (float) 1);

		try {
			protocol.sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			Logger.warn("rollCredits() packet did not get sent properly.");
		}
	}
}
