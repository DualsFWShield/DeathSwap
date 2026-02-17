# 🎮 DeathSwap Plugin

> **Un plugin Minecraft professionnel** pour Paper 1.21+ avec 3 modes de jeu, multi-arènes, dashboard admin et personnalisation complète.

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![Paper 1.21](https://img.shields.io/badge/Paper-1.21+-blue)](https://papermc.io)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## ✨ Fonctionnalités

### 🕹️ 3 Modes de Jeu
| Mode | Description |
|------|-------------|
| **DeathSwap** | Les joueurs sont échangés aléatoirement. Piège la zone avant le swap ! |
| **DeathShuffle** | Chaque round, un type de mort est assigné. Meurt de la bonne façon pour survivre ! |
| **BlockShuffle** | Trouve et tiens-toi sur le bon bloc avant la fin du timer ! |

### 🏟️ Multi-Arènes
- Chaque arène est **indépendante** (monde, joueurs, config, timers)
- Plusieurs parties simultanées possibles
- Configuration par arène via `config.yml`

### 🎛️ Dashboard Admin (`/ds admin`)
- Vue d'ensemble de toutes les arènes
- Force start/stop de jeux
- Régénération de monde Multiverse
- Gestion des joueurs (kick, ban, teleport, inventaire)

### 🔧 Personnalisation Profonde
- **UI Mode** : RICH (BossBar + ActionBar) ou CLEAN (chat uniquement)
- **Gamerules** : Configurable en jeu via GUI
- **Sons** : Chaque événement sonore est configurable
- **Seeds** : Système de vote avec seeds prédéfinies
- **Challenges** : Craft, mine, kill avec récompenses (DeathSwap)

### 📊 Statistiques
- Kills, morts, victoires, temps de jeu, parties jouées
- Leaderboards par catégorie (`/ds top`)
- Sauvegarde auto en YAML

---

## 📥 Installation

### Prérequis
| Composant | Version | Requis |
|-----------|---------|--------|
| Java | 21+ | ✅ |
| Paper | 1.21+ | ✅ |
| Multiverse-Core | 4.x | ✅ |
| CyberWorldReset | * | ⬜ Optionnel |

### Installation
```bash
# 1. Build le plugin
mvn clean package

# 2. Copie le JAR dans le serveur
cp target/deathswap-1.0.0.jar /chemin/serveur/plugins/

# 3. Redémarre le serveur
# Le fichier config.yml sera généré automatiquement
```

### Configuration rapide
1. Crée un monde lobby via Multiverse : `mv create DS_WaitingLobby normal`
2. Crée un monde de jeu : `mv create DeathSwap_Game normal`
3. Configure `plugins/DeathSwap/config.yml` (voir ci-dessous)
4. `/ds reload` pour appliquer

---

## 📋 Commandes

### Joueur
| Commande | Description | Permission |
|----------|-------------|------------|
| `/ds join [arène]` | Rejoindre une arène (défaut: `default`) | `deathswap.play` |
| `/ds leave` | Quitter la partie en cours | `deathswap.play` |
| `/ds stats [joueur]` | Voir les statistiques | `deathswap.play` |
| `/ds top [catégorie]` | Classement (wins/kills/deaths/time/games) | `deathswap.play` |
| `/ds vote <arène> <choix>` | Voter pour un seed | `deathswap.play` |
| `/ds list` | Liste des arènes et leur statut | `deathswap.play` |
| `/ds tp <joueur>` | TP vers un joueur (spectateur uniquement) | `deathswap.play` |

### Admin
| Commande | Description | Permission |
|----------|-------------|------------|
| `/ds start [debug]` | Lancer le jeu (debug = 1 joueur min) | `deathswap.admin` |
| `/ds stop [arène]` | Arrêter une arène | `deathswap.admin` |
| `/ds swapnow` | Forcer un swap immédiat | `deathswap.admin` |
| `/ds reload` | Recharger la configuration | `deathswap.admin` |
| `/ds admin` | Ouvrir le Dashboard Admin (GUI) | `deathswap.admin` |

---

## ⚙️ Configuration (`config.yml`)

<details>
<summary><b>📂 Voir la configuration complète commentée</b></summary>

```yaml
# =========================================
#   DeathSwap Configuration
# =========================================

# Monde hub (retour après game/kick)
hub-world: "MainLobby"

# Préfixes chat par mode de jeu
prefixes:
  deathswap: "&8[&6DeathSwap&8]"
  deathshuffle: "&8[&dDeathShuffle&8]"
  blockshuffle: "&8[&bBlockShuffle&8]"

# =========================================
#   Fonctionnalités & Toggles
# =========================================

stats:
  enabled: true           # Activer le système de statistiques
  auto-save-minutes: 5    # Intervalle de sauvegarde auto (minutes)

voting:
  enabled: true           # Activer le vote de seed
  vote-time: 15           # Durée du vote en secondes
  options-count: 3        # Nombre de choix proposés

challenges:
  enabled: false          # Activer les challenges (DeathSwap uniquement)
  list:
    - { type: CRAFT, target: CRAFTING_TABLE, amount: 1, reward: SPEED, description: "Craft une table de craft" }
    - { type: MINE, target: COAL_ORE, amount: 3, reward: NIGHT_VISION, description: "Mine 3 charbons" }
    - { type: KILL, target: ZOMBIE, amount: 1, reward: STRENGTH, description: "Tue un zombie" }

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

# =========================================
#   Arènes (chaque arène = partie indépendante)
# =========================================
arenas:
  default:
    # --- Mode de jeu ---
    # DEATHSWAP, DEATHSHUFFLE, BLOCKSHUFFLE
    game-type: DEATHSWAP

    # --- Mondes (Multiverse) ---
    game-world: "DeathSwap_Game"
    lobby-world: "DS_WaitingLobby"

    # --- Limites de joueurs ---
    min-players: 2
    max-players: 20

    # --- Mode d'interface ---
    # RICH = BossBar + ActionBar | CLEAN = Chat uniquement
    ui-mode: RICH

    # --- Timers (secondes) ---
    timers:
      load-time: 40              # Temps de chargement du monde
      swap-mode: FIXED           # FIXED ou RANDOM
      swap-interval: 300         # Mode FIXED : intervalle exact (sec)
      swap-min: 120              # Mode RANDOM : intervalle minimum
      swap-max: 420              # Mode RANDOM : intervalle maximum
      max-game-time: 1800        # Durée max de la partie (30 min)
      spawn-protection: 30       # Invulnérabilité au début (sec)

    # --- Timers par round (DeathShuffle / BlockShuffle) ---
    round-timers:
      easy: 90
      medium: 70
      hard: 50

    # --- Règles de jeu ---
    game:
      pvp-enabled: true
      nether-enabled: false
      end-enabled: false

    # --- Gamerules Minecraft ---
    gamerules:
      keepInventory: "false"
      immediateRespawn: "true"
      doDaylightCycle: "true"
      doWeatherCycle: "true"
      mobGriefing: "true"
      naturalRegeneration: "true"
      doMobSpawning: "true"

    # --- Seeds prédéfinies ---
    seeds:
      - { seed: "-3542283819777", name: "Temple & Village" }
      - { seed: "8490605437877207559", name: "Village & Ice Spikes" }
      - { seed: "-13377777", name: "Désert & Pyramide" }
      - { seed: "123456789", name: "Île de survie" }
      - { seed: "-69420", name: "Manoir" }
```

</details>

### 🏟️ Ajouter une arène

Pour ajouter une deuxième arène, duplique le bloc sous `arenas:` :

```yaml
arenas:
  default:
    game-type: DEATHSWAP
    game-world: "DeathSwap_Game"
    lobby-world: "DS_WaitingLobby"
    # ...

  arena2:
    game-type: DEATHSHUFFLE
    game-world: "DS_Game_2"
    lobby-world: "DS_Lobby_2"
    min-players: 2
    max-players: 10
    ui-mode: CLEAN
    timers:
      load-time: 30
      swap-mode: RANDOM
      swap-min: 60
      swap-max: 180
    round-timers:
      easy: 120
      medium: 90
      hard: 60
    game:
      pvp-enabled: false
      nether-enabled: true
    seeds:
      - { seed: "42", name: "The Answer" }
```

Puis crée les mondes via Multiverse : `mv create DS_Game_2 normal` et `mv create DS_Lobby_2 normal`.

---

## 🔐 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `deathswap.play` | Jouer au DeathSwap | `true` (tous) |
| `deathswap.admin` | Accès admin complet | `op` |

---

## 🎨 Modes d'Interface (UI Modes)

### RICH (défaut)
- **BossBar** pour le timer de swap/round
- **ActionBar** pour les infos en temps réel
- Titres visuels pour les events

### CLEAN
- Informations uniquement en **chat**
- Idéal pour les serveurs légers ou les joueurs qui préfèrent moins de HUD

Modifiable via :
- `config.yml` → `ui-mode: RICH` ou `CLEAN`
- In-game → GUI Settings (`/ds settings` par arène)

---

## 📊 Statistiques & Leaderboards

### Catégories trackées
| Stat | Commande |
|------|----------|
| Victoires | `/ds top wins` |
| Kills | `/ds top kills` |
| Morts | `/ds top deaths` |
| Temps de jeu | `/ds top time` |
| Parties jouées | `/ds top games` |

### Stockage
- Fichier YAML dans `plugins/DeathSwap/stats/`
- Sauvegarde auto configurable (`stats.auto-save-minutes`)

---

## 🎯 Challenges (DeathSwap uniquement)

Types disponibles :
- **CRAFT** – Fabriquer un objet
- **MINE** – Miner un bloc
- **KILL** – Tuer un mob

Récompenses = effets de potion (`SPEED`, `STRENGTH`, `NIGHT_VISION`, etc.)

Active-les dans `config.yml` → `challenges.enabled: true`

---

## 🗳️ Système de Vote

Quand activé, les joueurs votent pour un seed avant le début de la partie :
1. Le système propose `X` seeds aléatoires (configurable)
2. Les joueurs cliquent pour voter
3. Le seed gagnant est utilisé pour la génération du monde

---

## 🔊 Sons Personnalisés

Chaque événement sonore est configurable dans `config.yml` → `sounds.*`

| Événement | Clé config |
|-----------|-----------|
| Début de partie | `game-start` |
| Tick countdown | `countdown-tick` |
| Go ! | `countdown-go` |
| Swap | `swap` |
| Shuffle (nouveau round) | `shuffle` |
| Mort | `death` |
| Victoire | `win` |
| Round réussi | `round-success` |
| Round échoué | `round-fail` |
| Challenge complété | `challenge-complete` |
| Vote enregistré | `vote-cast` |

Désactive tous les sons : `sounds.enabled: false`

---

## 🛠️ Dashboard Admin

Accessible via `/ds admin` (permission `deathswap.admin`).

### Navigation
```
📋 Admin Dashboard
├── 🏟️ [Arène] (clic gauche → Détails, clic droit → TP lobby)
│   ├── ⚔️ Force Start / 🛑 Force Stop
│   ├── 💥 Régénérer Monde (CyberWorldReset) → ⚠ Confirmation
│   └── 👥 Gérer Joueurs
│       └── 👤 Actions Joueur
│           ├── 🔮 Téléporter
│           ├── 📦 Voir Inventaire
│           ├── 👢 Kick de l'Arène
│           └── ⛔ Bannir du Serveur → ⚠ Confirmation
├── ⭐ Recharger Config (Nether Star)
└── ❌ Fermer (Barrier)
```

> 💡 Les actions destructives (**Régénérer Monde** et **Bannir**) passent par un écran de confirmation "Êtes-vous sûr ?" pour éviter les erreurs.

---

## 🏗️ Architecture du Projet

```
src/main/java/be/dualsfwshield/deathswap/
├── DeathSwapPlugin.java      # Classe principale
├── GameInstance.java          # Logique de jeu (base)
├── GameState.java             # États (WAITING/STARTING/RUNNING/ENDED/DISABLED)
├── ArenaManager.java          # Gestion multi-arènes
├── ConfigManager.java         # Configuration YAML
├── commands/
│   └── DeathSwapCommand.java  # Toutes les commandes /ds
├── gui/
│   ├── SettingsGUI.java       # Settings par arène
│   ├── GamerulesGUI.java      # Gamerules en jeu
│   ├── SwapTimerGUI.java      # Timer de swap
│   ├── AdminGUI.java          # Dashboard admin
│   ├── ArenaDetailsGUI.java   # Détails arène
│   ├── PlayerListGUI.java     # Liste joueurs
│   ├── PlayerActionGUI.java   # Actions joueur
│   └── ConfirmationGUI.java   # Confirmation actions destructives
├── listeners/
│   └── GameListener.java      # Events Bukkit
├── modes/
│   ├── DeathShuffleInstance.java
│   ├── DeathShuffleListener.java
│   ├── BlockShuffleInstance.java
│   └── BlockShuffleListener.java
├── stats/
│   ├── PlayerStats.java
│   ├── StatsManager.java
│   └── LeaderboardManager.java
├── challenges/
│   ├── Challenge.java
│   ├── ChallengeManager.java
│   └── ChallengeListener.java
├── vote/
│   └── VoteManager.java
└── sounds/
    └── SoundManager.java
```

---

## 📄 Documentation Complète

📖 Voir [WIKI.md](WIKI.md) pour la documentation technique complète.

---

## 👤 Auteur

- **DualsFWShield** — [dualsfwshield.be](https://dualsfwshield.be) — [GitHub](https://github.com/DualsFWShield)

---

## 📝 Licence

MIT — Utilise, modifie et redistribue librement.
