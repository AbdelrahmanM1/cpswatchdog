package dev.abdelrahmanmoharram.cpswatchdog.listeners;

import dev.abdelrahmanmoharram.cpswatchdog.cpswatchdog;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClickListener implements Listener {
    private final cpswatchdog plugin;

    public ClickListener(cpswatchdog plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Track left clicks (attacks/mining)
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getCPSManager().recordLeftClick(player);
        }
        // Track right clicks (interactions/placing blocks)
        else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getCPSManager().recordRightClick(player);
        }
    }
}