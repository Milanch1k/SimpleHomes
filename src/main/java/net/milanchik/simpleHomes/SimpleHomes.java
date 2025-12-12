package net.milanchik.simpleHomes;

import net.milanchik.simpleHomes.commands.HomeCMD;
import net.milanchik.simpleHomes.commands.HomesCMD;
import net.milanchik.simpleHomes.commands.SimpleHomesCMD;
import net.milanchik.simpleHomes.commands.StreamerModeCMD;
import net.milanchik.simpleHomes.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class SimpleHomes extends JavaPlugin implements Listener {
    public Map<UUID, List<Location>> homesMap = new HashMap<>();
    private ConfigManager configManager;
    public Map<UUID, Integer> delayMap = new HashMap<>();
    public List<UUID> streamerMode = new ArrayList<>();

    public void setStreamerMode(List<UUID> streamerMode) {
        this.streamerMode = streamerMode;
    }

    public List<UUID> getStreamerMode() {
        return streamerMode;
    }

    public Map<UUID, List<Location>> getHomesMap() {
        loadHomesFromConfig();
        return homesMap;
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);

        configManager.setup("config.yml");
        configManager.setup("saves.yml");
        configManager.setup("lang.yml");

        loadHomesFromConfig();
        loadStreamersFromConfig();

        startDelayLoop();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("homes").setExecutor(new HomesCMD(this));
        getCommand("simplehomes").setExecutor(new SimpleHomesCMD(configManager));
        getCommand("simplehomes").setTabCompleter(new SimpleHomesCMD(configManager));
        getCommand("home").setExecutor(new HomeCMD(this, configManager));
        getCommand("home").setTabCompleter(new HomeCMD(this, configManager));
        getCommand("SHStreamerMode").setExecutor(new StreamerModeCMD(this, configManager));
    }

    @Override
    public void onDisable() {
        saveHomesToConfig();
        saveStreamersToConfig();
    }

    private void startDelayLoop() {
        BukkitRunnable delayTask = new BukkitRunnable() {
            @Override
            public void run() {
                int delay;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (delayMap.containsKey(player.getUniqueId())) {
                        delay = delayMap.get(player.getUniqueId()) - 1;
                        if (delayMap.get(player.getUniqueId()) == 0) {
                            delayMap.remove(player.getUniqueId());
                        } else {
                            delayMap.replace(player.getUniqueId(), delayMap.get(player.getUniqueId()), delay);
                        }
                    }
                }
            }
        };
        delayTask.runTaskTimer(this, 0L, 20L);
    }

    private void loadHomesFromConfig() {
        homesMap.clear();
        if (configManager.getConfig("saves.yml").contains("homes")) {
            for (String playerUUID : configManager.getConfig("saves.yml").getConfigurationSection("homes").getKeys(false)) {
                UUID uuid = UUID.fromString(playerUUID);
                List<Location> playerHomes = new ArrayList<>();

                List<String> homeLocations = configManager.getConfig("saves.yml").getStringList("homes." + playerUUID);
                for (String locationString : homeLocations) {
                    Location location = locationFromString(locationString);
                    if (location != null) {
                        playerHomes.add(location);
                    }
                }
                homesMap.put(uuid, playerHomes);
            }
        }
    }

    public void saveHomesToConfig() {
        for (UUID playerUUID : homesMap.keySet()) {
            List<Location> playerHomes = homesMap.get(playerUUID);
            List<String> homeLocations = new ArrayList<>();

            for (Location location : playerHomes) {
                homeLocations.add(locationToString(location));
            }

            configManager.getConfig("saves.yml").set("homes." + playerUUID.toString(), homeLocations);
        }
        configManager.saveConfig("saves.yml");
    }

    public void loadStreamersFromConfig() {
        streamerMode.clear();
        if (configManager.getConfig("saves.yml").contains("streamers")) {
            List<String> streamerModeString = configManager.getConfig("saves.yml").getStringList("streamers");
            for (String uuid : streamerModeString) {
                streamerMode.add(UUID.fromString(uuid));
            }
        }
    }

    public void saveStreamersToConfig() {
        List<String> uuids = new ArrayList<>();
        for (UUID playerUUID : streamerMode) {
            uuids.add(playerUUID.toString());
        }
        configManager.getConfig("saves.yml").set("streamers", uuids);
        configManager.saveConfig("saves.yml");
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + ";" +
                location.getX() + ";" +
                location.getY() + ";" +
                location.getZ() + ";" +
                location.getYaw() + ";" +
                location.getPitch();
    }

    private Location locationFromString(String locationString) {
        try {
            String[] parts = locationString.split(";");
            if (parts.length == 6) {
                return new Location(
                        Bukkit.getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]),
                        Float.parseFloat(parts[4]),
                        Float.parseFloat(parts[5])
                );
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load location: " + locationString);
        }
        return null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material material = block.getType();
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (player.getPose().equals(Pose.SNEAKING)) return;
        if (!material.name().endsWith("_BED")) return;
        if (!player.getWorld().getName().equals("world")) return;

        event.setCancelled(true);

        UUID playerUUID = player.getUniqueId();
        Location bedLocation = getMainBedLocation(block);

        List<Location> playerHomes = homesMap.getOrDefault(playerUUID, new ArrayList<>());

        int maxHomes = getMaxHomes(player);
        int currentHomes = playerHomes.size();

        if (currentHomes >= maxHomes) {
            player.sendMessage(configManager.getConfig("lang.yml").getString("max-homes-reached", "§cYou have reached the maximum number of homes (%max%)!").replace("%max%", String.valueOf(maxHomes)));
            return;
        }

        if (isBedAlreadyAdded(playerHomes, bedLocation)) {
            player.sendMessage(configManager.getConfig("lang.yml").getString("bed-already-added", "§cThis bed is already added as a home!"));
            return;
        }

        playerHomes.add(bedLocation);
        homesMap.put(playerUUID, playerHomes);
        saveHomesToConfig();

        String homeSetMessage = configManager.getConfig("lang.yml").getString("home-set-success", "§aHome #%index% set! Total homes: %current%/%max%");
        homeSetMessage = homeSetMessage.replace("%index%", String.valueOf(currentHomes + 1))
                .replace("%current%", String.valueOf(currentHomes + 1))
                .replace("%max%", String.valueOf(maxHomes));
        player.sendMessage(homeSetMessage);
    }

    private Location getMainBedLocation(Block clickedBlock) {
        Location location = clickedBlock.getLocation();
        Material bedMaterial = clickedBlock.getType();

        if (bedMaterial.toString().contains("_BED")) {
            Block relative = clickedBlock.getRelative(getBedDirection(clickedBlock));
            if (relative.getType() == bedMaterial) {
                Location loc1 = clickedBlock.getLocation();
                Location loc2 = relative.getLocation();

                if (loc1.getX() < loc2.getX() || loc1.getZ() < loc2.getZ() ||
                        (loc1.getX() == loc2.getX() && loc1.getZ() == loc2.getZ() && loc1.getY() <= loc2.getY())) {
                    return loc1;
                } else {
                    return loc2;
                }
            }
        }
        return location;
    }

    private org.bukkit.block.BlockFace getBedDirection(Block bedBlock) {
        org.bukkit.block.data.type.Bed bedData = (org.bukkit.block.data.type.Bed) bedBlock.getBlockData();
        return bedData.getFacing();
    }

    private int getMaxHomes(Player player) {
        if (player.hasPermission("simplehomes.quadruple")) {
            return 20;
        } else if (player.hasPermission("simplehomes.triple")) {
            return 15;
        } else if (player.hasPermission("simplehomes.double")) {
            return 10;
        } else {
            return 5;
        }
    }

    private boolean isBedAlreadyAdded(List<Location> playerHomes, Location newLocation) {
        for (Location existingLocation : playerHomes) {
            if (locationsEqual(existingLocation, newLocation)) {
                return true;
            }
        }
        return false;
    }

    private boolean locationsEqual(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
                loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String inventoryTitle = event.getView().getTitle();

        String expectedTitle = configManager.getConfig("lang.yml").getString("menu-title", "§fHomes");
        if (!inventoryTitle.equals(expectedTitle)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (delayMap.containsKey(player.getUniqueId())) {
            player.sendMessage(configManager.getConfig("lang.yml").getString("delay", "§cYou will be able to teleport in %secs% seconds")
                                .replace("%secs%", String.valueOf(delayMap.get(player.getUniqueId()))));
            return;
        }

        if (clickedItem.getType() == Material.BLUE_BED) {
            teleportToHome(player, event.getSlot(), inventory);
            delayMap.put(player.getUniqueId(), configManager.getConfig("config.yml").getInt("delay"));
            player.closeInventory();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        Location mainBedLocation = getMainBedLocation(block);

        if (!material.name().endsWith("_BED")) return;

        for (Map.Entry<UUID, List<Location>> entry : homesMap.entrySet()) {
            List<Location> playerHomes = entry.getValue();
            Iterator<Location> iterator = playerHomes.iterator();
            boolean removed = false;

            while (iterator.hasNext()) {
                Location homeLocation = iterator.next();
                if (locationsEqual(homeLocation, mainBedLocation)) {
                    iterator.remove();
                    removed = true;
                }
            }

            if (removed) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage(configManager.getConfig("lang.yml").getString("bed-destroyed", "§cOne of your homes has been destroyed!"));
                }
            }
        }
        saveHomesToConfig();
    }

    private void teleportToHome(Player player, int slot, Inventory inventory) {
        UUID playerUUID = player.getUniqueId();
        List<Location> playerHomes = homesMap.getOrDefault(playerUUID, new ArrayList<>());

        int inventorySize = inventory.getSize();
        int[] homeSlots = getHomeSlots(inventorySize);

        int homeIndex = -1;
        for (int i = 0; i < homeSlots.length; i++) {
            if (homeSlots[i] == slot) {
                homeIndex = i;
                break;
            }
        }

        if (homeIndex != -1 && homeIndex < playerHomes.size()) {
            Location homeLocation = playerHomes.get(homeIndex);

            if (homeLocation.getWorld() == null) {
                player.sendMessage(configManager.getConfig("lang.yml").getString("world-error", "§cHome world not found!"));
                return;
            }

            Block bedBlock = homeLocation.getBlock();
            if (!bedBlock.getType().name().endsWith("_BED")) {
                playerHomes.remove(homeIndex);
                homesMap.put(playerUUID, playerHomes);
                saveHomesToConfig();

                String bedDestroyedMessage = configManager.getConfig("lang.yml").getString("break-bed", "§cThe bed in home #%index% was destroyed! The home has been removed from the list.");
                player.sendMessage(bedDestroyedMessage.replace("%index%", String.valueOf(homeIndex + 1)));
                player.closeInventory();
                return;
            }

            final int[] tpDelay = {configManager.getConfig("config.yml").getInt("tp-delay")};
            if (tpDelay[0] < 0) {
                player.teleport(homeLocation.clone().add(0.5, 0, 0.5));

                player.sendMessage(configManager.getConfig("lang.yml").getString("teleport-success", "§aTeleport to home #%index%!").replace("%index%", String.valueOf(homeIndex + 1)));
                tpDelay[0] = tpDelay[0] - 1;
            } else {
                int finalHomeIndex = homeIndex;
                BukkitRunnable tpDelayRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (tpDelay[0] == 0) {
                            player.teleport(homeLocation.clone().add(0.5, 0, 0.5));

                            player.sendMessage(configManager.getConfig("lang.yml").getString("teleport-success", "§aTeleport to home #%index%!").replace("%index%", String.valueOf(finalHomeIndex + 1)));
                            tpDelay[0] = tpDelay[0] - 1;
                        } else if (tpDelay[0] > 0) {
                            String actionbarMsg = configManager.getConfig("lang.yml").getString("tp-delay", "§fYou will be teleported in §3%secs% §fseconds to home #§3%index%");
                            actionbarMsg = actionbarMsg.replace("%secs%", String.valueOf(tpDelay[0]))
                                    .replace("%index%", String.valueOf(finalHomeIndex + 1));
                            player.sendActionBar(actionbarMsg);
                            tpDelay[0] = tpDelay[0] - 1;
                        }
                    }
                };
                tpDelayRunnable.runTaskTimer(this, 0L, 20L);
            }
        }
    }

    public void createHomeMenu(Player player) {
        int size = 27;

        if (player.hasPermission("simplehomes.quadruple")) {
            size = 54;
        } else if (player.hasPermission("simplehomes.triple")) {
            size = 45;
        } else if (player.hasPermission("simplehomes.double")) {
            size = 36;
        }

        Inventory inventory = Bukkit.createInventory(null, size, configManager.getConfig("lang.yml").getString("menu-title", "§fHomes"));

        ItemStack sign = new ItemStack(Material.OAK_HANGING_SIGN);
        ItemMeta signMeta = sign.getItemMeta();
        signMeta.setDisplayName(configManager.getConfig("lang.yml").getString("menu-sign-text", "§funtitled.server.net"));
        sign.setItemMeta(signMeta);
        inventory.setItem(10, sign);

        UUID playerUUID = player.getUniqueId();
        List<Location> playerHomes = homesMap.getOrDefault(playerUUID, new ArrayList<>());

        Iterator<Location> iterator = playerHomes.iterator();
        while (iterator.hasNext()) {
            Location homeLocation = iterator.next();
            if (homeLocation.getWorld() == null || !homeLocation.getBlock().getType().name().endsWith("_BED")) {
                iterator.remove();
            }
        }
        homesMap.put(playerUUID, playerHomes);
        saveHomesToConfig();

        int[] homeSlots = getHomeSlots(size);

        for (int i = 0; i < homeSlots.length; i++) {
            if (i < playerHomes.size()) {
                Location homeLocation = playerHomes.get(i);

                ItemStack homeItem = new ItemStack(Material.BLUE_BED);
                ItemMeta homeMeta = homeItem.getItemMeta();

                List<String> lore = new ArrayList<>();
                if (!(streamerMode.contains(playerUUID))) {
                    lore.add("");
                    lore.add("§7World: " + homeLocation.getWorld().getName());
                    lore.add("§7X: " + (int) homeLocation.getX());
                    lore.add("§7Y: " + (int) homeLocation.getY());
                    lore.add("§7Z: " + (int) homeLocation.getZ());
                }
                lore.add("");
                lore.add("§eClick to teleport!");


                homeMeta.setDisplayName(configManager.getConfig("lang.yml").getString("home-item-name", "§fʜᴏᴍᴇ #§3%index%").replace("%index%", String.valueOf(i + 1)));
                homeMeta.setLore(lore);
                homeItem.setItemMeta(homeMeta);

                inventory.setItem(homeSlots[i], homeItem);
            } else {
                ItemStack emptyHome = new ItemStack(Material.WHITE_BED);
                ItemMeta emptyMeta = emptyHome.getItemMeta();
                emptyMeta.setDisplayName(configManager.getConfig("lang.yml").getString("empty-slot-name", "§cᴇᴍᴘᴛʏ"));
                List<String> emptyLore = new ArrayList<>();
                emptyLore.add("");
                emptyLore.add("§7Empty slot");
                emptyMeta.setLore(emptyLore);
                emptyHome.setItemMeta(emptyMeta);

                inventory.setItem(homeSlots[i], emptyHome);
            }
        }

        player.openInventory(inventory);
    }

    private int[] getHomeSlots(int inventorySize) {
        switch (inventorySize) {
            case 27:
                return new int[]{12, 13, 14, 15, 16};
            case 36:
                return new int[]{12, 13, 14, 15, 16, 21, 22, 23, 24, 25};
            case 45:
                return new int[]{12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34};
            case 54:
                return new int[]{12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 39, 40, 41, 42, 43};
            default:
                return new int[]{12, 13, 14, 15, 16};
        }
    }
}