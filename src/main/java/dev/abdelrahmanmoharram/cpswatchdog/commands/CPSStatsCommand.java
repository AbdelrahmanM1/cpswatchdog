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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CPSStatsCommand implements CommandExecutor {
    private final cpswatchdog plugin;
    private final CooldownService cooldownService;

    // Enum for better flag handling
    public enum StatsFlag {
        LEFT("-l", "--left"),
        RIGHT("-r", "--right"),
        HISTORY("-h", "--history"),
        DETAILED("-d", "--detailed"),
        EXPORT("-e", "--export");

        private final String shortForm;
        private final String longForm;

        StatsFlag(String shortForm, String longForm) {
            this.shortForm = shortForm;
            this.longForm = longForm;
        }

        public static StatsFlag fromString(String flag) {
            String lowerFlag = flag.toLowerCase();
            for (StatsFlag f : values()) {
                if (f.shortForm.equals(lowerFlag) || f.longForm.equals(lowerFlag)) {
                    return f;
                }
            }
            return null;
        }
    }

    public CPSStatsCommand(cpswatchdog plugin) {
        this.plugin = plugin;
        this.cooldownService = new CooldownService(3); // 3 second cooldown for stats command
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpswatchdog.stats")) {
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
            cooldownService.setCooldown(player);
        }

        try {
            CommandArgs parsedArgs = parseArguments(sender, args);
            if (parsedArgs == null) {
                return true; // Error already sent
            }

            displayStats(sender, parsedArgs);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while processing the command.");
            plugin.getLogger().severe("Error in CPSStatsCommand: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private static class CommandArgs {
        String targetName;
        Set<StatsFlag> flags = EnumSet.noneOf(StatsFlag.class);

        boolean isLeftOnly() { return flags.contains(StatsFlag.LEFT) && !flags.contains(StatsFlag.RIGHT); }
        boolean isRightOnly() { return flags.contains(StatsFlag.RIGHT) && !flags.contains(StatsFlag.LEFT); }
        boolean showHistory() { return flags.contains(StatsFlag.HISTORY); }
        boolean showDetailed() { return flags.contains(StatsFlag.DETAILED); }
        boolean exportData() { return flags.contains(StatsFlag.EXPORT); }
    }

    private CommandArgs parseArguments(CommandSender sender, String[] args) {
        CommandArgs result = new CommandArgs();

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                return null;
            }
            result.targetName = ((Player) sender).getName();
            return result;
        }

        // Check if first argument is a flag (player checking their own stats)
        StatsFlag firstFlag = StatsFlag.fromString(args[0]);
        if (firstFlag != null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player name when using flags.");
                return null;
            }
            result.targetName = ((Player) sender).getName();
            result.flags.add(firstFlag);

            // Parse additional flags
            for (int i = 1; i < args.length; i++) {
                StatsFlag flag = StatsFlag.fromString(args[i]);
                if (flag != null) {
                    result.flags.add(flag);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid flag: " + args[i]);
                    showUsage(sender);
                    return null;
                }
            }
        } else {
            // First argument is player name
            result.targetName = args[0];

            // Parse flags
            for (int i = 1; i < args.length; i++) {
                StatsFlag flag = StatsFlag.fromString(args[i]);
                if (flag != null) {
                    result.flags.add(flag);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid flag: " + args[i]);
                    showUsage(sender);
                    return null;
                }
            }
        }

        // Validate flag combinations
        if (result.flags.contains(StatsFlag.LEFT) && result.flags.contains(StatsFlag.RIGHT)) {
            sender.sendMessage(ChatColor.RED + "Cannot use both -l/--left and -r/--right flags together.");
            return null;
        }

        return result;
    }

    private void displayStats(CommandSender sender, CommandArgs args) {
        Player target = Bukkit.getPlayer(args.targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args.targetName + "' not found or not online.");
            return;
        }

        PlayerCPSData data = plugin.getCPSManager().getPlayerData(target.getUniqueId());
        if (data == null || data.getTotalClicks() == 0) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has no click data recorded.");
            return;
        }

        // Export data if requested (for staff analysis)
        if (args.exportData() && sender.hasPermission("cpswatchdog.export")) {
            exportPlayerData(sender, target, data);
            return;
        }

        // Get configuration values
        StatsContext context = new StatsContext(plugin, data);

        // Display appropriate stats based on flags
        if (args.isLeftOnly()) {
            showLeftClickStats(sender, target, context, args.showHistory(), args.showDetailed());
        } else if (args.isRightOnly()) {
            showRightClickStats(sender, target, context, args.showHistory(), args.showDetailed());
        } else {
            showCombinedStats(sender, target, context, args.showHistory(), args.showDetailed());
        }
    }

    private static class StatsContext {
        final PlayerCPSData data;
        final double highThreshold;
        final double extremeThreshold;
        final double varianceThreshold;
        final long sessionDuration;

        StatsContext(cpswatchdog plugin, PlayerCPSData data) {
            this.data = data;
            this.highThreshold = plugin.getCPSManager().getHighCPSThreshold();
            this.extremeThreshold = plugin.getCPSManager().getExtremeCPSThreshold();
            this.varianceThreshold = plugin.getCPSManager().getVarianceThreshold();
            this.sessionDuration = data.getSessionDuration() / 1000;
        }
    }

    private void showLeftClickStats(CommandSender sender, Player target, StatsContext ctx,
                                    boolean showHistory, boolean showDetailed) {
        sender.sendMessage(ChatColor.GOLD + "========== Left Click Stats for " + target.getName() + " ==========");

        if (showDetailed) {
            displayDetailedLeftStats(sender, ctx);
        } else {
            displayBasicLeftStats(sender, ctx);
        }

        if (showHistory) {
            sender.sendMessage("");
            showLeftClickHistory(sender, ctx.data);
        }

        showSessionInfo(sender, ctx);
    }

    private void showRightClickStats(CommandSender sender, Player target, StatsContext ctx,
                                     boolean showHistory, boolean showDetailed) {
        sender.sendMessage(ChatColor.GOLD + "========== Right Click Stats for " + target.getName() + " ==========");

        if (showDetailed) {
            displayDetailedRightStats(sender, ctx);
        } else {
            displayBasicRightStats(sender, ctx);
        }

        if (showHistory) {
            sender.sendMessage("");
            showRightClickHistory(sender, ctx.data);
        }

        showSessionInfo(sender, ctx);
    }

    private void showCombinedStats(CommandSender sender, Player target, StatsContext ctx,
                                   boolean showHistory, boolean showDetailed) {
        sender.sendMessage(ChatColor.GOLD + "========== CPS Stats for " + target.getName() + " ==========");

        if (showDetailed) {
            displayDetailedLeftStats(sender, ctx);
            sender.sendMessage("");
            displayDetailedRightStats(sender, ctx);
            sender.sendMessage("");
            displayCombinedAnalysis(sender, ctx);
        } else {
            displayBasicCombinedStats(sender, ctx);
        }

        if (showHistory) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "=== Click History ===");
            showLeftClickHistory(sender, ctx.data);
            sender.sendMessage("");
            showRightClickHistory(sender, ctx.data);
        }

        showSessionInfo(sender, ctx);
    }

    private void displayDetailedLeftStats(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;

        sender.sendMessage(ChatColor.AQUA + "=== Left Click Analysis ===");
        sender.sendMessage(formatStatLine("Current CPS", data.getCurrentLeftCPS(), ctx));
        sender.sendMessage(formatStatLine("Average CPS", data.getAverageLeftCPS()));
        sender.sendMessage(formatStatLine("Max CPS", data.getMaxLeftCPS()));
        sender.sendMessage(formatStatLine("Variance", data.getLeftVariance(), "%.4f"));
        sender.sendMessage(formatConsistencyLine("Consistency",
                data.isLeftClickPerfectlyConsistent(ctx.varianceThreshold)));
        sender.sendMessage(ChatColor.YELLOW + "Total Left Clicks: " + ChatColor.WHITE + data.getTotalLeftClicks());
    }

    private void displayDetailedRightStats(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "=== Right Click Analysis ===");
        sender.sendMessage(formatStatLine("Current CPS", data.getCurrentRightCPS(), ctx));
        sender.sendMessage(formatStatLine("Average CPS", data.getAverageRightCPS()));
        sender.sendMessage(formatStatLine("Max CPS", data.getMaxRightCPS()));
        sender.sendMessage(formatStatLine("Variance", data.getRightVariance(), "%.4f"));
        sender.sendMessage(formatConsistencyLine("Consistency",
                data.isRightClickPerfectlyConsistent(ctx.varianceThreshold)));
        sender.sendMessage(ChatColor.YELLOW + "Total Right Clicks: " + ChatColor.WHITE + data.getTotalRightClicks());
    }

    private void displayCombinedAnalysis(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;
        String overallThreat = getThreatLevel(data.getCurrentCPS(), ctx);

        sender.sendMessage(ChatColor.GREEN + "=== Overall Analysis ===");
        sender.sendMessage(formatStatLine("Combined Current CPS", data.getCurrentCPS(), ctx));
        sender.sendMessage(formatStatLine("Combined Average CPS", data.getAverageCPS()));
        sender.sendMessage(formatStatLine("Combined Max CPS", data.getMaxCPS()));
        sender.sendMessage(formatStatLine("Combined Variance", data.getVariance(), "%.4f"));
        sender.sendMessage(formatConsistencyLine("Overall Consistency",
                data.isPerfectlyConsistent(ctx.varianceThreshold)));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Overall Threat Level: " + overallThreat);
    }

    private void displayBasicLeftStats(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;
        sender.sendMessage(ChatColor.AQUA + "=== Left Click Stats ===");
        sender.sendMessage(formatStatLine("Current CPS", data.getCurrentLeftCPS(), ctx));
        sender.sendMessage(formatStatLine("Average CPS", data.getAverageLeftCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Total Clicks: " + ChatColor.WHITE + data.getTotalLeftClicks());
    }

    private void displayBasicRightStats(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "=== Right Click Stats ===");
        sender.sendMessage(formatStatLine("Current CPS", data.getCurrentRightCPS(), ctx));
        sender.sendMessage(formatStatLine("Average CPS", data.getAverageRightCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Total Clicks: " + ChatColor.WHITE + data.getTotalRightClicks());
    }

    private void displayBasicCombinedStats(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;
        sender.sendMessage(ChatColor.GREEN + "=== Combined Stats ===");
        sender.sendMessage(formatStatLine("Current CPS", data.getCurrentCPS(), ctx));
        sender.sendMessage(formatStatLine("Average CPS", data.getAverageCPS()));
        sender.sendMessage(ChatColor.YELLOW + "Total Clicks: " + ChatColor.WHITE + data.getTotalClicks() +
                ChatColor.GRAY + " (L:" + data.getTotalLeftClicks() + " R:" + data.getTotalRightClicks() + ")");
    }

    private void showSessionInfo(CommandSender sender, StatsContext ctx) {
        PlayerCPSData data = ctx.data;
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== Session Info ===");
        sender.sendMessage(ChatColor.YELLOW + "Violations: " + ChatColor.RED + data.getViolationCount());
        sender.sendMessage(ChatColor.YELLOW + "Suspicious Activities: " + ChatColor.RED + data.getSuspiciousActivityCount());
        sender.sendMessage(ChatColor.YELLOW + "Session Duration: " + ChatColor.WHITE + formatDuration(ctx.sessionDuration));
        sender.sendMessage(ChatColor.GRAY + "Thresholds: High=" + ctx.highThreshold + ", Extreme=" + ctx.extremeThreshold);
    }

    // Helper methods for formatting
    private String formatStatLine(String label, double value, StatsContext ctx) {
        return ChatColor.YELLOW + label + ": " + getColoredCPS(value, ctx.highThreshold, ctx.extremeThreshold) +
                String.format("%.2f", value);
    }

    private String formatStatLine(String label, double value) {
        return formatStatLine(label, value, "%.2f");
    }

    private String formatStatLine(String label, double value, String format) {
        return ChatColor.YELLOW + label + ": " + ChatColor.WHITE + String.format(format, value);
    }

    private String formatConsistencyLine(String label, boolean isPerfectlyConsistent) {
        String status = isPerfectlyConsistent ?
                ChatColor.RED + "SUSPICIOUS (Bot-like)" : ChatColor.GREEN + "NORMAL";
        return ChatColor.YELLOW + label + ": " + status;
    }

    // History display methods (simplified)
    private void showLeftClickHistory(CommandSender sender, PlayerCPSData data) {
        List<Double> history = data.getRecentLeftCPSValues();
        showClickHistory(sender, history, ChatColor.AQUA + "Left Click History", 10);
    }

    private void showRightClickHistory(CommandSender sender, PlayerCPSData data) {
        List<Double> history = data.getRecentRightCPSValues();
        showClickHistory(sender, history, ChatColor.LIGHT_PURPLE + "Right Click History", 10);
    }

    private void showClickHistory(CommandSender sender, List<Double> history, String title, int maxEntries) {
        if (history.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No " + title.toLowerCase() + " available.");
            return;
        }

        sender.sendMessage(title + ":");
        StringBuilder historyLine = new StringBuilder();

        int entries = Math.min(history.size(), maxEntries);
        for (int i = 0; i < entries; i++) {
            double cps = history.get(history.size() - 1 - i);
            ChatColor color = getHistoryColor(cps);
            historyLine.append(color).append(String.format("%.1f", cps));
            if (i < entries - 1) {
                historyLine.append(ChatColor.GRAY).append(" → ");
            }
        }

        sender.sendMessage(historyLine.toString());
        sender.sendMessage(ChatColor.GRAY + "← Recent    Older →");
    }

    // Data export for staff analysis
    private void exportPlayerData(CommandSender sender, Player target, PlayerCPSData data) {
        sender.sendMessage(ChatColor.GREEN + "Exporting data for " + target.getName() + "...");

        // Create comprehensive data summary
        StringBuilder export = new StringBuilder();
        export.append("=== CPS DATA EXPORT ===\n");
        export.append("Player: ").append(target.getName()).append("\n");
        export.append("UUID: ").append(target.getUniqueId()).append("\n");
        export.append("Export Time: ").append(System.currentTimeMillis()).append("\n\n");

        export.append("LEFT CLICK DATA:\n");
        export.append("Current: ").append(data.getCurrentLeftCPS()).append("\n");
        export.append("Average: ").append(data.getAverageLeftCPS()).append("\n");
        export.append("Max: ").append(data.getMaxLeftCPS()).append("\n");
        export.append("Variance: ").append(data.getLeftVariance()).append("\n");
        export.append("Total: ").append(data.getTotalLeftClicks()).append("\n\n");

        export.append("RIGHT CLICK DATA:\n");
        export.append("Current: ").append(data.getCurrentRightCPS()).append("\n");
        export.append("Average: ").append(data.getAverageRightCPS()).append("\n");
        export.append("Max: ").append(data.getMaxRightCPS()).append("\n");
        export.append("Variance: ").append(data.getRightVariance()).append("\n");
        export.append("Total: ").append(data.getTotalRightClicks()).append("\n\n");

        export.append("SESSION DATA:\n");
        export.append("Duration: ").append(data.getSessionDuration()).append(" ms\n");
        export.append("Violations: ").append(data.getViolationCount()).append("\n");
        export.append("Suspicious Activities: ").append(data.getSuspiciousActivityCount()).append("\n");

        // Log to console for staff review
        plugin.getLogger().info("CPS Data Export:\n" + export.toString());
        sender.sendMessage(ChatColor.GREEN + "Data exported to console log for analysis.");
    }

    // Utility methods (same as before but cleaner)
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

    private String getThreatLevel(double currentCPS, StatsContext ctx) {
        int score = 0;

        // CPS scoring
        if (currentCPS >= ctx.extremeThreshold) score += 3;
        else if (currentCPS >= ctx.highThreshold) score += 2;
        else if (currentCPS >= 10) score += 1;

        // Violation scoring
        if (ctx.data.getViolationCount() >= 10) score += 2;
        else if (ctx.data.getViolationCount() >= 5) score += 1;

        // Consistency scoring
        if (ctx.data.isPerfectlyConsistent(ctx.varianceThreshold)) score += 2;

        // Determine threat level
        if (score >= 5) return ChatColor.DARK_RED + "CRITICAL";
        if (score >= 3) return ChatColor.RED + "HIGH";
        if (score >= 2) return ChatColor.YELLOW + "MODERATE";
        if (score >= 1) return ChatColor.GREEN + "LOW";
        return ChatColor.GRAY + "MINIMAL";
    }

    private String formatDuration(long seconds) {
        if (seconds < 0) return "0s";

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
        sender.sendMessage(ChatColor.GRAY + "  -d, --detailed : Show detailed analysis");
        if (sender.hasPermission("cpswatchdog.export")) {
            sender.sendMessage(ChatColor.GRAY + "  -e, --export   : Export data for analysis");
        }
        sender.sendMessage(ChatColor.GRAY + "Examples:");
        sender.sendMessage(ChatColor.GRAY + "  /cpsstats Player123 -l -d");
        sender.sendMessage(ChatColor.GRAY + "  /cpsstats -h (your own stats)");
        sender.sendMessage(ChatColor.GRAY + "  /cpsstats Player123 -r -h");
    }
}