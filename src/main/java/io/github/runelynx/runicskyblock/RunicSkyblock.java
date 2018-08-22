package io.github.runelynx.runicskyblock;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

/**
 * Hello world!
 *
 */

public class RunicSkyblock extends JavaPlugin implements PluginMessageListener, Listener {

	private static Plugin instance;
	public String SERVER_NAME = "Unknown";
	public static Permission perms = null;
	public static Economy economy = null;

	public static HashMap<String, ChatColor> rankColors = new HashMap<String, ChatColor>();

	@Override
	public void onEnable() {

		instance = this;

		getServer().getPluginManager().registerEvents(this, this);

		Bukkit.getLogger().log(Level.INFO, "RunicSkyblock plugin is loading..");

		getConfig().options().copyDefaults(true);
		saveConfig();

		SERVER_NAME = instance.getConfig().getString("ServerName");

		setupPermissions();
		setupEconomy();

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

		getCommand("seeker").setExecutor(new Commands());
		getCommand("sbtest").setExecutor(new Commands());
		getCommand("rankup").setExecutor(new Commands());
		getCommand("joindates").setExecutor(new Commands());

		for (Player p : Bukkit.getOnlinePlayers()) {
			p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "...starting up RunicSkyblock plugin...");
		}

		rankColors.put("Skypirate", ChatColor.DARK_GRAY);
		rankColors.put("Swabbie", ChatColor.GREEN);
		rankColors.put("Scallywag", ChatColor.DARK_GREEN);
		rankColors.put("Striker", ChatColor.YELLOW);
		rankColors.put("PowderMonkey", ChatColor.GOLD);
		rankColors.put("Navigator", ChatColor.AQUA);
		rankColors.put("Gunner", ChatColor.DARK_AQUA);
		rankColors.put("Swashbuckler", ChatColor.BLUE);
		rankColors.put("FirstMate", ChatColor.LIGHT_PURPLE);
		rankColors.put("QuarterMaster", ChatColor.DARK_PURPLE);
		rankColors.put("Quartermaster", ChatColor.DARK_PURPLE);
		rankColors.put("Captain", ChatColor.RED);

		Bukkit.getLogger().log(Level.INFO, "RunicSkyblock plugin is loaded!");

