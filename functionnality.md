# DeathSwap Plugin - Bilan des Fonctionnalités

## 🕹️ Modes de Jeu (Game Instances)
- **DeathSwap** : Échange les joueurs après un cooldown ([GameInstance.java](file:///c:/Users/Toyger/OneDrive/Projects51c/DeathSwap/src/main/java/be/dualsfwshield/deathswap/GameInstance.java)). Gère TP, invulnérabilité au TP, et attribution de victoires quand un meurt.
- **DeathShuffle** : Chaque joueur a une cause de mort assignée (`DeathShuffleInstance.java`). Vérifie les `targetCauses` et `skipCauses`. Timer de shuffle spécifique.
- **BlockShuffle** : Chaque joueur a un bloc cible à tenir ou se tenir dessus (`BlockShuffleInstance.java`). Logique de scan actif (pas basé que sur des events).

## 🛡️ Entités & Managers
- **ArenaManager** : Gère l'attribution des arènes (multi-instances), map joueur -> arène, rechargement d'arène.
- **ConfigManager** : Gère settings globaux (seeds, modes) et configurations par arène (`arenas/<id>.yml`). Sauvegarde les gamerules. Découverte auto des blocs et causes.
- **Stats & Leaderboards** : Stockage des stats (kills, deaths, wins, playtime) par UUID. Affichage en jeu via commandes.
- **VoteManager** : Gère les votes de seeds avant le début de partie via interface de chat basique / item GUI (si implémenté). Durée de vote configurable par arène.
- **SoundManager** : Joue des sons customisables pour différents event clés (`countdown-tick`, `swap`, `win`, etc.).
- **ChallengeManager** : Défis secondaires en jeu (mine X, kill Y).

## 🎛️ GUIs Interactifs (`/ds admin`)
- **ArenaListGUI** : Vue générale des arènes, crée des arènes, teleport aux mondes.
- **SettingsGUI** : Configuration complète d'une arène (`setLobby`, `setGameWorld`, timers, maxPlayers, seeds exclusives, UI mode, Vote Time, Lightning Start).
- **SwapTimerGUI** : Configuration des durées d'échange ou de shuffle.
- **GamerulesGUI** : Bascule des règles du jeu Minecraft directement. Adapte dynamiquement la taille.
- **DeathShuffleGUI** : (Dés)activation et difficulté des causes de morts pour l'arène.
- **BlockShuffleGUI** : Liste des items trouvables et leur état (activé/désactivé/difficulté).
- **PlayerListGUI / PlayerActionGUI** : Gérer les joueurs (kick, inv view).
- **ConfirmationGUI / HelpGUI** : Actions destructrices (delete arena, reset) et aide.

## 📡 Listeners & Événements
- **ReadyListener** : (Lobby) Clique sur item de ready/unready, lit pour déclencher le compte à rebours. Gère déconnexions en attente.
- **GameListener** : (In-Game) Gère la mort d'un joueur, annule le mouvement si le jeu est en attente, protège le lobby des dégâts/build.
- **SpectatorListener** : Gère les permissions en mode spec (cancel interact, drop, etc).
- **BlockShuffle/DeathShuffleListener** : Écoute les actions spécifiques pour ces modes de jeux.

## 🚀 Fonctionnalités Core Protection
- **Anti-Solo** : Annule le start d'une partie avec < 2 joueurs. Fin anticipée si tous déconnectent sauf 1 (sauf Debug Mode).
- **AFK Force-Start** : Si N-1 joueurs sont prêts, un timer forcé démarre pour éviter l'AFK bloqueur.
- **Résilience de Chargement (World-Ready Polling)** : Le système attend obligatoirement que les mondes (Overworld, Nether, End) soient chargés par CWR/Multiverse avant de TP les joueurs, avec timeout de 60s.
- **Démarrage Éclair (Lightning Fast Start)** : Si activé, désactive les votes et réduit tous les délais d'attente (post-reset, post-countdown) à zéro/minimum pour une entrée en jeu instantanée.
- **Mondes Personnalisés & Hooks** : Support natif des dimensions Nether/End avec noms configurables par arène. Hooks activables pour forcer le chargement (`mv load`) avant CWR, ou le déchargement en fin de partie.

---
*Ce fichier garantit qu'aucune de ces fonctionnalités ne disparaisse ou ne soit altérée négativement pendant la phase de nettoyage, d'optimisation et de déduplication du code.*
