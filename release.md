# 🚀 Release Notes - DeathSwap (V1.0.0 Globale)

Bienvenue dans la version complète de **DeathSwap** ! Ce document résume l'intégralité du travail accompli, des toutes premières lignes de code aux dernières fonctionnalités de groupe.

## ✨ Fonctionnalités Majeures et Modes de Jeu
* **DeathSwap (Mode Classique)** : Implémentation complète du jeu de base, gestion de l'état de la partie, des joueurs (lobby, en cours, spectateurs) et protection anti-solo.
* **BlockShuffle** : Ajout d'un mode de jeu où les joueurs doivent trouver / se tenir sur des blocs spécifiques. Ajout de plusieurs niveaux de difficulté (incluant une difficulté *Extrême* très ludique) et gestion dynamique des blocs.
* **DeathShuffle** : Ajout d'un mode axé sur différentes manières de mourir, avec des sous-modes originaux comme "Item Race", "Death Run" ou "Unique Targets".
* **Système d'Équipes (Dernier ajout majeur !)** : 
  * Création d'un robuste `TeamManager` avec auto-équilibrage et tailles d'équipes dynamiques.
  * *DeathSwap en équipe* : Swaps collectifs, partages d'inventaires pour toute l'équipe, pénalité de vie (moitié de la vie perdue) pour les survivants lors de la mort d'un coéquipier, et perte d'un pourcentage aléatoire du butin partagé.
  * Les modes *BlockShuffle* et *DeathShuffle* intègrent également pleinement le travail coopératif et les scores de groupe.

## 🎛️ Interfaces Utilisateur (GUIs)
Le plugin a été pensé pour être 100% configurable en jeu via des menus interactifs.
* **Menus d'Administration et Arène** : `SettingsGUI`, `GamerulesGUI`, `ArenaListGUI`, `AdminGUI`, `ArenaDetailsGUI`. Permettent de configurer les règles, les arènes, et les propriétés à la volée.
* **Menus de Configuration des Joueurs** : 
  * `PlayerConfigGUI` (accès via objet Hopper au lobby) pour paramétrer rapidement la partie (Timer de Swap, PvP, protection et rayon de spawn, durée d'aveuglement post-swap, etc.).
  * `TeamSelectGUI` (accès via la Boussole) pour rejoindre manuellement une équipe par couleur.
* **Menus de Gestion / Aide** : `HelpGUI` interactif pour les commandes, et menus divers pour sélectionner les cibles/défis dans les modes Shuffle.

## 🛠️ Configuration & Gestion Avancée
* **ConfigManager Multi-Arènes** : Centralisation complète de la configuration. Chaque arène possède sa propre conf indépendante. Support total pour instancier plusieurs parties en simultané !
* **Gestion des Dimensions** : Prise en charge officielle du Nether et de l'End. Compatible *Multiverse* pour permettre l'assignation de mondes dédiés aux dimensions spécifiques avec reset indépendant.
* **Systèmes Annexes Intégrés** : Statistiques, Leaderboards, système de Votes, gestion des Défis (Challenges), API "DeathSwapAPI" pour les développeurs, et Sound Manager.

## 🌍 Traduction & Localisation (I18n)
* Système complet de localisation avec commutation de langue dynamique en jeu.
* Tous les messages, GUIs et actions traduits en **Français** (`messages_fr.yml`) et **Anglais** (`messages_en.yml`). 
* Support ouvert pour intégrer localement d'autres langues.

## 🐛 Corrections de Bugs & Stabilité (Depuis J0)
* **Compatibilité** : Mise à niveau de l'API pour assurer une validation sur Paper 1.21.11.
* **Améliorations Systèmes** :
  * Fixation des timers du jeu qui ne reposent plus sur des valeurs statiques mais sur des tâches programmées asynchrones (BukkitRunnable).
  * Rechargement dynamique correct (reload plugin) des configurations pour Block et Death Shuffle. Normalisation des clés config.
  * Correction massive des problèmes imports redondants, et de résolution des types génériques (`Map`, `List`, `UUID`) pour finaliser la version buildée.
  * Sécurisation du code empêchant le verrouillage de fichiers (`test-classes`) lors du nettoyage Maven.
  * Plusieurs sprints de stabilisation ("Many fixes to get to a stable version") au grand cœur du moteur de jeu.

## 👨‍💻 Environnement de Projet
* Structuration et documentation propre des sources (`README.md`, `WIKI`).
* Intégration sur GitHub Actions pour les Workflows de Release.
* Définition précise des licences (`LICENSE.md`) et règles d'exclusion (`.gitignore`).
