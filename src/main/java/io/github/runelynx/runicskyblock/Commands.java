/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicskyblock;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import static org.bukkit.Bukkit.getLogger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.connorlinfoot.titleapi.TitleAPI;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.wasteofplastic.askyblock.ASkyBlockAPI;

;

//import me.libraryaddict.disguise.DisguiseAPI;

//import me.libraryaddict.disguise.disguisetypes.DisguiseType;
//import me.libraryaddict.disguise.disguisetypes.MobDisguise;
//import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
//import mkremins.fanciful.FancyMessage;

/**
 * 
 * @author Andrewxwsaa
 */
public class Commands implements CommandExecutor {

	Ranks rank = new Ranks();

	// pointer to your main class, not required if you don't need methods fromfg
	// the main class
	private Plugin instance = RunicSkyblock.getInstance();

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label,

	String[] args) {
		// comment

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		// general approach is that errors will return immediately;
		// successful runs will return after the switch completes
		switch (cmd.getName()) {
		case "joindates":
			final Connection dbConn = MySQL.openConnection();
			int counter = 0;

			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.hasPermission("rp.staff")) {
					p.sendMessage(ChatColor.DARK_GRAY + "Staff Debug:"
							+ ChatColor.GRAY
							+ " Starting skyblock player join date update");
				}
			}

			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {

				try {
					PreparedStatement dStmt = dbConn
							.prepareStatement("UPDATE `rp_PlayerInfo` SET FirstSeenSB=? WHERE UUID=?;");
					dStmt.setLong(1, p.getFirstPlayed());
					dStmt.setString(2, p.getUniqueId().toString());
					dStmt.executeUpdate();
					dStmt.close();

				} catch (SQLException e) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"sc Failed skyblock player join date update");
				}
				counter++;
			}

			try {
				dbConn.close();
			} catch (SQLException e) {

			}

			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.hasPermission("rp.staff")) {
					p.sendMessage(ChatColor.DARK_GRAY + "Staff Debug:"
							+ ChatColor.GRAY + " Finished update. Found "
							+ counter + " Skyblock players.");
				}
			}

			break;
		case "rankup":
			Player p = ((Player) sender);
			Boolean problems = false;

			final Connection a = MySQL.openConnection();
			
			Long joinDate;

			try {
				Statement aStmt = a.createStatement();
				ResultSet playerData = aStmt
						.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `UUID` = '"
								+ p.getUniqueId().toString()
								+ "' ORDER BY `id` ASC LIMIT 1;");
				if (!playerData.isBeforeFirst()) {
					// Player doesn't exist in the DB!

					getLogger().log(
							Level.INFO,
							"[RP] Player " + p.getName()
									+ " isn't in our DB yet.");
					return true;
				} else {
					// Player does exist in the DB
					playerData.next();
					joinDate = playerData.getLong("FirstSeenSB");
				}

				a.close();
			} catch (SQLException e) {

				Bukkit.getLogger().log(
						Level.SEVERE,
						"Failed to get player's join date during skyblock rankup check - "
								+ p.getName());
				p.sendMessage("Something went wrong while looking you up in the database. Contact an admin.");
				return true;
			}
			
			float daysSinceJoin =  (new Date().getTime() - joinDate) / 86400000;
			float weeksSinceJoin =  (new Date().getTime() - joinDate) / (86400000*7);
			
			DecimalFormat df = new DecimalFormat("#.00");
			
	
			
			

			if (RunicSkyblock.perms.playerInGroup(p, "Captain")) {
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Avast, captain! Thar be no more ranks for you to climb!");
			} else if (RunicSkyblock.perms.playerInGroup(p, "QuarterMaster")) {
				// //////////////////////////////
				// QUARTERMASTER TO CAPTAIN
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of Captain...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 20000) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "20000 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "20000 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 12)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "12 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "12 weeks in Skyblock ");
					
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					
					
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.quartermaster")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"Captain");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"QuarterMaster");
					congratsPromotion(p.getDisplayName(), "Captain");
					logPromotion(p.getName(), "Captain", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "FirstMate")) {
				// //////////////////////////////
				// FIRSTMATE TO QUARTERMASTER
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of QuarterMaster...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 15000) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "15000 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "15000 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 10)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "10 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "10 weeks in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.firstmate")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"QuarterMaster");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"FirstMate");
					congratsPromotion(p.getDisplayName(), "QuarterMaster");
					logPromotion(p.getName(), "QuarterMaster", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "Swashbuckler")) {
				// //////////////////////////////
				// SWASHBUCKLER TO FIRSTMATE
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of FirstMate...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 12500) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "12500 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "12500 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 8)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "8 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "8 weeks in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.swashbuckler")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"FirstMate");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"Swashbuckler");
					congratsPromotion(p.getDisplayName(), "FirstMate");
					logPromotion(p.getName(), "FirstMate", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "Gunner")) {
				// //////////////////////////////
				// GUNNER TO SWASHBUCKLER
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of Swashbuckler...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 10000) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "10000 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "10000 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 6)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "6 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "6 weeks in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.gunner")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"Swashbuckler");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"Gunner");
					congratsPromotion(p.getDisplayName(), "Swashbuckler");
					logPromotion(p.getName(), "Swashbuckler", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "Navigator")) {
				// //////////////////////////////
				// Navigator to Gunner
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of Gunner...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 7500) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "7500 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "7500 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 5)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "5 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "5 weeks in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.navigator")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"Gunner");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"Navigator");
					congratsPromotion(p.getDisplayName(), "Gunner");
					logPromotion(p.getName(), "Gunner", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "PowderMonkey")) {
				// //////////////////////////////
				// POWDERMONKEY TO NAVIGATOR
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of Navigator...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 5000) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "5000 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "5000 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 4)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "4 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "4 weeks in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.powdermonkey")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"Navigator");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"PowderMonkey");
					congratsPromotion(p.getDisplayName(), "Navigator");
					logPromotion(p.getName(), "Navigator", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "Striker")) {
				// //////////////////////////////
				// STRIKER TO POWDERMONKEY
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of PowderMonkey...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 1500) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "1500 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "1500 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				// 86400000 = 1 day
				if (joinDate < (new Date().getTime() - 86400000 * 7 * 3)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "3 weeks in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "3 weeks in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(weeksSinceJoin) + " weeks");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.striker")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY
							+ "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY
							+ "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"PowderMonkey");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"Striker");
					congratsPromotion(p.getDisplayName(), "PowderMonkey");
					logPromotion(p.getName(), "PowderMonkey", new Date().getTime());
				}

			} else if (RunicSkyblock.perms.playerInGroup(p, "Scallywag")) {
				// //////////////////////////////
				// SCALLYWAG TO STRIKER
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of Striker...");

				// CHECK MONEY
				if (RunicSkyblock.economy.getBalance(p.getName()) >= 500) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "500 Runics ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "500 Runics ");
					p.sendMessage(ChatColor.GRAY
							+ "You only have " + RunicSkyblock.economy.getBalance(p.getName()) + " Runics");
					problems = true;
				}
				
				// CHECK TIME
				if (joinDate < (new Date().getTime() - 864000000)) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "10 days in Skyblock ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "10 days in Skyblock ");
					p.sendMessage(ChatColor.GRAY
							+ "You've been in Skyblock for " + df.format(daysSinceJoin) + " days");
					problems = true;
				}

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.scallywag")) {
					p.sendMessage(ChatColor.GREEN + "✔ " +ChatColor.GRAY + "Challenges Completed ");

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"Striker");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"Scallywag");
					congratsPromotion(p.getDisplayName(), "Striker");
					logPromotion(p.getName(), "Striker", new Date().getTime());
				}
			} else if (RunicSkyblock.perms.playerInGroup(p, "Swabbie")) {
				// //////////////////////////////
				// SWABBIE TO SCALLYWAG
				// ////////////////////////////
				p.sendMessage(ChatColor.DARK_AQUA
						+ "Let's see if ye be prepared for the rank of Scallywag...");

				// CHECK CHALLENGES
				if (p.hasPermission("rp.challenges.swabbie")) {
					p.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Challenges Completed ");
							

				} else {
					p.sendMessage(ChatColor.RED + "✘ " + ChatColor.GRAY + "Challenges Completed ");
					problems = true;
				}

				if (!problems) {
					RunicSkyblock.perms.playerAddGroup("ASkyBlock", p,
							"Scallywag");
					RunicSkyblock.perms.playerRemoveGroup("ASkyBlock", p,
							"Swabbie");
					congratsPromotion(p.getDisplayName(), "Scallywag");
					logPromotion(p.getName(), "Scallywag", new Date().getTime());
				}
			}

			
			
			Bukkit.getServer().getScheduler()
			.scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pex reload");
				}
			}, 30);

			break;
		case "sbtest":
			HashMap<String, Boolean> chalMap = ASkyBlockAPI.getInstance()
					.getChallengeStatus(((Player) sender).getUniqueId());
			Iterator it = chalMap.entrySet().iterator();
			String color = ChatColor.RED + "";

			while (it.hasNext()) {

				Map.Entry pair = (Map.Entry) it.next();
				if (pair.getValue().toString().equals("true")) {
					color = ChatColor.GREEN + "";
				}
				((Player) sender).sendMessage(pair.getKey() + " | " + color
						+ pair.getValue());
				it.remove(); // avoids a ConcurrentModificationException
				color = ChatColor.RED + "";
			}
			break;
		case "headofplayer":
		case "face":

			if (args.length == 1) {
				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();
					ResultSet playerData = dStmt
							.executeQuery("SELECT * FROM `rp_HeadCreations` WHERE `PlayerName` = '"
									+ sender.getName()
									+ "' AND `Timestamp` >= "
									+ (new Date().getTime() - 21600000)
									+ " ORDER BY `ID` DESC LIMIT 1;");

					if (playerData.isBeforeFirst()) {
						playerData.next();
						Long currentTime = new Date().getTime();
						Long loggedTime = playerData.getLong("Timestamp");
						Double diffHours = (currentTime - loggedTime)
								/ (60.0 * 60 * 1000);
						sender.sendMessage(ChatColor.RED
								+ "You can only use this command once every 6 hours. You last used it "
								+ diffHours + " hours ago.");

					} else {
						// No record found, proceed!
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
								"give " + sender.getName()
										+ " 397 1 3 {SkullOwner: " + args[0]
										+ "}");
						try {

							PreparedStatement insertStmt = d
									.prepareStatement("INSERT INTO rp_HeadCreations (PlayerName, UUID, Timestamp, HeadRequested) VALUES "
											+ "('"
											+ sender.getName()
											+ "', '"
											+ ((Player) sender).getUniqueId()
													.toString()
											+ "', "
											+ (new Date().getTime())
											+ ", '"
											+ args[0] + "');");
							insertStmt.executeUpdate();
							d.close();
							dStmt.close();

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Failed face record creation "
											+ e.getMessage());
						}
					}

					d.close();
				} catch (SQLException e) {
					getLogger().log(Level.SEVERE,
							"Failed /face check" + e.getMessage());
				}
			}// end if checking arg length
			else {
				sender.sendMessage(ChatColor.DARK_RED
						+ "Usage: "
						+ ChatColor.AQUA
						+ "/face <playername>"
						+ ChatColor.DARK_RED
						+ " Watch your spelling, you only get ONE chance every 6 hours!! Always enter FULL player names, NOT nicks!");
			}

			break;
		case "ranks":
			if (sender instanceof Player) {
				rank.showRequirements((Player) sender);
			}
			break;
		case "seeker":

			if (args.length == 0) {
				return true;
			}
			// tell the other server this one is reconnected to the universe
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Forward"); // So BungeeCord knows to forward it
			out.writeUTF("ONLINE");
			out.writeUTF("Seeker"); // The channel name to check if this
									// your data

			ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			DataOutputStream msgout = new DataOutputStream(msgbytes);

			try {
				msgout.writeUTF(args[0]); // You can do anything
				// msgout
				msgout.writeShort(123);
			} catch (IOException e) {
			}

			out.writeShort(msgbytes.toByteArray().length);
			out.write(msgbytes.toByteArray());

			// If you don't care about the player
			// Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(),
			// null);
			// Else, specify them

			((Player) sender).sendPluginMessage(instance, "BungeeCord",
					out.toByteArray());
			break;
		case "testerchat":
		case "tc":
			String senderName = "";
			if (sender instanceof Player) {
				Player player = (Player) sender;
				senderName = sender.getName();
			} else {
				senderName = "Console";

			}

			StringBuilder buffer = new StringBuilder();
			// change the starting i value to pick what argument to start from
			// 1 is the 2nd argument.
			for (int i = 0; i < args.length; i++) {
				buffer.append(' ').append(args[i]);
			}

			for (Player p1 : Bukkit.getOnlinePlayers()) {

				if (p1.hasPermission("rp.testers")) {
					if (args.length == 0) {
						Player player = (Player) sender;
						player.sendMessage(ChatColor.DARK_GRAY
								+ "Tester chat. Usage: /tc [message]");
						return true;
					} else {

						p1.sendMessage(ChatColor.DARK_GRAY + "["
								+ ChatColor.DARK_PURPLE + "Tester"
								+ ChatColor.LIGHT_PURPLE + "Chat"
								+ ChatColor.DARK_GRAY + "] " + ChatColor.WHITE
								+ senderName + ":" + ChatColor.LIGHT_PURPLE
								+ buffer.toString());

					}
				}
			}
			getLogger().log(Level.INFO,
					"[TesterChat] " + senderName + ": " + buffer.toString());
			break;
		case "staffchat":
		case "sc":

			String senderName1 = "";
			if (sender instanceof Player) {
				Player player = (Player) sender;
				senderName1 = sender.getName();
			} else {
				senderName1 = "Console";

			}

			StringBuilder buffer1 = new StringBuilder();
			// change the starting i value to pick what argument to start from
			// 1 is the 2nd argument.
			for (int i = 0; i < args.length; i++) {
				buffer1.append(' ').append(args[i]);
			}

			for (Player p1 : Bukkit.getOnlinePlayers()) {

				if (p1.hasPermission("rp.staff")) {
					if (args.length == 0) {
						Player player = (Player) sender;
						player.sendMessage(ChatColor.DARK_GRAY
								+ "Staff chat. Usage: /sc [message]");
						return true;
					} else {

						p1.sendMessage(ChatColor.DARK_GRAY + "["
								+ ChatColor.DARK_AQUA + "Staff"
								+ ChatColor.AQUA + "Chat" + ChatColor.DARK_GRAY
								+ "] " + ChatColor.WHITE + senderName1 + ":"
								+ ChatColor.AQUA + buffer1.toString());

					}
				}
			}
			getLogger().log(Level.INFO,
					"[StaffChat] " + senderName1 + ": " + buffer1.toString());
			break;
		default:
			break;
		}

		return true;
	}

	public boolean addAttemptedPromotion(String newGuyName, String promoterName) {

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			final Connection dbCon = MySQL.openConnection();

			String simpleProc = "{ call Add_Attempted_Promotion_Record(?, ?) }";
			CallableStatement cs = dbCon.prepareCall(simpleProc);
			cs.setString("NewPlayerName_param", Bukkit.getPlayer(newGuyName)
					.getName());
			cs.setString("PromoterName_param", Bukkit.getPlayer(promoterName)
					.getName());
			cs.executeUpdate();

			cs.close();
			dbCon.close();

			return true;

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed addAttemptedPromotion - " + z.getMessage());
			return false;
		}

	}

	public int checkAttemptedPromotion(String newGuyName, String promoterName) {

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection dbCon = MySQL.openConnection();

			String simpleProc = "{ call Count_Attempted_Promotion_Records(?, ?, ?) }";
			CallableStatement cs = dbCon.prepareCall(simpleProc);
			cs.setString("NewPlayerName_param", Bukkit.getPlayer(newGuyName)
					.getName());
			cs.setString("PromoterName_param", Bukkit.getPlayer(promoterName)
					.getName());
			cs.registerOutParameter("resultCount", java.sql.Types.INTEGER);
			cs.executeUpdate();

			int result = cs.getInt("resultCount");

			cs.close();
			dbCon.close();

			return result;

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed checkAttemptedPromotion - " + z.getMessage());
			return 0;
		}

	}

	public void congratsPromotion(String promoted, String newRank) {

		// RG.sendMessage(true, promoted, ChatColor.GOLD
		// + "[RunicRanks] Congratulations, " + promoted
		// + ", on promoting to " + newRank + "!");

		for (Player p : Bukkit.getOnlinePlayers()) {
			TitleAPI.sendTitle(p, 2, 3, 2,
					RunicSkyblock.rankColors.get(newRank) + "" + ChatColor.BOLD
							+ promoted, RunicSkyblock.rankColors.get(newRank)
							+ "has reached the rank of " + newRank);
		}
		
		

	}
	
	public static void logPromotion(String playerName, String newRank,
			Long timestamp) {
		final Plugin instance = RunicSkyblock.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {

			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			int tempD = dStmt
					.executeUpdate("INSERT INTO rp_PlayerPromotions (`PlayerName`, `NewRank`, `TimeStamp`, `Server`) VALUES "
							+ "('"
							+ playerName
							+ "', '"
							+ newRank
							+ "', "
							+ timestamp + ", 'Skyblock');");
			d.close();

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed DB check for restore grave cuz " + z.getMessage());
		}
	}

}
