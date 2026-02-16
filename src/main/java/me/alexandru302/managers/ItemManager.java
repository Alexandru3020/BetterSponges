package me.alexandru302.managers;

import me.alexandru302.BetterSponges;
import me.alexandru302.items.ICustomItem;
import me.alexandru302.items.SuperSponge;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ItemManager implements Listener {
    public static final NamespacedKey KEY = new NamespacedKey(BetterSponges.getInstance(), "custom_item");
    public static final PersistentDataType<String, String> TYPE = PersistentDataType.STRING;

    private static final HashMap<String, ICustomItem> items = new HashMap<>();
    private static final HashMap<String, ItemStack> itemStacks = new HashMap<>();

    public ItemManager(BetterSponges plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        ItemManager.register(new SuperSponge());
    }

    public static void register(ICustomItem customItem) {
        ItemStack item = customItem.getItem();
        if (item == null) return;

        applyId(item, customItem.getId());
        items.put(customItem.getId(), customItem);
        itemStacks.put(customItem.getId(), item);
        //BetterSponges.logInfo("Registered item: " + customItem.getId());
    }

    public static void refreshItemStacks() {
        itemStacks.clear();
        for (ICustomItem customItem : items.values()) {
            ItemStack item = customItem.getItem();
            if (item == null) continue;
            applyId(item, customItem.getId());
            itemStacks.put(customItem.getId(), item);
        }
    }

    private static void applyId(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, TYPE, id);
        item.setItemMeta(meta);
    }

    public static String getId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
        if (!container.has(KEY, TYPE)) return null;
        return container.get(KEY, TYPE);
    }

    public static ICustomItem fromItemStack(ItemStack stack) {
        String id = getId(stack);
        if (id == null) return null;
        return items.get(id);
    }

    public static boolean isItem(ItemStack stack, String id) {
        String stackId = getId(stack);
        return stackId != null && stackId.equals(id);
    }

    public static ItemStack getItemStack(String id) {
        return itemStacks.get(id);
    }
    public static List<String> getAllItemIds() {
        return items.keySet().stream().sorted().collect(Collectors.toList());
    }
}
