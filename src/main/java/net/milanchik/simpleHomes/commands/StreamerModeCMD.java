package net.milanchik.simpleHomes.commands;

import net.milanchik.simpleHomes.SimpleHomes;
import net.milanchik.simpleHomes.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class StreamerModeCMD implements CommandExecutor {
    private SimpleHomes simpleHomes;
    private List<UUID> streamerMode;
    private ConfigManager configManager;

    public StreamerModeCMD(SimpleHomes simpleHomes, ConfigManager configManager) {
        this.simpleHomes = simpleHomes;
        this.streamerMode = simpleHomes.getStreamerMode();
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            return true;
        }

        if (streamerMode.contains(player.getUniqueId())) {
            streamerMode.remove(player.getUniqueId());
            simpleHomes.setStreamerMode(streamerMode);
            simpleHomes.saveStreamersToConfig();
            commandSender.sendMessage(configManager.getConfig("lang.yml").getString("successfully", "§aSuccessfully"));
            return true;
        } else {
            streamerMode.add(player.getUniqueId());
            simpleHomes.setStreamerMode(streamerMode);
            simpleHomes.saveStreamersToConfig();
            commandSender.sendMessage(configManager.getConfig("lang.yml").getString("successfully", "§aSuccessfully"));
            return true;
        }
    }
}
