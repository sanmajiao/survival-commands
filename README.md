# 生存指令权限

Minecraft Java 版模组。按游戏版本和加载器选择对应的 JAR，复制到该实例的 `mods` 目录。

| 游戏版本 | 构建 | Java |
| --- | --- | --- |
| 1.20.1 | Forge、Fabric | 17 |
| 1.21.1 | Forge、Fabric | 21 |
| 26.3 | Forge、Fabric | 25 |

## 单人和局域网

菜单路径为 **ESC → 不开作弊允许指令**。局域网开房只需房主装本模组。房主可以编辑开关，服务端对成员执行相同的指令授权和创造锁。成员装本模组后还可以在自己的 ESC 菜单查看只读权限。整合包中其他模组各自的联机依赖照常适用。权限跟随存档，所有新增授权默认关闭。

- 搜索支持指令、别名、中文、拼音全拼和首字母。比如用 `chuansong`、`cs` 或 `tp` 查找传送。
- 开启父项会放行其全部参数和子指令。关闭父项后可以单独启用子项。例如关闭 `/time` 并启用 `/time set`，即可使用 `/time set day`，`/time add` 仍受原来的权限检查。
- `/tp` 开启后支持完整的传送语法。包括 `/tp 玩家名`、`/tp @s 玩家名`、坐标及原版支持的其他目标与参数。
- 列表会读取游戏已注册的字面量子指令。坐标、实体、物品等自由输入保留在所属子项的参数中。`/tick` 在有实际指令来源的版本或整合包中才可开启。
- 模组在支持的局域网开房界面将「允许作弊」置灰。对 mcwifipnp 的适配也处理「所有玩家作弊」。存档原来就有的作弊权限及 OP 名单仍由原来的配置管理。

## 创造模式锁

模组在世界第一次加载时记录默认游戏模式。由非创造模式创建并从创建时安装本模组的世界，会持续限制创造模式。修改原版存档默认模式或开房默认模式无法改变模组的记录。已经存在的存档以首次安装后观察到的模式为准。

要主动解除锁，退出世界，在对应存档的 `serverconfig/survivalcommands-guard.properties` 里把 `allowCreative=false` 改成 `allowCreative=true`，重新打开世界。回到关闭作弊的世界后，可以在指令页单独授予 `/gamemode creative`。开启整组 `/gamemode` 时，创造锁仍优先生效。

## 构建与扩展

```powershell
# 使用 JDK 17
.\gradlew.bat :fabric:build :forge:build

# 改用 JDK 21
.\gradlew.bat '-PminecraftVersion=1.21.1' :fabric:build :forge:build

# 改用 JDK 25
.\modern\gradlew.bat -p modern :fabric:build :forge:build
```

- `shared` 提供共用逻辑。`versions/<游戏版本>/shared` 提供游戏接口发生变化的源码覆盖。
- 26.3 首次打开升级的存档时会导入旧版模组的授权配置。同名指令和子指令继续按保存的授权生效。
- 创造锁、局域网开房、指令权限、ESC 设置页各有独立的 Mixin 配置。构建会读取仍存在的配置文件。移除局域网或创造锁配置后可以单独保留其他功能。移除指令权限后，设置页也可一并移除。
- 扩展模组可以在初始化时调用 `CommandCatalog.register("example.fly", "example:fly")` 增加指令项。提供 `survivalcommands.command.example.fly` 的语言翻译可增加本地化搜索。注册指令应适合授权使用，其字面量子指令会由菜单读取。
- 拼音字典采用 MIT 许可，许可证随源码和 JAR 一起分发。
