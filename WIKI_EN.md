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
| `/ds settings`                                  | Open Settings GUI for current arena           | `deathswap.admin` |
| `/ds help commands`                             | Display admin commands in chat                | `deathswap.admin` |
| `/ds admin`                                     | Open the Admin Dashboard (GUI)                | `deathswap.admin` |
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
# World players are sent to after game/kick
hub-world: "MainLobby"

# TP command. Placeholders: %player%, %world%, %x%, %y%, %z%, %yaw%, %pitch%
teleport-command: "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"

# Reset commands. Placeholders: %world%, %seed%. Empty [] = no reset.
world-reset-commands:
  - "cwr edit %world% setSeed %seed%"
  - "cwr reset %world%"

# Chat prefixes
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

challenges:
  enabled: false
  list:
    - { type: CRAFT, target: CRAFTING_TABLE, amount: 1, reward: SPEED, description: "..." }

sounds:
  enabled: true
  game-start: { type: "ENTITY_ENDER_DRAGON_GROWL", volume: 1.0, pitch: 1.0 }
  # ... (see README for full list)
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

round-timers:                # DeathShuffle/BlockShuffle
  easy: 90
  medium: 70
  hard: 50

game:
  pvp-enabled: true
  nether-enabled: true
  end-enabled: true

gamerules:                   # snake_case format
  keep_inventory: "false"
  immediate_respawn: "true"
  # ... (see README for full list)

# Advanced options (optional)
start-if-min-players-met: false
prevent-cancel-after-countdown: false

# Per-arena command overrides (optional, null = use global)
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

| Gamerule                | Value  | Description                    |
| ----------------------- | ------ | ------------------------------ |
| `keep_inventory`      | false  | Keep inventory on death        |
| `immediate_respawn`   | true   | Immediate respawn              |
| `do_daylight_cycle`   | true   | Day/night cycle                |
| `do_weather_cycle`    | true   | Weather cycle                  |
| `mob_griefing`        | true   | Mob griefing                   |
| `natural_regeneration`| true   | Natural health regeneration    |
| `do_mob_spawning`     | true   | Mob spawning                   |
| `send_command_feedback`| false | Command feedback               |
| `log_admin_commands`  | false  | Log admin commands             |
| `spawn_radius`        | 0      | Spawn radius                   |

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

### Voting System

When `voting.enabled: true` in `config.yml`:

1. On game start, the system randomly picks `options-count` seeds
2. Players vote via GUI (click) during `vote-time` seconds
3. The winning seed is used for world generation

If no seeds are defined, a random seed is used.

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
- Its own game world and lobby
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
2. Add the game world: `/cwr add DeathSwap_Game`
3. The `cwr reset <world>` command will be used automatically

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
  nether-enabled: true
  end-enabled: true

gamerules:
  keep_inventory: "false"
  immediate_respawn: "true"
  do_daylight_cycle: "true"
  do_weather_cycle: "true"
  mob_griefing: "true"
  natural_regeneration: "true"
  do_mob_spawning: "true"
  send_command_feedback: "false"
  log_admin_commands: "false"
  spawn_radius: "0"
  random_tick_speed: "3"
  announce_advancements: "true"

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
  - DROWNING
  - FALL
  - FIRE
  - CONTACT
  - STARVATION
  - SUFFOCATION
  - LAVA
  - EXPLOSION
  - PROJECTILE
  - MAGIC
  - HOT_FLOOR
  - FREEZE
  - LIGHTNING
  - FLY_INTO_WALL
  - FALLING_BLOCK
  - VOID
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
  - STONE
  - DIRT
  - GRASS_BLOCK
  - OAK_LOG
  - SAND
  - GRAVEL
  - COBBLESTONE
  - CRAFTING_TABLE
  - FURNACE
  - CHEST
  - WATER
  - LAVA
```

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
