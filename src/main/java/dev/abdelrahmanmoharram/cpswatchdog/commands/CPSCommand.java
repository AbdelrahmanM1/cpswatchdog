package dev.abdelrahmanmoharram.cpswatchdog.commands;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import dev.abdelrahmanmoharram.cpswatchdog.data.PlayerCPSData;
import dev.abdelrahmanmoharram.cpswatchdog.services.CooldownService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CPSCommand implements CommandExecutor {
    private final cpswatchdog plugin;
    private final CooldownService cooldownService;

    public CPSCommand(cpswatchdog plugin) {
        this.plugin = plugin;
        // Initialize cooldown service with 5 seconds cooldown
        this.cooldownService = new CooldownService(5);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpswatchdog.check")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Check cooldown for players (not console)
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (cooldownService.isOnCooldown(player)) {
                long remaining = cooldownService.getRemaining(player);
                sender.sendMessage(ChatColor.RED + "Please wait " + remaining + " seconds before using this command again.");
                return true;
            }
            // Set cooldown for the player
            cooldownService.setCooldown(player);
        }

        String targetName = null;
        boolean showDetailed = false;
        String clickType = "both"; // "left", "right", or "both"

        // Parse arguments
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                return true;
            }
            targetName = ((Player) sender).getName();
        } else if (args.length == 1) {
            // Could be player name or flag
            if (args[0].equalsIgnoreCase("-d") || args[0].equalsIgnoreCase("--detailed")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                    return true;
                }
                targetName = ((Player) sender).getName();
                showDetailed = true;
            } else if (args[0].equalsIgnoreCase("-l") || args[0].equalsIgnoreCase("--left")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                    return true;
                }
                targetName = ((Player) sender).getName();
                clickType = "left";
            } else if (args[0].equalsIgnoreCase("-r") || args[0].equalsIgnoreCase("--right")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                    return true;
                }
                targetName = ((Player) sender).getName();
                clickType = "right";
            } else {
                targetName = args[0];
            }
        } else if (args.length == 2) {
            targetName = args[0];
            String flag = args[1].toLowerCase();

            if (flag.equals("-d") || flag.equals("--detailed")) {
                showDetailed = true;
            } else if (flag.equals("-l") || flag.equals("--left")) {
                clickType = "left";
            } else if (flag.equals("-r") || flag.equals("--right")) {
                clickType = "right";
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid flag. Use -d/--detailed, -l/--left, or -r/--right");
                return true;
            }
        } else if (args.length == 3) {
            targetName = args[0];
            String flag1 = args[1].toLowerCase();
            String flag2 = args[2].toLowerCase();

            // Handle multiple flags
            if ((flag1.equals("-d") || flag1.equals("--detailed")) ||
                    (flag2.equals("-d") || flag2.equals("--detailed"))) {
                showDetailed = true;
            }

            if (flag1.equals("-l") || flag1.equals("--left") ||
                    flag2.equals("-l") || flag2.equals("--left")) {
                clickType = "left";
            } else if (flag1.equals("-r") || flag1.equals("--right") ||
                    flag2.equals("-r") || flag2.equals("--right")) {
                clickType = "right";
            }
        } else {
            showUsage(sender);
            return true;
        }

        if (showDetailed) {
            showDetailedCPS(sender, targetName, clickType);
        } else {
            showBasicCPS(sender, targetName, clickType);
        }

        return true;
    }

    private void showBasicCPS(CommandSender sender, String targetName, String clickType) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online.");
            return;
        }

        PlayerCPSData data = plugin.getCPSManager().getPlayerData(target.getUniqueId());
        if (data == null || data.getTotalClicks() == 0) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has no click data.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== CPS Report for " + target.getName() + " ===");

        switch (clickType) {
            case "left":
                showLeftClickStats(sender, data);
                break;
            case "right":
                showRightClickStats(sender, data);
                break;
            case "both":
            default:
                showCombinedStats(sender, data);
                break;
        }
    }

    private void showDetailedCPS(CommandSender sender, String targetName, String clickType) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online.");
            return;
        }

        PlayerCPSData data = plugin.getCPSManager().getPlayerData(target.getUniqueId());
        if (data == null || data.getTotalClicks() == 0) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has no click data.");
            return;
        }

        double highThreshold = plugin.getCPSManager().getHighCPSThreshold();
        double extremeThreshold = plugin.getCPSManager().getExtremeCPSThreshold();
        double varianceThreshold = plugin.getCPSManager().getVarianceThreshold();

        sender.sendMessage(ChatColor.GOLD + "=== Detailed CPS Report for " + target.getName() + " ===");

        switch (clickType) {
            case "left":
                showDetailedLeftStats(sender, data, highThreshold, extremeThreshold, varianceThreshold);
                break;
            case "right":
                showDetailedRightStats(sender, data, highThreshold, extremeThreshold, varianceThreshold);
                break;
            case "both":
            default:
                showDetailedCombinedStats(sender, data, highThreshold, extremeThreshold, varianceThreshold);
                break;
        }

        // Show session info
        long sessionDuration = data.getSessionDuration() / 1000;
        sender.sendMessage(ChatColor.YELLOW + "Session Duration: " + ChatColor.WHITE + formatDuration(sessionDuration));
        sender.sendMessage(ChatColor.YELLOW + "Violations: " + ChatColor.RED + data.getViolationCount());
        sender.sendMessage(ChatColor.GRAY + "Thresholds: High=" + highThreshold + ", Extreme=" + extremeThreshold);
    }

    private void showLeftClickStats(CommandSender sender, PlayerCPSData data) {
        double currentCPS = data.getCurrentLeftCPS();
        double averageCPS = data.getAverageLeftCPS();
        String status = getStatusColor(currentCPS) + getStatus(currentCPS);

        sender.sendMessage(ChatColor.AQUA + "Left Click Stats:");
        sender.sendMessage(ChatColor.YELLOW + "Current CPS: " + ChatColor.WHITE + String.format("%.1f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average CPS: " + ChatColor.WHITE + String.format("%.1f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Status: " + status);
        sender.sendMessage(ChatColor.YELLOW + "Total Left Clicks: " + ChatColor.WHITE + data.getTotalLeftClicks());
    }

    private void showRightClickStats(CommandSender sender, PlayerCPSData data) {
        double currentCPS = data.getCurrentRightCPS();
        double averageCPS = data.getAverageRightCPS();
        String status = getStatusColor(currentCPS) + getStatus(currentCPS);

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Right Click Stats:");
        sender.sendMessage(ChatColor.YELLOW + "Current CPS: " + ChatColor.WHITE + String.format("%.1f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average CPS: " + ChatColor.WHITE + String.format("%.1f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Status: " + status);
        sender.sendMessage(ChatColor.YELLOW + "Total Right Clicks: " + ChatColor.WHITE + data.getTotalRightClicks());
    }

    private void showCombinedStats(CommandSender sender, PlayerCPSData data) {
        double combinedCPS = data.getCurrentCPS();
        double averageCPS = data.getAverageCPS();
        String status = getStatusColor(combinedCPS) + getStatus(combinedCPS);

        sender.sendMessage(ChatColor.GREEN + "Combined Stats:");
        sender.sendMessage(ChatColor.YELLOW + "Current CPS: " + ChatColor.WHITE + String.format("%.1f", combinedCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average CPS: " + ChatColor.WHITE + String.format("%.1f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Status: " + status);
        sender.sendMessage(ChatColor.YELLOW + "Total Clicks: " + ChatColor.WHITE + data.getTotalClicks());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "└ Left: " + String.format("%.1f", data.getCurrentLeftCPS()) +
                " CPS (" + data.getTotalLeftClicks() + " clicks)");
        sender.sendMessage(ChatColor.GRAY + "└ Right: " + String.format("%.1f", data.getCurrentRightCPS()) +
                " CPS (" + data.getTotalRightClicks() + " clicks)");
    }

    private void showDetailedLeftStats(CommandSender sender, PlayerCPSData data, double highThreshold,
                                       double extremeThreshold, double varianceThreshold) {
        double currentCPS = data.getCurrentLeftCPS();
        double averageCPS = data.getAverageLeftCPS();
        double maxCPS = data.getMaxLeftCPS();
        double variance = data.getLeftVariance();

        boolean isConsistent = data.isLeftClickPerfectlyConsistent(varianceThreshold);
        String consistencyStatus = isConsistent ? ChatColor.RED + "SUSPICIOUS (Bot-like)" : ChatColor.GREEN + "NORMAL";

        sender.sendMessage(ChatColor.AQUA + "=== Left Click Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Current CPS: " + getColoredCPS(currentCPS, highThreshold, extremeThreshold) +
                String.format("%.2f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average CPS: " + ChatColor.WHITE + String.format("%.2f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Max CPS: " + ChatColor.WHITE + String.format("%.2f", maxCPS));
        sender.sendMessage(ChatColor.YELLOW + "Variance: " + ChatColor.WHITE + String.format("%.4f", variance));
        sender.sendMessage(ChatColor.YELLOW + "Consistency: " + consistencyStatus);
        sender.sendMessage(ChatColor.YELLOW + "Total Left Clicks: " + ChatColor.WHITE + data.getTotalLeftClicks());
    }

    private void showDetailedRightStats(CommandSender sender, PlayerCPSData data, double highThreshold,
                                        double extremeThreshold, double varianceThreshold) {
        double currentCPS = data.getCurrentRightCPS();
        double averageCPS = data.getAverageRightCPS();
        double maxCPS = data.getMaxRightCPS();
        double variance = data.getRightVariance();

        boolean isConsistent = data.isRightClickPerfectlyConsistent(varianceThreshold);
        String consistencyStatus = isConsistent ? ChatColor.RED + "SUSPICIOUS (Bot-like)" : ChatColor.GREEN + "NORMAL";

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "=== Right Click Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Current CPS: " + getColoredCPS(currentCPS, highThreshold, extremeThreshold) +
                String.format("%.2f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average CPS: " + ChatColor.WHITE + String.format("%.2f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Max CPS: " + ChatColor.WHITE + String.format("%.2f", maxCPS));
        sender.sendMessage(ChatColor.YELLOW + "Variance: " + ChatColor.WHITE + String.format("%.4f", variance));
        sender.sendMessage(ChatColor.YELLOW + "Consistency: " + consistencyStatus);
        sender.sendMessage(ChatColor.YELLOW + "Total Right Clicks: " + ChatColor.WHITE + data.getTotalRightClicks());
    }

    private void showDetailedCombinedStats(CommandSender sender, PlayerCPSData data, double highThreshold,
                                           double extremeThreshold, double varianceThreshold) {
        sender.sendMessage(ChatColor.GREEN + "=== Combined Analysis ===");
        sender.sendMessage("");

        // Left click stats
        showDetailedLeftStats(sender, data, highThreshold, extremeThreshold, varianceThreshold);
        sender.sendMessage("");

        // Right click stats
        showDetailedRightStats(sender, data, highThreshold, extremeThreshold, varianceThreshold);
        sender.sendMessage("");

        // Overall analysis
        double combinedCPS = data.getCurrentCPS();
        String overallThreat = getThreatLevel(combinedCPS, data, highThreshold, extremeThreshold, varianceThreshold);
        sender.sendMessage(ChatColor.GOLD + "=== Overall Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Combined Max CPS: " + ChatColor.WHITE + String.format("%.2f", combinedCPS));
        sender.sendMessage(ChatColor.YELLOW + "Overall Threat Level: " + overallThreat);
    }

    private String getStatus(double cps) {
        if (cps >= 20) return "EXTREME - Likely Cheating";
        if (cps >= 15) return "HIGH - Suspicious";
        if (cps >= 10) return "MODERATE - Above Average";
        if (cps >= 5) return "NORMAL";
        return "LOW";
    }

    private ChatColor getStatusColor(double cps) {
        if (cps >= 20) return ChatColor.DARK_RED;
        if (cps >= 15) return ChatColor.RED;
        if (cps >= 10) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    private ChatColor getColoredCPS(double cps, double high, double extreme) {
        if (cps >= extreme) return ChatColor.DARK_RED;
        if (cps >= high) return ChatColor.RED;
        if (cps >= 10) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    private String getThreatLevel(double currentCPS, PlayerCPSData data, double highThreshold,
                                  double extremeThreshold, double varianceThreshold) {
        int score = 0;

        // CPS scoring
        if (currentCPS >= extremeThreshold) score += 3;
        else if (currentCPS >= highThreshold) score += 2;
        else if (currentCPS >= 10) score += 1;

        // Violation scoring
        if (data.getViolationCount() >= 10) score += 2;
        else if (data.getViolationCount() >= 5) score += 1;

        // Consistency scoring
        if (data.isPerfectlyConsistent(varianceThreshold)) score += 2;

        // Determine threat level
        if (score >= 5) return ChatColor.DARK_RED + "CRITICAL";
        if (score >= 3) return ChatColor.RED + "HIGH";
        if (score >= 2) return ChatColor.YELLOW + "MODERATE";
        if (score >= 1) return ChatColor.GREEN + "LOW";
        return ChatColor.GRAY + "MINIMAL";
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /cps [player] [flags]");
        sender.sendMessage(ChatColor.GRAY + "Flags:");
        sender.sendMessage(ChatColor.GRAY + "  -d, --detailed : Show detailed statistics");
        sender.sendMessage(ChatColor.GRAY + "  -l, --left     : Show only left click stats");
        sender.sendMessage(ChatColor.GRAY + "  -r, --right    : Show only right click stats");
        sender.sendMessage(ChatColor.GRAY + "Examples:");
        sender.sendMessage(ChatColor.GRAY + "  /cps Player123 -d");
        sender.sendMessage(ChatColor.GRAY + "  /cps Player123 -l");
        sender.sendMessage(ChatColor.GRAY + "  /cps Player123 -d -l");
    }
}