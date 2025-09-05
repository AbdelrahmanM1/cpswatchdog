package dev.abdelrahmanmoharram.cpswatchdog.commands;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import dev.abdelrahmanmoharram.cpswatchdog.data.PlayerCPSData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CPSStatsCommand implements CommandExecutor {
    private final cpswatchdog plugin;

    public CPSStatsCommand(cpswatchdog plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpswatchdog.stats")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        String targetName = null;
        String clickType = "both"; // "left", "right", or "both"
        boolean showHistory = false;

        // Parse arguments
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                return true;
            }
            targetName = ((Player) sender).getName();
        } else if (args.length == 1) {
            // Could be player name or flag
            if (args[0].equalsIgnoreCase("-l") || args[0].equalsIgnoreCase("--left")) {
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
            } else if (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--history")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                    return true;
                }
                targetName = ((Player) sender).getName();
                showHistory = true;
            } else {
                targetName = args[0];
            }
        } else if (args.length == 2) {
            targetName = args[0];
            String flag = args[1].toLowerCase();

            if (flag.equals("-l") || flag.equals("--left")) {
                clickType = "left";
            } else if (flag.equals("-r") || flag.equals("--right")) {
                clickType = "right";
            } else if (flag.equals("-h") || flag.equals("--history")) {
                showHistory = true;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid flag. Use -l/--left, -r/--right, or -h/--history");
                return true;
            }
        } else if (args.length == 3) {
            targetName = args[0];
            String flag1 = args[1].toLowerCase();
            String flag2 = args[2].toLowerCase();

            // Handle multiple flags
            if (flag1.equals("-h") || flag1.equals("--history") ||
                    flag2.equals("-h") || flag2.equals("--history")) {
                showHistory = true;
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

        if (showHistory) {
            showDetailedStatsWithHistory(sender, targetName, clickType);
        } else {
            showDetailedStats(sender, targetName, clickType);
        }

        return true;
    }

    private void showDetailedStats(CommandSender sender, String targetName, String clickType) {
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

        // Get configuration values
        double highThreshold = plugin.getCPSManager().getHighCPSThreshold();
        double extremeThreshold = plugin.getCPSManager().getExtremeCPSThreshold();
        double varianceThreshold = plugin.getCPSManager().getVarianceThreshold();
        long sessionDuration = data.getSessionDuration() / 1000; // Convert to seconds

        switch (clickType) {
            case "left":
                showLeftClickDetailedStats(sender, target, data, highThreshold, extremeThreshold, varianceThreshold, sessionDuration);
                break;
            case "right":
                showRightClickDetailedStats(sender, target, data, highThreshold, extremeThreshold, varianceThreshold, sessionDuration);
                break;
            case "both":
            default:
                showCombinedDetailedStats(sender, target, data, highThreshold, extremeThreshold, varianceThreshold, sessionDuration);
                break;
        }
    }

    private void showLeftClickDetailedStats(CommandSender sender, Player target, PlayerCPSData data,
                                            double highThreshold, double extremeThreshold, double varianceThreshold,
                                            long sessionDuration) {
        double currentCPS = data.getCurrentLeftCPS();
        double averageCPS = data.getAverageLeftCPS();
        double maxCPS = data.getMaxLeftCPS();
        double variance = data.getLeftVariance();

        String consistencyStatus = data.isLeftClickPerfectlyConsistent(varianceThreshold) ?
                ChatColor.RED + "SUSPICIOUS (Bot-like)" : ChatColor.GREEN + "NORMAL";

        String threatLevel = getThreatLevel(currentCPS, data, highThreshold, extremeThreshold, varianceThreshold);

        sender.sendMessage(ChatColor.GOLD + "========== Left Click Stats for " + target.getName() + " ==========");
        sender.sendMessage(ChatColor.AQUA + "=== Left Click Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Current Left CPS: " + getColoredCPS(currentCPS, highThreshold, extremeThreshold) +
                String.format("%.2f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average Left CPS: " + ChatColor.WHITE + String.format("%.2f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Max Left CPS: " + ChatColor.WHITE + String.format("%.2f", maxCPS));
        sender.sendMessage(ChatColor.YELLOW + "Left Click Variance: " + ChatColor.WHITE + String.format("%.4f", variance));
        sender.sendMessage(ChatColor.YELLOW + "Left Click Consistency: " + consistencyStatus);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Total Left Clicks: " + ChatColor.WHITE + data.getTotalLeftClicks());
        sender.sendMessage(ChatColor.YELLOW + "Violations: " + ChatColor.RED + data.getViolationCount());
        sender.sendMessage(ChatColor.YELLOW + "Suspicious Activities: " + ChatColor.RED + data.getSuspiciousActivityCount());
        sender.sendMessage(ChatColor.YELLOW + "Session Duration: " + ChatColor.WHITE + formatDuration(sessionDuration));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Left Click Threat Level: " + threatLevel);
        sender.sendMessage(ChatColor.GRAY + "Thresholds: High=" + highThreshold + ", Extreme=" + extremeThreshold);
    }

    private void showRightClickDetailedStats(CommandSender sender, Player target, PlayerCPSData data,
                                             double highThreshold, double extremeThreshold, double varianceThreshold,
                                             long sessionDuration) {
        double currentCPS = data.getCurrentRightCPS();
        double averageCPS = data.getAverageRightCPS();
        double maxCPS = data.getMaxRightCPS();
        double variance = data.getRightVariance();

        String consistencyStatus = data.isRightClickPerfectlyConsistent(varianceThreshold) ?
                ChatColor.RED + "SUSPICIOUS (Bot-like)" : ChatColor.GREEN + "NORMAL";

        String threatLevel = getThreatLevel(currentCPS, data, highThreshold, extremeThreshold, varianceThreshold);

        sender.sendMessage(ChatColor.GOLD + "========== Right Click Stats for " + target.getName() + " ==========");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "=== Right Click Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Current Right CPS: " + getColoredCPS(currentCPS, highThreshold, extremeThreshold) +
                String.format("%.2f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Average Right CPS: " + ChatColor.WHITE + String.format("%.2f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Max Right CPS: " + ChatColor.WHITE + String.format("%.2f", maxCPS));
        sender.sendMessage(ChatColor.YELLOW + "Right Click Variance: " + ChatColor.WHITE + String.format("%.4f", variance));
        sender.sendMessage(ChatColor.YELLOW + "Right Click Consistency: " + consistencyStatus);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Total Right Clicks: " + ChatColor.WHITE + data.getTotalRightClicks());
        sender.sendMessage(ChatColor.YELLOW + "Violations: " + ChatColor.RED + data.getViolationCount());
        sender.sendMessage(ChatColor.YELLOW + "Suspicious Activities: " + ChatColor.RED + data.getSuspiciousActivityCount());
        sender.sendMessage(ChatColor.YELLOW + "Session Duration: " + ChatColor.WHITE + formatDuration(sessionDuration));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Right Click Threat Level: " + threatLevel);
        sender.sendMessage(ChatColor.GRAY + "Thresholds: High=" + highThreshold + ", Extreme=" + extremeThreshold);
    }

    private void showCombinedDetailedStats(CommandSender sender, Player target, PlayerCPSData data,
                                           double highThreshold, double extremeThreshold, double varianceThreshold,
                                           long sessionDuration) {
        double currentCPS = data.getCurrentCPS();
        double averageCPS = data.getAverageCPS();
        double maxCPS = data.getMaxCPS();
        double variance = data.getVariance();

        String consistencyStatus = data.isPerfectlyConsistent(varianceThreshold) ?
                ChatColor.RED + "SUSPICIOUS (Bot-like)" : ChatColor.GREEN + "NORMAL";

        String overallThreat = getThreatLevel(currentCPS, data, highThreshold, extremeThreshold, varianceThreshold);

        sender.sendMessage(ChatColor.GOLD + "========== Detailed CPS Stats for " + target.getName() + " ==========");

        // Left Click Section
        sender.sendMessage(ChatColor.AQUA + "=== Left Click Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Current Left CPS: " + getColoredCPS(data.getCurrentLeftCPS(), highThreshold, extremeThreshold) +
                String.format("%.2f", data.getCurrentLeftCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Average Left CPS: " + ChatColor.WHITE + String.format("%.2f", data.getAverageLeftCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Max Left CPS: " + ChatColor.WHITE + String.format("%.2f", data.getMaxLeftCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Left Variance: " + ChatColor.WHITE + String.format("%.4f", data.getLeftVariance()));
        sender.sendMessage(ChatColor.YELLOW + "Left Consistency: " +
                (data.isLeftClickPerfectlyConsistent(varianceThreshold) ? ChatColor.RED + "SUSPICIOUS" : ChatColor.GREEN + "NORMAL"));
        sender.sendMessage(ChatColor.YELLOW + "Total Left Clicks: " + ChatColor.WHITE + data.getTotalLeftClicks());

        sender.sendMessage("");

        // Right Click Section
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "=== Right Click Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Current Right CPS: " + getColoredCPS(data.getCurrentRightCPS(), highThreshold, extremeThreshold) +
                String.format("%.2f", data.getCurrentRightCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Average Right CPS: " + ChatColor.WHITE + String.format("%.2f", data.getAverageRightCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Max Right CPS: " + ChatColor.WHITE + String.format("%.2f", data.getMaxRightCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Right Variance: " + ChatColor.WHITE + String.format("%.4f", data.getRightVariance()));
        sender.sendMessage(ChatColor.YELLOW + "Right Consistency: " +
                (data.isRightClickPerfectlyConsistent(varianceThreshold) ? ChatColor.RED + "SUSPICIOUS" : ChatColor.GREEN + "NORMAL"));
        sender.sendMessage(ChatColor.YELLOW + "Total Right Clicks: " + ChatColor.WHITE + data.getTotalRightClicks());

        sender.sendMessage("");

        // Combined Analysis
        sender.sendMessage(ChatColor.GREEN + "=== Combined Analysis ===");
        sender.sendMessage(ChatColor.YELLOW + "Combined Max CPS: " + getColoredCPS(currentCPS, highThreshold, extremeThreshold) +
                String.format("%.2f", currentCPS));
        sender.sendMessage(ChatColor.YELLOW + "Combined Average CPS: " + ChatColor.WHITE + String.format("%.2f", averageCPS));
        sender.sendMessage(ChatColor.YELLOW + "Combined Max Recorded: " + ChatColor.WHITE + String.format("%.2f", maxCPS));
        sender.sendMessage(ChatColor.YELLOW + "Combined Variance: " + ChatColor.WHITE + String.format("%.4f", variance));
        sender.sendMessage(ChatColor.YELLOW + "Overall Consistency: " + consistencyStatus);

        sender.sendMessage("");

        // Session Information
        sender.sendMessage(ChatColor.GOLD + "=== Session Information ===");
        sender.sendMessage(ChatColor.YELLOW + "Total Clicks: " + ChatColor.WHITE + data.getTotalClicks() +
                ChatColor.GRAY + " (L:" + data.getTotalLeftClicks() + " R:" + data.getTotalRightClicks() + ")");
        sender.sendMessage(ChatColor.YELLOW + "Violations: " + ChatColor.RED + data.getViolationCount());
        sender.sendMessage(ChatColor.YELLOW + "Suspicious Activities: " + ChatColor.RED + data.getSuspiciousActivityCount());
        sender.sendMessage(ChatColor.YELLOW + "Session Duration: " + ChatColor.WHITE + formatDuration(sessionDuration));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Overall Threat Level: " + overallThreat);
        sender.sendMessage(ChatColor.GRAY + "Thresholds: High=" + highThreshold + ", Extreme=" + extremeThreshold);
    }

    private void showDetailedStatsWithHistory(CommandSender sender, String targetName, String clickType) {
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

        // Show regular stats first
        showDetailedStats(sender, targetName, clickType);

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== CPS History ===");

        switch (clickType) {
            case "left":
                showLeftClickHistory(sender, data);
                break;
            case "right":
                showRightClickHistory(sender, data);
                break;
            case "both":
            default:
                showCombinedClickHistory(sender, data);
                break;
        }
    }

    private void showLeftClickHistory(CommandSender sender, PlayerCPSData data) {
        List<Double> leftHistory = data.getRecentLeftCPSValues();
        if (leftHistory.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No left click history available.");
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "Recent Left Click CPS Values:");
        StringBuilder historyLine = new StringBuilder(ChatColor.WHITE.toString());

        for (int i = 0; i < leftHistory.size() && i < 10; i++) {
            double cps = leftHistory.get(leftHistory.size() - 1 - i); // Show most recent first
            ChatColor color = getHistoryColor(cps);
            historyLine.append(color).append(String.format("%.1f", cps));
            if (i < leftHistory.size() - 1 && i < 9) {
                historyLine.append(ChatColor.GRAY).append(" → ");
            }
        }

        sender.sendMessage(historyLine.toString());
        sender.sendMessage(ChatColor.GRAY + "↑ Most Recent    Oldest ↓");
    }

    private void showRightClickHistory(CommandSender sender, PlayerCPSData data) {
        List<Double> rightHistory = data.getRecentRightCPSValues();
        if (rightHistory.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No right click history available.");
            return;
        }

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Recent Right Click CPS Values:");
        StringBuilder historyLine = new StringBuilder(ChatColor.WHITE.toString());

        for (int i = 0; i < rightHistory.size() && i < 10; i++) {
            double cps = rightHistory.get(rightHistory.size() - 1 - i); // Show most recent first
            ChatColor color = getHistoryColor(cps);
            historyLine.append(color).append(String.format("%.1f", cps));
            if (i < rightHistory.size() - 1 && i < 9) {
                historyLine.append(ChatColor.GRAY).append(" → ");
            }
        }

        sender.sendMessage(historyLine.toString());
        sender.sendMessage(ChatColor.GRAY + "↑ Most Recent    Oldest ↓");
    }

    private void showCombinedClickHistory(CommandSender sender, PlayerCPSData data) {
        sender.sendMessage(ChatColor.GREEN + "Recent Click History:");
        sender.sendMessage("");
        showLeftClickHistory(sender, data);
        sender.sendMessage("");
        showRightClickHistory(sender, data);
    }

    private ChatColor getHistoryColor(double cps) {
        if (cps >= 20) return ChatColor.DARK_RED;
        if (cps >= 15) return ChatColor.RED;
        if (cps >= 10) return ChatColor.YELLOW;
        if (cps >= 5) return ChatColor.WHITE;
        return ChatColor.GRAY;
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
        sender.sendMessage(ChatColor.RED + "Usage: /cpsstats [player] [flags]");
        sender.sendMessage(ChatColor.GRAY + "Flags:");
        sender.sendMessage(ChatColor.GRAY + "  -l, --left     : Show only left click stats");
        sender.sendMessage(ChatColor.GRAY + "  -r, --right    : Show only right click stats");
        sender.sendMessage(ChatColor.GRAY + "  -h, --history  : Show CPS history");
        sender.sendMessage(ChatColor.GRAY + "Examples:");
        sender.sendMessage(ChatColor.GRAY + "  /cpsstats Player123 -l");
        sender.sendMessage(ChatColor.GRAY + "  /cpsstats Player123 -h");
        sender.sendMessage(ChatColor.GRAY + "  /cpsstats Player123 -l -h");
    }
}