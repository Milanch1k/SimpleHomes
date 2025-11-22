package net.milanchik.simpleHomes.commands;

import net.milanchik.simpleHomes.SimpleHomes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesCMD implements CommandExecutor {
    private SimpleHomes simpleHomes;

    public HomesCMD(SimpleHomes simpleHomes) {
        this.simpleHomes = simpleHomes;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("only player command");
            return true;
        }

        Player player = (Player) sender;

        simpleHomes.createHomeMenu(player);

        return true;
    }
}
