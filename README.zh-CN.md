# More Fireworks · 更多烟花

> 「为了充分释放玩家的创造力，我们决定让每一枚烟花都拥有几乎不可能重复的参数组合。」

一个 NeoForge 模组：用**两个物品**承载一个天文数字级的烟花参数空间。不枚举、不预生成、内存占用
不随变体数量增长——因为这个数量是：

| 量 | 精确规模 |
| --- | --- |
| 不同的烟火之星 `E` | `7.8804e+116`（117 位十进制） |
| 至少含一颗星的火箭 `T` | `1.8873e+818`（819 位十进制） |
| **物品变体总数**（`T` + 3 枚普通火箭） | **819 位十进制** |
| 视觉上可区分的火箭（不计颜色顺序） | `6.2887e+753` |

这些变体每一个都能由确定性算法生成，并且全部保存在同样两个数据组件里。

> **分支说明。** 本分支 `main` 是 Minecraft **26.2** / NeoForge 26.2.0.58（Java 25）。
> 另有一条 `mc1.21.1` 分支把同一个模组移植到 Minecraft **1.21.1** / NeoForge 21.1.x（Java 21），
> 组合数学、随机生成、819 位总数与命名算法完全一致，只改了 API 调用；两个分支的构建产物文件名
> 不同（`morefireworks-1.0.0.jar` 与 `morefireworks-1.0.0+mc1.21.1.jar`），互不覆盖。

---

## 1. 环境与版本

| 组件 | 版本 |
| --- | --- |
| Minecraft | **26.2** |
| NeoForge | **26.2.0.58** |
| Java 工具链 | **25** |
| Gradle | 9.x（wrapper 指向 9.9.1） |
| ModDev 插件 | `net.neoforged.moddev` 2.0.148 |

版本在写第一行代码之前就核对过官方元数据：

* `piston-meta.mojang.com` 的 26.2 清单里 `javaVersion.majorVersion = 25`，所以工具链必须是 JDK 25；
* `net.neoforged:neoforge:26.2.0.58` 真实存在于 NeoForged Maven（`installer`、`userdev`、`universal`
  齐全），依赖 `net.neoforged:neoform:26.2-2`。

## 2. 构建

```bash
./gradlew build      # 编译 + 单元测试 + 打包，产出 build/libs/morefireworks-1.0.0.jar
./gradlew test       # 只跑单元测试
./gradlew runServer  # 开发服务端（游戏目录 run/）
./gradlew runClient  # 开发客户端（游戏目录 run/）
```

