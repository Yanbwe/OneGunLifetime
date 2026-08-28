# OneGunLifetime（一人一枪一辈子 / One Gun Lifetime）

一个基于 **NeoForge 1.21.1** 的**玩法拓展模组**，为 [ModularShoot](https://github.com/Yanbwe/ModularShoot)（模块化射击框架）添加"灵魂绑定"玩法：枪械数值与插件加成等数据全部挂载到玩家身上，现在的枪只是拔插插件和发射的入口了。

A **gameplay addon mod** for **NeoForge 1.21.1** that adds "soul binding" to the [ModularShoot](https://github.com/Yanbwe/ModularShoot) framework: all gun stats and plugin bonuses are attached to the player themselves, and the gun is now just the entry point for attaching plugins and firing.

# 特色 / Features

## 一次约定，就要负责一辈子 / Once Bound, Bound for Life

通过一些神秘的方式就可以把一把枪与你的灵魂绑定，之后的日子，你就只能用它咯。

Through some mysterious means, a gun can be bound to your soul. From that day on, that is the only gun you will ever use.

## 插孔跑到玩家身上了 / Plugin Sockets Move onto the Player

枪械数值与插件加成不再依赖任何物品，而是直接挂在玩家本体上，你可以把这把陪你走完一生的灵魂枪械调教成想要的样子。

Gun stats and plugin bonuses no longer depend on any item, they are attached directly to the player. You can tune this soul gun, which accompanies you for a lifetime, into whatever shape you want.

## 只能有我一把枪哦 / Only One Gun Allowed

背包里有其它的枪呢，直接删除！你在尝试把灵魂枪械丢出去，放到箱子里？不可以，他会牢牢的粘在你身上！你用强硬的手段删除了身上的枪？5 秒后自动补到背包里！你直接把灵魂枪械替换了？那就让新枪直接变成灵魂枪械的样子吧~无论怎样，本模组都能保证你不会用上别的枪。

Other guns in your inventory are deleted on the spot! Trying to toss the soul gun or stash it in a chest? Not allowed — it clings firmly to you! Deleted it by force? It is automatically restored to your inventory 5 seconds later! Replaced it with a new gun? Then the new gun simply becomes the soul gun itself. No matter what, this mod guarantees you will never get to use another gun.

## 它也是认主人的 / The Gun Knows Its Owner

只有绑定者本人能发射灵魂枪。未绑定的玩家拾取他人的灵魂枪会得到一件"灵魂枪械纪念品"，原主人的枪自动补回，灵魂枪不认第二个人。

Only the bound owner can fire the soul gun. An unbound player who picks up someone else's soul gun receives a "soul gun memorial" keepsake instead, and the original owner's gun is automatically restored. A soul gun never acknowledges a second owner.

# 如何使用？/ How to Use?

正常情况下该模组无法通过正常生存游玩，需要使用指令或者 API 手动来为玩家绑定枪械。具体使用方法请查看 WIKI。

Normally this mod cannot be played through in normal survival, a gun must be bound to a player via commands or the developer API. See the WIKI for details.

# 依赖 / Requirements

- **[ModularShoot](https://github.com/Yanbwe/ModularShoot) 0.3.1+**（框架模组，必需 / required）

# 使用文档 / Documentation

[Yanbwe's WIKI](https://yanbwe.github.io/Yanbwe-Wiki/onegunlifetime/)

# 常见问题 / FAQ

**Q1：这个模组添加了什么新物品吗？**
**A1**：一件都没有。投影枪复用模块化射击的枪械定义与外观，本模组不注册任何物品、纹理。

**Q1: Does this mod add any new items?**
**A1**: Not a single one. Projection guns reuse ModularShoot's gun definitions and appearances; this mod registers no items or textures of its own.

# 许可证 / License

MIT
