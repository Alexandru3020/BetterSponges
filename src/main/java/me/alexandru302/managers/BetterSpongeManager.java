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
    private AbsorptionShape absorptionShape = AbsorptionShape.CUBE;

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        this.radius = config.getInt("super_sponge.radius", 8);
        this.replaceableMaterials = parseMaterials(config.getStringList("super_sponge.replace_blocks"));
        this.absorbIntervalTicks = config.getLong("super_sponge.absorb_interval_ticks", 1L);
        this.updateBlocks = config.getBoolean("super_sponge.update_blocks", true);
        this.absorptionShape = AbsorptionShape.fromConfig(config.getString("super_sponge.absorption_shape"));
    }

    public void startAbsorb(Location center) {
        World world = center.getWorld();
        Location key = center.clone();
        if (!activeAbsorbs.add(key)) return;

        Deque<Node> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();

        queue.add(new Node(x, y, z, 0));
        visited.add(new BlockPos(x, y, z));

        world.playSound(center, Sound.BLOCK_SPONGE_ABSORB, 2, 0);
        plugin.getLogger().severe("Starting to absorb");
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
                plugin.getLogger().severe("absorbing");
                if (isReplaceable) {
                    target.setType(Material.AIR, updateBlocks);
                }

                if ((node.dist == 0 || isReplaceable || isWaterlogged) && canExpandFrom(x, y, z, node.x, node.y, node.z)) {
                    for (BlockFace face : DIRECTIONS) {
                        int nx = node.x + face.getModX();
                        int ny = node.y + face.getModY();
                        int nz = node.z + face.getModZ();
                        BlockPos nextPos = new BlockPos(nx, ny, nz);

                        if (isWithinShape(x, y, z, nx, ny, nz) && visited.add(nextPos)) {
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
                ItemStack baseItem = ItemManager.getItemStack(SuperSponge.ID);
                plugin.getLogger().severe("base item" + baseItem.getAmount());
                if (baseItem != null) {
                    ItemStack spongeItem = baseItem.clone();
                    spongeItem.setAmount(1);

                    Location spawnLoc = center.clone().add(0.5, 0.5, 0.5);
                    world.spawnParticle(Particle.CLOUD, spawnLoc, 25, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.FALLING_WATER, spawnLoc, 25, 0.2, 0.2, 0.2, 0.1);
                    world.spawn(spawnLoc, Item.class, itemEntity -> {
                        plugin.getLogger().severe("Spawned");
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

    private boolean isWithinShape(int cx, int cy, int cz, int x, int y, int z) {
        return switch (absorptionShape) {
            case DIAMOND -> diamondDistance(cx, cy, cz, x, y, z) <= radius;
            case CUBE -> cubeDistance(cx, cy, cz, x, y, z) <= radius;
            case SPHERE -> sphereDistanceSquared(cx, cy, cz, x, y, z) <= squaredRadius();
        };
    }

    private boolean canExpandFrom(int cx, int cy, int cz, int x, int y, int z) {
        return switch (absorptionShape) {
            case DIAMOND -> diamondDistance(cx, cy, cz, x, y, z) < radius;
            case CUBE -> cubeDistance(cx, cy, cz, x, y, z) < radius;
            case SPHERE -> sphereDistanceSquared(cx, cy, cz, x, y, z) < squaredRadius();
        };
    }

    private int diamondDistance(int cx, int cy, int cz, int x, int y, int z) {
        return Math.abs(x - cx) + Math.abs(y - cy) + Math.abs(z - cz);
    }

    private int cubeDistance(int cx, int cy, int cz, int x, int y, int z) {
        return Math.max(Math.max(Math.abs(x - cx), Math.abs(y - cy)), Math.abs(z - cz));
    }

    private long sphereDistanceSquared(int cx, int cy, int cz, int x, int y, int z) {
        long dx = x - (long) cx;
        long dy = y - (long) cy;
        long dz = z - (long) cz;
        return dx * dx + dy * dy + dz * dz;
    }

    private long squaredRadius() {
        long r = radius;
        return r * r;
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
