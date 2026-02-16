package me.alexandru302.managers;

import me.alexandru302.BetterSponges;
import me.alexandru302.items.SuperSponge;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BetterSpongeManager {
    private final BetterSponges plugin = BetterSponges.getInstance();
    private final Set<Location> activeAbsorbs = ConcurrentHashMap.newKeySet();
    private final Set<Location> placedSuperSponges = ConcurrentHashMap.newKeySet();
    private static final BlockFace[] DIRECTIONS = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private int radius;
    private Set<Material> replaceableMaterials;
    private long absorbIntervalTicks;
    private boolean updateBlocks;

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        this.radius = config.getInt("super_sponge.radius", 8);
        this.replaceableMaterials = parseMaterials(config.getStringList("super_sponge.replace_blocks"));
        this.absorbIntervalTicks = config.getLong("super_sponge.absorb_interval_ticks", 1L);
        this.updateBlocks = config.getBoolean("super_sponge.update_blocks", true);
    }

    public void startAbsorb(Location center) {
        World world = center.getWorld();
        Location key = center.clone();
        if (!activeAbsorbs.add(key)) return;

        Deque<Node> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(new Node(center.getBlockX(), center.getBlockY(), center.getBlockZ(), 0));
        visited.add(new BlockPos(center.getBlockX(), center.getBlockY(), center.getBlockZ()));

        world.playSound(center, Sound.BLOCK_SPONGE_ABSORB, 2, 0);

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            int nodesThisTick = queue.size();
            for (int i = 0; i < nodesThisTick; i++) {
                Node node = queue.poll();
                Block target = world.getBlockAt(node.x, node.y, node.z);
                boolean isReplaceable = replaceableMaterials.contains(target.getType());
                boolean isWaterlogged = false;

                if (target.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
                    isWaterlogged = true;
                    waterlogged.setWaterlogged(false);
                    target.setBlockData(waterlogged, updateBlocks);
                }

                if (isReplaceable) {
                    target.setType(Material.AIR, updateBlocks);
                }

                if ((node.dist == 0 || isReplaceable || isWaterlogged) && node.dist < radius) {
                    for (BlockFace face : DIRECTIONS) {
                        int nx = node.x + face.getModX();
                        int ny = node.y + face.getModY();
                        int nz = node.z + face.getModZ();
                        BlockPos nextPos = new BlockPos(nx, ny, nz);
                        if (visited.add(nextPos)) {
                            queue.add(new Node(nx, ny, nz, node.dist + 1));
                        }
                    }
                }
            }

            if (queue.isEmpty()) {
                activeAbsorbs.remove(key);
                task.cancel();

                center.getBlock().setType(Material.AIR, true);
                placedSuperSponges.remove(key);
                ItemStack spongeItem = ItemManager.getItemStack(SuperSponge.ID);

                if (spongeItem != null) {
                    Location spawnLoc = center.clone().add(0.5, 0.5, 0.5);
                    world.spawnParticle(Particle.CLOUD, spawnLoc, 25, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.FALLING_WATER, spawnLoc, 25, 0.2, 0.2, 0.2, 0.1);
                    world.spawn(spawnLoc, Item.class, itemEntity -> {
                        itemEntity.setItemStack(spongeItem);
                        itemEntity.setGravity(false);
                        itemEntity.setPersistent(true);
                        itemEntity.setUnlimitedLifetime(true);
                        itemEntity.setGlowing(true);
                        itemEntity.setVelocity(new Vector(0, 0.01, 0));
                        world.playSound(spawnLoc, Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1);
                    });
                }
            }
        }, 0L, absorbIntervalTicks);
    }
    private Set<Material> parseMaterials(List<String> rawMats) {
        Set<Material> parsed = EnumSet.noneOf(Material.class);
        if (rawMats != null && !rawMats.isEmpty()) {
            for (String raw : rawMats) {
                Material mat = Material.matchMaterial(raw);
                if (mat != null) {
                    parsed.add(mat);
                } else {
                    plugin.getLogger().severe("Invalid material in replace_blocks config: " + raw);
                }
            }
        }
        return parsed;
    }
    public boolean isSuperSponge(ItemStack item) {
        return ItemManager.isItem(item, SuperSponge.ID);
    }

    public void markSuperSpongePlaced(Location location) {
        placedSuperSponges.add(location);
    }

    public boolean isSuperSpongeBlock(Location location) {
        return placedSuperSponges.contains(location);
    }

    private record Node(int x, int y, int z, int dist) {}
    private record BlockPos(int x, int y, int z) {}
}
