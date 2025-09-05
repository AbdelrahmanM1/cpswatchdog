package dev.abdelrahmanmoharram.cpswatchdog.manager;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import dev.abdelrahmanmoharram.cpswatchdog.data.PlayerCPSData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CPSManager {
    private final cpswatchdog plugin;
    private final Map<UUID, PlayerCPSData> playerData;
    private final Set<UUID> alertEnabledPlayers;

    // General configuration values
    private double highCPSThreshold;
    private double extremeCPSThreshold;
    private int violationsForAlert;

    // Separate thresholds for left/right clicks
    private boolean enableSeparateLeftThresholds;
    private boolean enableSeparateRightThresholds;
    private double leftClickHighThreshold;
    private double leftClickExtremeThreshold;
    private double rightClickHighThreshold;
    private double rightClickExtremeThreshold;

    // Alert settings
    private boolean notifyStaff;
    private boolean logToConsole;
    private boolean broadcastExtreme;
    private boolean enableSounds;
    private String highCPSSound;
    private String extremeCPSSound;

    // Message templates
    private String leftClickAlertMessage;
    private String rightClickAlertMessage;
    private String combinedAlertMessage;

    // Detection settings
    private boolean consistencyCheck;
    private double varianceThreshold;
    private int minimumClicksForAnalysis;
    private boolean patternDetection;
    private double patternThreshold;
    private boolean burstDetection;
    private double burstThreshold;
    private long burstDuration;
    private boolean analyzeSeparately;
    private boolean crossClickAnalysis;

    // Data management settings
    private long cleanupInterval;
    private long sessionTimeout;
    private boolean savePlayerData;
    private int dataRetentionDays;
    private boolean autoCleanup;

    // Performance settings
    private boolean asyncProcessing;
    private int maxPlayersPerTick;
    private boolean optimizeMemory;
    private int maxStoredClicks;

    public CPSManager(cpswatchdog plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.alertEnabledPlayers = new HashSet<>();

        loadConfiguration();
        startCleanupTask();
    }

    private void loadConfiguration() {
        // Load general thresholds
        highCPSThreshold = plugin.getConfig().getDouble("thresholds.high_cps", 15.0);
        extremeCPSThreshold = plugin.getConfig().getDouble("thresholds.extreme_cps", 20.0);
        violationsForAlert = plugin.getConfig().getInt("thresholds.violations_for_alert", 5);

        // Load separate click thresholds
        enableSeparateLeftThresholds = plugin.getConfig().getBoolean("thresholds.left_click.enable_separate", false);
        enableSeparateRightThresholds = plugin.getConfig().getBoolean("thresholds.right_click.enable_separate", false);

        leftClickHighThreshold = plugin.getConfig().getDouble("thresholds.left_click.high_cps", highCPSThreshold);
        leftClickExtremeThreshold = plugin.getConfig().getDouble("thresholds.left_click.extreme_cps", extremeCPSThreshold);
        rightClickHighThreshold = plugin.getConfig().getDouble("thresholds.right_click.high_cps", highCPSThreshold);
        rightClickExtremeThreshold = plugin.getConfig().getDouble("thresholds.right_click.extreme_cps", extremeCPSThreshold);

        // Load alert settings
        notifyStaff = plugin.getConfig().getBoolean("alerts.notify_staff", true);
        logToConsole = plugin.getConfig().getBoolean("alerts.log_to_console", true);
        broadcastExtreme = plugin.getConfig().getBoolean("alerts.broadcast_extreme", false);

        enableSounds = plugin.getConfig().getBoolean("alerts.sounds.enable_sounds", false);
        highCPSSound = plugin.getConfig().getString("alerts.sounds.high_cps_sound", "BLOCK_NOTE_BLOCK_PLING");
        extremeCPSSound = plugin.getConfig().getString("alerts.sounds.extreme_cps_sound", "ENTITY_WITHER_SPAWN");

        // Load message templates
        leftClickAlertMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("alerts.messages.left_click_alert",
                        "&c[CPSWatchdog] &e{player} &fhas suspicious left click behavior: &c{reason}"));
        rightClickAlertMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("alerts.messages.right_click_alert",
                        "&c[CPSWatchdog] &e{player} &fhas suspicious right click behavior: &c{reason}"));
        combinedAlertMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("alerts.messages.combined_alert",
                        "&c[CPSWatchdog] &e{player} &fhas suspicious click behavior: &c{reason}"));

        // Load detection settings
        consistencyCheck = plugin.getConfig().getBoolean("detection.consistency_check", true);
        varianceThreshold = plugin.getConfig().getDouble("detection.variance_threshold", 0.1);
        minimumClicksForAnalysis = plugin.getConfig().getInt("detection.minimum_clicks_for_analysis", 10);

        patternDetection = plugin.getConfig().getBoolean("detection.advanced.pattern_detection", true);
        patternThreshold = plugin.getConfig().getDouble("detection.advanced.pattern_threshold", 0.95);
        burstDetection = plugin.getConfig().getBoolean("detection.advanced.burst_detection", true);
        burstThreshold = plugin.getConfig().getDouble("detection.advanced.burst_threshold", 25.0);
        burstDuration = plugin.getConfig().getLong("detection.advanced.burst_duration", 1000);
        analyzeSeparately = plugin.getConfig().getBoolean("detection.advanced.analyze_separately", true);
        crossClickAnalysis = plugin.getConfig().getBoolean("detection.advanced.cross_click_analysis", false);

        // Load data management settings
        cleanupInterval = plugin.getConfig().getLong("data.cleanup_interval", 300000);
        sessionTimeout = plugin.getConfig().getLong("data.session_timeout", 300000);
        savePlayerData = plugin.getConfig().getBoolean("data.storage.save_player_data", false);
        dataRetentionDays = plugin.getConfig().getInt("data.storage.data_retention_days", 7);
        autoCleanup = plugin.getConfig().getBoolean("data.storage.auto_cleanup", true);

        // Load performance settings
        asyncProcessing = plugin.getConfig().getBoolean("performance.async_processing", false);
        maxPlayersPerTick = plugin.getConfig().getInt("performance.max_players_per_tick", 10);
        optimizeMemory = plugin.getConfig().getBoolean("performance.optimize_memory", true);
        maxStoredClicks = plugin.getConfig().getInt("performance.max_stored_clicks", 20);
    }

    public void recordLeftClick(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerCPSData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerCPSData(player.getName(), maxStoredClicks));

        data.addLeftClick();

        // Only analyze if player has enough clicks
        if (data.getTotalLeftClicks() >= minimumClicksForAnalysis) {
            if (asyncProcessing) {
                analyzePlayerBehaviorAsync(player, data, "left");
            } else {
                analyzePlayerBehavior(player, data, "left");
            }
        }
    }

    public void recordRightClick(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerCPSData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerCPSData(player.getName(), maxStoredClicks));

        data.addRightClick();

        // Only analyze if player has enough clicks
        if (data.getTotalRightClicks() >= minimumClicksForAnalysis) {
            if (asyncProcessing) {
                analyzePlayerBehaviorAsync(player, data, "right");
            } else {
                analyzePlayerBehavior(player, data, "right");
            }
        }
    }

    // Legacy method for backward compatibility
    public void recordClick(Player player) {
        recordLeftClick(player);
    }

    private void analyzePlayerBehaviorAsync(Player player, PlayerCPSData data, String clickType) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            analyzePlayerBehavior(player, data, clickType);
        });
    }

    private void analyzePlayerBehavior(Player player, PlayerCPSData data, String clickType) {
        double currentCPS;
        boolean suspicious = false;
        StringBuilder reasonBuilder = new StringBuilder();
        String clickTypeFormatted = clickType.equals("left") ? "Left" : "Right";

        // Get appropriate CPS and thresholds based on click type
        double highThreshold, extremeThreshold;
        if (clickType.equals("left")) {
            currentCPS = data.getCurrentLeftCPS();
            highThreshold = enableSeparateLeftThresholds ? leftClickHighThreshold : highCPSThreshold;
            extremeThreshold = enableSeparateLeftThresholds ? leftClickExtremeThreshold : extremeCPSThreshold;
        } else {
            currentCPS = data.getCurrentRightCPS();
            highThreshold = enableSeparateRightThresholds ? rightClickHighThreshold : highCPSThreshold;
            extremeThreshold = enableSeparateRightThresholds ? rightClickExtremeThreshold : extremeCPSThreshold;
        }

        boolean isExtreme = false;

        // Check CPS thresholds
        if (currentCPS >= extremeThreshold) {
            data.incrementViolations();
            suspicious = true;
            isExtreme = true;
            reasonBuilder.append("Extreme ").append(clickTypeFormatted)
                    .append(" CPS: ").append(String.format("%.1f", currentCPS));

            if (broadcastExtreme) {
                broadcastToAll(ChatColor.DARK_RED + "[CPSWatchdog] " + player.getName() +
                        " is " + clickType + " clicking at " + String.format("%.1f", currentCPS) + " CPS!");
            }
        } else if (currentCPS >= highThreshold) {
            data.incrementViolations();
            suspicious = true;
            reasonBuilder.append("High ").append(clickTypeFormatted)
                    .append(" CPS: ").append(String.format("%.1f", currentCPS));
        }

        // Check for burst detection
        if (burstDetection && currentCPS >= burstThreshold) {
            if (reasonBuilder.length() > 0) reasonBuilder.append(" & ");
            reasonBuilder.append("Click burst detected");
            suspicious = true;
        }

        // Check for bot-like consistency
        boolean isConsistent = false;
        if (consistencyCheck) {
            if (clickType.equals("left")) {
                isConsistent = data.isLeftClickPerfectlyConsistent(varianceThreshold);
            } else {
                isConsistent = data.isRightClickPerfectlyConsistent(varianceThreshold);
            }

            if (isConsistent) {
                data.incrementSuspiciousActivity();
                suspicious = true;
                if (reasonBuilder.length() > 0) reasonBuilder.append(" & ");
                reasonBuilder.append("Bot-like ").append(clickType).append(" click consistency");
            }
        }

        // Check for patterns if enabled
        if (patternDetection && data.hasDetectedPattern(clickType, patternThreshold)) {
            suspicious = true;
            if (reasonBuilder.length() > 0) reasonBuilder.append(" & ");
            reasonBuilder.append("Repetitive click pattern");
        }

        // Handle violations
        if (suspicious) {
            String reason = reasonBuilder.toString();
            handleSuspiciousActivity(player, data, reason, isExtreme, clickType);
        }

        // Reset violations if player is behaving normally
        if (currentCPS < highThreshold && !isConsistent) {
            if (data.getViolationCount() > 0) {
                data.resetViolations();
            }
        }
    }

    private void handleSuspiciousActivity(Player player, PlayerCPSData data, String reason, boolean extreme, String clickType) {
        // Log to console
        if (logToConsole) {
            String logMessage = String.format("[CPSWatchdog] %s: %s (Violations: %d)",
                    player.getName(), reason, data.getViolationCount());
            if (extreme) {
                plugin.getLogger().severe(logMessage);
            } else {
                plugin.getLogger().warning(logMessage);
            }
        }

        // Notify staff if violations reach threshold
        if (data.getViolationCount() >= violationsForAlert && notifyStaff) {
            notifyStaffMembers(player, data, reason, extreme, clickType);
        }
    }

    private void notifyStaffMembers(Player player, PlayerCPSData data, String reason, boolean extreme, String clickType) {
        String messageTemplate;
        if (clickType.equals("left")) {
            messageTemplate = leftClickAlertMessage;
        } else if (clickType.equals("right")) {
            messageTemplate = rightClickAlertMessage;
        } else {
            messageTemplate = combinedAlertMessage;
        }

        String message = messageTemplate
                .replace("{player}", player.getName())
                .replace("{reason}", reason)
                .replace("{violations}", String.valueOf(data.getViolationCount()));

        Sound alertSound = null;
        if (enableSounds) {
            try {
                String soundName = extreme ? extremeCPSSound : highCPSSound;
                alertSound = Sound.valueOf(soundName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound name in configuration: " + (extreme ? extremeCPSSound : highCPSSound));
            }
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("cpswatchdog.notify") && alertEnabledPlayers.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(message);

                if (alertSound != null) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), alertSound, 0.5f, 1.0f);
                }
            }
        }
    }

    private void broadcastToAll(String message) {
        Bukkit.broadcastMessage(message);
    }

    public void toggleAlerts(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (alertEnabledPlayers.contains(playerUUID)) {
            alertEnabledPlayers.remove(playerUUID);
            player.sendMessage(ChatColor.YELLOW + "[CPSWatchdog] Alerts disabled.");
        } else {
            alertEnabledPlayers.add(playerUUID);
            player.sendMessage(ChatColor.GREEN + "[CPSWatchdog] Alerts enabled.");
        }
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (autoCleanup) {
                    cleanupInactiveData();
                }
            }
        }.runTaskTimer(plugin, cleanupInterval / 50, cleanupInterval / 50); // Convert ms to ticks
    }

    private void cleanupInactiveData() {
        long currentTime = System.currentTimeMillis();
        int initialSize = playerData.size();

        playerData.entrySet().removeIf(entry -> {
            PlayerCPSData data = entry.getValue();
            return (currentTime - data.getLastClickTime()) > sessionTimeout;
        });

        int removedEntries = initialSize - playerData.size();

        if (removedEntries > 0 && logToConsole) {
            plugin.getLogger().info("Cleaned up " + removedEntries + " inactive player data entries.");
        }
    }

    public PlayerCPSData getPlayerData(UUID playerUUID) {
        return playerData.get(playerUUID);
    }

    public void reloadConfiguration() {
        loadConfiguration();
        plugin.getLogger().info("CPSWatchdog configuration reloaded.");
    }

    // Getters for configuration values
    public double getHighCPSThreshold() { return highCPSThreshold; }
    public double getExtremeCPSThreshold() { return extremeCPSThreshold; }
    public double getLeftClickHighThreshold() { return enableSeparateLeftThresholds ? leftClickHighThreshold : highCPSThreshold; }
    public double getLeftClickExtremeThreshold() { return enableSeparateLeftThresholds ? leftClickExtremeThreshold : extremeCPSThreshold; }
    public double getRightClickHighThreshold() { return enableSeparateRightThresholds ? rightClickHighThreshold : highCPSThreshold; }
    public double getRightClickExtremeThreshold() { return enableSeparateRightThresholds ? rightClickExtremeThreshold : extremeCPSThreshold; }
    public int getViolationsForAlert() { return violationsForAlert; }
    public boolean isNotifyStaff() { return notifyStaff; }
    public boolean isLogToConsole() { return logToConsole; }
    public boolean isBroadcastExtreme() { return broadcastExtreme; }
    public boolean isConsistencyCheck() { return consistencyCheck; }
    public double getVarianceThreshold() { return varianceThreshold; }
    public int getMinimumClicksForAnalysis() { return minimumClicksForAnalysis; }
    public boolean isPatternDetection() { return patternDetection; }
    public boolean isBurstDetection() { return burstDetection; }
    public boolean isAnalyzeSeparately() { return analyzeSeparately; }
    public boolean isAsyncProcessing() { return asyncProcessing; }
    public int getActivePlayerCount() { return playerData.size(); }
}