package me.alexandru302.managers;

import me.alexandru302.BetterSponges;
import me.alexandru302.items.ICustomItem;
import me.alexandru302.items.SuperSponge;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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

    public static void refreshOnlineInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshInventory(player.getInventory());
            refreshInventory(player.getEnderChest());
            refreshInventory(player.getOpenInventory().getTopInventory());
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

    private static void refreshInventory(Inventory inventory) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            ItemStack updated = refreshItemStack(stack);
            if (updated != stack) {
                inventory.setItem(i, updated);
            }
        }
    }

    private static ItemStack refreshItemStack(ItemStack stack) {
        ICustomItem customItem = fromItemStack(stack);
        if (customItem == null) return stack;

        if (!SuperSponge.ID.equals(customItem.getId())) return stack;
        applySuperSpongeTooltipRadius(stack);
        return stack;
    }

    private static void applySuperSpongeTooltipRadius(ItemStack stack) {
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        List<String> loreTemplate = BetterSponges.getInstance()
                .getConfig()
                .getStringList("super_sponge.item.lore");
        if (loreTemplate.isEmpty()) {
            loreTemplate = List.of("&7Absorbs all water in a {radius} block radius.");
        }

        int radius = BetterSponges.getInstance().getConfig().getInt("super_sponge.radius", 8);
        meta.lore(loreTemplate.stream()
                .map(line -> parseTemplate(line, radius))
                .collect(Collectors.toList()));
        stack.setItemMeta(meta);
    }

    private static Component parseTemplate(String raw, int radius) {
        String resolved = raw == null ? "" : raw.replace("{radius}", String.valueOf(radius));
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(resolved)
                .decoration(TextDecoration.ITALIC, false);
    }
}
