package dev.abdelrahmanmoharram.cpswatchdog.commands;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import dev.abdelrahmanmoharram.cpswatchdog.services.CooldownService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CPSAlertCommand implements CommandExecutor {
    private final cpswatchdog plugin;
    private final CooldownService cooldownService;

    public CPSAlertCommand(cpswatchdog plugin) {
        this.plugin = plugin;
        // 5 second cooldown for command usage
        this.cooldownService = new CooldownService(5);
        // Start the cleanup task to prevent memory leaks
        this.cooldownService.startCleanupTask(plugin);
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

        // Check if player is on cooldown (except for help command)
        if (args.length == 0 || !args[0].equalsIgnoreCase("help")) {
            if (cooldownService.isOnCooldown(player)) {
                long remaining = cooldownService.getRemaining(player);
                sender.sendMessage(ChatColor.RED + "Please wait " + remaining + " seconds before using this command again.");
                return true;
            }
        }

        try {
            if (args.length == 0) {
                // Set cooldown before processing the toggle
                cooldownService.setCooldown(player);

                // Toggle alerts and provide feedback
                boolean enabled = plugin.getCPSManager().toggleAlerts(player);
                String status = enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
                sender.sendMessage(ChatColor.YELLOW + "CPS alerts are now " + status + ChatColor.YELLOW + "!");

                if (enabled) {
                    sender.sendMessage(ChatColor.GRAY + "You will now receive notifications about suspicious CPS activity.");
                } else {
                    sender.sendMessage(ChatColor.GRAY + "You will no longer receive CPS alert notifications.");
                }
            } else if (args[0].equalsIgnoreCase("status")) {
                // Set cooldown for status command as well
                cooldownService.setCooldown(player);

                // Show current alert status
                boolean enabled = plugin.getCPSManager().hasAlertsEnabled(player);
                String status = enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
                sender.sendMessage(ChatColor.YELLOW + "Your CPS alerts are currently " + status);
            } else if (args[0].equalsIgnoreCase("help")) {
                // No cooldown for help command
                showHelp(sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Unknown argument. Use '/cpsalert help' for usage information.");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while processing your request.");
            plugin.getLogger().severe("Error in CPSAlertCommand: " + e.getMessage());
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== CPS Alert Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/cpsalert" + ChatColor.GRAY + " - Toggle CPS alert notifications");
        sender.sendMessage(ChatColor.YELLOW + "/cpsalert status" + ChatColor.GRAY + " - Check if alerts are enabled");
        sender.sendMessage(ChatColor.YELLOW + "/cpsalert help" + ChatColor.GRAY + " - Show this help message");
        sender.sendMessage(ChatColor.GRAY + "Alerts notify you when players exceed CPS thresholds or show suspicious patterns.");
        sender.sendMessage(ChatColor.DARK_GRAY + "Note: Commands have a 5-second cooldown to prevent spam.");
    }

    /**
     * Clean up resources when the command handler is no longer needed
     * Call this method when the plugin is disabled
     */
    public void cleanup() {
        if (cooldownService != null) {
            cooldownService.stopCleanupTask();
        }
    }
}