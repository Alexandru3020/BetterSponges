package me.alexandru302.events;

import me.alexandru302.BetterSponges;
import me.alexandru302.managers.BetterSpongeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;

public class BetterSpongeEvents implements Listener {
    private final BetterSpongeManager manager = BetterSponges.getInstance().getSpongeManager();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!manager.isSuperSponge(event.getItemInHand())) return;

        manager.markSuperSpongePlaced(event.getBlockPlaced().getLocation());
        manager.startAbsorb(event.getBlockPlaced().getLocation());
    }

    @EventHandler
    public void onSponging(SpongeAbsorbEvent event) {
        if (manager.isSuperSpongeBlock(event.getBlock().getLocation())) event.setCancelled(true);
    }
}
