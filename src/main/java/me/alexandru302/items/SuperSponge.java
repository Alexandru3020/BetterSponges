package me.alexandru302.items;

import me.alexandru302.BetterSponges;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class SuperSponge implements ICustomItem {
    public static final String ID = "super_sponge";

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.SPONGE);
        ItemMeta meta = item.getItemMeta();

        int radius = BetterSponges.getInstance().getConfig().getInt("super_sponge.radius", 8);
        meta.displayName(Component.text("Super Sponge", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Absorbs all water in a " + radius + " block radius.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String getId() {
        return ID;
    }
}
