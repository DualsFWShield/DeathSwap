# 📖 DeathSwap — Wiki Complet

> Guide technique et utilisateur complet pour le plugin DeathSwap.  
> Dernière mise à jour : Février 2026

---

## 📑 Table des Matières

1. [Installation et Prérequis](#-installation-et-prérequis)
2. [Premiers Pas](#-premiers-pas)
3. [Modes de Jeu](#-modes-de-jeu)
4. [Commandes Complètes](#-commandes-complètes)
5. [Configuration Détaillée](#-configuration-détaillée)
6. [Système de Permissions](#-système-de-permissions)
7. [Interface Utilisateur (UI Modes)](#-interface-utilisateur-ui-modes)
8. [Gamerules Minecraft](#-gamerules-minecraft)
9. [Système de Seeds et Votes](#-système-de-seeds-et-votes)
10. [Statistiques et Leaderboards](#-statistiques-et-leaderboards)
11. [Challenges](#-challenges)
12. [Sons Personnalisés](#-sons-personnalisés)
13. [Dashboard Admin](#-dashboard-admin)
14. [Multi-Arènes](#-multi-arènes)
15. [Dépendances](#-dépendances)
16. [FAQ et Dépannage](#-faq-et-dépannage)

---

## 📥 Installation et Prérequis

### Prérequis système

| Composant | Version minimum | Rôle |
|-----------|----------------|------|
| **Java** | 21+ | Runtime |
| **Paper** | 1.21+ | Serveur Minecraft |
| **Multiverse-Core** | 4.x | Gestion des mondes (création, chargement, TP) |
| **CyberWorldReset** | * | Régénération de mondes (reset) |

### Étapes d'installation

```bash
# 1. Compiler le plugin
mvn clean package

# 2. Copier le JAR dans le dossier plugins
cp target/deathswap-1.0.0.jar /chemin/vers/serveur/plugins/

# 3. Démarrer le serveur
# Le fichier config.yml est auto-généré au premier lancement
```

### Configuration initiale requise

Après le premier lancement :

1. **Créer les mondes** via Multiverse :
   ```
   /mv create DS_WaitingLobby normal
   /mv create DeathSwap_Game normal
   ```
2. **Configurer CyberWorldReset** pour le monde de jeu :
   ```
   /cwr add DeathSwap_Game
   ```
3. **Éditer** `plugins/DeathSwap/config.yml` pour pointer vers vos mondes
4. **Recharger** via `/ds reload`

---

## 🚀 Premiers Pas

### Pour les joueurs

1. Rejoindre une arène : `/ds join` (ou `/ds join <nom_arène>`)
2. Attendre que tous les joueurs soient prêts
3. Le jeu démarre automatiquement (ou un admin le force avec `/ds start`)
4. Survivre !

### Pour les admins

1. Ouvrir le dashboard : `/ds admin`
2. Cliquer sur une arène pour la gérer
3. Utiliser `/ds reload` après chaque modification de config

---

## 🕹️ Modes de Jeu

### DeathSwap (Classique)

> Piège la zone avant d'être échangé avec un autre joueur !

- Les joueurs sont **téléportés aléatoirement** les uns aux positions des autres
- Intervalle de swap configurable (fixe ou aléatoire)
- Le dernier joueur en vie gagne
- **PvP optionnel**, Nether/End configurables

**Déroulement :**
1. 🏠 Lobby → Tous les joueurs cliquent "Prêt"
2. ⏳ Countdown + chargement du monde
3. 🌍 Dispersion aléatoire dans le monde
4. ⚡ Protection de spawn (configurable)
5. 🔄 Swaps à intervalles réguliers
6. 🏆 Dernier vivant déclare vainqueur

### DeathShuffle

> Chaque round, un type de mort t'est assigné. Meurt de la bonne façon ! 

- Rounds successifs avec un **type de mort** assigné
- Exemples : mourir de lave, de chute, de noyade, d'explosion...
- Timer par round basé sur la difficulté (easy/medium/hard)
- Si tu ne meurs pas du bon type → **éliminé**

**Types de mort par difficulté :**
| Facile | Moyen | Difficile |
|--------|-------|-----------|
| Chute | Lave | Lightning |
| Noyade | Cactus | Wither |
| Feu | Explosion | Void |
| Mob | Flèche | Cramming |

### BlockShuffle

> Trouve et tiens-toi debout sur le bon bloc avant la fin du timer !

- Chaque round, un **bloc ou item** est assigné
- Les joueurs doivent trouver et se **tenir debout sur** ce bloc
- Timer par round basé sur la rareté du bloc
- Dernier joueur éliminé à chaque round

---

## 📋 Commandes Complètes

### Commandes Joueur

| Commande | Arguments | Description | Exemples |
|----------|-----------|-------------|----------|
| `/ds join` | `[arène]` | Rejoindre une arène. Sans argument = arène `default` | `/ds join`, `/ds join arena2` |
| `/ds leave` | — | Quitter la partie/lobby en cours | `/ds leave` |
| `/ds list` | — | Voir toutes les arènes, leur statut et joueurs | `/ds list` |
| `/ds stats` | `[joueur]` | Voir ses stats ou celles d'un joueur | `/ds stats`, `/ds stats Steve` |
| `/ds top` | `[catégorie]` | Classement. Catégories : `wins`, `kills`, `deaths`, `time`, `games` | `/ds top`, `/ds top kills` |
| `/ds vote` | `<arène> <choix>` | Voter pour un seed (déclenché par clic en jeu) | `/ds vote default 2` |
| `/ds tp` | `<joueur>` | Téléporter vers un joueur (spectateurs uniquement) | `/ds tp Steve` |

### Commandes Admin

| Commande | Arguments | Description | Exemples |
|----------|-----------|-------------|----------|
| `/ds start` | `[debug]` | Lancer le jeu. `debug` = ignore le min de joueurs | `/ds start`, `/ds start debug` |
| `/ds stop` | `[arène]` | Arrêter une arène. Sans argument = `default` | `/ds stop`, `/ds stop arena2` |
| `/ds swapnow` | — | Forcer un swap immédiat (DeathSwap) | `/ds swapnow` |
| `/ds reload` | — | Recharger toute la configuration | `/ds reload` |
| `/ds settings` | — | *(Réservé)* GUI Settings de l'arène | `/ds settings` |
| `/ds admin` | — | Ouvrir le Dashboard Admin (GUI) | `/ds admin` |

> **Alias :** `/deathswap` fonctionne aussi à la place de `/ds`

---

## ⚙️ Configuration Détaillée

Le fichier `plugins/DeathSwap/config.yml` contrôle 100% du comportement du plugin.

### Structure générale

```yaml
hub-world: "MainLobby"          # Monde de retour après partie/kick
prefixes: { ... }               # Préfixes chat par mode
stats: { ... }                  # Statistiques
voting: { ... }                 # Système de vote
challenges: { ... }             # Challenges (DeathSwap)
sounds: { ... }                 # Sons personnalisés
arenas:                         # Toutes les arènes
  default: { ... }
  arena2: { ... }
```

### `hub-world`

```yaml
hub-world: "MainLobby"
```

Le nom du monde Multiverse où les joueurs sont envoyés après :
- Fin de partie
- Kick d'arène
- Commande `/ds leave`

> ⚠️ Ce monde **doit exister** dans Multiverse. Créez-le avec `mv create MainLobby normal`.

### `teleport-command` & `world-reset-commands`

```yaml
teleport-command: "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"
world-reset-commands:
  - "cwr edit %world% setSeed %seed%"
  - "cwr reset %world%"
```

- **`teleport-command`** : Commande exécutée pour téléporter les joueurs.
  - **Défaut** : Utilise Multiverse (`mvtp`).
  - **Vanilla** : `execute in %world% run tp %player% %x% %y% %z% %yaw% %pitch%`
- **`world-reset-commands`** : Liste de commandes pour reset le monde.
  - **Défaut** : Utilise CyberWorldReset (`cwr`).
  - **Sans Reset** : Laissez la liste vide `[]` pour jouer sur une map statique.

---

## 📂 Fichiers de Configuration de Référence

### `config.yml` Par Défaut
```yaml
# DeathSwap Global Configuration
# ------------------------------

# Monde de retour après partie/kick
hub-world: "MainLobby"

# Commande de téléportation.
# Placeholders: %player%, %world%, %x%, %y%, %z%, %yaw%, %pitch%
# Défaut (Multiverse): "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"
# Vanilla: "execute in %world% run tp %player% %x% %y% %z% %yaw% %pitch%"
teleport-command: "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"

# Commandes de reset du monde avant la partie.
# Placeholders: %world%, %seed%
# Liste vide [] = pas de reset (map statique).
world-reset-commands:
  - "cwr edit %world% setSeed %seed%"
  - "cwr reset %world%"

prefixes:
  deathswap: "&c[DS]"
  deathshuffle: "&6[DSh]"
  blockshuffle: "&b[BS]"

stats:
  enabled: true
  auto-save-minutes: 5

sounds:
  enabled: true
  # ... (voir section Sons)

challenges:
  enabled: true
  list:
    - type: CRAFT
      target: CRAFTING_TABLE
      amount: 1
      reward: SPEED
      description: "Craft une table de craft"

voting:
  enabled: true
  vote-time: 30
  options-count: 3
```

### Exemple Arène (`arenas/example.yml`)
> **Note :** Copiez ce fichier pour créer de nouvelles arènes (ex: `monarene.yml`). Le plugin ignore le fichier `example.yml` par défaut.

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
  respawn_radius: "0"

seeds: []
```

### `prefixes`

```yaml
prefixes:
  deathswap: "&8[&6DeathSwap&8]"
  deathshuffle: "&8[&dDeathShuffle&8]"
  blockshuffle: "&8[&bBlockShuffle&8]"
```

Préfixes affichés dans le chat pour chaque mode de jeu. Supporte les codes couleur Minecraft (`&` notation).

| Code | Couleur |
|------|---------|
| `&0` – `&9` | Noir → Bleu clair |
| `&a` – `&f` | Vert clair → Blanc |
| `&l` | **Gras** |
| `&o` | *Italique* |
| `&n` | Souligné |
| `&r` | Reset |

### `stats`

```yaml
stats:
  enabled: true           # true = activer les stats, false = désactiver
  auto-save-minutes: 5    # Sauvegarde auto toutes les X minutes
```

- **`enabled`** : Active/désactive le système de statistiques complet
- **`auto-save-minutes`** : Intervalle de sauvegarde automatique. Les stats sont aussi sauvegardées à l'arrêt du serveur

### `voting`

```yaml
voting:
  enabled: true           # Activer le vote de seed
  vote-time: 15           # Durée du vote en secondes
  options-count: 3        # Nombre de seeds proposées au vote
```

- **`vote-time`** : Combien de temps les joueurs ont pour voter (en secondes)
- **`options-count`** : Nombre de seeds aléatoires proposées parmi la liste `seeds`

### `challenges`

```yaml
challenges:
  enabled: false
  list:
    - { type: CRAFT, target: CRAFTING_TABLE, amount: 1, reward: SPEED, description: "Craft une table de craft" }
    - { type: MINE, target: COAL_ORE, amount: 3, reward: NIGHT_VISION, description: "Mine 3 charbons" }
    - { type: KILL, target: ZOMBIE, amount: 1, reward: STRENGTH, description: "Tue un zombie" }
```

**Uniquement en mode DeathSwap.** Les challenges donnent des objectifs bonus avec des récompenses.

| Paramètre | Valeurs possibles |
|-----------|-------------------|
| `type` | `CRAFT`, `MINE`, `KILL` |
| `target` | Nom du Material/EntityType Bukkit (ex: `IRON_ORE`, `ZOMBIE`, `FURNACE`) |
| `amount` | Nombre requis (entier) |
| `reward` | Effet de potion : `SPEED`, `STRENGTH`, `NIGHT_VISION`, `RESISTANCE`, `FASTER_DIGGING` |
| `description` | Texte affiché au joueur |

### `sounds`

```yaml
sounds:
  enabled: true
  game-start: { type: "ENTITY_ENDER_DRAGON_GROWL", volume: 1.0, pitch: 1.0 }
  swap: { type: "ENTITY_ENDERMAN_TELEPORT", volume: 1.0, pitch: 1.0 }
  # ... etc
```

Chaque événement sonore est configurable individuellement.

| Clé | Quand il joue |
|-----|--------------|
| `game-start` | La partie démarre |
| `countdown-tick` | Chaque seconde du countdown |
| `countdown-go` | Fin du countdown, GO ! |
| `swap` | Swap entre joueurs (DeathSwap) |
| `shuffle` | Nouveau round (DeathShuffle/BlockShuffle) |
| `death` | Un joueur meurt |
| `win` | Un joueur gagne la partie |
| `round-success` | Round réussi (Shuffle modes) |
| `round-fail` | Round échoué (Shuffle modes) |
| `challenge-complete` | Challenge terminé |
| `vote-cast` | Vote enregistré |

**Paramètres :**
- `type` : Nom du son Bukkit (ex: `ENTITY_ENDERMAN_TELEPORT`). Voir [liste complète](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html)
- `volume` : Volume (0.0 à 2.0)
- `pitch` : Hauteur (0.5 à 2.0)

Pour **désactiver tous les sons** : `sounds.enabled: false`

### `arenas`

Chaque bloc sous `arenas:` définit une arène indépendante.

```yaml
arenas:
  default:              # ID unique de l'arène
    game-type: DEATHSWAP
    game-world: "DeathSwap_Game"
    lobby-world: "DS_WaitingLobby"
    min-players: 2
    max-players: 20
    ui-mode: RICH
    timers: { ... }
    round-timers: { ... }
    game: { ... }
    gamerules: { ... }
    seeds: [ ... ]
```

#### Paramètres par arène

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `game-type` | Enum | `DEATHSWAP` | Mode de jeu : `DEATHSWAP`, `DEATHSHUFFLE`, `BLOCKSHUFFLE` |
| `game-world` | String | `DeathSwap_Game` | Nom du monde Multiverse pour le jeu |
| `lobby-world` | String | `DS_WaitingLobby` | Nom du monde Multiverse pour le lobby |
| `min-players` | Int | `2` | Minimum de joueurs pour démarrer |
| `max-players` | Int | `20` | Maximum de joueurs acceptés |
| `ui-mode` | Enum | `RICH` | Mode d'affichage : `RICH` ou `CLEAN` |

#### `timers` (DeathSwap)

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `load-time` | Int (sec) | `40` | Temps de chargement du monde avant la partie |
| `swap-mode` | Enum | `FIXED` | `FIXED` = intervalle fixe, `RANDOM` = aléatoire |
| `swap-interval` | Int (sec) | `300` | Intervalle de swap en mode FIXED (5 min) |
| `swap-min` | Int (sec) | `120` | Intervalle minimum en mode RANDOM (2 min) |
| `swap-max` | Int (sec) | `420` | Intervalle maximum en mode RANDOM (7 min) |
| `max-game-time` | Int (sec) | `1800` | Durée maximum de la partie (30 min) |
| `spawn-protection` | Int (sec) | `30` | Invulnérabilité au début |

#### `round-timers` (DeathShuffle / BlockShuffle)

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `easy` | Int (sec) | `90` | Durée d'un round facile |
| `medium` | Int (sec) | `70` | Durée d'un round moyen |
| `hard` | Int (sec) | `50` | Durée d'un round difficile |

#### `game`

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `pvp-enabled` | Boolean | `true` | PvP activé entre joueurs |
| `nether-enabled` | Boolean | `false` | Accès au Nether autorisé |
| `end-enabled` | Boolean | `false` | Accès à l'End autorisé |

#### `gamerules`

```yaml
gamerules:
  keepInventory: "false"
  immediateRespawn: "true"
  doDaylightCycle: "true"
  doWeatherCycle: "true"
  mobGriefing: "true"
  naturalRegeneration: "true"
  doMobSpawning: "true"
```

Appliqués au monde de jeu au lancement de la partie. Toutes les gamerules Minecraft sont supportées. Valeurs entre guillemets (`"true"` / `"false"`).

> 💡 Les gamerules sont aussi éditables **en jeu** via le GUI Gamerules (accessible depuis Settings).

#### `seeds`

```yaml
seeds:
  - { seed: "-3542283819777", name: "Temple & Village" }
  - { seed: "123456789", name: "Île de survie" }
```

Liste de seeds prédéfinies pour le vote. Chaque entrée a :
- `seed` : La graine Minecraft (en string)
- `name` : Nom affiché au joueur pendant le vote

---

## 🔐 Système de Permissions

| Permission | Description | Défaut | Commandes associées |
|------------|-------------|--------|---------------------|
| `deathswap.play` | Accès joueur de base | `true` (tout le monde) | `join`, `leave`, `list`, `stats`, `top`, `vote`, `tp` |
| `deathswap.admin` | Accès administrateur complet | `op` | `start`, `stop`, `swapnow`, `reload`, `settings`, `admin` |

### Intégration avec LuckPerms (exemple)

```
/lp group moderator permission set deathswap.admin true
/lp group default permission set deathswap.play true
```

---

## 🎨 Interface Utilisateur (UI Modes)

### Mode RICH (défaut)

| Élément | Affichage |
|---------|-----------|
| Timer de swap | **BossBar** en haut de l'écran (barre jaune animée) |
| Infos round | **ActionBar** (texte au-dessus de la hotbar) |
| Événements majeurs | **Titre** plein écran (swap, mort, victoire) |
| Messages système | Chat |

### Mode CLEAN

| Élément | Affichage |
|---------|-----------|
| Timer de swap | Message chat périodique |
| Infos round | Message chat |
| Événements majeurs | Message chat coloré |
| Messages système | Chat |

### Comment changer le mode

**Via config :**
```yaml
arenas:
  default:
    ui-mode: CLEAN    # ou RICH
```

**Via GUI en jeu :**
Le GUI Settings (accessible depuis le Dashboard admin ou `/ds settings`) permet de basculer entre les deux modes.

---

## 🎮 Gamerules Minecraft

Les gamerules sont des règles Minecraft appliquées au monde de jeu. Elles sont configurables de deux façons :

### Via config.yml

```yaml
arenas:
  default:
    gamerules:
      keepInventory: "false"
      naturalRegeneration: "true"
      # Toute gamerule Minecraft valide
```

### Via GUI en jeu

1. Ouvrir le Dashboard Admin (`/ds admin`)
2. Clic molette sur l'arène → GUI Settings
3. Naviguer vers "Gamerules"
4. Cliquer pour basculer chaque règle

### Gamerules par défaut

| Gamerule | Défaut | Description |
|----------|--------|-------------|
| `keepInventory` | `false` | Garder son inventaire à la mort |
| `immediateRespawn` | `true` | Réapparition instantanée |
| `doDaylightCycle` | `true` | Cycle jour/nuit |
| `doWeatherCycle` | `true` | Cycle météo |
| `mobGriefing` | `true` | Mobs détruisent les blocs |
| `naturalRegeneration` | `true` | Régénération naturelle de vie |
| `doMobSpawning` | `true` | Spawn de mobs |

---

## 🗳️ Système de Seeds et Votes

### Fonctionnement

1. Quand tous les joueurs sont prêts, un **vote** s'ouvre (si activé)
2. Le système pioche `options-count` seeds aléatoires dans la liste
3. Les joueurs voient les options en chat et cliquent pour voter
4. Après `vote-time` secondes, le seed gagnant est appliqué
5. Le monde est généré avec ce seed

### Configuration

```yaml
voting:
  enabled: true
  vote-time: 15      # Durée du vote
  options-count: 3    # Nombre d'options

arenas:
  default:
    seeds:
      - { seed: "12345", name: "Village + Temple" }
      - { seed: "-99999", name: "Désert infini" }
      # Ajoute autant de seeds que tu veux
```

### Ajouter des seeds

Pour trouver de bons seeds :
- [chunkbase.com](https://www.chunkbase.com/apps/seed-map) pour explorer des seeds
- Utilise le seed de ta partie préférée !
- Le seed peut être un nombre positif ou négatif

---

## 📊 Statistiques et Leaderboards

### Stats trackées par joueur

| Stat | Description |
|------|-------------|
| **Victoires** (`wins`) | Nombre de parties gagnées |
| **Kills** (`kills`) | Nombre de kills total |
| **Morts** (`deaths`) | Nombre de morts |
| **Temps de jeu** (`time`) | Temps total en jeu (secondes) |
| **Parties** (`games`) | Nombre de parties jouées |

### Voir ses stats

```
/ds stats           → Tes propres stats
/ds stats Steve     → Stats de Steve
```

### Leaderboards

```
/ds top             → Top 10 victoires (défaut)
/ds top kills       → Top 10 kills
/ds top deaths      → Top 10 morts
/ds top time        → Top 10 temps de jeu
/ds top games       → Top 10 parties jouées
```

### Stockage

Les stats sont sauvegardées en YAML dans `plugins/DeathSwap/stats/`. La sauvegarde automatique est configurable via `stats.auto-save-minutes`.

---

## 🎯 Challenges

> Mode DeathSwap uniquement. Désactivé par défaut.

Les challenges sont des objectifs bonus qui donnent des **effets de potion** en récompense.

### Types de challenges

| Type | Objectif | Exemple |
|------|----------|---------|
| `CRAFT` | Fabriquer un objet | Craft une table de craft |
| `MINE` | Miner un bloc | Mine 3 charbons |
| `KILL` | Tuer un mob | Tue un zombie |

### Récompenses disponibles

| Reward | Effet |
|--------|-------|
| `SPEED` | Vitesse |
| `STRENGTH` | Force |
| `NIGHT_VISION` | Vision nocturne |
| `RESISTANCE` | Résistance |
| `FASTER_DIGGING` | Célérité |

### Ajouter un challenge

```yaml
challenges:
  enabled: true
  list:
    # Format : { type: TYPE, target: MATERIAL/ENTITY, amount: N, reward: EFFECT, description: "texte" }
    - { type: CRAFT, target: DIAMOND_PICKAXE, amount: 1, reward: STRENGTH, description: "Craft une pioche en diamant" }
    - { type: MINE, target: DIAMOND_ORE, amount: 5, reward: SPEED, description: "Mine 5 diamants" }
    - { type: KILL, target: ENDERMAN, amount: 3, reward: NIGHT_VISION, description: "Tue 3 endermen" }
```

---

## 🔊 Sons Personnalisés

Chaque événement du jeu peut avoir un son personnalisé. La liste complète des sons Bukkit est disponible sur la [documentation Spigot](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html).

### Exemples populaires

| Son | Utilisation suggérée |
|-----|---------------------|
| `ENTITY_ENDER_DRAGON_GROWL` | Début de partie épique |
| `ENTITY_ENDERMAN_TELEPORT` | Swap |
| `ENTITY_WITHER_DEATH` | Mort dramatique |
| `UI_TOAST_CHALLENGE_COMPLETE` | Victoire |
| `ENTITY_PLAYER_LEVELUP` | Round réussi |
| `BLOCK_NOTE_BLOCK_CHIME` | Nouveau round |

### Personnaliser un son

```yaml
sounds:
  swap: { type: "ENTITY_GHAST_SCREAM", volume: 0.5, pitch: 1.5 }
```

- **`volume`** : de `0.0` (silencieux) à `2.0` (fort)
- **`pitch`** : de `0.5` (grave) à `2.0` (aigu). `1.0` = normal

---

## 🛠️ Dashboard Admin

Le Dashboard Admin est un système de GUI accessible via `/ds admin`. Il fournit une interface graphique complète pour gérer les arènes et les joueurs.

### Accès

- **Commande :** `/ds admin`
- **Permission requise :** `deathswap.admin`
- **Joueur uniquement** (pas console)

### Pages du Dashboard

#### 1. Page Principale (Liste des Arènes)

| Élément | Action |
|---------|--------|
| **Item arène** (béton coloré) | Affiche le statut et nombre de joueurs |
| **Clic gauche** sur arène | → Ouvre les détails de l'arène |
| **Clic droit** sur arène | → Téléporte au lobby de l'arène |
| **Clic molette** sur arène | → Ouvre les settings GUI |
| **Étoile du Nether** | Recharger toute la configuration |
| **Barrier** | Fermer le GUI |

**Couleurs de statut :**

| Couleur | État |
|---------|------|
| 🟡 Jaune | `WAITING` — En attente de joueurs |
| 🟢 Vert clair | `STARTING` — Countdown en cours |
| 🟩 Vert | `RUNNING` — Partie en cours |
| 🔴 Rouge | `ENDED` — Partie terminée |
| ⬛ Barrier | `DISABLED` — Arène désactivée |

#### 2. Détails de l'Arène

| Bouton | Action |
|--------|--------|
| ⚔️ **Force Start** | Lance le jeu immédiatement (visible si WAITING/STARTING) |
| 🛑 **Force Stop** | Arrête le jeu (visible si RUNNING) |
| 💥 **Régénérer Monde** | Reset le monde via CyberWorldReset (uniquement si partie non en cours) → **⚠ Confirmation requise** |
| 👥 **Gérer Joueurs** | → Ouvre la liste des joueurs |
| ⬅️ **Retour** | → Retour à la page principale |

#### 3. Liste des Joueurs

Affiche tous les joueurs dans l'arène avec leur **tête**, leur **vie** (❤) et leur **faim** (🍗).

| Action | Résultat |
|--------|----------|
| **Clic gauche** sur un joueur | → Ouvre les actions pour ce joueur |
| **⬅️ Retour** | → Retour aux détails de l'arène |

#### 4. Actions Joueur

| Bouton | Action | Description |
|--------|--------|-------------|
| 🔮 **Téléporter** | `admin.teleport(target)` | TP l'admin vers le joueur |
| 📦 **Voir Inventaire** | `admin.openInventory(target)` | Voir/modifier l'inventaire |
| 👢 **Kick de l'Arène** | `arena.sendToHub(target)` | Renvoyer au hub |
| ⛔ **Bannir** | `sendToHub` + `/ban` → **⚠ Confirmation requise** |
| ⬅️ **Retour** | — | Retour à la liste des joueurs |

#### 5. Écran de Confirmation

Les actions destructives (**Régénérer Monde** et **Bannir**) passent par un écran de confirmation pour éviter les erreurs.

| Slot | Item | Action |
|------|------|--------|
| 11 | 🟩 **Laine verte** | **Annuler** — retour à l'écran précédent |
| 13 | 💥 **TNT** | Info : nom de l'action + description + ⚠ "Cette action est irréversible !" |
| 15 | 🟥 **Laine rouge** | **Confirmer** — exécute l'action |

**Sons :**
- Confirmation → `BLOCK_ANVIL_USE` (son d'enclume)
- Annulation → `BLOCK_NOTE_BLOCK_BASS` (note grave)

---

## 🏟️ Multi-Arènes

### Concept

Chaque arène est une **instance indépendante** avec :
- Son propre monde de jeu
- Son propre lobby
- Sa propre configuration (mode, timers, seeds, etc.)
- Ses propres joueurs

### Ajouter une arène

1. **Créer les mondes** :
   ```
   /mv create MonArene_Game normal
   /mv create MonArene_Lobby normal
   /cwr add MonArene_Game
   ```

2. **Ajouter dans config.yml** :
   ```yaml
   arenas:
     default:
       # ... arène existante ...
     
     mon_arene:
       game-type: DEATHSHUFFLE
       game-world: "MonArene_Game"
       lobby-world: "MonArene_Lobby"
       min-players: 3
       max-players: 8
       ui-mode: CLEAN
       timers:
         load-time: 30
         swap-mode: RANDOM
         swap-min: 60
         swap-max: 300
       round-timers:
         easy: 120
         medium: 90
         hard: 60
       game:
         pvp-enabled: false
         nether-enabled: true
         end-enabled: false
       seeds:
         - { seed: "42", name: "The Answer" }
   ```

3. **Recharger** : `/ds reload`

4. **Rejoindre** : `/ds join mon_arene`

### Supprimer une arène

1. Supprimer le bloc dans `config.yml`
2. `/ds reload`
3. Optionnel : supprimer les mondes via Multiverse (`/mv delete MonArene_Game`)

---

## 🔧 Dépendances

### Multiverse-Core (requis)

Utilisé pour :
- Créer et charger les mondes (`mv create`, `mv load`)
- Téléporter les joueurs (`mv tp`)
- Gérer les mondes de lobby et de jeu

### CyberWorldReset (requis)

Utilisé pour :
- Régénérer les mondes de jeu entre les parties (`cwr reset`)
- Disponible depuis le bouton "Régénérer" dans le Dashboard Admin

### Configuration CyberWorldReset

1. Installez le plugin CyberWorldReset
2. Ajoutez le monde de jeu :
   ```
   /cwr add DeathSwap_Game
   ```
3. La commande `cwr reset <monde>` sera utilisée automatiquement par le Dashboard Admin

---

## ❓ FAQ et Dépannage

### Le plugin ne démarre pas

- Vérifiez que Java 21+ est installé (`java -version`)
- Vérifiez que Paper 1.21+ est utilisé
- Vérifiez les logs de la console pour les erreurs

### Les mondes ne se chargent pas

- Vérifiez que **Multiverse-Core** est installé et actif
- Vérifiez les noms de mondes dans `config.yml` correspondent à ceux créés dans Multiverse
- Utilisez `/mv list` pour voir les mondes disponibles

### Le swap ne fonctionne pas

- Vérifiez que `timers.swap-mode` est bien `FIXED` ou `RANDOM`
- En mode `RANDOM`, vérifiez que `swap-min` < `swap-max`
- Vérifiez qu'il y a au moins 2 joueurs vivants

### Les stats ne s'enregistrent pas

- Vérifiez que `stats.enabled: true` dans `config.yml`
- Vérifiez les permissions d'écriture du dossier `plugins/DeathSwap/stats/`

### La régénération ne fonctionne pas

- Vérifiez que **CyberWorldReset** est installé
- Vérifiez que le monde est ajouté : `/cwr add <nom_monde>`
- La régénération ne fonctionne pas si une partie est en cours

### Les sons ne jouent pas

- Vérifiez que `sounds.enabled: true`
- Vérifiez que les noms de sons sont valides (voir doc Spigot)
- Le volume du client Minecraft doit être activé

### Je ne peux pas rejoindre une arène

- L'arène est peut-être pleine (`max-players` atteint)
- Tu es peut-être déjà dans une arène (`/ds leave` d'abord)
- L'arène n'existe pas (vérifie le nom avec `/ds list`)

---

## 📝 Notes de Version

### v1.0.0
- 🎮 3 modes de jeu (DeathSwap, DeathShuffle, BlockShuffle)
- 🏟️ Support multi-arènes
- 🎛️ Dashboard Admin complet
- 📊 Statistiques et leaderboards
- 🗳️ Système de vote de seed
- 🎯 Challenges avec récompenses
- 🔊 Sons personnalisables
- 🎨 Deux modes UI (RICH / CLEAN)
- ⚙️ Gamerules configurables en jeu
- 🌍 Régénération de monde via CyberWorldReset
- ⚠️ Écran de confirmation pour les actions destructives (ban, regen)

---

*Documentation rédigée pour DeathSwap v1.0.0 — Plugin par [DualsFWShield](https://dualsfwshield.be)*
