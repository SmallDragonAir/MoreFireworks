# 设计说明 / Design notes

## 1. 目标与约束 / Goal and constraints

目标：用**合理的软件工程**实现**极其不合理的组合规模**。

硬约束（来自需求）：

* 不能为每个变体注册物品；
* 不能预生成 JSON 或资源；
* 不能把组合预先载入内存；
* 不能每帧重新计算名称；
* 内存占用不能与变体数量线性相关。

推论：变体必须是**数据**，而不是**注册项**。

## 2. 为什么只有两个物品 / Why exactly two items

| 方案 | 注册项 | 存档大小 | 缺点 |
| --- | --- | --- | --- |
| 每个变体一个物品 | `10^818` | — | 物理上不可能 |
| 一个物品 + NBT/组件 | 1 | 每条 ~40–300 B | 需要自定义命名与提示 |
| 每个**形状**一个物品 | 5 | 同上 | 仍然要组件存颜色 |

选第二种：`spec_firework_rocket` 存整枚火箭（含爆炸序列），`spec_firework_star` 存单颗星。
星单独存在的意义是让"自定义 RGB 颜色制作烟火之星"这条需求有真实载体，而不是只能靠命令。

## 3. 数据组件 / Data components

两个组件都以**规范字符串**为持久化形式（`core/SpecCodec`）：

* `persistent(Codec)` + `networkSynchronized(StreamCodec)`，客户端与服务端看到同一份数据；
* 解码失败 → `DataResult.error`，而不是抛异常，恶意 NBT 只会被丢弃；
* 字符串格式便于调试、日志、命令输出与单元测试；
* 上限 1024 字符，超长数据在解码前就被拒绝。

**为什么不用 record codec（自动生成的复合 codec）？**
字符串形式天然是规范的（一个 spec ⇔ 一个字符串），而 record codec 会接受字段顺序不同、
多余字段缺失等变体，产生多种磁盘表示。规范表示让"同一组参数始终对应同一种数据表示"成为
可以断言的属性（`SpecCodecTest.encodingIsCanonical`）。

## 4. 与原版共存 / Living with vanilla

* 每枚火箭同时写入原版 `minecraft:fireworks` 组件；
* 飞行中的实体只携带**第 0 段**，因此飞行轨迹、拖尾与第一次爆炸完全是原版行为；
* 后续段由 `StagedDetonation` 通过 `ServerLevel#createFireworks` 依次播放。

这样做的代价是需要维护两份表示，收益是：原版 tooltip、发射器、`/summon`、其它模组的读取
逻辑都继续工作，玩家不会遇到"这个烟花在原版界面里是空的"。

### 合成路径 / The crafting path

生存模式能造出任意组合，靠的是**一个**自定义配方 `morefireworks:star_assembly`：

* 输入是原版标签 `#morefireworks:rocket_shells`（原版或本模组的火箭）与
  `#morefireworks:firework_stars`（原版或本模组的烟火之星），都是 `HolderSet` 形式的标签，
  因此不需要为每种组合写 JSON；
* 配方本身是 **shapeless**，但装配时按**槽位顺序**读取星星，于是"先放红后放蓝"与"先放蓝后放红"
  得到两枚不同的火箭——这正是原版配方丢失的信息；
* `morefireworks:spec_firework_star` 与 `minecraft:firework_star` 可以混用，原版烟火之星的
  `minecraft:firework_explosion` 组件会被 `VanillaBridge.toStar` 转换成 spec。

合成结果的数量受飞行等级限制（7 / 6 / 5 颗）。当星星多到该等级装不下时，配方**直接不匹配**：
格子里没有结果，材料原样保留——这比"匹配成功、静默截断、多出来的星星被吞掉"要好得多。

## 5. 顺序爆炸 / Ordered detonation

原版 `Fireworks` 组件里的 `explosions` 列表在**同一 tick**全部爆炸，顺序不可观察。
既然顺序是参数空间的一部分（需求明确要求 `A→B` 与 `B→A` 不同），就必须自己实现：

```
launch:  entity carries stage 0
         timeline = [stage 1 … stage n]

tick:    rocket removed  →  countdown 6 ticks
         fire next stage at the rocket's last position
         repeat until the deque is empty
```

三条硬边界：

1. `maxActiveTimelines = 64` —— 同时存在的序列上限，超出的发射退化为"只有第一段"，
   宁可少效果也不卡服；
2. `stageTimeoutTicks = 200` —— 实体因为任何原因没有消失时，序列仍然会结束；
3. 每段只调用一次 `createFireworks`，不生成实体、不造成伤害。

