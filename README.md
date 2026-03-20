# Command Alias (Fabric 1.21.11)

为 Minecraft Java 版 **1.21.11**（Fabric）提供 **自定义命令别名** 功能，支持：
- 命令别名（类似快捷指令）
- 多命令顺序执行（`;` 分隔）
- 权限等级控制
- 玩家名 / UUID 白名单与黑名单
- 热重载配置

---

## 环境要求
- Minecraft Java 1.21.11
- Fabric Loader
- Fabric API
- Java 21（本项目已配置 `org.gradle.java.home`）

---

## 构建
```bash
./gradlew build
```
构建产物在 `build/libs/`。

---

## 资源包（i18n）
如果只在服务端安装本模组，客户端不会加载语言文件。  
你可以使用项目生成的资源包让客户端自动下载语言文本：

- 资源包文件：`command_alias_lang_pack.zip`
- SHA1：`见发行版SHA1`

在 `server.properties` 中配置：
```
resource-pack=<你的资源包URL>
resource-pack-sha1=见发行版SHA1
```

---

## 默认权限
`/cmd add` 会使用配置里的默认权限等级（默认 `0`）。  
可在配置文件中修改`defaultPermission`：
```json
{
  "defaultPermission": 0,
  "commands": {
    "hub": {
      "permissionLevel": 2,
      "actions": [
        "tp @s 0 100 0",
        "kill @s"
      ]
    }
  }
}
```

---

## 基础命令
> 管理命令需要 OP 权限（默认权限等级 2）。

1. 添加别名（默认权限 0）
```
/cmd add <alias> <command...>
```
示例：
```
/cmd add hub kill @s
```

2. 添加别名（自定义权限等级 0-4）
```
/cmd addop <alias> <permission> <command...>
```
示例：
```
/cmd addop hub 0 tp @s 0 100 0; kill @s
```

> `add` / `addop` 的 `<command...>` 参数支持 **完整命令补全**（含子参数）。

3. 编辑别名
```
/cmd edit <alias> <command...>
```

4. 删除别名
```
/cmd remove <alias>
```

5. 列出所有别名
```
/cmd list
```

6. 重载配置（无需重启）
```
/cmd reload
```

---

## 白名单 / 黑名单
> 黑名单优先；若设置了白名单（名字或 UUID），必须命中白名单才允许执行。

### 玩家名
```
/cmd allowname <alias> <playerName>
/cmd denyname <alias> <playerName>
/cmd unallowname <alias> <playerName>
/cmd undenyname <alias> <playerName>
```

### UUID
```
/cmd allowuuid <alias> <uuid>
/cmd denyuuid <alias> <uuid>
/cmd unallowuuid <alias> <uuid>
/cmd undenyuuid <alias> <uuid>
```

### 清空列表
```
/cmd clearlists <alias>
```

---

## 配置文件
生成路径：`config/command-mapper.json`

示例：
```json
{
  "commands": {
    "hub": {
      "permissionLevel": 2,
      "actions": [
        "tp @s 0 100 0",
        "kill @s"
      ],
      "allowNames": ["meowhuan"],
      "denyNames": [],
      "allowUuids": ["01234567-89ab-cdef-0123-456789abcdef"],
      "denyUuids": []
    }
  }
}
```

---

## 行为说明
- 别名执行时会 **以管理员权限执行**（越权执行），但是否允许触发仍受 `permissionLevel` 与白/黑名单控制。
- 多命令使用 `;` 分隔，按顺序执行。

---

## 许可证
本项目使用 **AGPL-3.0** 许可。