wrapper 已生成，但在某些网络下无法下载其 distribution；本机验证时改用本地 Gradle **9.9.1** 二进制
（`gradle build`），[第 7 节](#7-测试与实机证据)记录的就是真正执行过的命令。Java 工具链由
`java { toolchain … }` 固定，缺失时 Gradle 会自动下载。

### 在无法访问 `maven.neoforged.net` 的网络里构建

`build.gradle` 里有一段显式的兼容代码：

```groovy
repositories.withType(MavenArtifactRepository).configureEach { repo ->
    if (repo.url.toString().contains('maven.neoforged.net')) {
        repo.url = uri('https://neoforged.forgecdn.net/releases')
    }
}
```

ModDev 插件会在脚本执行**之前**就把 `maven.neoforged.net` 加进工程仓库列表，所以「在下面再追加一个
镜像」是没用的——原条目会先被查询，而它的连接超时是致命的。上面这段把原条目改写到 NeoForged 官方
CDN 镜像，坐标与路径完全相同。如果你能直连主站，删掉它即可。

这样的网络还需要两处调整，都已提交在仓库里：

* `mavenCentral()` 换成 `https://repo1.maven.org/maven2`，因为 Gradle 默认的 `repo.maven.apache.org`
  可能被同样地投毒；
* `net.neoforged:minecraft-dependencies:26.2` 是一个**生成出来的** POM（内容只是罗列 Mojang 的库），
  CDN 镜像上没有。`local-maven/` 因此放了一份同坐标的替身 POM，由
  [`tools/gen-mcdeps.js`](tools/gen-mcdeps.js) 从 Mojang 公布的清单重建
  （`node tools/gen-mcdeps.js`，`--check` 用于检查是否过期）。它是派生数据而非秘密，所以入库。

## 3. 三分钟上手

1. 把 `build/libs/morefireworks-1.0.0.jar` 放进 `mods/`，客户端与服务端使用同一个 jar。
2. 进游戏后打开创意标签页 **更多烟花**，里面有普通火箭、五种形状的示例、以及 random / complex /
   extreme 三个种子固定的样例。
3. 手持火箭右键方块，从点击面发射；右键空气，朝准星方向发射。
4. 想看极端效果：`/morefireworks random extreme`，然后右键。
5. 想看参数空间有多大：`/morefireworks count`。

生存模式也不缺路径：原版配方做星星，工作台里「一枚火箭壳 + 1~7 颗星星」按**槽位顺序**合成出分段
火箭（见[合成与装配](#合成与装配)）。

## 4. 玩家能玩到什么

### 目录结构

```
.
├── build.gradle, settings.gradle, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/          生成的 wrapper（9.9.1）
├── local-maven/                                   生成的替身 POM（见第 2 节）
├── tools/gen-mcdeps.js                            从 Mojang 清单重建它
├── docs/                                          COMBINATORICS.md / NAMING.md / DESIGN.md
├── src/main/java/com/morefireworks/
│   ├── core/        StarSpec、RocketSpec、SpecCodec、RocketAssembly、组合数学、随机生成
│   ├── naming/      命名引擎、词表、组件渲染、LRU 名称缓存
│   ├── logic/       VanillaBridge（spec ⇄ 原版 Fireworks）、StagedDetonation
│   ├── recipe/      StarAssemblyRecipe（无序合成、保序）
│   ├── registry/    数据组件、物品、配方序列化器、创意标签页
│   ├── command/     /morefireworks
│   ├── MFConfig     可配置的运行时上限（COMMON 配置）
│   └── MoreFireworks 模组入口
├── src/main/resources/                            assets（语言、模型）与 data（配方、物品标签）
├── src/main/templates/META-INF/neoforge.mods.toml  构建时展开
└── src/test/java/                                 47 个单元测试
```

### 物品

| 物品 | 数据组件 | 用途 |
| --- | --- | --- |
| `morefireworks:spec_firework_rocket` | `morefireworks:rocket_spec` | 0~7 段**有序**爆炸的火箭 |
| `morefireworks:spec_firework_star` | `morefireworks:star_spec` | 单颗烟火之星（形状、颜色、特效） |

两个物品同时携带由 spec 派生的**原版**组件（`minecraft:fireworks`、`minecraft:firework_explosion`），
所以原版 tooltip、发射器、原版火箭实体以及其它模组都能照常工作；星星还能借原版染色在背包里显示
自己的配色，不需要自定义渲染。

### 发射

右键方块时从点击面发射（对齐原版手感），右键空气时朝准星方向发射。无论哪种方式，火箭实体只携带
第 0 段，其余段交给下面的分段爆炸引擎。

### 合成与装配

生存玩法所需的一切来自两条配方，且都不枚举变体：

1. **星星用原版配方做。** 任何染料、形状修正、萤石粉或钻石的 `minecraft:firework_star` 都能作为
   输入；`/morefireworks star <#RRGGBB> [shape]` 则能拿到任意 24 位颜色的星星。
2. **火箭用 `morefireworks:star_assembly` 装配。** 一个 2×2 或 3×3 的无序配方，恰好包含一枚火箭壳
   （`#morefireworks:rocket_shells`，原版或本模组的都行）与 1~7 颗星星
   （`#morefireworks:firework_stars`），产出按**槽位顺序**排列分段的 spec 火箭。原版配方会把同样的
   星星收进一个无序集合、一次性全部炸开——而这正是本模组要额外提供的那个维度。

超过该飞行等级上限的格子会直接**不匹配**（格子里没有结果），材料原样保留，不会被静默吞掉。

### 命令

| 命令 | 作用 |
| --- | --- |
| `/morefireworks count` | 打印精确理论参数空间（BigInteger，819 位） |
| `/morefireworks random [seed]` | 给出一枚随机合法火箭，种子可复现 |
| `/morefireworks random complex [seed]` | 多段、带渐变、闪烁与拖尾的火箭 |
| `/morefireworks random extreme [seed]` | 最大合法火箭：飞行 1、7 颗星、8+8 颜色、双特效全开 |
| `/morefireworks info` | 打印手持物品的规范文本形式 |
| `/morefireworks name` | 打印中英文的短名与全名 |
| `/morefireworks star <#RRGGBB> [shape]` | 给出一颗任意 RGB 颜色的星星 |
| `/morefireworks status` | 报告当前正在飞行的分段爆炸数量 |
| `/morefireworks testlaunch [random\|complex\|extreme]` | 在命令执行位置发射一枚生成的火箭，服务端控制台可用 |
| `/morefireworks recipecheck` | 用真实配方系统跑一次合成并打印结果 |

后三条存在的意义是**无需客户端也能验证运行时行为**：`testlaunch` 走一遍火箭实体与分段链路，
`recipecheck` 走真实的 `RecipeManager` 查询而不是复制一份配方逻辑。

### 分段爆炸

原版会在同一个 tick 引爆火箭的全部爆炸，所以爆炸**顺序**永远不可观测。既然顺序是本模组参数空间的
一部分，就实现了显式时序（`logic/StagedDetonation`）：

1. 飞行中的火箭只带第 0 段，飞行与第一次爆炸与原版完全一致；
2. 其余段在服务端按列表顺序、间隔 6 tick，在火箭爆炸的位置依次引爆；
3. 每段都用 `ServerLevel#createFireworks` 发出，纯视觉效果。

上限是设计好的：最多 64 条并发时间线，每条最多 7 段，即使火箭实体永不消失，200 tick 后也会强制结束。

## 5. 架构

```
com.morefireworks
├── core/          纯 Java 模型：StarSpec、RocketSpec、SpecCodec、RocketAssembly、
│                  组合数学、随机生成、上限常量
├── naming/        命名引擎（纯文本形式 + 可翻译组件形式 + 缓存）
├── logic/         VanillaBridge（spec ⇄ 原版 Fireworks）、StagedDetonation
├── recipe/        StarAssemblyRecipe（无序、保序）
├── registry/      数据组件、物品、配方序列化器、创意标签页
├── command/       /morefireworks
├── MFConfig       可配置运行时上限（COMMON 配置，morefireworks-common.toml）
└── MoreFireworks  模组入口
```

这个切分很关键：`core` 与 `naming` **完全不 import Minecraft**，因此最有趣的部分能用普通 JUnit
测试，不需要启动游戏。

### 数据格式

一枚火箭的 spec 保存为单个规范字符串（见 `core/SpecCodec`）：

```
v1;<飞行等级>;<形状>:<初始颜色>:<渐变颜色>:<标志>|<形状>:...
```

* `形状` — `small_ball`、`large_ball`、`star`、`creeper`、`burst`
* `颜色` / `渐变` — 小写 `rrggbb`，逗号分隔，**顺序保留**
* `标志` — `f` 闪烁、`t` 拖尾，都没有则为空

示例：

```
v1;2;star:b02e26,3c44aa:fed83d:ft|large_ball:3ab3da:f9fffe:t
```

这个格式是**无损**的（顺序与重复都保留）、**规范**的（一个 spec ⇔ 恰好一个字符串）、**有界**的
（超过 1024 字符拒绝）、**带校验**的（每次反序列化都走与 Java 模型相同的范围检查）。网络同步时
同一字符串加 16 位长度前缀。

### 为什么内存占用是常数

* 两种物品、两种组件、一个创意标签页，启动时各注册一次。
* 一个物品栈只存一个字符串；一枚满配火箭通常 40~300 字节。
* 名称按需计算并记忆在 512 条 LRU 缓存里；**不会每帧重算**，因为 `getName` / `appendHoverText`
  只在构建 tooltip 时被调用。
* 计算命令求的是闭式公式，从不遍历变体。

## 6. 安全限制

结构性上限是编译期常量，因为它们属于数据格式——存档里的烟花必须永远保持同样的含义：

| 限制 | 取值 | 位置 |
| --- | --- | --- |
| 每枚火箭的星数 | 飞行 1/2/3 → 7/6/5 | `RocketSpec.maxStars` |
| 单颗星的初始颜色数 | 8 | `StarSpec.MAX_COLORS` |
| 单颗星的渐变颜色数 | 8 | `StarSpec.MAX_FADE_COLORS` |
| spec 字符串长度 | 1024 字符 | `FireworkLimits.MAX_SPEC_CHARS` |
| 名称缓存 | 512 条（LRU） | `FireworkNameCache.CAPACITY` |

运行时上限可在 `config/morefireworks-common.toml` 调整（`MFConfig`，注册为 `COMMON` 配置）：

| 选项 | 默认 | 范围 | 含义 |
| --- | --- | --- | --- |
| `maxActiveTimelines` | 64 | 1–4096 | 并发分段爆炸数；超出后新的发射只保留第一段 |
| `stageIntervalTicks` | 6 | 1–100 | 两段爆炸之间的 tick 间隔 |
| `stageTimeoutTicks` | 200 | 20–6000 | 火箭实体永不消失时的强制截止时间 |

凡是解码得到的值都会重新校验：空形状、空颜色数组、越界 RGB、超过飞行等级允许的星数、超长字符串、
畸形数字，全部变成数据错误，而不是能逃进游戏主循环的异常。

## 7. 测试与实机证据

`./gradlew test` 运行 **47 个测试**，分布在五个类：

* `SpecCodecTest` — 往返、规范形式、顺序敏感性，以及垃圾输入、超长载荷、非法段数的拒绝。
* `FireworkCombinatoricsTest` — 闭式公式与暴力求和互验，十进制位数与科学计数法，视觉模型用
  Pascal 恒等式验证。
* `FireworkNamerTest` — 确定性、分段与名称一致、相邻合并、中英差异、已记录的短名碰撞，以及需求里
  给的那条中文短名 `三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭` 与英文空格规则。
* `RocketRandomizerTest` — 种子可复现、1500 枚生成结果全部合法、`extreme` 确实是最大合法火箭。
* `RocketAssemblyTest` — 合成台的顺序规则与飞行等级截断规则。

### 真正跑过的命令与结果

| 步骤 | 命令 | 结果 |
| --- | --- | --- |
| 编译 | `gradle compileJava` | `BUILD SUCCESSFUL` |
| 测试 | `gradle clean test build` | 47 tests，0 failures，0 errors |
| 打包 | 同上 | `build/libs/morefireworks-1.0.0.jar`（85,556 字节，71 个条目） |
| 专用服务端 | `gradle runServer` | `More Fireworks 1.0.0 (morefireworks)` 加载，`Done (0.204s)!` |
| 客户端 | `gradle runClient` | `LWJGL version 3.4.1+2`、`OpenGL 3.3.0 NVIDIA 621.01`，资源重载包含 `mod/morefireworks`，`Sound engine started`，无缺失模型警告 |
| 组合数学 | RCON `morefireworks count` | 819 位、`1.8873e+818` |
| 配方 | RCON `morefireworks recipecheck` | 命中 `morefireworks:star_assembly`，产出 2 段有序 spec，并打印中英文名称 |
| 分段爆炸 | RCON `morefireworks testlaunch extreme` → `morefireworks status` | 一次发射 7 段、1 条活动时间线；状态在约 8 秒后回到 0，日志无异常 |
| 持久化 | `summon` 带组件 → `save-all` → `stop` → 冷启动 → `data get entity` | 组件原样读回，动态名称由组件现场重建 |
| 配置 | 服务端启动 | 生成 `run/config/morefireworks-common.toml`，三个选项与文档一致 |

表格里所有 Gradle 调用都使用本地 Gradle 9.9.1 二进制 + **JDK 25**。`./gradlew` 在能下载 wrapper
distribution 的网络里执行的是同一批任务（见第 2 节）。用于验证的服务端与客户端都在 `run/`（被 git
忽略），全部通过 RCON 或读取日志取证，因此上面的证据来自真实进程，而不是读代码推断。

持久化检查走完了「保存 → 关机 → 冷启动」全流程；物品的动态名称是从重新载入的组件**现场生成**的，
并没有被存下来。

## 8. 这一版不做什么

* **没有 GUI 编辑器。** 星星与火箭来自创意标签页、装配配方或命令。`RocketSpec` / `StarSpec` 都是纯
  record，将来做编辑器只需要构造一个对象再写回组件。
* **没有自定义渲染。** 爆炸是原版粒子；参数顺序在数据里完整保留、并且通过分段爆炸可观测，但**单颗星
  内部多个初始颜色的先后顺序在视觉上无法区分**（原版会取平均）。本文档不会宣称相反的话。
* **没有 JEI/REI 插件。** 那意味着枚举变体，而项目明确拒绝这么做。
* **分段爆炸默认间隔约 6 tick**（可配置），所以 5 颗星的火箭会比原版稍久一点。这个延迟就是「让顺序
  真正可见」的代价。

## 9. 组合数学说明

`E` 的计算方式（`R = 2^24 = 16 777 216`）：

```
E = 5（形状）× 2（闪烁）× 2（拖尾）× (Σ R^i, i=1..8) × (Σ R^j, j=0..8)
```

火箭总数（顺序有意义、允许重复，且至少一颗星）：

```
T = Σ(d=1..3) Σ(n=1..8-d) E^n
```

外加 3 枚不含爆炸效果的普通火箭（飞行 1/2/3）。全部用 `BigInteger` 精确计算，不遍历、不用 `double`
保存精确值。完整推导与三组数字的定义（数学组合数 / 可生成数 / 视觉可区分数）见
[`docs/COMBINATORICS.md`](docs/COMBINATORICS.md)。

## 10. 命名

同一身份，两种显示形式（详见 [`docs/NAMING.md`](docs/NAMING.md)）：

* **短名**——`三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭`，作为物品标题。故意有损：重复特效
  合并为数量，颜色最多显示三种。
* **全名**——`三级{1–2星形红色/蓝色→黄色(闪烁,拖尾)}{3大球形淡蓝色→白色(拖尾)}{4小球形黄绿色}复合烟花火箭`，
  作为 tooltip，每种相同的星占一行。无损、带位置编号、相邻相同参数按有机化学的位置编号方式合并。
  同一 spec 的英文形式是
  `tier III {1–2 star red/blue→yellow (flicker,trail)} {3 large ball light blue→white (trail)} {4 small ball lime} compound firework rocket`
  ——构成完全相同，只有空格不同，因为中文按化合物习惯连写。

游戏内 tooltip 由**可翻译组件**拼装，因此显示语言跟随客户端语言设置，切换语言后已有物品栈立即变化；
命令与日志则使用 `FireworkNamer` 产生的纯文本形式。

## 11. 文档

* [`docs/COMBINATORICS.md`](docs/COMBINATORICS.md)——完整推导、精确数值，以及三种「数量」的区分。
* [`docs/NAMING.md`](docs/NAMING.md)——命名规则与实例。
* [`docs/DESIGN.md`](docs/DESIGN.md)——架构决策、与原版共存的取舍。
* 英文版说明：[`README.md`](README.md)。

## 12. 许可证

MIT，见 `LICENSE`。
