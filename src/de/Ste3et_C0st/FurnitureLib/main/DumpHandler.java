package de.Ste3et_C0st.FurnitureLib.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.Ste3et_C0st.FurnitureLib.main.Type.ProtocolFields;
import de.Ste3et_C0st.FurnitureLib.main.Type.SQLAction;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class DumpHandler {

	public DumpHandler(CommandSender sender) {
		JsonObject coreObject = new JsonObject();
		
		/*
		 * Add Server Informations !
		 */
		
		JsonObject spigotObject = new JsonObject();
		spigotObject.addProperty("serverVersion", Bukkit.getVersion());
		spigotObject.addProperty("platformName", Bukkit.getServer().getName());
		spigotObject.addProperty("platformVersion", Type.version);
		spigotObject.addProperty("javaVersion", System.getProperty("java.version"));
		spigotObject.addProperty("operatingSystem", System.getProperty("os.name"));
		
		/*
		 * Add Furniture Informations !
		 */
		
		AtomicInteger armorStands = new AtomicInteger(0);
		Predicate<ObjectID> filter = entry -> Objects.nonNull(entry) && SQLAction.REMOVE != entry.getSQLAction();
		FurnitureManager.getInstance().getObjectList().stream().filter(filter).forEach(obj -> {
			armorStands.addAndGet(obj.getPacketList().size());
		});
		
		JsonObject packetInfos = new JsonObject();
		packetInfos.addProperty("pluginVersion", FurnitureLib.getInstance().getDescription().getVersion());
		packetInfos.addProperty("furnitureModels", FurnitureManager.getInstance().getProjects().size());
		packetInfos.addProperty("furnitureObjects", FurnitureManager.getInstance().getObjectList().size());
		packetInfos.addProperty("armorStands", armorStands.get() + "");
		packetInfos.addProperty("viewDistanceFurniture", FurnitureLib.getInstance().getViewDistance());
		packetInfos.addProperty("syncLoading", FurnitureLib.getInstance().isSync());
		packetInfos.addProperty("purgeTime", FurnitureLib.getInstance().getPurgeTime());
		packetInfos.addProperty("autoPure", FurnitureLib.getInstance().isAutoPurge());
		packetInfos.addProperty("useGamemode", FurnitureLib.getInstance().useGamemode());
		packetInfos.addProperty("language", FurnitureLib.getInstance().getConfig().getString("config.Language"));
		packetInfos.addProperty("regionMemberAccess", FurnitureLib.getInstance().haveRegionMemberAccess());
		packetInfos.addProperty("eventType", FurnitureLib.getInstance().getDefaultEventType().name());
		packetInfos.addProperty("language", FurnitureLib.getInstance().getDefaultPublicType().name());
		packetInfos.addProperty("databaseType", FurnitureLib.getInstance().getSQLManager().getDatabase().getType().name());
		
		JsonObject packetInformations = new JsonObject();
		try {
			ProtocolFields projectFields = FurnitureLib.getInstance().getField();
			packetInformations.addProperty("bitMask", projectFields.bitMask);
			packetInformations.addProperty("wrapperBit", projectFields.wrapperBit);
			packetInformations.addProperty("healthField", projectFields.healthField);
			packetInformations.addProperty("headRotation", projectFields.HeadRotation);
			packetInformations.addProperty("bodyRotation", projectFields.BodyRotation);
			packetInformations.addProperty("leftArmRotation", projectFields.LeftArmRotation);
			packetInformations.addProperty("rightArmRotation", projectFields.RightArmRotation);
			packetInformations.addProperty("leftLegRotation", projectFields.LeftLegRotation);
			packetInformations.addProperty("rightLegRotation", projectFields.RightLegRotation);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		/*
		 * Add Plugin Informations !
		 */
		
		List<JsonObject> pluginList = new ArrayList<JsonObject>();
		for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
			if(!plugin.getName().equalsIgnoreCase("FurnitureLib")) {
				JsonObject pluginInformation = new JsonObject();
				pluginInformation.addProperty("pluginName", plugin.getName());
				pluginInformation.addProperty("enabled", plugin.isEnabled());
				pluginInformation.addProperty("version", plugin.getDescription().getVersion());
				pluginInformation.addProperty("mainClass", plugin.getDescription().getMain());
				pluginInformation.add("authors", new Gson().toJsonTree(plugin.getDescription().getAuthors()));
				pluginList.add(pluginInformation);
			}
		};
		
		coreObject.add("server", spigotObject);
		coreObject.add("furnitureLib", packetInfos);
		coreObject.add("packets", packetInformations);
		coreObject.add("plugins", new Gson().toJsonTree(pluginList));
		
		this.sendToHost(coreObject, sender);
	}
	
	private void sendToHost(JsonObject dump, CommandSender sender) {
		try {
			URL url = new URL ("http://api.dicecraft.de/furniture/dump.php");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			try(OutputStream os = con.getOutputStream()) {
			    byte[] input = dump.toString().getBytes("utf-8");
			    os.write(input, 0, input.length);           
			}
			
			try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
				StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					 response.append(responseLine.trim());
				}
				if(Objects.nonNull(response)) {
					if(response.toString().equalsIgnoreCase("#insertException#")) {
						sender.sendMessage("§cThe dump can't be handeld");
					}else {
						sender.sendMessage("§7FurnitureLib dump file upload §2§lSuccess");
						ComponentBuilder builder = new ComponentBuilder("§7You can find it here: ");
						builder.append("§ehere").event(new ClickEvent(Action.OPEN_URL, "https://dicecraft.de/furniture/dump.php?id=" + response.toString().replace("#SERIAL:", "")));
						sender.spigot().sendMessage(builder.create());
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
