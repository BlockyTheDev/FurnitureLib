package de.Ste3et_C0st.FurnitureLib.Listener.render;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;

import de.Ste3et_C0st.FurnitureLib.main.FurnitureLib;

public class RenderWithProtocols extends RenderEventHandler{

	private final PacketListener chunkLoadListener, chunkUnloadListener;
	
	public RenderWithProtocols() {
		this.chunkLoadListener = onChunkLoad();
		this.chunkUnloadListener = onChunkUnload();
	}
	
	@Override
	public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(chunkLoadListener);
        ProtocolLibrary.getProtocolManager().addPacketListener(chunkUnloadListener);
	}

	@Override
	public void remove() {
		ProtocolLibrary.getProtocolManager().removePacketListener(chunkLoadListener);
		ProtocolLibrary.getProtocolManager().removePacketListener(chunkUnloadListener);
	}

	private PacketListener onChunkLoad() {
		return new PacketAdapter(FurnitureLib.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK) {
			public void onPacketSending(PacketEvent event) {
				int chunkX = event.getPacket().getIntegers().read(0);
				int chunkZ = event.getPacket().getIntegers().read(1);
				final Player player = event.getPlayer();
				Bukkit.getScheduler().runTask(FurnitureLib.getInstance(), () -> {
					World world = player.getWorld();
					getFurnitureManager().updatePlayerView(player, chunkX, chunkZ, world);
				});
			}
		};
	}
	
	private PacketListener onChunkUnload() {
		return new PacketAdapter(FurnitureLib.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Server.UNLOAD_CHUNK) {
			public void onPacketSending(PacketEvent event) {
				int chunkX = event.getPacket().getIntegers().read(0);
				int chunkZ = event.getPacket().getIntegers().read(1);
				final Player player = event.getPlayer();
				Bukkit.getScheduler().runTask(FurnitureLib.getInstance(), () -> {
					World world = player.getWorld();
					getFurnitureManager().destroyChunkPlayerView(player, chunkX, chunkZ, world);
				});
			}
		};
	}
	
}
