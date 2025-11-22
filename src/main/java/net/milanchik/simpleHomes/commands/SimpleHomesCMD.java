package net.milanchik.simpleHomes.commands;

import net.milanchik.simpleHomes.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleHomesCMD implements CommandExecutor, TabCompleter {
    private ConfigManager configManager;

    public SimpleHomesCMD(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reloadall")) {
            configManager.reloadAll();
            commandSender.sendMessage("§aAll yml files have been successfully reloaded");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reload") && args[1].equalsIgnoreCase("config.yml")) {
            configManager.reloadConfig("config.yml");
            commandSender.sendMessage("§aconfig.yml successfully reloaded");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reload") && args[1].equalsIgnoreCase("lang.yml")) {
            configManager.reloadConfig("lang.yml");
            commandSender.sendMessage("§alang.yml successfully reloaded");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reload") && args[1].equalsIgnoreCase("saves.yml")) {
            configManager.reloadConfig("saves.yml");
            commandSender.sendMessage("§asaves.yml successfully reloaded");
        } else {
            commandSender.sendMessage("§cWrong syntax");
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("reloadall");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            completions.add("config.yml");
            completions.add("lang.yml");
            completions.add("saves.yml");
        }

        return completions;
    }
}
