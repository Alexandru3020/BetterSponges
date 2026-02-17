package me.alexandru302.items;

import me.alexandru302.BetterSponges;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.stream.Collectors;

public class SuperSponge implements ICustomItem {
    public static final String ID = "super_sponge";

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.SPONGE);
        ItemMeta meta = item.getItemMeta();

        int radius = BetterSponges.getInstance().getConfig().getInt("super_sponge.radius", 8);
        String nameRaw = BetterSponges.getInstance().getConfig().getString("super_sponge.item.name", "&dSuper Sponge");
        List<String> loreRaw = BetterSponges.getInstance().getConfig().getStringList("super_sponge.item.lore");
        if (loreRaw.isEmpty()) {
            loreRaw = List.of("&7Absorbs all water in a {radius} block radius.");
        }

        meta.displayName(parse(nameRaw, radius));
        meta.lore(loreRaw.stream().map(line -> parse(line, radius)).collect(Collectors.toList()));
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String getId() {
        return ID;
    }

    private Component parse(String raw, int radius) {
        String resolved = raw == null ? "" : raw.replace("{radius}", String.valueOf(radius));
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(resolved)
                .decoration(TextDecoration.ITALIC, false);
    }
}
