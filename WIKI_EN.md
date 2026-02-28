# 📚 DeathSwap — Technical Wiki (English)

> **[Wiki Français](WIKI.md)**
>
> Complete technical documentation for the DeathSwap plugin for **Paper 1.21.11** (and compatible forks: Purpur, etc.). Tested on 1.21.11.
>
> ⚠️ **Not compatible with Spigot/Bukkit** — The plugin uses Paper's native Adventure API.

---

## 📋 Table of Contents

- [Full Commands](#-full-commands)
- [Detailed Configuration](#️-detailed-configuration)
- [Permissions](#-permissions)
- [Interface Modes (UI)](#-interface-modes)
- [Gamerules](#-gamerules)
- [Seeds & Voting](#-seeds--voting)
- [Statistics](#-statistics)
- [Challenges](#-challenges)
- [Custom Sounds](#-custom-sounds)
- [Admin Dashboard](#️-admin-dashboard)
- [Multi-Arenas](#️-multi-arenas)
- [Admin Set Properties](#-admin-set-properties)
- [Dependencies](#-dependencies)
- [Anti-Solo Protection](#️-anti-solo-protection)
- [Developer API](#️-developer-api-custom-game-modes)
- [FAQ and Troubleshooting](#-faq-and-troubleshooting)
- [Reference Configuration Files](#-reference-configuration-files)
- [License](#-license)

---

## 📋 Full Commands

### Player Commands

| Command                       | Description                                    | Permission         |
| ----------------------------- | ---------------------------------------------- | ------------------ |
| `/ds join [arena]`          | Join an arena (`default` by default)          | `deathswap.play` |
| `/ds leave`                  | Leave the current game                         | `deathswap.play` |
| `/ds stats [player]`         | Display statistics                             | `deathswap.play` |
| `/ds top [category]`         | Leaderboard (wins/kills/deaths/time/games)     | `deathswap.play` |
| `/ds vote <arena> <choice>`  | Vote for a seed                                | `deathswap.play` |
| `/ds help`                   | Main help in chat                              | `deathswap.play` |
| `/ds help gui`               | Open the visual help menu (GUI)                | `deathswap.play` |
| `/ds list`                   | List arenas and their status                   | `deathswap.play` |
| `/ds tp <player>`            | TP to a player (spectator only)                | `deathswap.play` |

### Admin Commands

| Command                                          | Description                                   | Permission          |
| ------------------------------------------------ | --------------------------------------------- | ------------------- |
| `/ds start [debug]`                             | Start game (debug = 1 player min)             | `deathswap.admin` |
| `/ds stop [arena]`                              | Stop an arena                                 | `deathswap.admin` |
| `/ds swapnow`                                   | Force an immediate swap                       | `deathswap.admin` |
| `/ds reload`                                    | Reload full configuration                     | `deathswap.admin` |
| `/ds admin gamerule <arena> set <r> <v>`        | Add/modify a gamerule                         | `deathswap.admin` |
| `/ds admin gamerule <arena> remove <r>`         | Remove a gamerule                             | `deathswap.admin` |
| `/ds admin list`                                | List arenas (GUI)                             | `deathswap.admin` |
| `/ds admin list`                                | List arenas (GUI)                             | `deathswap.admin` |
| `/ds admin create <name>`                       | Create an arena                               | `deathswap.admin` |
| `/ds admin edit <arena>`                        | Open Settings GUI for an arena                | `deathswap.admin` |
| `/ds admin delete <name>`                       | Delete an arena (confirmation required)       | `deathswap.admin` |
| `/ds admin clone <src> <dst>`                   | Clone an arena                                | `deathswap.admin` |
| `/ds admin save`                                | Save global configuration                     | `deathswap.admin` |
| `/ds admin set <arena> <prop> <val>`            | Modify an arena property                      | `deathswap.admin` |
| `/ds admin gamerule <arena> set <r> <v>`        | Add/modify a gamerule                         | `deathswap.admin` |
| `/ds admin gamerule <arena> remove <r>`         | Remove a gamerule                             | `deathswap.admin` |
| `/ds admin command <arena> tp <command>`        | Change TP command (or `default`/`none`)       | `deathswap.admin` |
| `/ds admin command <arena> reset <preset>`      | Change reset: `cwr`, `mv`, `none` or custom (`;`-separated) | `deathswap.admin` |

> **Alias:** `/deathswap` also works.

---

## ⚙️ Detailed Configuration

### `config.yml` File (Global)

The `config.yml` contains **only** global settings. Arenas are in `arenas/<id>.yml`.

```yaml
# =========================================
#   DeathSwap Configuration
# =========================================

# World players are sent to when leaving / after game ends
hub-world: "MainLobby"

# Teleport command.
teleport-command: "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"

# Commands used to reset the game world before a match.
# Empty list [] = no reset (static map).
world-reset-commands:
  - "cwr edit %world% setSeed %seed%"
  - "cwr reset %world%"

# Chat prefixes per game mode
prefixes:
  deathswap: "&8[&6DeathSwap&8]"
  deathshuffle: "&8[&dDeathShuffle&8]"
  blockshuffle: "&8[&bBlockShuffle&8]"

stats:
  enabled: true
  auto-save-minutes: 5

voting:
  enabled: true
  vote-time: 15
  options-count: 3

challenges:
  enabled: false # Default disabled as requested
  list:
    - { type: CRAFT, target: CRAFTING_TABLE, amount: 1, reward: SPEED, description: "Craft a Workbench" }
    - { type: MINE, target: COAL_ORE, amount: 3, reward: NIGHT_VISION, description: "Mine 3 Coal" }
    - { type: KILL, target: ZOMBIE, amount: 1, reward: STRENGTH, description: "Kill a Zombie" }
    - { type: CRAFT, target: FURNACE, amount: 1, reward: FASTER_DIGGING, description: "Craft a Furnace" }
    - { type: MINE, target: IRON_ORE, amount: 1, reward: RESISTANCE, description: "Find Iron" }

sounds:
  enabled: true
  game-start: { type: "ENTITY_ENDER_DRAGON_GROWL", volume: 1.0, pitch: 1.0 }
  # ... (see full file for the complete list of sounds)
```

### Arena Structure

```
plugins/DeathSwap/arenas/
├── example.yml     ← Reference only (ignored by the plugin)
├── default.yml     ← "default" arena
├── arena2.yml      ← Another arena...
└── ...
```

Each `.yml` file contains the **complete** configuration for an arena:

```yaml
game-type: DEATHSWAP        # DEATHSWAP, DEATHSHUFFLE, BLOCKSHUFFLE
game-world: "DS_Game"        # Game world
lobby-world: "DS_Lobby"      # Lobby world
min-players: 2
max-players: 20
ui-mode: RICH                # RICH or CLEAN

timers:
  load-time: 40
  swap-mode: FIXED           # FIXED or RANDOM
  swap-interval: 300         # Used if FIXED
  swap-min: 120              # Used if RANDOM
  swap-max: 420              # Used if RANDOM
  max-game-time: 1800        # 0 = unlimited
  spawn-protection: 30

round-timers:                # For DeathShuffle/BlockShuffle
  easy: 90
  medium: 70
  hard: 50

game:
  pvp-enabled: true
  nether-enabled: true
  end-enabled: true

gamerules:                   # snake_case format 1.21.11+
  keep_inventory: "false"
  natural_health_regeneration: "true"
  mob_griefing: "true"
  do_fire_tick: "true"
  show_death_messages: "true"
  announce_advancements: "true"
  immediate_respawn: "true"
  random_tick_speed: "3"

# Advanced startup options
start-if-min-players-met: false
prevent-cancel-after-countdown: false
lightning-fast-start: false

# Per-arena command overrides (optional)
# teleport-command: "..."
# world-reset-commands: [...]

seeds:
  - { seed: "123", name: "My Seed" }
```

---

## 🔐 Permissions

| Permission          | Description         | Default         |
| ------------------- | ------------------- | --------------- |
| `deathswap.play`  | Play DeathSwap      | `true` (all)  |
| `deathswap.admin` | Full admin access   | `op`          |

### Details

- **`deathswap.play`**: Grants access to `join`, `leave`, `stats`, `top`, `vote`, `help`, `help gui`, `list`, `tp`.
- **`deathswap.admin`**: Grants access to **everything**: `start`, `stop`, `swapnow`, `reload`, `settings`, `help commands`, `admin` and all sub-commands.

---

## 🎨 Interface Modes

| Mode        | BossBar | ActionBar | Chat | Titles |
| ----------- | ------- | --------- | ---- | ------ |
| **RICH**  | ✅       | ✅         | ✅    | ✅      |
| **CLEAN** | ❌       | ❌         | ✅    | ❌      |

- **RICH** (default): Immersive experience with BossBar for timers, ActionBar for real-time info, and Titles for events (swap, death, victory).
- **CLEAN**: Chat only. Ideal for lightweight servers.

Modifiable via:
- Arena file: `ui-mode: RICH`
- Settings GUI: `/ds settings` or `/ds admin edit <arena>`
- Command: `/ds admin set <arena> uimode CLEAN`

---

## 🎮 Gamerules

Minecraft gamerules are configurable **per arena**, in **snake_case** format (standard Minecraft 1.21.11+).

### Default Gamerules

| Gamerule | Value | Description |
| --- | --- | --- |
| `keep_inventory` | false | Keep inventory on death |
| `natural_health_regeneration` | true | Natural health regeneration |
| `mob_griefing` | true | Mob griefing |
| `do_fire_tick` | true | Fire tick spreading |
| `show_death_messages` | true | Show death messages |
| `announce_advancements` | true | Announce advancements |
| `immediate_respawn` | true | Immediate respawn |
| `random_tick_speed` | 3 | Random tick speed |
| `advance_time` | true | Day/night cycle (1.21.0+) |
| `advance_weather` | true | Weather cycle (1.21.0+) |

### In-Game Modification

1. **GUI**: `/ds settings` → Gamerules section → Click to toggle
2. **Set Command**: `/ds admin gamerule <arena> set <rule> <value>`
3. **Remove Command**: `/ds admin gamerule <arena> remove <rule>`

> **Important:** Keys use **snake_case** format (e.g., `keep_inventory`, not `keepInventory`).

---

## 🌱 Seeds & Voting

### Predefined Seeds

Each arena can contain a list of seeds in its config file:

```yaml
seeds:
  - { seed: "-3542283819777", name: "Temple & Village" }
  - { seed: "8490605437877207559", name: "Village & Ice Spikes" }
  - { seed: "-13377777", name: "Desert & Pyramid" }
```

### Global Seeds (`seeds.yml`)

If an arena has no local seeds configured or to enrich your games, you can configure a large list of global seeds in the `seeds.yml` file. 
These will be added to the available seed pool at startup.

```yaml
seeds:
  - seed: "8214184745"
    name: "Ruined Portal & Jungle Temple 001"
  - seed: "8554217320"
    name: "Shipwreck & Portal 001"
```

### Voting System

When `voting.enabled: true` in `config.yml`:

1. On game start, the system randomly picks `options-count` seeds
2. Players vote via GUI (click) during `vote-time` seconds
3. The winning seed is used for world generation

If no seeds are defined in the arena or globally (or if disabled), a classic random seed is generated.

> **Note:** It is possible to isolate an arena so that it **only** uses its own local seeds by enabling the `custom-arena-seed-only` option in `/ds settings`.

---

## 📊 Statistics

### Categories

| Stat            | Description                      |
| --------------- | -------------------------------- |
| `wins`        | Number of wins                   |
| `kills`       | Number of kills                  |
| `deaths`      | Number of deaths                 |
| `gamesPlayed` | Number of games played           |
| `playTime`    | Total play time (seconds)        |

### Commands

- `/ds stats` — View your own stats
- `/ds stats <player>` — View another player's stats
- `/ds top [category]` — Global leaderboard

### Storage

- YAML file per player in `plugins/DeathSwap/stats/`
- Auto-save every `stats.auto-save-minutes` minutes
- Saves on player disconnect and plugin shutdown

---

## 🎯 Challenges

> **DeathSwap mode only**

### Challenge Types

| Type    | Description    | Example target           |
| ------- | -------------- | ------------------------ |
| `CRAFT` | Craft an item  | `DIAMOND_PICKAXE`       |
| `MINE`  | Mine a block   | `DIAMOND_ORE`           |
| `KILL`  | Kill a mob     | `ENDERMAN`              |

### Rewards (potion effects)

| Effect        | Config name         |
| ------------- | ------------------- |
| Speed         | `SPEED`           |
| Strength      | `STRENGTH`        |
| Night Vision  | `NIGHT_VISION`    |
| Resistance    | `RESISTANCE`      |
| Haste         | `FASTER_DIGGING`  |

### Example Config

```yaml
challenges:
  enabled: true
  list:
    - { type: CRAFT, target: DIAMOND_PICKAXE, amount: 1, reward: STRENGTH, description: "Craft a diamond pickaxe" }
    - { type: MINE, target: DIAMOND_ORE, amount: 5, reward: SPEED, description: "Mine 5 diamonds" }
    - { type: KILL, target: ENDERMAN, amount: 3, reward: NIGHT_VISION, description: "Kill 3 endermen" }
```

---

## 🔊 Custom Sounds

Every game event can have a custom sound. The full list of Bukkit sounds is available on the [Spigot documentation](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html).

### Sound Events

| Config key             | Event                  |
| ---------------------- | ---------------------- |
| `game-start`         | Game start             |
| `countdown-tick`     | Countdown tick         |
| `countdown-go`       | Go! (start)            |
| `swap`               | Player swap            |
| `shuffle`            | New round              |
| `death`              | Player death           |
| `win`                | Victory                |
| `round-success`      | Round passed           |
| `round-fail`         | Round failed           |
| `challenge-complete` | Challenge completed    |
| `vote-cast`          | Vote registered        |

### Customization Format

```yaml
sounds:
  swap: { type: "ENTITY_GHAST_SCREAM", volume: 0.5, pitch: 1.5 }
```

- **`volume`**: from `0.0` (silent) to `2.0` (loud)
- **`pitch`**: from `0.5` (low) to `2.0` (high). `1.0` = normal

---

## 🛠️ Admin Dashboard

The Admin Dashboard is a GUI system accessible via `/ds admin`. It provides a complete graphical interface for managing arenas and players.

### Access

- **Command:** `/ds admin`
- **Permission required:** `deathswap.admin`
- **Player only** (not console)

### Dashboard Pages

#### 1. Main Page (Arena List)

| Element | Action |
|---------|--------|
| **Arena item** (colored concrete) | Shows status and player count |
| **Left-click** on arena | → Opens arena details |
| **Right-click** on arena | → Teleport to arena lobby |
| **Shift+Click** on arena | → Opens Settings GUI |
| **Nether Star** | Reload full configuration |
| **Barrier** | Close GUI |

### 🛠️ In-Game Configuration (Settings GUI)

Accessible via **Shift+Click** on an arena in the Dashboard, or via `/ds settings` / `/ds admin edit <arena>`.
This menu allows modifying **all** arena settings without touching files:

- **Worlds**: Change Lobby and Game world (keyboard input via chat)
- **Game Mode**: Switch between DeathSwap, DeathShuffle, BlockShuffle
- **Gamerules**: Enable/Disable rules (keep_inventory, etc.)
- **Timers**: Adjust swap times, max game time, etc.
- **Commands**: Configure TP and Reset commands
- **Resilience**: Enable robust start options

**Status Colors:**

| Color | State |
|-------|-------|
| 🟡 Yellow | `WAITING` — Waiting for players |
| 🟢 Light green | `STARTING` — World loading & players are invulnerable |
| 🟩 Green | `RUNNING` — Game in progress |
| 🔴 Red | `ENDED` — Game finished |
| ⬛ Barrier | `DISABLED` — Arena disabled |

#### 2. Arena Details

| Button | Action |
|--------|--------|
| ⚔️ **Force Start** | Start game immediately (if WAITING/STARTING) |
| 🛑 **Force Stop** | Stop the game (if RUNNING) |
| 💥 **Regenerate World** | Reset the world → **⚠ Confirmation required** |
| 👥 **Manage Players** | → Opens player list |
| ⬅️ **Back** | → Return to main page |

#### 3. Player List

Displays all players in the arena with their **head**, **health** (❤) and **hunger** (🍗).

| Action | Result |
|--------|--------|
| **Left-click** on a player | → Opens actions for that player |
| **⬅️ Back** | → Return to arena details |

#### 4. Player Actions

| Button | Action | Description |
|--------|--------|-------------|
| 🔮 **Teleport** | TP admin to player | Direct teleportation |
| 📦 **View Inventory** | Opens player inventory | View/modify inventory |
| 👢 **Kick from Arena** | Send to hub | Remove from arena |
| ⛔ **Ban** | Kick + `/ban` | → **⚠ Confirmation required** |
| ⬅️ **Back** | — | Return to player list |

#### 5. Confirmation Screen

Destructive actions (**Regenerate World** and **Ban**) go through a confirmation screen.

| Slot | Item | Action |
|------|------|--------|
| 11 | 🟩 **Green Wool** | **Cancel** — return |
| 13 | 💥 **TNT** | Info: description + ⚠ "This action is irreversible!" |
| 15 | 🟥 **Red Wool** | **Confirm** — executes action |

---

## 🏟️ Multi-Arenas

### Concept

Each arena is an **independent instance** with:
- Its own game world and lobby (including configurable Nether and End dimensions)
- Its own complete configuration
- Its own players and game

### Adding an Arena

**Method 1: Via file**

1. Copy `plugins/DeathSwap/arenas/example.yml` → `plugins/DeathSwap/arenas/myarena.yml`
2. Edit the values (worlds, timers, mode, etc.)
3. `/ds reload`
4. Join: `/ds join myarena`

**Method 2: Via command**

1. `/ds admin create myarena` — Creates with default values
2. `/ds admin edit myarena` — Opens GUI to configure
3. Or via `/ds admin set myarena <prop> <val>`

**Method 3: Via cloning**

1. `/ds admin clone default myarena` — Copies an existing arena
2. Modify if needed via GUI or commands

### Deleting an Arena

- **Command:** `/ds admin delete myarena` (with confirmation)
- **Manually:** Delete the file `arenas/myarena.yml` then `/ds reload`

---

## 📝 Admin Set Properties

The `/ds admin set <arena> <property> <value>` command supports the following properties:

| Property          | Type    | Description                        | Example                               |
| ----------------- | ------- | ---------------------------------- | ------------------------------------- |
| `lobby`         | String  | Lobby world                        | `/ds admin set default lobby DS_Lobby` |
| `game`          | String  | Game world                         | `/ds admin set default game DS_Game` |
| `gametype`      | Enum    | Game mode                          | `DEATHSWAP`, `DEATHSHUFFLE`, `BLOCKSHUFFLE` |
| `minplayers`    | Int     | Minimum players                    | `2`                                   |
| `maxplayers`    | Int     | Maximum players                    | `20`                                  |
| `uimode`        | Enum    | Interface mode                     | `RICH` or `CLEAN`                    |
| `loadtime`      | Int     | Load time (seconds)                | `40`                                  |
| `swapmode`      | Enum    | Swap mode                          | `FIXED` or `RANDOM`                  |
| `swapinterval`  | Int     | Fixed swap interval (sec)          | `300`                                 |
| `swapmin`       | Int     | Random swap min (sec)              | `120`                                 |
| `swapmax`       | Int     | Random swap max (sec)              | `420`                                 |
| `maxgametime`   | Int     | Max game duration (sec)            | `1800`                                |
| `spawnprotection` | Int   | Spawn protection (sec)             | `30`                                  |
| `roundtimeeasy` | Int     | Easy round time (sec)              | `90`                                  |
| `roundtimemedium` | Int   | Medium round time (sec)            | `70`                                  |
| `roundtimehard` | Int     | Hard round time (sec)              | `50`                                  |
| `pvp`           | Boolean | PvP enabled                        | `true` / `false`                     |
| `nether`        | Boolean | Nether enabled                     | `true` / `false`                     |
| `end`           | Boolean | End enabled                        | `true` / `false`                     |
| `resilience`    | Boolean | Enable both robust start options   | `true` / `false`                     |

---

## 🔧 Dependencies

### Multiverse-Core (recommended)

Used for:
- Creating and loading worlds (`mv create`, `mv load`)
- Teleporting players (`mv tp`)
- Managing lobby and game worlds

> The plugin can work without Multiverse if you configure an alternative TP command via `teleport-command`.

### CyberWorldReset (optional)

Used for:
- Regenerating game worlds between games (`cwr reset`)
- Available from the "Regenerate" button in Admin Dashboard

> Not needed for static maps (set `world-reset-commands: []`).

### CyberWorldReset Configuration

1. Install CyberWorldReset
2. Add the game world and its dimensions with their options.

Here are the recommended configuration files for CWR to work optimally with DeathSwap:

**`plugins/CyberWorldReset/config.yml`**
```yaml
config:
  confirmation:
    enabled: false
    seconds: 15
  save-world-before-reset: false
  loading-type: ULTRA-FAST
  loading-radius: 0
  timer-load-delay: 1
  world-reset-delay: 10
  recursive-teleporting:
    enabled: false
    milliseconds: 150
  detailed-messages: false
  fix-suffocation-teleport-1_8-1_9: false
  fix-suffocation-on-join: true
  hooks:
    world-guard-delete: false
    save-cmi-warps: false
    refresh-cmi-portals: false
    refresh-mv-portals: false
  auto-update-configs:
    config: true
    lang: true
  lang: en
```

**`plugins/CyberWorldReset/worlds.yml`**
```yaml
worlds:
  # Main World
  DeathSwap_Game:
    enabled: true
    last-saved: false
    settings:
      time: []
      message: '&8[&6DeathSwap&8] &aThe map has been successfully regenerated!'
      seed: RANDOM
      environment: DEFAULT
      generator: DEFAULT
      safe-world:
        enabled: true
        world: MainLobby
        delay: 5 # Small delay to let the server breathe
        spawn: DEFAULT
      warning:
        enabled: true
        message: '&cWarning: resetting world {world} in {time}.'
        time: [5, 1]
        title:
          title: Reset in progress
          sub-title: Please wait...
          fade: [10, 40, 10]

  # Nether World
  DeathSwap_Game_nether:
    enabled: true
    last-saved: false
    settings:
      time: []
      message: '&8[&6DeathSwap&8] &cNether reset!'
      seed: RANDOM
      environment: NETHER
      generator: DEFAULT
      safe-world:
        enabled: true
        world: MainLobby
        delay: 0 # Instant reset
        spawn: DEFAULT
      warning:
        enabled: false # Handled by the main world
      commands: []

  # End World
  DeathSwap_Game_the_end:
    enabled: true
    last-saved: false
    settings:
      time: []
      message: '&8[&6DeathSwap&8] &5The End has been reset!'
      seed: RANDOM
      environment: THE_END
      generator: DEFAULT
      safe-world:
        enabled: true
        world: MainLobby
        delay: 0 # Instant reset
        spawn: DEFAULT
      warning:
        enabled: false
      commands: []
```

---

## ❓ FAQ and Troubleshooting

### Plugin does not start

- Check that Java 21+ is installed (`java -version`)
- Check that **Paper 1.21.11** (or a fork like Purpur) is used — Spigot/Bukkit are **not** supported
- Check console logs for errors

### Worlds do not load

- Check that **Multiverse-Core** is installed and active
- Check world names in `arenas/*.yml` files match those in Multiverse
- Use `/mv list` to see available worlds

### Swap does not work

- Check that `timers.swap-mode` is `FIXED` or `RANDOM`
- In `RANDOM` mode, check that `swap-min` < `swap-max`
- Check that there are at least 2 players alive

### Stats are not saving

- Check that `stats.enabled: true` in `config.yml`
- Check write permissions for `plugins/DeathSwap/stats/` folder

### Regeneration does not work

- Check that **CyberWorldReset** is installed
- Check that the world is added: `/cwr add <world_name>`
- Regeneration does not work if a game is running

### Sounds are not playing

- Check that `sounds.enabled: true`
- Check that sound names are valid (see Spigot docs)
- Minecraft client volume must be enabled

### Cannot join an arena

- Arena might be full (`max-players` reached)
- You might already be in an arena (`/ds leave` first)
- Arena doesn't exist (check name with `/ds list`)

### Gamerules not applying

- Check that keys use **snake_case** format (`keep_inventory`, not `keepInventory`)
- Gamerules are only applied at game start

---

## 📂 Reference Configuration Files

### Default `config.yml`

```yaml
# =========================================
#   DeathSwap Global Configuration
# =========================================

hub-world: "MainLobby"

teleport-command: "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"

world-reset-commands:
  - "cwr edit %world% setSeed %seed%"
  - "cwr reset %world%"

prefixes:
  deathswap: "§8[§6DeathSwap§8]"
  deathshuffle: "§8[§dDeathShuffle§8]"
  blockshuffle: "§8[§bBlockShuffle§8]"

stats:
  enabled: true
  auto-save-minutes: 5

voting:
  enabled: true
  vote-time: 15
  options-count: 3
  vote-time-per-arena: true

challenges:
  enabled: false
  list:
    - { type: CRAFT, target: CRAFTING_TABLE, amount: 1, reward: SPEED, description: "Craft a Workbench" }
    - { type: MINE, target: COAL_ORE, amount: 3, reward: NIGHT_VISION, description: "Mine 3 coal ores" }
    - { type: KILL, target: ZOMBIE, amount: 1, reward: STRENGTH, description: "Kill a zombie" }

sounds:
  enabled: true
  game-start: { type: "ENTITY_ENDER_DRAGON_GROWL", volume: 1.0, pitch: 1.0 }
  countdown-tick: { type: "BLOCK_NOTE_BLOCK_HAT", volume: 1.0, pitch: 1.0 }
  countdown-go: { type: "ENTITY_EXPERIENCE_ORB_PICKUP", volume: 1.0, pitch: 1.2 }
  swap: { type: "ENTITY_ENDERMAN_TELEPORT", volume: 1.0, pitch: 1.0 }
  shuffle: { type: "BLOCK_NOTE_BLOCK_CHIME", volume: 1.0, pitch: 1.5 }
  death: { type: "ENTITY_WITHER_DEATH", volume: 0.5, pitch: 1.0 }
  win: { type: "UI_TOAST_CHALLENGE_COMPLETE", volume: 1.0, pitch: 1.0 }
  round-success: { type: "ENTITY_PLAYER_LEVELUP", volume: 1.0, pitch: 1.5 }
  round-fail: { type: "ENTITY_VILLAGER_NO", volume: 1.0, pitch: 0.8 }
  challenge-complete: { type: "ENTITY_PLAYER_LEVELUP", volume: 1.0, pitch: 1.5 }
  vote-cast: { type: "ENTITY_UI_BUTTON_CLICK", volume: 1.0, pitch: 2.0 }
```

### Example Arena (`arenas/example.yml`)

> **Note:** Copy this file to create new arenas (e.g., `default.yml`). The plugin ignores `example.yml`.

```yaml
game-type: DEATHSWAP
game-world: "example_Game"
game-world-nether: "example_Game_nether"
game-world-end: "example_Game_the_end"
lobby-world: "example_Lobby"
min-players: 2
max-players: 20
ui-mode: RICH

timers:
  load-time: 40
  swap-mode: FIXED
  swap-interval: 300
  swap-min: 120
  swap-max: 420
  max-game-time: 1800
  spawn-protection: 30

round-timers:
  easy: 90
  medium: 70
  hard: 50

game:
  pvp-enabled: true
  nether-enabled: false
  end-enabled: false
  world-load-enabled: true
  world-unload-enabled: false
  world-load-command: "mv load %world%"
  world-unload-command: "mv unload %world%"

gamerules:
  keep_inventory: "false"
  natural_health_regeneration: "true"
  mob_griefing: "true"
  show_death_messages: "true"
  announce_advancements: "true"
  immediate_respawn: "true"
  random_tick_speed: "3"

# Advanced startup options
start-if-min-players-met: false
prevent-cancel-after-countdown: false
lightning-fast-start: false

seeds: []
```

### Game Modes

#### `modes/deathshuffle.yml`

```yaml
name: "Death Shuffle"
description: "Swap positions and try to kill your opponent!"
authors: ["DualsFWShield"]
version: "1.0"

swap-time-min: 30
swap-time-max: 120
health: 20.0
pvp: true
kill-on-swap: false

causes:
  DROWNING:
    enabled: true
    difficulty: 1
  FALL:
    enabled: true
    difficulty: 1
  # ... etc (Up to 29 configurable causes)
```

#### `modes/blockshuffle.yml`

```yaml
name: "Block Shuffle"
description: "Stand on specific blocks to score points!"
authors: ["DualsFWShield"]
version: "1.0"

swap-time: 300
health: 20.0
pvp: false

blocks:
  STONE:
    enabled: true
    difficulty: 1

  CRAFTING_TABLE:
    enabled: true
    difficulty: 1

  # ... etc (Around 80 configurable items by default)
```

### ⚙️ Configuration Interfaces (GUIs)

*Shuffles* modes now feature advanced options available via the **"Configure Mode"** button inside the `/ds settings` GUI:

1. **Item Race** (BlockShuffle only): The first player to find/craft the item wins the round (others fail).
2. **Death Run** (DeathShuffle only): The first player to die from the specified cause wins the round (others fail).
3. **Unique Targets/Causes**: Assigns a different block/cause to each player for the current round.
4. **Pool Configuration**: Edit targets (Blocks/Causes) directly in-game using the paginated menu (Left click to toggle ON/OFF, Right click to change difficulty).
```

---

## 🛡️ Anti-Solo Protection

To prevent a game from running indefinitely when a player is left alone (opponent forfeit or disconnect), a software protection has been implemented:

1. At startup, if the arena contains **less than 2 players**, the countdown is automatically cancelled.
2. Mid-game, if the number of alive players drops to `1`, the remaining player instantly wins and the game gracefully ends, returning them to the Hub.
3. *Exception:* Activating **`debug-mode`** in the arena settings (`/ds settings` GUI) bypasses this security and allows starting or playing a game alone (useful for testing Shuffle blocks).

---

## 🛠️ Developer API (Custom Game Modes)

DeathSwap now offers an API that allows any third-party developer to create and register their own minigames! You can access it via the `DeathSwapAPI` class.

### How to register a new game mode?

1. Ensure you have `DeathSwap` as a dependency (depend or softdepend) in your `plugin.yml`.
2. Create a class extending `GameInstance` containing your game logic.
3. Register your Factory with the API during server startup:

```java
import be.dualsfwshield.deathswap.api.DeathSwapAPI;

public class MyAddon extends JavaPlugin {
    @Override
    public void onEnable() {
        // Registering the new game mode
        DeathSwapAPI.registerMode(
            "MY_MODE",            // Internal ID
            "My Awesome Mode",    // Display name in GUIs
            "&8[&aMyMode&8]",     // Default chat prefix
            
            // Factory returning your class instance
            MyAwesomeModeInstance::new
        );
    }
}
```

Once this is called, players will be able to use your mode exactly like any built-in mode, server admins will see it inside config GUIs, and `/ds start` will work natively.

---

## 📝 Release Notes

### v1.0.0
- 🎮 3 game modes (DeathSwap, DeathShuffle, BlockShuffle)
- 🏟️ Multi-arena support (individual files in `arenas/`)
- 🎛️ Complete Admin Dashboard with GUI
- 📊 Statistics and leaderboards
- 🗳️ Seed voting system
- 🎯 Challenges with rewards
- 🔊 Customizable sounds
- 🎨 Two UI modes (RICH / CLEAN)
- ⚙️ In-game configurable gamerules (snake_case)
- 🌍 Configurable world regeneration
- ⚠️ Confirmation screen for destructive actions
- 🌐 French and English support

---

*Documentation for DeathSwap v1.0.0 — Plugin by [DualsFWShield](https://dualsfwshield.be)*

---

## 📜 License

This project is under a **custom license**.
See the [LICENSE.md](LICENSE.md) file for full details.

* **Usage and Modification**: Free (private or public).
* **Redistribution**: Allowed with mandatory credit.
* **Commercial Use**: Strictly prohibited without prior agreement.
