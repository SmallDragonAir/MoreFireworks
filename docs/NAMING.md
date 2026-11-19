# 命名规则 / Naming rules

命名系统模仿有机化学的**取代基命名法**（substitutive nomenclature）：

* 先写"母体"（这里是飞行等级 + 火箭类别）；
* 再按位置编号列出每一个"取代基"（这里是每一段爆炸）；
* 相邻的相同星合并成位置区间（类似 `2,3-二甲基`）；
* 相同参数用数量前缀合并（`双闪烁`、`四拖尾`）；
* 不省略任何决定唯一性的数据。

The nomenclature imitates substitutive chemical naming: parent first (tier + rocket class), then
every stage by locant, with adjacent identical stages merged into locant ranges and repeated
parameters folded into count prefixes.

---

## 1. 两种形式 / Two forms

| 形式 | 用途 | 无损 | 长度 |
| --- | --- | --- | --- |
| **short** 简短名 | 物品标题 | 否（故意丢失信息） | 通常 < 60 字符 |
| **full** 完整名 | Tooltip、`/morefireworks name` | 是 | 可长达成百字符，按行拆分 |

两种形式都是 spec 的**纯函数**：没有随机、没有时钟、不依赖机器语言环境。
同一个 spec 在任何时候、任何一端都得到同一个字符串。

## 2. 完整名 / The full form

语法（BNF 风格）：

```
full  ::= tier run ( run )* suffix
run   ::= "{" locant shape "[" colours ( "→" fades )? "]" ( "(" effects ")" )? "}"
locant ::= number | number "–" number
```

中文直接用 `tier` 紧接 `{`；英文在每个 token 之间插入空格（`tier I {1 star red→yellow (flicker)}`），
因为中文复合词连写、英文必须分词。

示例（三级、四段爆炸，前两段完全相同；以下是程序真实输出）：

```
三级{1–2星形红色/蓝色→黄色(闪烁,拖尾)}{3大球形淡蓝色→白色(拖尾)}{4小球形黄绿色}复合烟花火箭
```

同一 spec 的英文形：

```
tier III {1–2 star red/blue→yellow (flicker,trail)} {3 large ball light blue→white (trail)} {4 small ball lime} compound firework rocket
```

要点：

* `1–2` 是位置区间（en dash），表示第 1 和第 2 段是同一颗星；
* `红色/蓝色→黄色` 中 `/` 分隔**初始颜色**，`→` 之后是**渐变颜色**，顺序被完整保留；
  简短名用单字颜色（`红/蓝→黄`），完整名用全称（`红色/蓝色→黄色`）；
* `(闪烁,拖尾)` 只在该段有效果时出现；英文在每段前后加空格，中文连写；
* 任意颜色若不是 16 种染料之一，写成 `#RRGGBB`（无损）。

英文的简短名示例：

```
tier III-2 flicker-3 trail-red/blue/yellow+ gradient-star/large ball/small ball compound firework rocket
```

（`+` 表示颜色被截断到前三个，`等` 是它的中文对应写法。）

## 3. 简短名 / The short form

示例（程序真实输出，与规格书中的示例同名）：

```
三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭
```

组成顺序固定：

1. **飞行等级**：`一级` / `二级` / `三级`（英文 `tier I/II/III`）——等级用数字，不用 `双`；
2. **效果计数**：`双闪烁`、`四拖尾`（中文用 双/三/四/五/六/七，数量为 1 时省略数字，
   英文用 `2 flicker`）；
3. **颜色摘要**：去重后的前三个颜色（中文 `红蓝`，英文 `red/blue`），超过三个时用 `等`/`+`；
   有任何渐变时追加 `渐变`；
4. **形状摘要**：出现过的形状，按首次出现顺序，用 `-` 连接；
5. **后缀**：`复合烟花火箭` / `普通烟花火箭`。

> 简短名**可以碰撞**。例如 `[红]` 与 `[红, 红]` 的简短名相同，但完整名不同。
> 这是有意的设计：简短名给人读，完整名给数据读。`FireworkNamer.sameShortName` 就是为
> 记录这一事实而存在的。

## 4. 合并规则 / Merging

| 场景 | 规则 |
| --- | --- |
| 相邻相同的星 | 合并为位置区间 `1–2` |
| 不相邻但相同的星 | **不合并**，因为位置不同即数据不同（`1–2` vs `2,4`） |
| 重复出现的形状 | 简短名里只出现一次 |
| 重复出现的颜色 | 简短名里只出现一次，但完整名保留重数 |

## 5. 双语 / Bilingual

游戏内名称由 **translatable component** 组成，而不是在服务端拼好字符串：

* `morefireworks.tier.1..3`、`morefireworks.shape.*`、`morefireworks.effect.*`、
  `morefireworks.color.*` 等键在 `en_us.json` 与 `zh_cn.json` 中各有一份；
* 语言由客户端决定，切换语言后**已存在的物品**立即显示新语言；
* 数字、`#RRGGBB`、位置区间与 `→`/`/` 等符号是语言无关的字面量。

命令行与日志使用 `FireworkNamer` 生成的纯文本（`NamingLanguage.ZH_CN` / `EN_US`），
两者内容一致，只是渲染通道不同。

## 6. 缓存 / Caching

`FireworkNameCache` 是一个 512 条目的访问序 LRU（`LinkedHashMap` + `removeEldestEntry`），
键为 `(spec, language, mode)`。

* 只有构建 tooltip / 物品名时才查询；
* 不会每帧计算：`getName` 与 `appendHoverText` 只在需要时被调用；
* 缓存有上界，因此内存不会随玩家见过的烟花数量无限增长。

## 7. Tooltip 布局 / Tooltip layout

完整名不按字符数硬折行（那会把 `#12AB34` 之类的 token 切断），而是**按结构拆分**：

```
三级复合烟花火箭          ← 标题行
1–2 星形 红/蓝→黄 (闪烁,拖尾)
3 大球形 淡蓝→白 (拖尾)
4 小球形 黄绿
```

每一行对应一个 `FireworkNamer.StarRun`，因此无论参数多复杂，行宽都是有界的。