		final MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"), "3306", "rpgame",
				instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
		Connection z = MySQL.openConnection();
		try {
			// clear the table
			Statement insertStmt = z.createStatement();
			getLogger().log(Level.SEVERE, "Connected to MySQL successfully. ");
			z.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed MySQL connection test in onEnable: " + e.getMessage());
		}
		getLogger().log(Level.INFO, "Debug: " + instance.getConfig().getString("debug"));

	}

	@Override
	public void onDisable() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "...shutting down RunicSkyblock plugin...");
		}
	}

	public static Plugin getInstance() {
		return instance;
	}

	@EventHandler
	// When player breaks a block:
	public void onBlockBreak(BlockBreakEvent event) {
		// if Block is a leaf Block and Player's held item
		if (event.getBlock().getType().equals(Material.LEAVES)) {

			int randomNumber = 1 + (int)(Math.random() * 100); 

			if (randomNumber >= 94) {
				event.getPlayer().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(),
						new ItemStack(Material.APPLE, 1));
				event.getPlayer().sendMessage("You found a shiny red apple!");
			}

		}
	}

	public void onPluginMessageReceived(String channel, Player player, byte[] message) {

		if (!channel.equals("BungeeCord")) {
			return;
		}

		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subChannel = in.readUTF();
		short len = in.readShort();
		byte[] msgbytes = new byte[len];
		in.readFully(msgbytes);

		DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
		try {
			String somedata = msgin.readUTF();
			short somenumber = msgin.readShort();
			/*
			 * if (subChannel.equals("Chat")) { for (Player p :
			 * Bukkit.getOnlinePlayers()) { p.sendMessage(somedata); } }
			 */
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // Read the data in the same way you wrote it

	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent pje) {

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
			public void run() {
				if (pje.getPlayer().getFirstPlayed() >= (new Date().getTime() - 20000)) {
					// if player was first seen in skyblock within the
					// last 20 seconds... update their first seen date i
					// nDB

					MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
							instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
							instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
					final Connection dbConn = MySQL.openConnection();

					try {
						PreparedStatement dStmt = dbConn
								.prepareStatement("UPDATE `rp_PlayerInfo` SET FirstSeenSB=? WHERE UUID=?;");
						dStmt.setLong(1, pje.getPlayer().getFirstPlayed());
						dStmt.setString(2, pje.getPlayer().getUniqueId().toString());
						dStmt.executeUpdate();
						dStmt.close();

					} catch (SQLException e) {

					}
					Bukkit.getLogger().log(Level.INFO,
							"Updated Skyblock FirstSeen for " + pje.getPlayer().getDisplayName());

					try {
						dbConn.close();
					} catch (SQLException e) {

					}
				}
			}
		}, 60);
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent pqe) {

		int rankNum = 1;
		String rankText = "Swabbie";

		switch (perms.getPrimaryGroup(pqe.getPlayer())) {

		case "Swabbie":
			rankNum = 1;
			rankText = "Swabbie";
			break;
		case "Scallywag":
			rankNum = 2;
			rankText = "Scallywag";
			break;
		case "Striker":
			rankNum = 3;
			rankText = "Striker";
			break;
		case "PowderMonkey":
			rankNum = 4;
			rankText = "PowderMonkey";
			break;
		case "Navigator":
			rankNum = 5;
			rankText = "Navigator";
			break;
		case "Gunner":
			rankNum = 6;
			rankText = "Gunner";
			break;
		case "Swashbuckler":
			rankNum = 7;
			rankText = "Swashbuckler";
			break;
		case "FirstMate":
			rankNum = 8;
			rankText = "FirstMate";
			break;
		case "Quartermaster":
			rankNum = 9;
			rankText = "Quartermaster";
			break;
		case "Captain":
			rankNum = 10;
			rankText = "Captain";
			break;
		default:
			break;

		}

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
				instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
				instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
		final Connection dbConn = MySQL.openConnection();

		try {
			PreparedStatement dStmt = dbConn
					.prepareStatement("UPDATE `rp_PlayerInfo` SET SkyblockRank=?, SkyblockRankText=? WHERE UUID=?;");
			dStmt.setInt(1, rankNum);
			dStmt.setString(2, rankText);
			dStmt.setString(3, pqe.getPlayer().getUniqueId().toString());
			dStmt.executeUpdate();
			dStmt.close();

		} catch (SQLException e) {

		}

		try {
			dbConn.close();
		} catch (SQLException e) {

		}

	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) throws IOException {

		String staffPrefix = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "SB ";

		// //// HANDLE STAFF
		if (event.getPlayer().hasPermission("rp.staff")) {
			if ((event.getPlayer().hasPermission("rp.staff.admin"))) {
				staffPrefix += ChatColor.DARK_RED + "<Admin> ";
			} else if ((event.getPlayer().hasPermission("rp.staff.mod+"))) {
				staffPrefix += ChatColor.DARK_RED + "<Mod+> ";
			} else if ((event.getPlayer().hasPermission("rp.staff.mod"))) {
				staffPrefix += ChatColor.DARK_RED + "<Mod> ";
			} else if ((event.getPlayer().hasPermission("rp.staff.director"))) {
				staffPrefix += ChatColor.DARK_RED + "<Director> ";
			} else if ((event.getPlayer().hasPermission("rp.staff.architect"))) {
				staffPrefix += ChatColor.DARK_RED + "<Architect> ";
			} else if ((event.getPlayer().hasPermission("rp.staff.enforcer"))) {
				staffPrefix += ChatColor.DARK_RED + "<Enforcer> ";
			} else if ((event.getPlayer().hasPermission("rp.staff.helper"))) {
				staffPrefix += ChatColor.DARK_RED + "<Helper> ";
			}

		} else if (event.getPlayer().hasPermission("rp.guide")) {
			staffPrefix += ChatColor.DARK_GREEN + "<Guide> ";
		}

		if (RunicSkyblock.rankColors.get(perms.getPrimaryGroup(event.getPlayer())) == null) {
			event.getPlayer()
					.sendMessage(ChatColor.DARK_RED + "ERROR! " + ChatColor.WHITE + "Your primary group is "
							+ ChatColor.YELLOW + RunicSkyblock.rankColors.get(perms.getPrimaryGroup(event.getPlayer()))
							+ ChatColor.WHITE + " but there is no color associated to that. Notify Rune!");
		}

		event.setFormat(staffPrefix + RunicSkyblock.rankColors.get(perms.getPrimaryGroup(event.getPlayer()))
				+ perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY + " "
				+ RunicSkyblock.rankColors.get(perms.getPrimaryGroup(event.getPlayer()))
				+ event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");

	}

	@EventHandler
	public void processServerWarpPortals(PlayerMoveEvent event) {

		// player has entered the portal in the Hub cave
		if (event.getTo().getWorld().getName().equalsIgnoreCase("world")) {
			if ((event.getTo().getX() >= 1779 && event.getTo().getX() <= 1781)
					&& (event.getTo().getY() >= 13 && event.getTo().getY() <= 14)
					&& (event.getTo().getZ() >= 344 && event.getTo().getZ() <= 346)) {

				if (!event.getPlayer().hasPermission("ru.astrid.survival")) {
					perms.playerAdd(event.getTo().getWorld().getName(), event.getPlayer(), "ru.astrid.survival");
					event.getPlayer().sendMessage(
							ChatColor.ITALIC + "" + ChatColor.DARK_AQUA + "You have unlocked the Survival warp!");
					event.getPlayer().teleport(new Location(event.getTo().getWorld(), 1786.495, 16.0, 345.55));
					showHubAstridTravelMenu(event.getPlayer());
				} else {
					event.getPlayer().teleport(new Location(event.getTo().getWorld(), 1786.495, 16.0, 345.55));
					showHubAstridTravelMenu(event.getPlayer());
				}

			}

		}

	}

	public void showHubAstridTravelMenu(Player p) {

		Inventory faithInventory = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "Astrid Warp Menu");

		ItemMeta meta;
		Random random = new Random();
		ArrayList<String> treeLore = new ArrayList<String>();
		treeLore.add(ChatColor.YELLOW + "Astrid is the great tree at the heart of");
		treeLore.add(ChatColor.YELLOW + "Runic Universe. She has the power to send");
		treeLore.add(ChatColor.YELLOW + "you between worlds. Just click where you want to go!");
		treeLore.add(ChatColor.YELLOW + "You must find the relevant portal");
		treeLore.add(ChatColor.YELLOW + "near Astrid to unlock it here, though.");

		ItemStack tree = new ItemStack(Material.SAPLING, 1, (short) 3);
		meta = tree.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Astrid Warp Menu");
		meta.setLore(treeLore);
		tree.setItemMeta(meta);

		ItemStack flower1 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower1.getItemMeta();
		meta.setDisplayName("");
		flower1.setItemMeta(meta);
		ItemStack flower2 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower2.getItemMeta();
		meta.setDisplayName("");
		flower2.setItemMeta(meta);
		ItemStack flower3 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower3.getItemMeta();
		meta.setDisplayName("");
		flower3.setItemMeta(meta);
		ItemStack flower4 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower4.getItemMeta();
		meta.setDisplayName("");
		flower4.setItemMeta(meta);
		ItemStack flower5 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower5.getItemMeta();
		meta.setDisplayName("");
		flower5.setItemMeta(meta);
		ItemStack flower6 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower6.getItemMeta();
		meta.setDisplayName("");
		flower6.setItemMeta(meta);
		ItemStack flower7 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower7.getItemMeta();
		meta.setDisplayName("");
		flower7.setItemMeta(meta);
		ItemStack flower8 = new ItemStack(Material.RED_ROSE, 1, (short) random.nextInt(8 + 1));
		meta = flower8.getItemMeta();
		meta.setDisplayName("");
		flower8.setItemMeta(meta);

		faithInventory.setItem(0, flower1);
		faithInventory.setItem(1, flower2);
		faithInventory.setItem(2, flower3);
		faithInventory.setItem(3, flower4);
		faithInventory.setItem(4, tree);
		faithInventory.setItem(5, flower5);
		faithInventory.setItem(6, flower6);
		faithInventory.setItem(7, flower7);
		faithInventory.setItem(8, flower8);

		ItemStack skyblockIcon;
		ItemStack survivalIcon;

		if (p.hasPermission("ru.astrid.survival")) {

			survivalIcon = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);

			meta = survivalIcon.getItemMeta();
			meta.setDisplayName("Warp to Survival");
			survivalIcon.setItemMeta(meta);
		} else {
			survivalIcon = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);

			ArrayList<String> survivalIconLore = new ArrayList<String>();
			survivalIconLore.add(ChatColor.RED + "Find the portal to the survival world");
			survivalIconLore.add(ChatColor.RED + "somewhere around Astrid to unlock this warp.");

			meta = survivalIcon.getItemMeta();
			meta.setDisplayName("Survival Warp Locked");
			meta.setLore(survivalIconLore);
			survivalIcon.setItemMeta(meta);
		}

		if (p.hasPermission("ru.astrid.skyblock")) {
			skyblockIcon = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 3);

			meta = skyblockIcon.getItemMeta();
			meta.setDisplayName("Warp to Skyblock");
			skyblockIcon.setItemMeta(meta);
		} else {
			skyblockIcon = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);

			ArrayList<String> skyblockIconLore = new ArrayList<String>();
			skyblockIconLore.add(ChatColor.RED + "Find the portal to the skyblock world");
			skyblockIconLore.add(ChatColor.RED + "somewhere around Astrid to unlock this warp.");

			meta = skyblockIcon.getItemMeta();
			meta.setDisplayName("Skyblock Warp Locked");
			meta.setLore(skyblockIconLore);
			skyblockIcon.setItemMeta(meta);
		}

		faithInventory.setItem(20, survivalIcon);
		faithInventory.setItem(24, skyblockIcon);

		p.openInventory(faithInventory);

	}

	@SuppressWarnings("deprecation")
	public void updatePlayerInfoOnJoin(String name, UUID pUUID) {
		final Date now = new Date();
		final String playerName = name;
		final UUID playerUUID = pUUID;

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
				instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
				instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
		final Connection dbConn = MySQL.openConnection();
		int rowCount = -1;
		int rowCountnameMatch = -1;

		try {
			PreparedStatement dStmt = dbConn
					.prepareStatement("SELECT COUNT(*) as Total FROM rp_PlayerInfo WHERE UUID = ?;");
			dStmt.setString(1, playerUUID.toString());
			ResultSet dbResult = dStmt.executeQuery();
			while (dbResult.next()) {
				rowCount = dbResult.getInt("Total");
			}
			dStmt.close();

			PreparedStatement zStmt = dbConn
					.prepareStatement("SELECT COUNT(*) as Total FROM rp_PlayerInfo WHERE PlayerName = ?;");
			zStmt.setString(1, playerName);
			ResultSet zResult = zStmt.executeQuery();
			while (zResult.next()) {
				rowCountnameMatch = zResult.getInt("Total");
			}
			zStmt.close();

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Cant check for row count in updatePlayerInfoOnJoin for " + playerName
					+ " because: " + e.getMessage());
		}

		if (rowCount != rowCountnameMatch) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Name change detected for " + playerName);
			Bukkit.getLogger().log(Level.INFO, "[RP] Name change detected for " + playerName);
		}

		try {

			Long firstPlayedSB;

			if (Bukkit.getOfflinePlayer(pUUID).hasPlayedBefore()) {
				firstPlayedSB = Bukkit.getOfflinePlayer(pUUID).getFirstPlayed();
			} else {
				firstPlayedSB = now.getTime();
			}

			// if this player has no rows in the table yet
			if (rowCount == 0) {

				// /////////////////////
				PreparedStatement dStmt = dbConn.prepareStatement(
						"INSERT INTO rp_PlayerInfo (`PlayerName`, `UUID`, `ActiveFaith`, `LastIP`, `FirstSeen`, `FirstSeenSB`, `LastSeen`) VALUES "
								+ "(?, ?, ?, ?, ?, ?);");
				dStmt.setString(1, playerName);
				dStmt.setString(2, playerUUID.toString());
				dStmt.setString(3, "Sun");
				dStmt.setString(4, Bukkit.getPlayer(playerUUID).getAddress().getAddress().getHostAddress());
				dStmt.setLong(5, now.getTime());
				dStmt.setLong(6, firstPlayedSB);
				dStmt.setLong(7, now.getTime());

				dStmt.executeUpdate();
				dStmt.close();

				// if this player has 1 row in the table
			} else if (rowCount == 1) {
				PreparedStatement dStmt = dbConn.prepareStatement(
						"UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=?, LastIP=? WHERE UUID=?;");
				dStmt.setLong(1, now.getTime());
				dStmt.setString(2, playerName);
				dStmt.setString(3, Bukkit.getPlayer(playerUUID).getAddress().getAddress().getHostAddress());
				dStmt.setString(4, playerUUID.toString());
				dStmt.executeUpdate();
				dStmt.close();
				Bukkit.getLogger().log(Level.INFO, "[RP] PlayerInfo data updated for " + playerName);

				// if this player has MORE than 1 row in the
				// table
			} else if (rowCount > 1) {
				int counter = 1;
				PreparedStatement zStmt = dbConn
						.prepareStatement("SELECT * FROM rp_PlayerInfo WHERE UUID = ? ORDER BY ID ASC;");
				zStmt.setString(1, playerUUID.toString());
				ResultSet zResult = zStmt.executeQuery();
				while (zResult.next()) {
					// The first row is our valid one - update
					// it!
					if (counter == 1) {
						PreparedStatement dStmt = dbConn
								.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=? WHERE UUID=?;");
						dStmt.setLong(1, now.getTime());
						dStmt.setString(2, playerName);
						dStmt.setString(3, playerUUID.toString());
						dStmt.executeUpdate();
						dStmt.close();

						Bukkit.getLogger().log(Level.INFO,
								"[RP] PlayerInfo data [row " + zResult.getInt("ID") + "] updated for " + playerName);
						// All further rows are invalid, delete
						// them!
					} else if (counter > 1) {
						PreparedStatement dStmt = dbConn
								.prepareStatement("DELETE FROM `rp_PlayerInfo` WHERE ID = ? LIMIT 1;");
						dStmt.setInt(1, zResult.getInt("ID"));
						dStmt.executeUpdate();
						dStmt.close();
						Bukkit.getLogger().log(Level.INFO,
								"[RP] PlayerInfo dupe row cleanup (name change?)! Deleted row " + zResult.getInt("ID"));
					}

					counter++;
				}
				zStmt.close();

			}

			dbConn.close();

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Cant work with DB updatePlayerInfoOnJoin for " + playerName + " because: " + e.getMessage());
		}

	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
				.getRegistration(net.milkbowl.vault.economy.Economy.class);
		economy = rsp.getProvider();
		return economy != null;
	}

}