前两条以及 `stageIntervalTicks` 都可以在 `config/morefireworks-common.toml` 里改（`MFConfig`）；
而 8 色、7/6/5 段、1024 字符这些**结构**上限是编译期常量，因为它们属于数据格式，
改了会让存档里的烟花失去意义。

`tick()` 在 map 为空时立即返回，因此没有烟花时开销为一次 `isEmpty()`。

## 6. 命名 / Naming

三层：

```
core  ──► FireworkNamer（纯 Java，返回 String 与 List<NameSegment>）
        │
        ├─► 命令 / 日志（英文或中文纯文本）
        │
        └─► NameComponents（MC Component 树，translatable）
                │
                └─► 物品标题（简短名）+ Tooltip（完整名，按段拆分）
```

* 服务端永远不需要知道玩家语言 —— 显示交给 `translatable`；
* `FireworkNamer` 不 import 任何 Minecraft 类，因此可以在没有游戏环境的 JUnit 里测试；
* `StarRun.group` 同时服务两种渲染路径，合并规则只有一份实现。

## 7. 随机生成 / Random generation

`RocketRandomizer` 用 `SplittableRandom(seed)`：

* 直接抽样合法参数（形状、颜色数、颜色值、效果、星数），**不**先枚举再挑；
* 同一种子 ⇒ 完全相同的结果（`/morefireworks random 12345` 可复现）；
* `extreme` 固定取最大合法规模：1 级火箭（7 星）× 8+8 颜色 × 闪烁 + 拖尾。

## 8. 测试策略 / Testing

| 层次 | 测试 | 是否需要游戏 |
| --- | --- | --- |
| 数据格式 | `SpecCodecTest` | 否 |
| 组合数学 | `FireworkCombinatoricsTest` | 否 |
| 命名 | `FireworkNamerTest` | 否 |
| 随机 | `RocketRandomizerTest` | 否 |

`core` 与 `naming` 包刻意不依赖 Minecraft，因此 `./gradlew test` 不需要启动游戏或下载资源，
几秒钟就能跑完。Minecraft 相关的部分（组件、实体、命令）靠真实编译与 `runClient` 验证。

## 9. 已知取舍 / Known trade-offs

1. **不带自定义渲染**：全部复用原版粒子，因此视觉效果与参数空间的上限由原版决定
   （颜色顺序不可观察、渐变按原版规则插值）。项目不声称数学上不同的组合在视觉上必然不同。
2. **顺序爆炸有 6 tick 间隔**：为了让玩家"看得出顺序"，代价是总时长比原版长。
3. **组件是字符串而不是二进制**：可读性换来了少量空间开销（每枚火箭约多 10–20% 字节）。
4. **编辑界面**：第一阶段未实现图形化编辑器，参数通过命令与创造模式物品栏获得。
   数据层的所有接口都已就绪，GUI 只需要构造 `RocketSpec` 并写回组件。

## 10. 移植到 1.21.1 / Down-porting to 1.21.1

26.2 的工程保持在 `main` 分支，1.21.1 的移植在 `mc1.21.1` 分支上通过 git worktree 独立构建
（`D:/Project/MoreFireworks-1.21.1`），因此两个 jar 永远不会互相覆盖。

移植的原则是"只改 API 调用，不改算法"：`core`、`naming` 两个包除 `Component` 之外不接触
Minecraft，所以 47 个单元测试在 1.21.1 上原样通过，819 位理论总数与命名结果逐字节一致。

1.21.1 与 26.2 的具体差异（Tooltip、`InteractionResultHolder`、权限判定、`RecipeSerializer`
由类改为接口、`assemble` 多出 `HolderLookup.Provider`、`Ingredient` 的旧版 JSON 结构、
`registerItem` 没有 `UnaryOperator` 重载、物品染色从物品模型定义改为 `ItemColor`）已在
[README §8](../README.md#8-what-changed-in-the-1211-port) 逐条列出。

值得单独说明的一个设计决定是**客户端专属代码的隔离**：1.21.1 没有物品模型定义的 tint 机制，
烟火之星的图标颜色只能由 `ItemColor` 提供。这个类放在 `client/MFClientSetup`，并用
`@EventBusSubscriber(value = Dist.CLIENT)` 标注 —— 专用服务器永远不会加载它，因此"服务端不能
引用客户端类"这条约束由类加载器本身保证，而不是靠开发者的自觉。验证方式是直接启动专用服务器：
日志里没有任何 `NoClassDefFoundError`，且客户端与服务端可以互相连接。
