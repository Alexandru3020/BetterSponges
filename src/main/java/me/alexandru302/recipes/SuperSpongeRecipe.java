package me.alexandru302.recipes;

import me.alexandru302.BetterSponges;
import me.alexandru302.managers.ItemManager;
import me.alexandru302.items.SuperSponge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class SuperSpongeRecipe {
    private final NamespacedKey recipeKey;
    private final Plugin plugin = BetterSponges.getInstance();

    public SuperSpongeRecipe() {
        this.recipeKey = new NamespacedKey(plugin, "super_sponge_recipe");
    }

    public void register() {
        Bukkit.removeRecipe(recipeKey);

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("super_sponge.recipe.enabled", true)) return;

        ItemStack result = ItemManager.getItemStack(SuperSponge.ID);
        if (result == null) return;


        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        List<String> shape = config.getStringList("super_sponge.recipe.shape");
        if (shape.isEmpty() || shape.size() > 3) {
            plugin.getLogger().severe("Invalid recipe shape in config. Expected 1-3 rows.");
            return;
        }
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredientsSection = config.getConfigurationSection("super_sponge.recipe.ingredients");
        if (ingredientsSection == null) {
            plugin.getLogger().severe("Missing recipe ingredients in config.");
            return;
        }

        for (String key : ingredientsSection.getKeys(false)) {
            if (key.length() != 1) {
                plugin.getLogger().severe("Invalid ingredient key length: " + key);
                continue;
            }

            char symbol = key.charAt(0);
            RecipeChoice choice = parseRecipeChoice(ingredientsSection.get(key));
            if (choice == null) {
                plugin.getLogger().severe("Invalid ingredient for key: " + key);
                continue;
            }
            recipe.setIngredient(symbol, choice);
        }

        Bukkit.addRecipe(recipe);
    }

    public void unregister() {
        Bukkit.removeRecipe(recipeKey);
    }

    private RecipeChoice parseRecipeChoice(Object value) {
        if (value instanceof String rawMaterial) {
            Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase());
            return material == null ? null : new RecipeChoice.MaterialChoice(material);
        }

        if (value instanceof List<?> rawList) {
            List<Material> materials = new ArrayList<>();
            for (Object raw : rawList) {
                if (raw instanceof String rawMaterial) {
                    Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase());
                    if (material != null) {
                        materials.add(material);
                    }
                }
            }
            return materials.isEmpty() ? null : new RecipeChoice.MaterialChoice(materials);
        }

        return null;
    }
}