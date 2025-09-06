package dev.abdelrahmanmoharram.cpswatchdog.services;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownService {
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final long cooldownMillis;
    private BukkitTask cleanupTask;

    public CooldownService(int seconds) {
        this.cooldownMillis = seconds * 1000L;
    }

    /**
     * Start automatic cleanup task to remove expired cooldowns
     * This prevents memory leaks from players who leave the server
     */
    public void startCleanupTask(org.bukkit.plugin.Plugin plugin) {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Run cleanup every 5 minutes (6000 ticks)
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }

    /**
     * Stop the cleanup task (call this when plugin is disabled)
     */
    public void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    /**
     * Check if a player is currently on cooldown
     */
    public boolean isOnCooldown(Player player) {
        return isOnCooldown(player.getUniqueId());
    }

    /**
     * Check if a UUID is currently on cooldown
     */
    public boolean isOnCooldown(UUID uuid) {
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        boolean onCooldown = (currentTime - lastUsed) < cooldownMillis;

        // Remove expired cooldown immediately to save memory
        if (!onCooldown) {
            cooldowns.remove(uuid);
        }

        return onCooldown;
    }

    /**
     * Get remaining cooldown time in seconds
     */
    public long getRemaining(Player player) {
        return getRemaining(player.getUniqueId());
    }

    /**
     * Get remaining cooldown time in seconds for UUID
     */
    public long getRemaining(UUID uuid) {
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed == null) {
            return 0;
        }

        long timePassed = System.currentTimeMillis() - lastUsed;
        long remaining = cooldownMillis - timePassed;

        return Math.max(0, remaining / 1000);
    }

    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingMillis(UUID uuid) {
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed == null) {
            return 0;
        }

        long timePassed = System.currentTimeMillis() - lastUsed;
        long remaining = cooldownMillis - timePassed;

        return Math.max(0, remaining);
    }

    /**
     * Set cooldown for a player
     */
    public void setCooldown(Player player) {
        setCooldown(player.getUniqueId());
    }

    /**
     * Set cooldown for a UUID
     */
    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    /**
     * Remove cooldown for a player (admin override)
     */
    public boolean removeCooldown(Player player) {
        return removeCooldown(player.getUniqueId());
    }

    /**
     * Remove cooldown for a UUID (admin override)
     */
    public boolean removeCooldown(UUID uuid) {
        return cooldowns.remove(uuid) != null;
    }

    /**
     * Clear all cooldowns
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    /**
     * Get the number of active cooldowns
     */
    public int getActiveCooldownCount() {
        return cooldowns.size();
    }

    /**
     * Get cooldown duration in seconds
     */
    public long getCooldownDuration() {
        return cooldownMillis / 1000;
    }

    /**
     * Remove expired cooldowns to prevent memory leaks
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = cooldowns.entrySet().iterator();

        int removedCount = 0;
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (currentTime - entry.getValue() >= cooldownMillis) {
                iterator.remove();
                removedCount++;
            }
        }

        // Log cleanup results if significant
        if (removedCount > 10) {
            System.out.println("[CPSWatchdog] Cleaned up " + removedCount + " expired cooldowns");
        }
    }

    /**
     * Format remaining time as a human-readable string
     */
    public String formatRemainingTime(UUID uuid) {
        long remaining = getRemaining(uuid);
        if (remaining <= 0) {
            return "0s";
        }

        if (remaining >= 60) {
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            return minutes + "m " + seconds + "s";
        } else {
            return remaining + "s";
        }
    }
}