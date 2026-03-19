package com.meowhuan.commandalias;

import com.meowhuan.commandalias.ConfigStore.CommandConfig;
import com.meowhuan.commandalias.ConfigStore.CommandEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import java.util.Map;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class ManagementCommands {
    private static final int ADMIN_PERMISSION = 2;

    private ManagementCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("cmd")
            .requires(source -> hasPermission(source, ADMIN_PERMISSION));

        root.then(buildAdd());
        root.then(buildAddOp());
        root.then(buildEdit());
        root.then(buildRemove());
        root.then(buildList());
        root.then(buildHelp());
        root.then(buildAllowName());
        root.then(buildDenyName());
        root.then(buildUnallowName());
        root.then(buildUndenyName());
        root.then(buildAllowUuid());
        root.then(buildDenyUuid());
        root.then(buildUnallowUuid());
        root.then(buildUndenyUuid());
        root.then(buildClearLists());
        root.then(buildReload());

        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAdd() {
        return CommandManager.literal("add")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String command = StringArgumentType.getString(ctx, "command");
                        boolean ok = CustomCommandManager.addOrUpdate(alias, command, ADMIN_PERMISSION);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.added", "已添加别名 %s", "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.add_failed", "添加别名失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAddOp() {
        return CommandManager.literal("addop")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("permission", IntegerArgumentType.integer(0, 4))
                    .then(CommandManager.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String alias = StringArgumentType.getString(ctx, "alias");
                            int permission = IntegerArgumentType.getInteger(ctx, "permission");
                            String command = StringArgumentType.getString(ctx, "command");
                            boolean ok = CustomCommandManager.addOrUpdate(alias, command, permission);
                            if (ok) {
                                ctx.getSource().sendFeedback(
                                    () -> tr("command_alias.msg.added_with_perm", "已添加别名 %s（权限 %s）", "/" + alias, permission),
                                    false
                                );
                                return 1;
                            }
                            ctx.getSource().sendError(tr("command_alias.msg.add_failed", "添加别名失败。"));
                            return 0;
                        }))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildEdit() {
        return CommandManager.literal("edit")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String command = StringArgumentType.getString(ctx, "command");
                        CommandEntry existing = CustomCommandManager.getConfig().commands.get(alias.toLowerCase(Locale.ROOT));
                        int permission = existing == null ? ADMIN_PERMISSION : existing.permissionLevel;
                        boolean ok = CustomCommandManager.addOrUpdate(alias, command, permission);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.updated", "已更新别名 %s", "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.update_failed", "更新别名失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRemove() {
        return CommandManager.literal("remove")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .executes(ctx -> {
                    String alias = StringArgumentType.getString(ctx, "alias");
                    boolean ok = CustomCommandManager.remove(alias);
                    if (ok) {
                        ctx.getSource().sendFeedback(
                            () -> tr("command_alias.msg.removed", "已删除别名 %s", "/" + alias),
                            false
                        );
                        return 1;
                    }
                    ctx.getSource().sendError(tr("command_alias.msg.alias_not_found", "未找到该别名。"));
                    return 0;
                }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildList() {
        return CommandManager.literal("list")
            .executes(ctx -> {
                CommandConfig config = CustomCommandManager.getConfig();
                if (config.commands.isEmpty()) {
                    ctx.getSource().sendFeedback(() -> tr("command_alias.msg.list_empty", "暂无别名配置。"), false);
                    return 1;
                }
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.list_header", "别名列表："), false);
                for (Map.Entry<String, CommandEntry> entry : config.commands.entrySet()) {
                    String alias = entry.getKey();
                    CommandEntry value = entry.getValue();
                    String actions = String.join("; ", value.actions);
                    String allowNames = String.join(", ", value.allowNames);
                    String denyNames = String.join(", ", value.denyNames);
                    String allowUuids = String.join(", ", value.allowUuids);
                    String denyUuids = String.join(", ", value.denyUuids);
                    ctx.getSource().sendFeedback(
                        () -> tr(
                            "command_alias.msg.list_item",
                            "%s -> [%s]（权限 %s）",
                            "/" + alias,
                            actions,
                            value.permissionLevel
                        ),
                        false
                    );
                    if (!allowNames.isEmpty() || !denyNames.isEmpty() || !allowUuids.isEmpty() || !denyUuids.isEmpty()) {
                        ctx.getSource().sendFeedback(
                            () -> tr(
                                "command_alias.msg.list_acl",
                                "  allowNames=[%s] denyNames=[%s] allowUuids=[%s] denyUuids=[%s]",
                                allowNames,
                                denyNames,
                                allowUuids,
                                denyUuids
                            ),
                            false
                        );
                    }
                }
                return 1;
            });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildHelp() {
        return CommandManager.literal("help")
            .executes(ctx -> {
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.header", "Command Alias 帮助："), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.add", "/cmd add <alias> <command...> - 添加别名（默认权限2）"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.addop", "/cmd addop <alias> <permission> <command...> - 添加别名（自定义权限0-4）"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.edit", "/cmd edit <alias> <command...> - 编辑别名"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.remove", "/cmd remove <alias> - 删除别名"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.list", "/cmd list - 列出所有别名"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.reload", "/cmd reload - 重载配置"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.allowname", "/cmd allowname <alias> <playerName> - 名字白名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.denyname", "/cmd denyname <alias> <playerName> - 名字黑名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.unallowname", "/cmd unallowname <alias> <playerName> - 移除名字白名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.undenyname", "/cmd undenyname <alias> <playerName> - 移除名字黑名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.allowuuid", "/cmd allowuuid <alias> <uuid> - UUID 白名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.denyuuid", "/cmd denyuuid <alias> <uuid> - UUID 黑名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.unallowuuid", "/cmd unallowuuid <alias> <uuid> - 移除 UUID 白名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.undenyuuid", "/cmd undenyuuid <alias> <uuid> - 移除 UUID 黑名单"), false);
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.help.clearlists", "/cmd clearlists <alias> - 清空白/黑名单"), false);
                return 1;
            });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAllowName() {
        return CommandManager.literal("allowname")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String name = StringArgumentType.getString(ctx, "name");
                        boolean ok = CustomCommandManager.updateNameList(alias, name, true, true);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.allow_name_added", "已允许玩家名 %s 执行 %s", name, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.allow_name_failed", "更新玩家名白名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildDenyName() {
        return CommandManager.literal("denyname")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String name = StringArgumentType.getString(ctx, "name");
                        boolean ok = CustomCommandManager.updateNameList(alias, name, false, true);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.deny_name_added", "已禁止玩家名 %s 执行 %s", name, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.deny_name_failed", "更新玩家名黑名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildUnallowName() {
        return CommandManager.literal("unallowname")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String name = StringArgumentType.getString(ctx, "name");
                        boolean ok = CustomCommandManager.updateNameList(alias, name, true, false);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.allow_name_removed", "已移除允许玩家名 %s 对 %s 的权限", name, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.allow_name_failed", "更新玩家名白名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildUndenyName() {
        return CommandManager.literal("undenyname")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String name = StringArgumentType.getString(ctx, "name");
                        boolean ok = CustomCommandManager.updateNameList(alias, name, false, false);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.deny_name_removed", "已移除玩家名 %s 对 %s 的禁用", name, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.deny_name_failed", "更新玩家名黑名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAllowUuid() {
        return CommandManager.literal("allowuuid")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("uuid", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String uuid = StringArgumentType.getString(ctx, "uuid");
                        boolean ok = CustomCommandManager.updateUuidList(alias, uuid, true, true);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.allow_uuid_added", "已允许 UUID %s 执行 %s", uuid, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.allow_uuid_failed", "更新 UUID 白名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildDenyUuid() {
        return CommandManager.literal("denyuuid")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("uuid", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String uuid = StringArgumentType.getString(ctx, "uuid");
                        boolean ok = CustomCommandManager.updateUuidList(alias, uuid, false, true);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.deny_uuid_added", "已禁止 UUID %s 执行 %s", uuid, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.deny_uuid_failed", "更新 UUID 黑名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildUnallowUuid() {
        return CommandManager.literal("unallowuuid")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("uuid", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String uuid = StringArgumentType.getString(ctx, "uuid");
                        boolean ok = CustomCommandManager.updateUuidList(alias, uuid, true, false);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.allow_uuid_removed", "已移除允许 UUID %s 对 %s 的权限", uuid, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.allow_uuid_failed", "更新 UUID 白名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildUndenyUuid() {
        return CommandManager.literal("undenyuuid")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .then(CommandManager.argument("uuid", StringArgumentType.word())
                    .executes(ctx -> {
                        String alias = StringArgumentType.getString(ctx, "alias");
                        String uuid = StringArgumentType.getString(ctx, "uuid");
                        boolean ok = CustomCommandManager.updateUuidList(alias, uuid, false, false);
                        if (ok) {
                            ctx.getSource().sendFeedback(
                                () -> tr("command_alias.msg.deny_uuid_removed", "已移除 UUID %s 对 %s 的禁用", uuid, "/" + alias),
                                false
                            );
                            return 1;
                        }
                        ctx.getSource().sendError(tr("command_alias.msg.deny_uuid_failed", "更新 UUID 黑名单失败。"));
                        return 0;
                    })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildClearLists() {
        return CommandManager.literal("clearlists")
            .then(CommandManager.argument("alias", StringArgumentType.word())
                .executes(ctx -> {
                    String alias = StringArgumentType.getString(ctx, "alias");
                    boolean ok = CustomCommandManager.clearLists(alias);
                    if (ok) {
                        ctx.getSource().sendFeedback(
                            () -> tr("command_alias.msg.cleared_lists", "已清空 %s 的白/黑名单", "/" + alias),
                            false
                        );
                        return 1;
                    }
                    ctx.getSource().sendError(tr("command_alias.msg.cleared_lists_failed", "清空白/黑名单失败。"));
                    return 0;
                }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildReload() {
        return CommandManager.literal("reload")
            .executes(ctx -> {
                CustomCommandManager.reloadFromDisk();
                CustomCommandManager.registerAll();
                ctx.getSource().sendFeedback(() -> tr("command_alias.msg.reloaded", "命令别名已重载。"), false);
                return 1;
            });
    }

    private static Text tr(String key, String fallback, Object... args) {
        return Text.translatableWithFallback(key, fallback, args);
    }

    private static boolean hasPermission(ServerCommandSource source, int level) {
        PermissionLevel permissionLevel = PermissionLevel.fromLevel(level);
        Permission permission = new Permission.Level(permissionLevel);
        return source.getPermissions().hasPermission(permission);
    }
}
