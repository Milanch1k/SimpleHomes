package net.milanchik.simpleHomes.commands;

import net.milanchik.simpleHomes.SimpleHomes;
import net.milanchik.simpleHomes.utils.ConfigManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeCMD implements CommandExecutor, TabCompleter {

    private final SimpleHomes simpleHomes;
    private final ConfigManager configManager;
    private final Map<UUID, List<Location>> homesMap;

    public HomeCMD(SimpleHomes simpleHomes, ConfigManager configManager) {
        this.simpleHomes = simpleHomes;
        this.configManager = configManager;
        this.homesMap = simpleHomes.getHomesMap();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(configManager.getConfig("lang.yml")
                    .getString("wrong-syntax", "§cWrong syntax"));
            return true;
        }

        int homeIndex;

        try {
            homeIndex = Integer.parseInt(args[0]) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getConfig("lang.yml")
                    .getString("wrong-syntax", "§cWrong syntax"));
            return true;
        }

        List<Location> homes = homesMap.get(player.getUniqueId());

        if (homes == null || homes.isEmpty()) {
            player.sendMessage(configManager.getConfig("lang.yml")
                    .getString("no-homes", "§cYou have no homes"));
            return true;
        }

        if (homeIndex < 0 || homeIndex >= homes.size()) {
            player.sendMessage(configManager.getConfig("lang.yml")
                    .getString("many-houses", "§cThere aren't that many houses"));
            return true;
        }

        teleportToHome(player, homeIndex);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command cmd,
                                                @NotNull String label,
                                                @NotNull String[] args) {

        if (!(sender instanceof Player player)) return null;

        List<String> completions = new ArrayList<>();
        List<Location> homes = homesMap.get(player.getUniqueId());

        if (homes == null || homes.isEmpty()) return completions;

        if (args.length == 1) {
            for (int i = 1; i <= homes.size(); i++) {
                completions.add(String.valueOf(i));
            }
        }

        return completions;
    }

    private void teleportToHome(Player player, int homeIndex) {

        List<Location> homes = homesMap.get(player.getUniqueId());
        Location homeLocation = homes.get(homeIndex);

        Block bedBlock = homeLocation.getBlock();

        if (!bedBlock.getType().name().endsWith("_BED")) {

            homes.remove(homeIndex);
            simpleHomes.saveHomesToConfig();

            String msg = configManager.getConfig("lang.yml")
                    .getString("break-bed",
                            "§cThe bed in home #%index% was destroyed! Home removed.");

            player.sendMessage(msg.replace("%index%", String.valueOf(homeIndex + 1)));
            return;
        }

        int delay = configManager.getConfig("config.yml").getInt("tp-delay");

        if (delay <= 0) {
            player.teleport(homeLocation.clone().add(0.5, 0, 0.5));
            sendTeleportMessage(player, homeIndex);
            return;
        }

        new BukkitRunnable() {
            int timeLeft = delay;

            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    player.teleport(homeLocation.clone().add(0.5, 0, 0.5));
                    sendTeleportMessage(player, homeIndex);
                    cancel();
                    return;
                }

                String msg = configManager.getConfig("lang.yml")
                        .getString("tp-delay",
                                "§fTeleport in §3%secs% seconds to home #§3%index%");

                msg = msg.replace("%secs%", String.valueOf(timeLeft))
                        .replace("%index%", String.valueOf(homeIndex + 1));

                player.sendActionBar(msg);

                timeLeft--;
            }

        }.runTaskTimer(simpleHomes, 0L, 20L);
    }

    private void sendTeleportMessage(Player player, int index) {
        String msg = configManager.getConfig("lang.yml")
                .getString("teleport-success",
                        "§aTeleported to home #%index%!");

        player.sendMessage(msg.replace("%index%", String.valueOf(index + 1)));
    }
}
