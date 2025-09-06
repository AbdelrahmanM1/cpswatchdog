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

        // Show loading message
        sender.sendMessage(ChatColor.YELLOW + "[CPSWatchdog] Reloading configuration...");

        try {
            // Store old values for comparison
            double oldHighThreshold = plugin.getCPSManager().getHighCPSThreshold();
            double oldExtremeThreshold = plugin.getCPSManager().getExtremeCPSThreshold();
            int oldViolationsForAlert = plugin.getCPSManager().getViolationsForAlert();
            boolean oldNotifyStaff = plugin.getCPSManager().isNotifyStaff();
            boolean oldLogToConsole = plugin.getCPSManager().isLogToConsole();

            // Reload configuration
            plugin.reloadPluginConfig();

            sender.sendMessage(ChatColor.GREEN + "[CPSWatchdog] Configuration reloaded successfully!");

            // Show what changed (if anything)
            showConfigChanges(sender, oldHighThreshold, oldExtremeThreshold, oldViolationsForAlert,
                    oldNotifyStaff, oldLogToConsole);

            // Show current configuration values
            showCurrentConfig(sender);

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "[CPSWatchdog] Error reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            e.printStackTrace(); // Log full stack trace for debugging
        }

        return true;
    }

    private void showConfigChanges(CommandSender sender, double oldHighThreshold, double oldExtremeThreshold,
                                   int oldViolationsForAlert, boolean oldNotifyStaff, boolean oldLogToConsole) {
        boolean hasChanges = false;

        sender.sendMessage(ChatColor.AQUA + "Configuration changes:");

        if (oldHighThreshold != plugin.getCPSManager().getHighCPSThreshold()) {
            sender.sendMessage(ChatColor.GRAY + "- High CPS Threshold: " + oldHighThreshold +
                    ChatColor.YELLOW + " → " + ChatColor.WHITE + plugin.getCPSManager().getHighCPSThreshold());
            hasChanges = true;
        }

        if (oldExtremeThreshold != plugin.getCPSManager().getExtremeCPSThreshold()) {
            sender.sendMessage(ChatColor.GRAY + "- Extreme CPS Threshold: " + oldExtremeThreshold +
                    ChatColor.YELLOW + " → " + ChatColor.WHITE + plugin.getCPSManager().getExtremeCPSThreshold());
            hasChanges = true;
        }

        if (oldViolationsForAlert != plugin.getCPSManager().getViolationsForAlert()) {
            sender.sendMessage(ChatColor.GRAY + "- Violations for Alert: " + oldViolationsForAlert +
                    ChatColor.YELLOW + " → " + ChatColor.WHITE + plugin.getCPSManager().getViolationsForAlert());
            hasChanges = true;
        }

        if (oldNotifyStaff != plugin.getCPSManager().isNotifyStaff()) {
            sender.sendMessage(ChatColor.GRAY + "- Staff Notifications: " + (oldNotifyStaff ? "Enabled" : "Disabled") +
                    ChatColor.YELLOW + " → " + ChatColor.WHITE + (plugin.getCPSManager().isNotifyStaff() ? "Enabled" : "Disabled"));
            hasChanges = true;
        }

        if (oldLogToConsole != plugin.getCPSManager().isLogToConsole()) {
            sender.sendMessage(ChatColor.GRAY + "- Console Logging: " + (oldLogToConsole ? "Enabled" : "Disabled") +
                    ChatColor.YELLOW + " → " + ChatColor.WHITE + (plugin.getCPSManager().isLogToConsole() ? "Enabled" : "Disabled"));
            hasChanges = true;
        }

        if (!hasChanges) {
            sender.sendMessage(ChatColor.GRAY + "- No configuration values changed");
        }
    }

    private void showCurrentConfig(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Current configuration:");
        sender.sendMessage(ChatColor.GRAY + "- High CPS Threshold: " + ChatColor.WHITE + plugin.getCPSManager().getHighCPSThreshold());
        sender.sendMessage(ChatColor.GRAY + "- Extreme CPS Threshold: " + ChatColor.WHITE + plugin.getCPSManager().getExtremeCPSThreshold());
        sender.sendMessage(ChatColor.GRAY + "- Violations for Alert: " + ChatColor.WHITE + plugin.getCPSManager().getViolationsForAlert());
        sender.sendMessage(ChatColor.GRAY + "- Staff Notifications: " + ChatColor.WHITE + (plugin.getCPSManager().isNotifyStaff() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.GRAY + "- Console Logging: " + ChatColor.WHITE + (plugin.getCPSManager().isLogToConsole() ? "Enabled" : "Disabled"));

        // Add variance threshold if available
        try {
            double varianceThreshold = plugin.getCPSManager().getVarianceThreshold();
            sender.sendMessage(ChatColor.GRAY + "- Variance Threshold: " + ChatColor.WHITE + varianceThreshold);
        } catch (Exception ignored) {
            // Method might not exist in all versions
        }
    }
}
