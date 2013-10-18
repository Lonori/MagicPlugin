package com.elmakers.mine.bukkit.plugins.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.dao.BlockData;

public class MagicPlugin extends JavaPlugin
{	
	/*
	 * Public API
	 */
	public Spells getSpells()
	{
		return spells;
	}

	/*
	 * Plugin interface
	 */

	public void onEnable() 
	{
		initialize();

		BlockData.setServer(getServer());
		PluginManager pm = getServer().getPluginManager();

		pm.registerEvents(spells, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled");
	}

	protected void initialize()
	{
		spells.initialize(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		String commandName = cmd.getName();

		if (commandName.equalsIgnoreCase("magic") && args.length > 0)
		{
			String subCommand = args[0];
			if (sender instanceof Player)
			{
				if (!spells.hasPermission((Player)sender, "Magic.commands.magic." + subCommand)) return true;
			}
			if (subCommand.equalsIgnoreCase("reload"))
			{
				spells.clear();
				spells.load();
				return true;
			}
			if (subCommand.equalsIgnoreCase("reset"))
			{   
				spells.reset();
				return true;
			}
		}

		// Everything beyond this point is is-game only
		if (!(sender instanceof Player)) return false;

		Player player = (Player)sender;

		if (commandName.equalsIgnoreCase("wand"))
		{
			String subCommand = "";
			String[] args2 = args;

			if (args.length > 0) {
				subCommand = args[0];;
				args2 = new String[args.length - 1];
				for (int i = 1; i < args.length; i++) {
					args2[i - 1] = args[i];
				}
			}

			if (subCommand.equalsIgnoreCase("add"))
			{
				if (!spells.hasPermission(player, "Magic.commands.wand." + subCommand)) return true;

				onWandAdd(player, args2);
				return true;
			}
			if (subCommand.equalsIgnoreCase("remove"))
			{   
				if (!spells.hasPermission(player, "Magic.commands.wand." + subCommand)) return true;

				onWandRemove(player, args2);
				return true;
			}

			if (subCommand.equalsIgnoreCase("name"))
			{
				if (!spells.hasPermission(player, "Magic.commands.wand." + subCommand)) return true;

				onWandName(player, args2);
				return true;
			}

			if (!spells.hasPermission(player, "Magic.commands.wand")) return true;
			return onWand(player, args);
		}

		if (commandName.equalsIgnoreCase("cast"))
		{
			if (!spells.hasPermission(player, "Magic.commands.cast")) return true;
			return onCast(player, args);
		}

		if (commandName.equalsIgnoreCase("spells"))
		{
			if (!spells.hasPermission(player, "Magic.commands.spells")) return true;
			return onSpells(player, args);
		}

		return false;
	}

	public boolean onWandAdd(Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			player.sendMessage("Use: /wand add <spell>");
			return true;
		}
		
		Wand wand = Wand.getActiveWand(player);
		if (wand == null) {
			player.sendMessage("Equip a wand first (use /wand if needed)");
			return true;
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		String spellName = parameters[0];
		Spell spell = playerSpells.getSpell(spellName);
		if (spell == null)
		{
			player.sendMessage("Spell '" + spellName + "' unknown, Use /spells for spell list");
			return true;
		}
		
		wand.addSpell(spellName);
		wand.updateInventory(playerSpells);

		return true;
	}

	public boolean onWandRemove(Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			player.sendMessage("Use: /wand remove <spell>");
			return true;
		}
		
		Wand wand = Wand.getActiveWand(player);
		if (wand == null) {
			player.sendMessage("Equip a wand first (use /wand if needed)");
			return true;
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		String spellName = parameters[0];

		wand.removeSpell(spellName);
		wand.updateInventory(playerSpells);

		return true;
	}

	public boolean onWandName(Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			player.sendMessage("Use: /wand name <name>");
			return true;
		}
		
		Wand wand = Wand.getActiveWand(player);
		if (wand == null) {
			player.sendMessage("Equip a wand first (use /wand if needed)");
			return true;
		}
		
		wand.setName(parameters[0]);

		return true;
	}

	public boolean onWand(Player player, String[] parameters)
	{
		boolean holdingWand = Wand.isActive(player);
		String wandName = "default";
		if (parameters.length > 0)
		{
			wandName = parameters[0];
		}

		if (!holdingWand)
		{
			Wand wand = Wand.createWand(wandName);
		
			// Place directly in hand if possible
			PlayerInventory inventory = player.getInventory();
			ItemStack inHand = inventory.getItemInHand();
			if (inHand == null || inHand.getType() == Material.AIR) {
				PlayerSpells playerSpells = spells.getPlayerSpells(player);
				inventory.setItem(inventory.getHeldItemSlot(), wand.getItem());
				if (playerSpells.storeInventory()) {
					// Create spell inventory
					wand.updateInventory(playerSpells);
				}
			} else {
				player.getInventory().addItem(wand.getItem());
			}

			player.sendMessage("Use /wand again for help, /spells for spell list");
		}
		else 
		{
			showWandHelp(player);
		}
		return true;
	}

	private void showWandHelp(Player player)
	{
		player.sendMessage("How to use your wand:");
		player.sendMessage(" The active spell is farthest to left");
		player.sendMessage(" Left-click your wand to cast");
		player.sendMessage(" Right-click to cycle spells");
	}

	public boolean onCast(Player player, String[] castParameters)
	{
		if (castParameters.length < 1) return false;

		String spellName = castParameters[0];
		String[] parameters = new String[castParameters.length - 1];
		for (int i = 1; i < castParameters.length; i++)
		{
			parameters[i - 1] = castParameters[i];
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Spell spell = playerSpells.getSpell(spellName);
		if (spell == null)
		{
			return false;
		}

		spell.cast(parameters);

		return true;
	}

	public boolean onReload(CommandSender sender, String[] parameters)
	{
		spells.load();
		sender.sendMessage("Configuration reloaded.");
		return true;
	}

	public boolean onSpells(Player player, String[] parameters)
	{
		int pageNumber = 1;
		String category = null;
		if (parameters.length > 0)
		{
			try
			{
				pageNumber = Integer.parseInt(parameters[0]);
			}
			catch (NumberFormatException ex)
			{
				pageNumber = 1;
				category = parameters[0];
			}
		}
		listSpells(player, pageNumber, category);

		return true;
	}


	/* 
	 * Help commands
	 */

	public void listSpellsByCategory(Player player,String category)
	{
		List<Spell> categorySpells = new ArrayList<Spell>();
		List<Spell> spellVariants = spells.getAllSpells();
		for (Spell spell : spellVariants)
		{
			if (spell.getCategory().equalsIgnoreCase(category) && spell.hasSpellPermission(player))
			{
				categorySpells.add(spell);
			}
		}

		if (categorySpells.size() == 0)
		{
			player.sendMessage("You don't know any spells");
			return;
		}

		Collections.sort(categorySpells);
		for (Spell spell : categorySpells)
		{
			String name = spell.getName();
			String description = spell.getDescription();
			if (!name.equals(spell.getKey())) {
				description = name + ", " + description;
			}
			player.sendMessage(spell.getKey() + " [" + spell.getMaterial().name().toLowerCase() + "] : " + description);
		}
	}

	public void listCategories(Player player)
	{
		HashMap<String, Integer> spellCounts = new HashMap<String, Integer>();
		List<String> spellGroups = new ArrayList<String>();
		List<Spell> spellVariants = spells.getAllSpells();

		for (Spell spell : spellVariants)
		{
			if (!spell.hasSpellPermission(player)) continue;

			Integer spellCount = spellCounts.get(spell.getCategory());
			if (spellCount == null || spellCount == 0)
			{
				spellCounts.put(spell.getCategory(), 1);
				spellGroups.add(spell.getCategory());
			}
			else
			{
				spellCounts.put(spell.getCategory(), spellCount + 1);
			}
		}
		if (spellGroups.size() == 0)
		{
			player.sendMessage("You don't know any spells");
			return;
		}

		Collections.sort(spellGroups);
		for (String group : spellGroups)
		{
			player.sendMessage(group + " [" + spellCounts.get(group) + "]");
		}
	}

	public void listSpells(Player player, int pageNumber, String category)
	{
		if (category != null)
		{
			listSpellsByCategory(player, category);
			return;
		}

		HashMap<String, SpellGroup> spellGroups = new HashMap<String, SpellGroup>();
		List<Spell> spellVariants = spells.getAllSpells();

		int spellCount = 0;
		for (Spell spell : spellVariants)
		{
			if (!spell.hasSpellPermission(player))
			{
				continue;
			}
			spellCount++;
			SpellGroup group = spellGroups.get(spell.getCategory());
			if (group == null)
			{
				group = new SpellGroup();
				group.groupName = spell.getCategory();
				spellGroups.put(group.groupName, group);	
			}
			group.spells.add(spell);
		}

		List<SpellGroup> sortedGroups = new ArrayList<SpellGroup>();
		sortedGroups.addAll(spellGroups.values());
		Collections.sort(sortedGroups);

		int maxLines = 5;
		int maxPages = spellCount / maxLines + 1;
		if (pageNumber > maxPages)
		{
			pageNumber = maxPages;
		}

		player.sendMessage("You know " + spellCount + " spells. [" + pageNumber + "/" + maxPages + "]");

		int currentPage = 1;
		int lineCount = 0;
		int printedCount = 0;
		for (SpellGroup group : sortedGroups)
		{
			if (printedCount > maxLines) break;

			boolean isFirst = true;
			Collections.sort(group.spells);
			for (Spell spell : group.spells)
			{
				if (printedCount > maxLines) break;

				if (currentPage == pageNumber)
				{
					if (isFirst)
					{
						player.sendMessage(group.groupName + ":");
						isFirst = false;
					}
					String name = spell.getName();
					String description = spell.getDescription();
					if (!name.equals(spell.getKey())) {
						description = name + ", " + description;
					}
					player.sendMessage(" " + spell.getKey() + " [" + spell.getMaterial().name().toLowerCase() + "] : " + description);
					printedCount++;
				}
				lineCount++;
				if (lineCount == maxLines)
				{
					lineCount = 0;
					currentPage++;
				}	
			}
		}
	}

	public void onDisable() 
	{
		spells.clear();
	}

	/*
	 * Private data
	 */	
	private final Spells spells = new Spells();
	private final Logger log = Logger.getLogger("Minecraft");
}
