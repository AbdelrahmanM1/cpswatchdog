package dev.abdelrahmanmoharram.cpswatchdog;

import dev.abdelrahmanmoharram.cpswatchdog.commands.CPSCommand;
import dev.abdelrahmanmoharram.cpswatchdog.commands.CPSStatsCommand;
import dev.abdelrahmanmoharram.cpswatchdog.commands.CPSAlertCommand;
import dev.abdelrahmanmoharram.cpswatchdog.commands.CPSReloadCommand;
import dev.abdelrahmanmoharram.cpswatchdog.listeners.ClickListener;
import dev.abdelrahmanmoharram.cpswatchdog.manager.CPSManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class cpswatchdog extends JavaPlugin {
    private static cpswatchdog instance;
    private CPSManager cpsManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config first
        saveDefaultConfig();

        // Initialize CPS Manager (this will load configuration)
        cpsManager = new CPSManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new ClickListener(this), this);

        // Register commands
        getCommand("cps").setExecutor(new CPSCommand(this));
        getCommand("cpsstats").setExecutor(new CPSStatsCommand(this));
        getCommand("cpsalert").setExecutor(new CPSAlertCommand(this));
        getCommand("cpsreload").setExecutor(new CPSReloadCommand(this));

        getLogger().info("CPSWatchdog has been Enabled -- made by 3bdoabk!");
        getLogger().info("Anti-cheat system is now monitoring player clicks!");

        // Log configuration status
        getLogger().info("Configuration loaded:");
        getLogger().info("- High CPS Threshold: " + cpsManager.getHighCPSThreshold());
        getLogger().info("- Extreme CPS Threshold: " + cpsManager.getExtremeCPSThreshold());
        getLogger().info("- Staff Notifications: " + (cpsManager.isNotifyStaff() ? "Enabled" : "Disabled"));
        getLogger().info("- Console Logging: " + (cpsManager.isLogToConsole() ? "Enabled" : "Disabled"));
    }

    @Override
    public void onDisable() {
        getLogger().info("CPSWatchdog has been disabled -- made by 3bdoabk!");
    }

    public static cpswatchdog getInstance() {
        return instance;
    }

    public CPSManager getCPSManager() {
        return cpsManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        cpsManager.reloadConfiguration();
        getLogger().info("CPSWatchdog configuration reloaded!");
    }
}