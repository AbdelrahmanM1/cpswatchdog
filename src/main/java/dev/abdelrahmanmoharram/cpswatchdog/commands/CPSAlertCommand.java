package dev.abdelrahmanmoharram.cpswatchdog.commands;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CPSAlertCommand implements CommandExecutor {
    private final cpswatchdog plugin;

    public CPSAlertCommand(cpswatchdog plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!sender.hasPermission("cpswatchdog.alert")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getCPSManager().toggleAlerts(player);

        return true;
    }
}