# Villager Pickup

[![build](https://github.com/AaLonG-2012/villager-pickup/actions/workflows/build.yml/badge.svg)](https://github.com/AaLonG-2012/villager-pickup/actions/workflows/build.yml)

把村民收进物品，再原样放回。 / Pick up villagers into an item, put them back exactly as they were.

![icon](src/main/resources/assets/villager-pickup/icon.png)

## 玩法 / Usage

| 操作 | 效果 |
|---|---|
| **潜行 + 右键村民**（主手空着） | 村民被封进「手持村民」物品，进入手里 |
| **手持该物品右键方块** | 在原地放出该村民 |
| Sneak + right-click a villager (empty hand) | Picks the villager up into a *Handheld Villager* item |
| Right-click a block while holding it | Releases the villager again |

物品提示会显示村民的职业与等级；如果村民本来有名字，物品名就是那个名字。

## 「原样放回」是怎么做到的 / Why it is identical

放回后的村民与原来**逐字段一致**——不只是"长得像"：

- **完整快照**：`Entity#saveWithoutId` 会写出实体持久化的全部字段：`VillagerData`（职业 / 等级 / 种类）、`Offers`（交易表与价格浮动）、`Xp`、`Health`、`attributes`、`active_effects`、`equipment`、`Inventory`、`Gossips`、`Brain`（AI 记忆）、`FoodLevel`、`RestocksToday`、`LastRestock`、`CustomName`……
- **只剔除 19 个瞬态字段**（`Pos`、`Motion`、`Rotation`、`Fire`、`Air`、`HurtTime`、`sleeping_pos` 等）——它们只描述"捕获那一刻它恰好在哪、烧没烧着"。
- **UUID 也保留**：原版刷怪蛋会给实体分配新 UUID，这里还原原 UUID，所以真的是同一个村民。若该 UUID 已在世界中存在（物品被复制过），自动退让分配新 UUID 避免冲突。
- **自动校验**：每次放回后模组会重新序列化实体，与物品里的快照逐字段 diff，结果写进日志：
  ```
  [villager-pickup] round-trip check: IDENTICAL (43 keys)
  ```

## 物品限制 / Item restrictions

| 限制 | 实现 |
|---|---|
| 不能堆叠 | `stacksTo(1)` |
| 不能附魔 | 不提供 `ENCHANTABLE` 组件 → 附魔台 / 铁砧 / `/enchant` 全部拒绝 |
| 不会被烧毁 | `fireResistant()` |
| 带附魔光效 | `ENCHANTMENT_GLINT_OVERRIDE = true`（无实际附魔） |
| 旁观者不能拾取 | `isSpectator()` 拦截 |
| 交易中的村民不能被抢 | `isTrading()` 拦截 |

## 兼容性 / Compatibility

| | |
|---|---|
| Minecraft | **26.3** |
| 加载器 / Loader | **Fabric**（Loader 0.20.1） |
| Fabric API | 0.166.2+26.3 |
| Java | **25** |
| 环境 / Environment | 客户端与服务端（`environment: "*"`） |

多人联机：所有状态变更只在服务端线程执行，客户端只负责发交互包；全程使用原版数据组件（`ENTITY_DATA` / `CUSTOM_NAME` / `LORE`），不需要任何自定义网络包。专用服务器已实测可正常加载。

## 从源码构建 / Build

```bash
./gradlew build        # 产物在 build/libs/
./gradlew runClient    # 启动开发客户端
./gradlew runServer    # 启动开发服务端
./gradlew genSources   # 反编译 Minecraft 源码
```

> `genSources` 与 `build` 不能放在同一次 Gradle 调用里（Gradle 9 会报 implicit dependency）。
> `gradle.properties` 中的 configuration-cache 必须关闭，Loom 1.19.4 与之不兼容。

## 实现笔记 / Implementation notes

- 村民数据存进原版刷怪蛋同款组件 `DataComponents.ENTITY_DATA`（`TypedEntityData<EntityType<?>>`），不自定义组件。
- 序列化必须带 registry context：`TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess())`，否则 `VillagerData` 里的 holder 编码会失败。
- 放回走 `EntityType.create(ServerLevel, PostSpawnProcessor, ...)`（原版落点逻辑），但**只套用 `ENTITY_DATA`**：原版 `createDefaultStackConfig()` 会顺带把物品自身的 `CUSTOM_NAME` 组件写到实体上，导致村民凭空多个名字。
- 26.3 的类结构变化：`Villager` 在 `net.minecraft.world.entity.npc.villager`、`Identifier` 在 `net.minecraft.resources`、`ResourceKey#location()` 改名为 `identifier()`。

## 许可证 / License

CC0-1.0，见 [LICENSE](LICENSE)。
