package me.alexandru302;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.alexandru302.commands.BetterSpongeCommand;
import me.alexandru302.events.BetterSpongeEvents;
import me.alexandru302.managers.ItemManager;
import me.alexandru302.recipes.SuperSpongeRecipe;
import me.alexandru302.managers.BetterSpongeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterSponges extends JavaPlugin {
    private BetterSpongeManager betterSpongeManager;
    private SuperSpongeRecipe superSpongeRecipe;

    @Override
    public void onEnable() {
        new ItemManager(this);
        saveDefaultConfig();
        betterSpongeManager = new BetterSpongeManager();
        betterSpongeManager.reloadConfig();
        superSpongeRecipe = new SuperSpongeRecipe();
        superSpongeRecipe.register();
        getServer().getPluginManager().registerEvents(new BetterSpongeEvents(betterSpongeManager), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("bettersponges", new BetterSpongeCommand(betterSpongeManager));
        });
    }

    @Override
    public void onDisable() {
        if (superSpongeRecipe != null) superSpongeRecipe.unregister();
    }

    public static BetterSponges getInstance() {
        return getPlugin(BetterSponges.class);
    }
}
