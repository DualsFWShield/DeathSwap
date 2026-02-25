# 🎮 DeathSwap

**A professional DeathSwap minigame plugin** for Paper 1.21.11 with 3 game modes, multi-arenas, admin dashboard, and full customization.

> ⚠️ **Paper 1.21.11 required** (or compatible forks like Purpur). Not compatible with Spigot/Bukkit.

---

## ✨ Features

🕹️ **3 Game Modes** — DeathSwap, DeathShuffle, BlockShuffle
🏟️ **Multi-Arenas** — Independent instances with their own worlds, configs, and players
🎛️ **Admin Dashboard** — Full GUI to manage arenas, players, gamerules, and settings
📊 **Statistics** — Kills, deaths, wins, play time with leaderboards
🗳️ **Seed Voting** — Players vote for predefined seeds before each game
🎯 **Challenges** — Craft, mine, and kill objectives with potion effect rewards
🔊 **Custom Sounds** — Every event sound is fully configurable
🎨 **Two UI Modes** — RICH (BossBar + ActionBar) or CLEAN (chat only)
⚙️ **In-Game Config** — Modify everything via GUI or commands, no file editing needed
🛠️ **Custom GameMode API** — Developers can register their own modes using the DeathSwapAPI
🌐 **Bilingual** — French and English support

---

## 🕹️ Game Modes

| Mode | How it works |
|------|-------------|
| **DeathSwap** | Players trappers are randomly swapped — trap your area and survive your opponent's trap! |
| **DeathShuffle** | Each round assigns a death type. Die the right way to survive! Configurable causes via in-game GUI, includes "Death Run". |
| **BlockShuffle** | Find and stand on the target block before time runs out! Configurable blocks via in-game GUI, includes "Item Race". |

---

## 📋 Quick Start

1. Drop the JAR in `plugins/`
2. Create worlds (Multiverse recommended): `mv create DS_Game normal` + `mv create DS_Lobby normal`
3. Copy `arenas/example.yml` → `arenas/default.yml`, edit your world names
4. `/ds reload` then `/ds join`

---

## 📋 Commands

| Command | Description |
|---------|-------------|
| `/ds join [arena]` | Join an arena |
| `/ds leave` | Leave current game |
| `/ds stats` | View stats |
| `/ds top [category]` | Leaderboard |
| `/ds help gui` | Visual help menu |
| `/ds admin` | Admin dashboard (GUI) |
| `/ds admin create/delete/clone` | Manage arenas |
| `/ds settings` | In-game settings GUI |
| `/ds reload` | Reload config |

---

## ⚙️ Requirements

| Component | Version | Status |
|-----------|---------|--------|
| Java | 21+ | ✅ Required |
| Paper (or fork) | 1.21.11 | ✅ Required |
| Multiverse-Core | 4.x | ⬜ Recommended |
| CyberWorldReset | * | ⬜ Optional |

---

## 📚 Documentation

For complete documentation including all commands, configuration reference, admin dashboard guide, and troubleshooting:

📖 **[Full Documentation (English)](https://github.com/DualsFWShield/DeathSwap/blob/main/WIKI_EN.md)**

📖 **[Documentation Complète (Français)](https://github.com/DualsFWShield/DeathSwap/blob/main/WIKI.md)**

📄 **[README (English)](https://github.com/DualsFWShield/DeathSwap/blob/main/README_EN.md)**

📄 **[README (Français)](https://github.com/DualsFWShield/DeathSwap/blob/main/README.md)**

---

## 📝 License

Custom license — free to use and modify, redistribution with credit, no commercial use without agreement. See [LICENSE.md](https://github.com/DualsFWShield/DeathSwap/blob/main/LICENSE.md).

---

*Made with ❤️ by [DualsFWShield](https://dualsfwshield.be)*
