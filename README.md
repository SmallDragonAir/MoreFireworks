# More Fireworks · 更多烟花

> 「为了充分释放玩家的创造力，我们决定让每一枚烟花都拥有几乎不可能重复的参数组合。」
>
> "To fully unleash the player's creativity, we decided to give every firework a parameter
> combination that is practically impossible to repeat."

A NeoForge mod that implements an **astronomically large firework parameter space** with a
registry footprint of exactly **two items**. Nothing is enumerated, nothing is pre-generated, and
memory use does not grow with the number of variants — because the number of variants is:

| Quantity | Exact size |
| --- | --- |
| Distinct firework stars `E` | `7.8804e+116` (117 decimal digits) |
| Rockets carrying at least one star `T` | `1.8873e+818` (819 decimal digits) |
| **Total item variants** (`T` + 3 plain rockets) | **819 decimal digits** |
| Visually distinct rockets (order-insensitive colour model) | `6.2887e+753` |

Every one of those variants is reachable through a deterministic algorithm, and every one of them
is stored in the same two data components.

> **Branches.** `main` is the Minecraft **26.2** / NeoForge 26.2.0.58 build. This branch,
> `mc1.21.1`, is the *same* mod ported to Minecraft **1.21.1** / NeoForge **21.1.x** (developed
> against 21.1.244 and intended to run on any 21.1.x server). Both branches are complete, built and
> verified; building here produces `morefireworks-1.0.0+mc1.21.1.jar` and never touches the 26.2
> artifacts. The port kept the combinatorics, the randomiser, the 819-digit total and the naming
> algorithm byte-for-byte — only the Minecraft/NeoForge API calls differ, and every difference is
> listed in [§8](#8-what-changed-in-the-1211-port).

---

## 1. Requirements

| Component | Version |
| --- | --- |
| Minecraft | **1.21.1** |
| NeoForge | **21.1.244** to develop; the jar declares `versionRange = "[21.1.0,)"` so any 21.1.x server accepts it |
| Java (toolchain) | **21** |
| Gradle | 9.x (the bundled wrapper targets 9.9.1; any 9.x works) |
| NeoForge ModDev plugin | `net.neoforged.moddev` 2.0.148 |

The version triple was verified against the official metadata before any code was written:

* `https://piston-meta.mojang.com/.../1.21.1.json` reports `id = 1.21.1`, `type = release`, and a
  Java 21 runtime component.
* `net.neoforged:neoforge:21.1.244` exists on the NeoForged Maven (`installer`, `userdev`,
  `universal` artifacts) and depends on `net.neoforged:neoform:1.21.1-20240808.144430`.

## 2. Building

```bash
./gradlew build          # compiles, runs the unit tests, produces build/libs/morefireworks-1.0.0+mc1.21.1.jar
./gradlew test           # unit tests only
./gradlew runClient      # dev client (game dir: run-client/)
./gradlew runClientJoin  # dev client that joins 127.0.0.1:25565 straight away
./gradlew runServer      # dev server (game dir: run/)
```

The wrapper is generated but its distribution cannot be fetched on every network; on this
development machine the builds above were run with a local Gradle **9.9.1** binary instead
(`gradle build`), which is why the exact command that was executed is recorded in
[§7](#7-tests). Java 21 must be reachable as a toolchain; the `java` block pins it and Gradle
downloads it if needed.

The dev client and the dev server use **different game directories** (`run-client/` and `run/`)
exactly so they can run at the same time: sharing one directory makes both processes fight over
`logs/latest.log` and `options.txt`, which breaks the client/server compatibility check. The
`runClientJoin` configuration additionally passes `--quickPlayMultiplayer 127.0.0.1:25565`, so the
dev client connects to the dev server without any GUI interaction; that is how the connection row
in [§7](#7-tests) was produced.

### Building without `maven.neoforged.net`

`build.gradle` contains a small, explicit compatibility block:

```groovy
repositories.withType(MavenArtifactRepository).configureEach { repo ->
    if (repo.url.toString().contains('maven.neoforged.net')) {
        repo.url = uri('https://neoforged.forgecdn.net/releases')
    }
}
```

The ModDev plugin adds `maven.neoforged.net` to the project repositories before the build script
runs, so simply appending a mirror is not enough — the original entry is consulted first and its
connection timeout is fatal. The block above rewrites that entry to the official NeoForged CDN
mirror, which serves the same coordinates under the same paths. Remove it if you can reach the
primary host directly.

Two more adjustments are needed on such a network, and both are in the repository:

* `mavenCentral()` is replaced by `https://repo1.maven.org/maven2`, because Gradle's default
  `repo.maven.apache.org` host can be poisoned in the same way;
* `net.neoforged:minecraft-dependencies:1.21.1` — a *generated* POM that merely lists Mojang's
  libraries — is missing from the CDN mirror. `local-maven/` therefore contains a stand-in POM with
  the same coordinates, rebuilt from Mojang's published 1.21.1 manifest by
  [`tools/gen-mcdeps.js`](tools/gen-mcdeps.js) (`node tools/gen-mcdeps.js`, or `--check` to see
  whether it is still current; `MC_VERSION=1.21.1` selects the version, the default is `26.2` for
  the other branch). It is checked in because it is derived data, not a secret.

## 3. What the player gets

### Project layout

```
.
├── build.gradle, settings.gradle, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/          generated wrapper (9.9.1)
├── local-maven/                                   generated stand-in POM (see §2)
├── tools/gen-mcdeps.js                            regenerates it from Mojang's manifest
├── docs/                                          COMBINATORICS.md, NAMING.md, DESIGN.md
├── src/main/java/com/morefireworks/
│   ├── core/       StarSpec, RocketSpec, SpecCodec, RocketAssembly, combinatorics, randomiser
│   ├── naming/     nomenclature engine, vocabulary, component renderer, LRU cache
│   ├── logic/      VanillaBridge, StagedDetonation
│   ├── recipe/     StarAssemblyRecipe (+ its 1.21.1 serializer record)
│   ├── registry/   data components, items, recipe serializer, creative tab
│   ├── command/    /morefireworks
│   ├── client/     MFClientSetup — client-only star tint (Dist.CLIENT, never loaded server-side)
│   └── MFConfig    configurable runtime limits (COMMON config)
├── src/main/resources/                            assets (lang, models) and data (recipe, item tags)
├── src/main/templates/META-INF/neoforge.mods.toml  expanded at build time
└── src/test/java/                                 47 unit tests
```

### Items

| Item | Component | Purpose |
| --- | --- | --- |
| `morefireworks:spec_firework_rocket` | `morefireworks:rocket_spec` | a rocket with 0–7 ordered explosion stages |
| `morefireworks:spec_firework_star` | `morefireworks:star_spec` | one firework star (shape, colours, effects) |

Both items also carry a **vanilla** `minecraft:fireworks` component derived from the spec, so
vanilla tooltips, dispensers, the vanilla rocket entity and other mods keep working. Stars
additionally carry `minecraft:firework_explosion`, which vanilla's `minecraft:firework` item tint
reads, so every generated star shows its own palette in the inventory without custom rendering.

### Launching

Right-click a block to launch the rocket from the clicked face (mirroring vanilla placement), or
right-click the air to launch it straight ahead. Either way the rocket entity only carries stage 0;
the remaining stages are handed to the staged detonation engine described below.

### Crafting and assembly

Everything needed for survival play comes from two recipes, neither of which enumerates variants:

1. **Vanilla recipes build the stars.** A `minecraft:firework_star` with any dye, shape modifier,
   glowstone or diamond works as an input, and `/morefireworks star <#RRGGBB> [shape]` produces a
   star with an arbitrary 24-bit colour.
2. **`morefireworks:star_assembly` builds the rocket.** A shapeless 2×2 or 3×3 grid containing
   exactly one rocket shell (`#morefireworks:rocket_shells` — vanilla or modded) and 1–7 firework
   stars (`#morefireworks:firework_stars`) yields one spec rocket whose stages appear in **slot
   order**. Vanilla's own recipe would collect the same stars into an unordered set and fire them
   all at once, which is exactly the information this mod adds.

### Commands

| Command | Effect |
| --- | --- |
| `/morefireworks count` | prints the exact theoretical parameter space (BigInteger, 819 digits) |
| `/morefireworks random [seed]` | gives a random legal rocket; the seed makes it reproducible |
| `/morefireworks random complex [seed]` | multi-stage rocket with gradients, flicker and trail |
| `/morefireworks random extreme [seed]` | the largest legal rocket: flight 1, 7 stars, 8+8 colours, both effects |
| `/morefireworks info` | prints the canonical text form of the held item |
| `/morefireworks name` | prints the short and full names in Chinese and English |
| `/morefireworks star <#RRGGBB> [shape]` | gives a custom RGB star (any of the 16 777 216 colours) |
| `/morefireworks status` | reports how many staged detonations are currently in flight |
| `/morefireworks testlaunch [random\|complex\|extreme]` | launches a generated rocket at the command source — usable from the server console |
| `/morefireworks recipecheck` | runs the real assembly recipe against a synthetic grid and prints the result |

The last three exist so the runtime behaviour can be verified without a client: `testlaunch`
exercises the rocket entity and the staged timeline, and `recipecheck` exercises the actual
`RecipeManager` lookup instead of duplicating the recipe logic.

### Staged detonation

Vanilla detonates every explosion of a rocket on the same tick, so a rocket's explosion *order* is
never observable. Since order is part of this mod's parameter space, the mod implements explicit
sequencing (`logic/StagedDetonation`):

1. the flying rocket carries only stage 0, so flight and the first burst look exactly vanilla;
2. the remaining stages are detonated server-side, 6 ticks apart, in list order, at the position
   where the rocket went off;
3. each stage is emitted with `ServerLevel#createFireworks`, so it is purely visual.

Bounded by design: at most 64 concurrent timelines, each with at most 7 stages, each expiring
after 200 ticks even if the rocket entity never disappears.

## 4. Architecture

```
com.morefireworks
├── core/          pure Java model: StarSpec, RocketSpec, SpecCodec, RocketAssembly,
│                  combinatorics, randomiser, limits
├── naming/        the nomenclature engine (text form + translatable component form + cache)
├── logic/         VanillaBridge (spec ⇄ vanilla Fireworks), StagedDetonation
├── recipe/        StarAssemblyRecipe (shapeless, order-preserving)
├── registry/      data components, items, recipe serializer, creative tab
├── command/       /morefireworks
├── MFConfig       configurable runtime limits (COMMON config, morefireworks-common.toml)
└── MoreFireworks  mod entry point
```

The split matters: `core` and `naming` have **no Minecraft imports at all**, so the interesting
parts are testable with plain JUnit, without a game bootstrap.

### Data format

A spec is stored as one canonical string (see `core/SpecCodec`):

```
v1;<flight>;<shape>:<colors>:<fades>:<flags>|<shape>:...
```

* `shape` — `small_ball`, `large_ball`, `star`, `creeper` or `burst`
* `colors` / `fades` — comma separated lowercase `rrggbb` values, order preserved
* `flags` — `f` for flicker, `t` for trail, empty for neither

Example:

```
v1;2;star:b02e26,3c44aa:fed83d:ft|large_ball:3ab3da:f9fffe:t
```

This is **lossless** (order and duplicates are preserved), **canonical** (one spec ⇒ exactly one
string), **bounded** (rejected above 1024 characters) and **validating** (every decode goes through
the same range checks as the Java model). On the wire the same string is sent with a 16-bit length
prefix.

### Why the memory use is constant

* Two item types, two component types, one creative tab — registered once at startup.
* A stack stores one string; a full rocket is typically 40–300 bytes of NBT.
* Names are computed on demand and memoised in a 512-entry LRU cache; nothing is recomputed per
  frame, because `getName`/`appendHoverText` are only called when a tooltip is built.
* The combinatorics command evaluates closed forms, never loops over variants.

## 5. Safety limits

Structural limits are compile-time constants, because they are part of the data format — a saved
firework must keep its meaning:

| Limit | Value | Where |
| --- | --- | --- |
| Max stars per rocket | 7 / 6 / 5 for flight 1 / 2 / 3 | `RocketSpec.maxStars` |
| Max initial colours per star | 8 | `StarSpec.MAX_COLORS` |
| Max fade colours per star | 8 | `StarSpec.MAX_FADE_COLORS` |
| Max spec string length | 1024 chars | `FireworkLimits.MAX_SPEC_CHARS` |
| Name cache size | 512 entries (LRU) | `FireworkNameCache.CAPACITY` |

Runtime limits are configurable in `config/morefireworks-common.toml`
(`MFConfig`, registered as a `COMMON` config):

| Option | Default | Range | Meaning |
| --- | --- | --- | --- |
| `maxActiveTimelines` | 64 | 1–4096 | concurrent multi-stage detonations; launches beyond it keep their first stage only |
| `stageIntervalTicks` | 6 | 1–100 | ticks between two staged bursts |
| `stageTimeoutTicks` | 200 | 20–6000 | hard deadline for a staged detonation whose rocket never disappears |

Every decoded value is re-validated: null shapes, empty colour lists, out-of-range RGB values,
too many stages for the flight tier, oversized strings and malformed numbers all produce a data
error (never an exception that can escape into the game loop).

## 6. Naming

Two forms, one identity (see `docs/NAMING.md`):

* **short** — `三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭`, the item title. Deliberately lossy:
  repeated effects are merged into counts and at most three distinct colours are shown.
* **full** — `三级{1–2星形[红/蓝→黄](闪烁,拖尾)}·{3大球形[#12AB34]}复合烟花火箭`, the tooltip, one line per
  run of identical stars. Lossless, positional, merges adjacent identical stars like locants.
  The English rendering of the same spec is
  `tier I {1 star red→yellow (flicker)} {2 large ball #12AB34} compound firework rocket` — the
  parts are identical, only the spacing differs, because Chinese writes compounds closed up.

In game the tooltip is built from **translatable components**, so the client's own language setting
selects Chinese or English, and switching languages updates existing stacks immediately. Commands
and logs use the plain-text form produced by `FireworkNamer`.

## 7. Tests

`./gradlew test` runs **47 tests** across five classes:

* `SpecCodecTest` — round trips, canonical form, order sensitivity, rejection of garbage,
  oversized payloads and illegal stage counts.
* `FireworkCombinatoricsTest` — the closed forms are checked against brute-force sums, the digit
  count and scientific notation are verified, and the visual model is checked against Pascal's
  identity.
* `FireworkNamerTest` — determinism, segment/name consistency, run merging, language differences,
  documented short-name collisions, the exact Chinese short name from the specification
  (`三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭`) and English spacing.
* `RocketRandomizerTest` — seed reproducibility, legality of 1500 generated rockets, and the
  guarantee that `extreme` really is the largest legal rocket.
* `RocketAssemblyTest` — the crafting table's ordering and flight-tier clamping rules.

### What was actually built and run

| Step | Command | Result |
| --- | --- | --- |
| Compile | `gradle compileJava` | `BUILD SUCCESSFUL` |
| Test | `gradle test` | 47 tests, 0 failures, 0 errors |
| Package | `gradle build` | `build/libs/morefireworks-1.0.0+mc1.21.1.jar` (88,251 bytes, 71 entries) |
| Dedicated server | `gradle runServer` | `More Fireworks 1.0.0+mc1.21.1 (morefireworks)` loaded, `Done (0.825s)!` |
| Client | `gradle runClient` | `LWJGL version 3.3.3+5`, resource reload included `mod/morefireworks`, `Sound engine started`, no missing-model warnings |
| Client ↔ server | `gradle runClientJoin` against the running dev server | client: `Connecting to 127.0.0.1, 25565`; server: `Dev[/127.0.0.1:52279] logged in with entity id 27` and `Dev joined the game` |
| Combinatorics | RCON `morefireworks count` | `Total item variants: 1887290165…535569923`, 819 digits, `1.8873e+818` |
| Recipe | RCON `morefireworks recipecheck` | matched `morefireworks:star_assembly`, produced 2 ordered stages, item name rendered as `Tier I-1 flicker-1 trail-red/blue/yellow gradient-star-large ball compound firework rocket` |
| Staged detonation | RCON `morefireworks testlaunch extreme`, then `morefireworks status` | `7 stages`, 1 timeline active, drains back to 0; the log shows `fired stage 1/6 … 6/6` in ascending order, so the bursts really are sequential |
| Persistence | RCON `summon` an item carrying `morefireworks:rocket_spec`, `save-all`, `stop`, cold start, `data get entity` | component came back unchanged: `"morefireworks:rocket_spec": "v1;3;star:b02e26:3c44aa:ft\|large_ball:3c44aa::t"` |
| Config | server start | `run/config/morefireworks-common.toml` written with the three documented options |
| Client chat | RCON `give Dev morefireworks:spec_firework_rocket[…]` | the client printed the dynamic name in chat: `[Tier III-1 flicker-2 trail-red/blue gradient-star-large ball compound firework rocket]` |

All Gradle invocations in this table used the local Gradle 9.9.1 binary with **JDK 21**
(`JAVA_HOME` pointing at a JDK 21 install). `./gradlew` runs exactly the same tasks wherever the
wrapper distribution can be downloaded (see §2). The server used for these checks lives in `run/`,
the client in `run-client/` (both git-ignored), and everything was driven over RCON or by reading
the two log files, so the evidence above comes from real 1.21.1 processes rather than from reading
the code.

The persistence check went through a full save → shutdown → cold start cycle; the item's dynamic
name (`Tier III-1 flicker-2 trail-red/blue gradient-star-large ball compound firework rocket`) was
regenerated from the reloaded component, not stored anywhere. The only error in the client log was
`Failed to request yggdrasil public key` — expected for an offline dev client and unrelated to the
mod.

## 8. What changed in the 1.21.1 port

The parameter system, the codec, the combinatorics, the naming engine, the randomiser and the
staged-detonation design are identical to the 26.2 branch: those classes touch no Minecraft API
beyond `Component` construction. Everything that *did* change is an API call:

| Area | 26.2 | 1.21.1 |
| --- | --- | --- |
| Tooltips | `appendHoverText(ItemStack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` | `appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` — `TooltipDisplay` does not exist yet, so lines are appended to the list directly |
| Primary use | `use(...)` returns `InteractionResult` | returns `InteractionResultHolder<ItemStack>` |
| Permissions | `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` helper | no helper in 1.21.1: `.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))` |
| Recipe serializer | `RecipeSerializer` was an instantiable record-like class | `RecipeSerializer` is an **interface**, so `StarAssemblyRecipe.Serializer` is a small record exposing `codec()`/`streamCodec()` |
| Recipe plumbing | `assemble(CraftingInput)`, no `canCraftInDimensions` | `assemble(CraftingInput, HolderLookup.Provider)` plus `canCraftInDimensions(int, int)`; `CustomRecipe` also requires a `CraftingBookCategory` |
| Ingredient JSON | `Ingredient.CODEC` is a `HolderSet` codec (`"#tag"` strings) | `Ingredient.CODEC` is the classic JSON object, so the recipe uses `{"tag": "morefireworks:rocket_shells"}` |
| Item registration | `registerItem(name, factory, UnaryOperator<Item.Properties>)` | no `UnaryOperator` overload: `registerItem(name, factory, new Item.Properties()…)` |
| Item tints | `assets/<ns>/items/*.json` item model definitions carry the tints | that system does not exist in 1.21.1, so `client/MFClientSetup` registers an `ItemColor` through `RegisterColorHandlersEvent.Item`, gated with `value = Dist.CLIENT` so a dedicated server never loads it |
| Test dependency | `com.mojang:datafixerupper:10.0.21` | removed — 1.21.1 ships DFU 8.0.16 and no unit test needs it |

## 9. What this first version does not do

* **No GUI editor.** Stars and rockets are obtained from the creative tab, the assembly recipe or
  the commands. `RocketSpec`/`StarSpec` are plain records, so an editor only has to build one and
  write it back into the component.
* **No custom rendering.** Explosions are vanilla particles; the parameter order is preserved in
  data and observable through staged detonation, but colour *order* inside a single star is not
  visually distinguishable (vanilla averages it). The README never claims otherwise.
* **No JEI/REI plugin.** Adding one would mean enumerating variants, which the project explicitly
  refuses to do.
* **Sequential bursts are ~6 ticks apart** by default (configurable), so a 5-star rocket is a bit
  longer than a vanilla one. That delay is the price of making the order actually visible.

## 10. Documentation

* [`docs/COMBINATORICS.md`](docs/COMBINATORICS.md) — the full derivation, exact values and the
  distinction between *mathematical*, *generatable* and *visually distinct* variants.
* [`docs/NAMING.md`](docs/NAMING.md) — the nomenclature rules, with worked examples.
* [`docs/DESIGN.md`](docs/DESIGN.md) — architecture decisions and the vanilla-reuse trade-offs.
* [`README.zh-CN.md`](README.zh-CN.md) — the full Chinese edition of this document (完整中文版说明).

## 11. Licence

MIT. See `LICENSE`.
