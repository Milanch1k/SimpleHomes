package net.milanchik.simpleHomes.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Создаёт и/или загружает YAML файл с указанным именем (например "config.yml" или "saves.yml").
     */
    public void setup(String fileName) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            try {
                // Если файл есть в ресурсах плагина, копируем его, иначе создаём пустой
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                    plugin.getLogger().info("Copied default " + fileName);
                } else {
                    file.createNewFile();
                    plugin.getLogger().info("Created new " + fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + fileName + "!");
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);
        files.put(fileName, file);
    }

    /**
     * Получить конфигурацию по имени файла.
     */
    public FileConfiguration getConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        if (config == null) {
            setup(fileName);
            config = configs.get(fileName);
        }
        return config;
    }

    /**
     * Сохранить файл по имени.
     */
    public void saveConfig(String fileName) {
        File file = files.get(fileName);
        FileConfiguration config = configs.get(fileName);

        if (file == null || config == null) {
            plugin.getLogger().severe("Cannot save " + fileName + " because it was not loaded!");
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + fileName + "!");
            e.printStackTrace();
        }
    }

    /**
     * Перезагрузить файл по имени.
     */
    public void reloadConfig(String fileName) {
        File file = files.get(fileName);
        if (file == null) {
            plugin.getLogger().warning(fileName + " is not loaded yet, loading it now...");
            setup(fileName);
            return;
        }
        configs.put(fileName, YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Сохранить все конфигурации.
     */
    public void saveAll() {
        for (String fileName : configs.keySet()) {
            saveConfig(fileName);
        }
    }

    /**
     * Перезагрузить все конфигурации.
     */
    public void reloadAll() {
        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }
    }
}
