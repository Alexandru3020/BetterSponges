package me.alexandru302.commands;

import me.alexandru302.BetterSponges;
import me.alexandru302.managers.BetterSpongeManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.alexandru302.managers.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BetterSpongeCommand implements BasicCommand {

    private final BetterSpongeManager betterSpongeManager;

    public BetterSpongeCommand(BetterSpongeManager betterSpongeManager) {
        this.betterSpongeManager = betterSpongeManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            source.getSender().sendMessage(Component.text("Usage: /bettersponges <reload|give>", NamedTextColor.RED));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(source);
            case "give" -> handleGive(source, args);
            default -> source.getSender().sendMessage(Component.text("Unknown sub-command.", NamedTextColor.RED));
        }
    }

    private void handleReload(CommandSourceStack source) {
        BetterSponges.getInstance().reloadConfig();
        betterSpongeManager.reloadConfig();
        ItemManager.refreshItemStacks();
        ItemManager.refreshOnlineInventories();
        BetterSponges.getInstance().restartTooltipRefreshTask();
        source.getSender().sendMessage(Component.text("BetterSponges reloaded.", NamedTextColor.GREEN));
    }

    private void handleGive(CommandSourceStack source, String[] args) {
        if (args.length < 2) {
            source.getSender().sendMessage(Component.text("Usage: /bettersponges give <itemId> [amount] [player]", NamedTextColor.RED));
            return;
        }

        ItemStack item = ItemManager.getItemStack(args[1]);
        if (item == null) {
            source.getSender().sendMessage(Component.text("Unknown item id: " + args[1], NamedTextColor.RED));
            return;
        }

        int amount = 1;
        Player target = source.getSender() instanceof Player meowl ? meowl : null;

        if (args.length >= 3) {
            if (isNumber(args[2])) {
                amount = Math.min(Integer.parseInt(args[2]), 64);
            } else {
                target = Bukkit.getPlayerExact(args[2]);
            }
        }

        if (args.length >= 4 && target == null) {
            target = Bukkit.getPlayerExact(args[3]);
        }

        if (target == null) {
            source.getSender().sendMessage(Component.text("Target player not found or not specified.", NamedTextColor.RED));
            return;
        }

        item.setAmount(Math.max(1, amount));
        target.getInventory().addItem(item.clone());

        source.getSender().sendMessage(Component.text()
                .append(Component.text("Gave "))
                .append(Component.text(amount + " "))
                .append(Objects.requireNonNull(item.getItemMeta().displayName()))
                .append(Component.text(" to " + target.getName(), NamedTextColor.WHITE))
                .build());
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSourceStack source, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            return StringUtil.copyPartialMatches(args.length == 0 ? "" : args[0], List.of("reload", "give"), new ArrayList<>());
        }

        if (args[0].equalsIgnoreCase("give")) {
            return switch (args.length) {
                case 2 -> StringUtil.copyPartialMatches(args[1], ItemManager.getAllItemIds(), new ArrayList<>());
                case 3 -> StringUtil.copyPartialMatches(args[2], getPlaceholderGiveCount(), new ArrayList<>());
                case 4 -> StringUtil.copyPartialMatches(args[3], getPlayerNames(), new ArrayList<>());
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<String> getPlaceholderGiveCount() {
        List<String> options = new ArrayList<>(List.of("1", "16", "32", "64"));
        options.addAll(getPlayerNames());
        return options;
    }

    private List<String> getPlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public @NotNull String permission() {
        return "bettersponge.admin";
    }

    private static boolean isNumber(String raw) {
        if (raw == null || raw.isEmpty()) return false;
        return raw.chars().allMatch(Character::isDigit);
    }
}