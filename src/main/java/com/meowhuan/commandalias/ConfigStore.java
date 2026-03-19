package com.meowhuan.commandalias;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "command-mapper.json";

    private ConfigStore() {}

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static CommandConfig loadOrCreate() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            CommandConfig created = CommandConfig.createDefault();
            save(created);
            return created;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CommandConfig config = GSON.fromJson(reader, CommandConfig.class);
            if (config == null || config.commands == null) {
                return CommandConfig.createDefault();
            }
            return config;
        } catch (IOException | JsonSyntaxException ex) {
            return CommandConfig.createDefault();
        }
    }

    public static void save(CommandConfig config) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ignored) {
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (IOException ignored) {
        }
    }

    public static final class CommandConfig {
        public Map<String, CommandEntry> commands = new LinkedHashMap<>();

        public static CommandConfig createDefault() {
            CommandConfig config = new CommandConfig();
            CommandEntry sample = new CommandEntry();
            sample.permissionLevel = 2;
            sample.actions = new String[] { "kill @s" };
            config.commands.put("hub", sample);
            return config;
        }
    }

    public static final class CommandEntry {
        public int permissionLevel = 2;
        public String[] actions = new String[0];
        public String[] allowNames = new String[0];
        public String[] denyNames = new String[0];
        public String[] allowUuids = new String[0];
        public String[] denyUuids = new String[0];
    }
}
