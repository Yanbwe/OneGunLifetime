# OneGunLifetime（一人一枪一辈子 / One Gun Lifetime）

一个基于 **NeoForge 1.21.1** 的**玩法 addon 模组**，为 [ModularShoot](https://github.com/Yanbwe/ModularShoot)（模块化射击框架）添加灵魂绑定玩法：一生只有一把枪——把灵魂绑上去，枪械属性常驻本体，其余枪械进入背包会被自动同化。

A **gameplay addon mod** for **NeoForge 1.21.1** that brings soul-binding to the [ModularShoot](https://github.com/Yanbwe/ModularShoot) framework: one gun for a lifetime. Bind your soul to it, keep its attributes permanently, and assimilate every other gun that enters your inventory.

# 特色 / Features

## 灵魂绑定 / Soul Binding

用 `/onegun bind <gunId>` 把灵魂绑定到任意一把已注册的 ModularShoot 枪械上，获得它的"投影枪"。绑定一生只有一次，且灵魂数据跟随玩家跨死亡保留——重生之后，枪还是那把枪。

Bind your soul to any registered ModularShoot gun with `/onegun bind <gunId>` and receive its "projection gun". You get one binding per lifetime, and the soul data survives death — after respawning, it's still the same gun.

## 属性常驻 / Always-On Attributes

枪械数值与插件加成不再依赖手持，而是直接挂在玩家本体上——空手也生效。配合 `/onegun stat` 与 `/onegun trait`，你可以把这把陪你走完一生的枪调教成想要的样子。

Gun stats and plugin bonuses are mounted on the player instead of the held item — they work even bare-handed. With `/onegun stat` and `/onegun trait`, you can shape the gun that will accompany you for the rest of your life.

## 枪械同化 / Gun Assimilation

背包容不下第二把枪：其它 ModularShoot 枪械一进入背包就会被自动同化为你的绑定枪，插件也随之并入。抢在同化完成前扣扳机？会被拒绝——它正在成为你的枪。

There is no room for a second gun: any other ModularShoot gun entering your inventory is assimilated into your bound gun, plugins carried over. Pull the trigger before assimilation completes? Denied — it's busy becoming your gun.

## 丢失补回 / Loss Recovery

投影枪无法丢弃；丢进岩浆、仙人掌或任何会销毁物品的地方，它都会自动回到你手中。`/clear` 可以清掉它（一次性放行，方便管理员清空背包），除此之外的"意外"统统会被补回。

The projection gun cannot be dropped; thrown into lava, cactus, or anything destructive, it comes back on its own. `/clear` is allowed once (for admins emptying an inventory) — every other "accident" gets recovered.

## 容器守卫 / Container Guard

投影枪塞不进任何容器；漏斗、管道等漏网之鱼会被低频扫描找到并归还主人。

It cannot be stored in any container; anything that slips through — hoppers, pipes, ... — is found by a background scan and returned to its owner.

## 主人限定 / Owner-Locked

只有绑定者本人能发射投影枪。未绑定的玩家拾取他人的投影枪会得到一件"灵魂枪械纪念品"，原主人的枪自动补回——灵魂枪不认第二个人。

Only the owner can fire the projection gun. An unbound player picking up someone else's projection gun receives a "soul gun memorial" keepsake while the owner's gun is restored automatically — a soul gun acknowledges only one person.

# 命令 / Commands

- **`/onegun bind <gunId>`** — 将灵魂绑定到一把已注册的枪械，并获得投影枪 / Bind your soul to a registered gun and receive the projection gun
- **`/onegun stat set <stat> <value>`** / **`/onegun stat reset`** — 覆写或重置属性数值 / Override or reset stat values
- **`/onegun trait add|remove <trait>`** — 添加或移除特性 / Add or remove traits
- **`/onegun info`** — 查看自己的灵魂枪械信息 / Inspect your soul gun
- **`/onegun unbind <player>`** — 解绑指定玩家（需要管理员权限） / Unbind a player (admin only)

# 依赖 / Requirements

- **[ModularShoot](https://github.com/Yanbwe/ModularShoot) 0.3.1+**（框架模组，必需 / required）

# 开发者 API / Developer API

其他模组可通过 Maven 依赖使用完整 Java API（门面类 `org.yanbwe.onegunlifetime.OneGunLifetimeAPI`），以编程方式完成绑定/解绑、属性与特性调整、插件装卸与数值面板读取——不再局限于指令。

Other mods can use the full Java API via a Maven dependency (facade class `org.yanbwe.onegunlifetime.OneGunLifetimeAPI`) to bind/unbind players, adjust stats and traits, install or remove plugins, and read effective values programmatically — no commands required.

| 能力 / Capability | 方法 / Methods |
|---|---|
| 查询 / Queries | `getSoulData` `isBound` `getOwnerOf` `getPlugins` `getGunState` `getEffectiveValues` |
| 生命周期 / Lifecycle | `bindAndGive` `unbindAll` `rescan` |
| 属性与特性 / Stats & Traits | `setStatOverride` `removeStatOverride` `clearStatOverrides` `addTrait` `removeTrait` |
| 插件与枪态 / Plugins & State | `addPlugin` `removePlugin` `setGunState` |

发布坐标 / Maven coordinates：`org.yanbwe.onegunlifetime:onegunlifetime:1.1.0`（GitHub Packages：`https://maven.pkg.github.com/Yanbwe/OneGunLifetime`）。

# 使用文档 / Documentation

[Yanbwe's WIKI](https://yanbwe.github.io/Yanbwe-Wiki/onegunlifetime/)

# 常见问题 / FAQ

**Q1**：绑定之后反悔了，想换一把枪怎么办？  
**A1**：换不了，一生一把枪。实在要换，请管理员使用 `/onegun unbind` 解绑后再重新绑定。

**Q1**: Changed your mind and want a different gun?  
**A1**: You can't — one gun for a lifetime. If you truly must, ask an admin to `/onegun unbind` you, then bind again.

**Q2**：这个模组添加了什么新物品吗？  
**A2**：一件都没有。投影枪复用 ModularShoot 的枪械定义与外观，本模组不注册任何物品、模型或贴图。

**Q2**: Does this mod add any new items?  
**A2**: Not a single one. The projection gun reuses ModularShoot's gun definitions and visuals — no items, models, or textures are registered here.

**Q3**：我把投影枪扔进岩浆里了！  
**A3**：它已经回到你的物品栏了，去看看。

**Q3**: I threw my projection gun into lava!  
**A3**: It's already back in your inventory. Go check.

**Q4**：我捡到了别人的灵魂枪，能开枪吗？  
**A4**：不能。绑定玩家会发现它正在同化成自己的枪；未绑定的玩家则会得到一件"灵魂枪械纪念品"，原主人的枪会自动补回。

**Q4**: I picked up someone else's soul gun. Can I fire it?  
**A4**: No. A bound player will find it assimilating into their own gun; an unbound player receives a "soul gun memorial" instead, and the original owner gets their gun back automatically.

**Q5**：为什么其它枪一进我背包就变了？  
**A5**：同化。背包只容得下一把枪。

**Q5**: Why does every other gun transform the moment it enters my inventory?  
**A5**: Assimilation. There is only room for one gun.

# 许可证 / License

All Rights Reserved
