# 📚 DeathSwap — Wiki Technique (Français)

> **[English Wiki](WIKI_EN.md)**
>
> Documentation technique complète du plugin DeathSwap pour **Paper 1.21.11** (et forks compatibles : Purpur, etc.). Testé en 1.21.11.
>
> ⚠️ **Non compatible Spigot/Bukkit** — Le plugin utilise l'API Adventure native de Paper.

---

## 📋 Table des Matières

- [Commandes Complètes](#-commandes-complètes)
- [Configuration Détaillée](#️-configuration-détaillée)
- [Système de Permissions](#-permissions)
- [Modes d'Interface (UI)](#-modes-dinterface)
- [Gamerules](#-gamerules)
- [Seeds & Vote](#-seeds--vote)
- [Statistiques](#-statistiques)
- [Challenges](#-challenges)
- [Sons Personnalisés](#-sons-personnalisés)
- [Dashboard Admin](#️-dashboard-admin)
- [Multi-Arènes](#️-multi-arènes)
- [Propriétés Admin Set](#-propriétés-admin-set)
- [Dépendances](#-dépendances)
- [Protection Anti-Solo](#️-protection-anti-solo)
- [API Développeur](#️-api-développeur-custom-game-modes)
- [FAQ et Dépannage](#-faq-et-dépannage)
- [Configuration de Référence](#-fichiers-de-configuration-de-référence)
- [Licence](#-licence)

---

## 📋 Commandes Complètes

### Commandes Joueur

| Commande                      | Description                                     | Permission         |
| ----------------------------- | ----------------------------------------------- | ------------------ |
| `/ds join [arène]`         | Rejoint une arène (`default` par défaut)     | `deathswap.play` |
| `/ds leave`                 | Quitte la partie en cours                       | `deathswap.play` |
| `/ds stats [joueur]`        | Affiche les statistiques                        | `deathswap.play` |
| `/ds top [catégorie]`      | Classement (wins/kills/deaths/time/games)       | `deathswap.play` |
| `/ds vote <arène> <choix>` | Vote pour un seed                               | `deathswap.play` |
| `/ds help`                  | Aide principale en chat                         | `deathswap.play` |
| `/ds help gui`              | Ouvre le menu d'aide visuel (GUI)               | `deathswap.play` |
| `/ds list`                  | Liste les arènes et leur statut                | `deathswap.play` |
| `/ds tp <joueur>`           | TP vers un joueur (spectateur uniquement)       | `deathswap.play` |

### Commandes Admin

| Commande                                         | Description                                   | Permission          |
| ------------------------------------------------ | --------------------------------------------- | ------------------- |
| `/ds start [debug]`                             | Lance la partie (debug = 1 joueur min)        | `deathswap.admin` |
| `/ds stop [arène]`                             | Arrête une arène                             | `deathswap.admin` |
| `/ds swapnow`                                   | Force un swap immédiat                       | `deathswap.admin` |
| `/ds reload`                                    | Recharge la configuration complète            | `deathswap.admin` |
| `/ds settings`                                  | Ouvre le GUI Settings de l'arène courante    | `deathswap.admin` |
| `/ds help commands`                             | Affiche les commandes admin en chat           | `deathswap.admin` |
| `/ds admin`                                     | Ouvre le Dashboard Admin (GUI)                | `deathswap.admin` |
| `/ds admin list`                                | Liste les arènes (GUI)                       | `deathswap.admin` |
| `/ds admin create <nom>`                        | Crée une arène                               | `deathswap.admin` |
| `/ds admin edit <arène>`                       | Ouvre le GUI Settings d'une arène            | `deathswap.admin` |
| `/ds admin delete <nom>`                        | Supprime une arène (confirmation requise)     | `deathswap.admin` |
| `/ds admin clone <src> <dst>`                   | Clone une arène                               | `deathswap.admin` |
| `/ds admin save`                                | Sauvegarde la configuration globale            | `deathswap.admin` |
| `/ds admin set <arène> <prop> <val>`           | Modifie une propriété d'arène               | `deathswap.admin` |
| `/ds admin gamerule <arène> set <r> <v>`       | Ajouter/modifier une gamerule                 | `deathswap.admin` |
| `/ds admin gamerule <arène> remove <r>`        | Supprimer une gamerule                        | `deathswap.admin` |
| `/ds admin command <arène> tp <commande>`      | Changer la commande de TP (ou `default`/`none`) | `deathswap.admin` |
| `/ds admin command <arène> reset <preset>`     | Changer le reset : `cwr`, `mv`, `none` ou custom (séparées par `;`) | `deathswap.admin` |

> **Alias :** `/deathswap` fonctionne aussi.

---

## ⚙️ Configuration Détaillée

### Fichier `config.yml` (Global)

Le `config.yml` contient **uniquement** les paramètres globaux. Les arènes sont dans `arenas/<id>.yml`.

```yaml
# Monde de retour après partie/kick
hub-world: "MainLobby"

# Commande de TP. Placeholders: %player%, %world%, %x%, %y%, %z%, %yaw%, %pitch%
teleport-command: "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%"

# Commandes de reset. Placeholders: %world%, %seed%. Vide [] = pas de reset.
world-reset-commands:
  - "cwr edit %world% setSeed %seed%"
  - "cwr reset %world%"

# Préfixes chat
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
  # ... (voir README pour la liste complète)
```

### Structure des arènes

```
plugins/DeathSwap/arenas/
├── example.yml     ← Référence uniquement (ignoré par le plugin)
├── default.yml     ← Arène "default"
├── arena2.yml      ← Autre arène...
└── ...
```

Chaque fichier `.yml` contient la configuration **complète** d'une arène :

```yaml
game-type: DEATHSWAP        # DEATHSWAP, DEATHSHUFFLE, BLOCKSHUFFLE
game-world: "DS_Game"        # Monde de jeu
lobby-world: "DS_Lobby"      # Monde de lobby
min-players: 2
max-players: 20
ui-mode: RICH                # RICH ou CLEAN

timers:
  load-time: 40
  swap-mode: FIXED           # FIXED ou RANDOM
  swap-interval: 300         # Utilisé si FIXED
  swap-min: 120              # Utilisé si RANDOM
  swap-max: 420              # Utilisé si RANDOM
  max-game-time: 1800        # 0 = illimité
  spawn-protection: 30

round-timers:                # DeathShuffle/BlockShuffle
  easy: 90
  medium: 70
  hard: 50

game:
  pvp-enabled: true
  nether-enabled: true
  end-enabled: true

gamerules:                   # Format snake_case
  keep_inventory: "false"
  immediate_respawn: "true"
  # ... (voir README pour la liste complète)

# Options avancées (optionnel)
start-if-min-players-met: false
prevent-cancel-after-countdown: false
debug-mode: false
custom-arena-seed-only: false

# Surcharge de commandes par arène (optionnel, null = utilise global)
# teleport-command: "..."
# world-reset-commands: [...]

seeds:
  - { seed: "123", name: "Mon seed" }
```

---

## 🔐 Permissions

| Permission          | Description          | Défaut         |
| ------------------- | -------------------- | --------------- |
| `deathswap.play`  | Jouer au DeathSwap   | `true` (tous) |
| `deathswap.admin` | Accès admin complet | `op`          |

### Détails

- **`deathswap.play`** : Donne accès à `join`, `leave`, `stats`, `top`, `vote`, `help`, `help gui`, `list`, `tp`.
- **`deathswap.admin`** : Donne accès à **tout** : `start`, `stop`, `swapnow`, `reload`, `settings`, `help commands`, `admin` et tous ses sous-commandes.

---

## 🎨 Modes d'Interface

| Mode        | BossBar | ActionBar | Chat | Titres |
| ----------- | ------- | --------- | ---- | ------ |
| **RICH**  | ✅       | ✅         | ✅    | ✅      |
| **CLEAN** | ❌       | ❌         | ✅    | ❌      |

- **RICH** (défaut) : Expérience immersive avec BossBar pour les timers, ActionBar pour les infos, et Titres pour les événements (swap, mort, victoire).
- **CLEAN** : Tout en chat. Idéal pour les serveurs légers.

Modifiable via :
- Fichier arène : `ui-mode: RICH`
- GUI Settings : `/ds settings` ou `/ds admin edit <arène>`
- Commande : `/ds admin set <arène> uimode CLEAN`

---

## 🎮 Gamerules

Les gamerules Minecraft sont configurables **par arène**, en format **snake_case** (standard Minecraft 1.21.11+).

### Gamerules par défaut

| Gamerule                | Valeur | Description                    |
| ----------------------- | ------ | ------------------------------ |
| `keep_inventory`      | false  | Garder l'inventaire à la mort |
| `immediate_respawn`   | true   | Réapparition immédiate       |
| `do_daylight_cycle`   | true   | Cycle jour/nuit                |
| `do_weather_cycle`    | true   | Cycle météo                  |
| `mob_griefing`        | true   | Griefing des mobs              |
| `natural_regeneration`| true   | Régénération naturelle        |
| `do_mob_spawning`     | true   | Spawn des mobs                 |
| `send_command_feedback`| false | Feedback des commandes         |
| `log_admin_commands`  | false  | Log des commandes admin        |
| `spawn_radius`        | 0      | Rayon de spawn                 |

### Modifier en jeu

1. **GUI** : `/ds settings` → Section Gamerules → Cliquer pour toggle
2. **Commande Set** : `/ds admin gamerule <arène> set <règle> <valeur>`
3. **Commande Remove** : `/ds admin gamerule <arène> remove <règle>`

> **Important :** Les clés utilisent le format **snake_case** (ex: `keep_inventory`, pas `keepInventory`).

---

## 🌱 Seeds & Vote

### Seeds prédéfinis

Chaque arène peut contenir une liste de seeds dans son fichier de configuration :

```yaml
seeds:
  - { seed: "-3542283819777", name: "Temple & Village" }
  - { seed: "8490605437877207559", name: "Village & Ice Spikes" }
  - { seed: "-13377777", name: "Désert & Pyramide" }
```

### Système de vote

Quand `voting.enabled: true` dans `config.yml` :

1. Au lancement, le système choisit aléatoirement `options-count` seeds
2. Les joueurs votent via GUI (clic) pendant `vote-time` secondes
3. Le seed gagnant est utilisé pour la génération du monde

Si aucun seed n'est défini dans l'arène ou en global (ou si désactivé), un seed aléatoire classique est généré.

> **Note :** Il est possible d'isoler une arène pour qu'elle n'utilise **que** ses propres seeds locaux en activant l'option `custom-arena-seed-only` dans `/ds settings`.

---

## 📊 Statistiques

### Catégories

| Statistique     | Description                      |
| --------------- | -------------------------------- |
| `wins`        | Nombre de victoires              |
| `kills`       | Nombre de kills                  |
| `deaths`      | Nombre de morts                  |
| `gamesPlayed` | Nombre de parties jouées        |
| `playTime`    | Temps de jeu total (secondes)    |

### Commandes

- `/ds stats` — Voir ses propres stats
- `/ds stats <joueur>` — Voir les stats d'un autre joueur
- `/ds top [catégorie]` — Classement global

### Stockage

- Fichier YAML par joueur dans `plugins/DeathSwap/stats/`
- Sauvegarde automatique toutes les `stats.auto-save-minutes` minutes
- Sauvegarde à la déconnexion du joueur et à l'arrêt du plugin

---

## 🎯 Challenges

> Mode **DeathSwap uniquement**

### Types de challenges

| Type    | Description          | Exemple target           |
| ------- | -------------------- | ------------------------ |
| `CRAFT` | Fabriquer un objet   | `DIAMOND_PICKAXE`       |
| `MINE`  | Miner un bloc        | `DIAMOND_ORE`           |
| `KILL`  | Tuer un mob          | `ENDERMAN`              |

### Récompenses (effets de potion)

| Effet          | Nom config         |
| -------------- | ------------------- |
| Vitesse        | `SPEED`           |
| Force          | `STRENGTH`        |
| Vision Nocturne | `NIGHT_VISION`    |
| Résistance    | `RESISTANCE`      |
| Célérité     | `FASTER_DIGGING`  |

### Exemple config

```yaml
challenges:
  enabled: true
  list:
    - { type: CRAFT, target: DIAMOND_PICKAXE, amount: 1, reward: STRENGTH, description: "Craft une pioche en diamant" }
    - { type: MINE, target: DIAMOND_ORE, amount: 5, reward: SPEED, description: "Mine 5 diamants" }
    - { type: KILL, target: ENDERMAN, amount: 3, reward: NIGHT_VISION, description: "Tue 3 endermen" }
```

---

## 🔊 Sons Personnalisés

Chaque événement du jeu peut avoir un son personnalisé. La liste complète des sons Bukkit est disponible sur la [documentation Spigot](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html).

### Événements sonores

| Son config            | Événement                |
| ---------------------- | ----------------------- |
| `game-start`         | Début de partie         |
| `countdown-tick`     | Tick du countdown       |
| `countdown-go`       | Go ! (début)           |
| `swap`               | Swap entre joueurs      |
| `shuffle`            | Nouveau round           |
| `death`              | Mort d'un joueur       |
| `win`                | Victoire                |
| `round-success`      | Round réussi           |
| `round-fail`         | Round échoué          |
| `challenge-complete` | Challenge complété    |
| `vote-cast`          | Vote enregistré        |

### Format de personnalisation

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
| **Shift+Clic** sur arène | → Ouvre les Settings GUI |
| **Étoile du Nether** | Recharger toute la configuration |
| **Barrier** | Fermer le GUI |

### 🛠️ Configuration en jeu (Settings GUI)

Accessible via le **Shift+Clic** sur une arène dans le Dashboard, ou via `/ds settings` / `/ds admin edit <arène>`.
Ce menu permet de modifier **tous** les aspects de l'arène sans toucher aux fichiers :

- **Mondes** : Changer le monde Lobby et Game (saisie clavier dans le chat)
- **Mode de Jeu** : Changer entre DeathSwap, DeathShuffle, BlockShuffle
- **Gamerules** : Activer/Désactiver les règles (keep_inventory, etc.)
- **Timers** : Ajuster les temps de swap, max game time, etc.
- **Commandes** : Configurer la commande de TP et de Reset
- **Résilience** : Activer les options de démarrage robuste

**Couleurs de statut :**

| Couleur | État |
|---------|------|
| 🟡 Jaune | `WAITING` — En attente de joueurs |
| 🟢 Vert clair | `STARTING` — Le monde charge & les joueurs sont invulnérables |
| 🟩 Vert | `RUNNING` — Partie en cours |
| 🔴 Rouge | `ENDED` — Partie terminée |
| ⬛ Barrier | `DISABLED` — Arène désactivée |

#### 2. Détails de l'Arène

| Bouton | Action |
|--------|--------|
| ⚔️ **Force Start** | Lance le jeu immédiatement (si WAITING/STARTING) |
| 🛑 **Force Stop** | Arrête le jeu (si RUNNING) |
| 💥 **Régénérer Monde** | Reset le monde → **⚠ Confirmation requise** |
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
| 🔮 **Téléporter** | TP l'admin vers le joueur | Téléportation directe |
| 📦 **Voir Inventaire** | Ouvre l'inventaire du joueur | Voir/modifier l'inventaire |
| 👢 **Kick de l'Arène** | Renvoie au hub | Retire de l'arène |
| ⛔ **Bannir** | Kick + `/ban` | → **⚠ Confirmation requise** |
| ⬅️ **Retour** | — | Retour à la liste des joueurs |

#### 5. Écran de Confirmation

Les actions destructives (**Régénérer Monde** et **Bannir**) passent par un écran de confirmation.

| Slot | Item | Action |
|------|------|--------|
| 11 | 🟩 **Laine verte** | **Annuler** — retour |
| 13 | 💥 **TNT** | Info : description + ⚠ "Cette action est irréversible !" |
| 15 | 🟥 **Laine rouge** | **Confirmer** — exécute l'action |

---

## 🏟️ Multi-Arènes

### Concept

Chaque arène est une **instance indépendante** avec :
- Son propre monde de jeu et lobby
- Sa propre configuration complète
- Ses propres joueurs et partie

### Ajouter une arène

**Méthode 1 : Via fichier**

1. Copier `plugins/DeathSwap/arenas/example.yml` → `plugins/DeathSwap/arenas/monarene.yml`
2. Éditer les valeurs (mondes, timers, mode, etc.)
3. `/ds reload`
4. Rejoindre : `/ds join monarene`

**Méthode 2 : Via commande**

1. `/ds admin create monarene` — Crée avec les valeurs par défaut
2. `/ds admin edit monarene` — Ouvre le GUI pour configurer
3. Ou via `/ds admin set monarene <prop> <val>`

**Méthode 3 : Via clonage**

1. `/ds admin clone default monarene` — Copie une arène existante
2. Modifier si nécessaire via GUI ou commandes

### Supprimer une arène

- **Commande :** `/ds admin delete monarene` (avec confirmation)
- **Manuellement :** Supprimer le fichier `arenas/monarene.yml` puis `/ds reload`

---

## 📝 Propriétés Admin Set

La commande `/ds admin set <arène> <propriété> <valeur>` supporte les propriétés suivantes :

| Propriété        | Type    | Description                        | Exemple                              |
| ----------------- | ------- | ---------------------------------- | ------------------------------------ |
| `lobby`         | String  | Monde lobby                        | `/ds admin set default lobby DS_Lobby` |
| `game`          | String  | Monde de jeu                       | `/ds admin set default game DS_Game` |
| `gametype`      | Enum    | Mode de jeu                        | `DEATHSWAP`, `DEATHSHUFFLE`, `BLOCKSHUFFLE` |
| `minplayers`    | Int     | Minimum de joueurs                 | `2`                                  |
| `maxplayers`    | Int     | Maximum de joueurs                 | `20`                                 |
| `uimode`        | Enum    | Mode d'interface                   | `RICH` ou `CLEAN`                   |
| `loadtime`      | Int     | Temps de chargement (secondes)     | `40`                                 |
| `swapmode`      | Enum    | Mode de swap                       | `FIXED` ou `RANDOM`                 |
| `swapinterval`  | Int     | Intervalle de swap fixe (sec)      | `300`                                |
| `swapmin`       | Int     | Swap min aléatoire (sec)          | `120`                                |
| `swapmax`       | Int     | Swap max aléatoire (sec)          | `420`                                |
| `maxgametime`   | Int     | Durée max de la partie (sec)      | `1800`                               |
| `spawnprotection` | Int   | Protection de spawn (sec)          | `30`                                 |
| `roundtimeeasy` | Int     | Temps round facile (sec)           | `90`                                 |
| `roundtimemedium` | Int   | Temps round moyen (sec)            | `70`                                 |
| `roundtimehard` | Int     | Temps round difficile (sec)        | `50`                                 |
| `pvp`           | Boolean | PvP activé                        | `true` / `false`                    |
| `nether`        | Boolean | Nether activé                     | `true` / `false`                    |
| `end`           | Boolean | End activé                        | `true` / `false`                    |
| `resilience`    | Boolean | Active les deux options robustes   | `true` / `false`                    |

---

## 🔧 Dépendances

### Multiverse-Core (recommandé)

Utilisé pour :
- Créer et charger les mondes (`mv create`, `mv load`)
- Téléporter les joueurs (`mv tp`)
- Gérer les mondes de lobby et de jeu

> Le plugin peut fonctionner sans Multiverse si vous configurez une commande de TP alternative via `teleport-command`.

### CyberWorldReset (optionnel)

Utilisé pour :
- Régénérer les mondes de jeu entre les parties (`cwr reset`)
- Disponible depuis le bouton "Régénérer" dans le Dashboard Admin

> Pas nécessaire pour les maps statiques (mettez `world-reset-commands: []`).

### Configuration CyberWorldReset

1. Installez le plugin CyberWorldReset
2. Ajoutez le monde de jeu : `/cwr add DeathSwap_Game`
3. La commande `cwr reset <monde>` sera utilisée automatiquement

---

## ❓ FAQ et Dépannage

### Le plugin ne démarre pas

- Vérifiez que Java 21+ est installé (`java -version`)
- Vérifiez que **Paper 1.21.11** (ou un fork comme Purpur) est utilisé — Spigot/Bukkit ne sont **pas** supportés
- Vérifiez les logs de la console pour les erreurs

### Les mondes ne se chargent pas

- Vérifiez que **Multiverse-Core** est installé et actif
- Vérifiez les noms de mondes dans les fichiers `arenas/*.yml` correspondent à ceux créés dans Multiverse
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

### Les gamerules ne s'appliquent pas

- Vérifiez que les clés utilisent le format **snake_case** (`keep_inventory`, pas `keepInventory`)
- Les gamerules ne s'appliquent qu'au début de la partie

---

## 📂 Fichiers de Configuration de Référence

### `config.yml` par défaut

```yaml
# =========================================
#   Configuration Globale DeathSwap
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
```

### Arène exemple (`arenas/example.yml`)

> **Note :** Copiez ce fichier pour créer de nouvelles arènes (ex: `default.yml`). Le plugin ignore `example.yml`.

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

### Modes de jeu

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
  # ... etc (Jusqu'à 29 causes configurables)
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

  # ... etc (Environ 80 items configurables par défaut)
```

### ⚙️ Interfaces de Configuration (GUIs)

Les modes *Shuffles* disposent désormais d'options avancées accessibles via le bouton **"Configure Mode"** dans le GUI `/ds settings` :

1. **Course d'Item (Item Race)** : (BlockShuffle uniquement) Le premier joueur à trouver/crafter l'item gagne la manche (les autres perdent).
2. **Death Run** : (DeathShuffle uniquement) Le premier joueur à mourir de la bonne cause gagne la manche (les autres perdent).
3. **Cibles/Morts Uniques** : Donne une cible/cause différente à chaque joueur pour ce round.
4. **Configuration du Pool** : Éditez les cibles (Blocks/Causes) directement en jeu grâce au menu paginé (clic gauche pour activer/désactiver, clic droit pour changer la difficulté).
```

---

## 🛡️ Protection Anti-Solo

Pour éviter qu'une partie ne tourne indéfiniment lorsqu'un joueur se retrouve seul (abandon de l'adversaire ou déconnexion), une protection logicielle a été implémentée :

1. Au démarrage, si l'arène contient **moins de 2 joueurs**, le compte à rebours est annulé automatiquement.
2. En cours de partie, si le nombre de joueurs en vie tombe à `1`, le joueur restant remporte instantanément la victoire et la partie s'achève proprement pour retourner au Hub.
3. *Exception :* L'activation du **`debug-mode`** dans les paramètres de l'arène (GUI `/ds settings`) permet de contourner cette sécurité et de lancer ou jouer à une partie tout seul (pratique pour tester les blocs du Shuffle).

---

## 🛠️ API Développeur (Custom Game Modes)

DeathSwap offre désormais une API qui permet à n'importe quel développeur tiers de créer et d'enregistrer ses propres mini-jeux ! Vous pouvez y accéder via la classe `DeathSwapAPI`.

### Comment enregistrer un nouveau mode de jeu ?

1. Assurez-vous d'avoir `DeathSwap` comme dépendance (depend ou softdepend) dans votre `plugin.yml`.
2. Créez une classe étendant `GameInstance` contenant la logique de votre jeu.
3. Enregistrez votre Factory auprès de l'API au démarrage du serveur :

```java
import be.dualsfwshield.deathswap.api.DeathSwapAPI;

public class MonPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Enregistrement du nouveau mode de jeu
        DeathSwapAPI.registerMode(
            "MON_MODE",             // ID Interne
            "Mon Super Mode",       // Nom d'affichage GUI
            "&8[&aMonMode&8]",      // Préfixe de chat par défaut
            
            // Factory pour retourner votre instance de classe
            (plugin, arenaId, config) -> new MonSuperModeInstance(plugin, arenaId, config)
        );
    }
}
```

Une fois cette ligne appelée, les joueurs pourront utiliser votre mode comme n'importe quel mode intégré, les administrateurs le verront dans les GUIs de configuration, et `/ds start` fonctionnera de manière native.

---

## 📝 Notes de Version

### v1.0.0
- 🎮 3 modes de jeu (DeathSwap, DeathShuffle, BlockShuffle)
- 🏟️ Support multi-arènes (fichiers individuels dans `arenas/`)
- 🎛️ Dashboard Admin complet avec GUI
- 📊 Statistiques et leaderboards
- 🗳️ Système de vote de seed
- 🎯 Challenges avec récompenses
- 🔊 Sons personnalisables
- 🎨 Deux modes UI (RICH / CLEAN)
- ⚙️ Gamerules configurables en jeu (snake_case)
- 🌍 Régénération de monde configurable
- ⚠️ Écran de confirmation pour les actions destructives
- 🌐 Support Français et Anglais

---

*Documentation pour DeathSwap v1.0.0 — Plugin par [DualsFWShield](https://dualsfwshield.be)*

---

## 📜 Licence

Ce projet est sous **licence personnalisée**.
Voir le fichier [LICENSE.md](LICENSE.md) pour les détails complets.

* **Utilisation et modification** : Libres (privé ou public).
* **Redistribution** : Autorisée avec crédit obligatoire.
* **Usage commercial** : Strictement interdit sans accord préalable.
