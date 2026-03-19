package com.meowhuan.commandalias;

import com.meowhuan.commandalias.ConfigStore.CommandConfig;
import com.meowhuan.commandalias.ConfigStore.CommandEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CustomCommandManager {
    private static CommandDispatcher<ServerCommandSource> dispatcher;
    private static final Set<String> registeredAliases = new HashSet<>();
    private static CommandConfig config = new CommandConfig();

    private CustomCommandManager() {}

    public static void initialize(CommandDispatcher<ServerCommandSource> dispatcher) {
        CustomCommandManager.dispatcher = dispatcher;
        reloadFromDisk();
        registerAll();
    }

    public static void reloadFromDisk() {
        config = ConfigStore.loadOrCreate();
    }

    public static void registerAll() {
        if (dispatcher == null) {
            return;
        }
        List<String> toRemove = new ArrayList<>();
        for (String alias : registeredAliases) {
            if (!config.commands.containsKey(alias)) {
                toRemove.add(alias);
            }
        }
        for (String alias : toRemove) {
            unregisterAlias(alias);
        }
        for (Map.Entry<String, CommandEntry> entry : config.commands.entrySet()) {
            String alias = entry.getKey().toLowerCase(Locale.ROOT);
            if (alias.isBlank()) {
                continue;
            }
            if (!registeredAliases.contains(alias)) {
                registerAlias(alias);
            }
        }
        refreshCommandTree();
    }

    private static void registerAlias(String alias) {
        LiteralArgumentBuilder<ServerCommandSource> node = CommandManager.literal(alias)
            .requires(source -> isAllowedForAlias(source, alias))
            .executes(ctx -> executeMapped(ctx.getSource(), alias));

        dispatcher.register(node);
        registeredAliases.add(alias);
    }

    private static void unregisterAlias(String alias) {
        if (dispatcher == null) {
            return;
        }
        try {
            Object root = dispatcher.getRoot();
            removeChild(root, alias);
            registeredAliases.remove(alias);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeChild(Object node, String alias) {
        for (String fieldName : new String[] { "children", "literals", "arguments" }) {
            try {
                var field = node.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(node);
                if (value instanceof Map<?, ?> map) {
                    ((Map<String, ?>) map).remove(alias);
                }
            } catch (NoSuchFieldException ex) {
                try {
                    var field = node.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(node);
                    if (value instanceof Map<?, ?> map) {
                        ((Map<String, ?>) map).remove(alias);
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static int executeMapped(ServerCommandSource source, String alias) {
        CommandEntry entry = config.commands.get(alias);
        if (entry == null) {
            source.sendError(Text.translatableWithFallback(
                "command_alias.msg.alias_not_found",
                "未找到该别名。"
            ));
            return 0;
        }
        MinecraftServer server = source.getServer();
        ServerCommandSource elevated = source.withPermissions(PermissionPredicate.ALL);
        int executed = 0;
        for (String raw : entry.actions) {
            String trimmed = raw == null ? "" : raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            var parse = server.getCommandManager().getDispatcher().parse(trimmed, elevated);
            server.getCommandManager().execute(parse, trimmed);
            executed++;
        }
        if (executed == 0) {
            source.sendError(Text.translatableWithFallback(
                "command_alias.msg.alias_no_commands",
                "该别名没有可执行的命令。"
            ));
        }
        return executed;
    }

    private static boolean isAllowedForAlias(ServerCommandSource source, String alias) {
        CommandEntry entry = config.commands.get(alias);
        if (entry == null) {
            return false;
        }
        return hasPermission(source, entry.permissionLevel) && isAllowed(source, entry);
    }

    public static CommandConfig getConfig() {
        return config;
    }

    public static boolean addOrUpdate(String alias, String commandText, int permissionLevel) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        List<String> actions = splitActions(commandText);
        String key = alias.toLowerCase(Locale.ROOT);
        CommandEntry entry = config.commands.get(key);
        if (entry == null) {
            entry = new CommandEntry();
        }
        entry.permissionLevel = permissionLevel;
        entry.actions = actions.toArray(new String[0]);
        config.commands.put(key, entry);
        ConfigStore.save(config);
        registerAll();
        return true;
    }

    public static boolean remove(String alias) {
        if (alias == null) {
            return false;
        }
        String key = alias.toLowerCase(Locale.ROOT);
        CommandEntry removed = config.commands.remove(key);
        if (removed == null) {
            return false;
        }
        ConfigStore.save(config);
        unregisterAlias(key);
        registerAll();
        return true;
    }

    public static List<String> splitActions(String commandText) {
        List<String> actions = new ArrayList<>();
        if (commandText == null) {
            return actions;
        }
        for (String part : commandText.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                actions.add(trimmed);
            }
        }
        return actions;
    }

    public static boolean updateNameList(String alias, String name, boolean allow, boolean add) {
        if (alias == null || alias.isBlank() || name == null || name.isBlank()) {
            return false;
        }
        CommandEntry entry = config.commands.get(alias.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return false;
        }
        if (allow) {
            entry.allowNames = updateStringArray(entry.allowNames, name, add);
        } else {
            entry.denyNames = updateStringArray(entry.denyNames, name, add);
        }
        ConfigStore.save(config);
        registerAll();
        return true;
    }

    public static boolean updateUuidList(String alias, String uuidText, boolean allow, boolean add) {
        if (alias == null || alias.isBlank() || uuidText == null || uuidText.isBlank()) {
            return false;
        }
        CommandEntry entry = config.commands.get(alias.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return false;
        }
        try {
            UUID.fromString(uuidText);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (allow) {
            entry.allowUuids = updateStringArray(entry.allowUuids, uuidText, add);
        } else {
            entry.denyUuids = updateStringArray(entry.denyUuids, uuidText, add);
        }
        ConfigStore.save(config);
        registerAll();
        return true;
    }

    private static String[] updateStringArray(String[] values, String value, boolean add) {
        Set<String> set = new HashSet<>();
        if (values != null) {
            for (String item : values) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                set.add(item.toLowerCase(Locale.ROOT));
            }
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (add) {
            set.add(normalized);
        } else {
            set.remove(normalized);
        }
        return set.toArray(new String[0]);
    }

    public static boolean clearLists(String alias) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        CommandEntry entry = config.commands.get(alias.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return false;
        }
        entry.allowNames = new String[0];
        entry.denyNames = new String[0];
        entry.allowUuids = new String[0];
        entry.denyUuids = new String[0];
        ConfigStore.save(config);
        registerAll();
        return true;
    }

    private static void refreshCommandTree() {
        MinecraftServer server = CommandAliasMod.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            server.getCommandManager().sendCommandTree(player);
        }
    }

    private static boolean isAllowed(ServerCommandSource source, CommandEntry entry) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return true;
        }
        String name = player.getName().getString();
        String uuid = player.getUuidAsString();

        boolean denied = matches(entry.denyNames, name) || matches(entry.denyUuids, uuid);
        if (denied) {
            return false;
        }

        boolean hasAllowList = hasEntries(entry.allowNames) || hasEntries(entry.allowUuids);
        if (!hasAllowList) {
            return true;
        }
        return matches(entry.allowNames, name) || matches(entry.allowUuids, uuid);
    }

    private static boolean matches(String[] values, String target) {
        if (values == null || target == null) {
            return false;
        }
        String normalized = target.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (value.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEntries(String[] values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPermission(ServerCommandSource source, int level) {
        PermissionLevel permissionLevel = PermissionLevel.fromLevel(level);
        Permission permission = new Permission.Level(permissionLevel);
        return source.getPermissions().hasPermission(permission);
    }
}
