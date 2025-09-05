package dev.abdelrahmanmoharram.cpswatchdog.commands;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CPSReloadCommand implements CommandExecutor {
    private final cpswatchdog plugin;

    public CPSReloadCommand(cpswatchdog plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpswatchdog.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        try {
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "[CPSWatchdog] Configuration reloaded successfully!");

            // Show updated configuration values
            sender.sendMessage(ChatColor.YELLOW + "Updated settings:");
            sender.sendMessage(ChatColor.GRAY + "- High CPS Threshold: " + plugin.getCPSManager().getHighCPSThreshold());
            sender.sendMessage(ChatColor.GRAY + "- Extreme CPS Threshold: " + plugin.getCPSManager().getExtremeCPSThreshold());
            sender.sendMessage(ChatColor.GRAY + "- Violations for Alert: " + plugin.getCPSManager().getViolationsForAlert());
            sender.sendMessage(ChatColor.GRAY + "- Staff Notifications: " + (plugin.getCPSManager().isNotifyStaff() ? "Enabled" : "Disabled"));
            sender.sendMessage(ChatColor.GRAY + "- Console Logging: " + (plugin.getCPSManager().isLogToConsole() ? "Enabled" : "Disabled"));

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "[CPSWatchdog] Error reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
        }

        return true;
    }
}