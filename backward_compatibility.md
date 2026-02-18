# Backward Compatibility Guide

This document outlines breaking changes, migration steps, and compatibility notes for DeathSwap versions.

## Version 1.1.0 (Current)

### Breaking Changes
- **Game Mode Configurations**:
  - `BlockShuffle` targets and `DeathShuffle` causes are no longer hardcoded.
  - They are now loaded from customizable YAML files in the `modes/` directory:
    - `plugins/DeathSwap/modes/blockshuffle.yml`
    - `plugins/DeathSwap/modes/deathshuffle.yml`
  - **Action Required**: If updating from a previous dev build, ensure you allow the plugin to generate these files or manually create them.

- **Configuration Structure**:
  - `config.yml` remains largely the same, but mode-specific lists are removed from code/config and moved to separate files.

### Migration Guide
1. **Backup** your existing `plugins/DeathSwap` folder.
2. **Delete** `config.yml` if you want to regenerate it with fresh comments (optional).
3. **Start** the server with the new JAR.
4. The plugin will automatically create the `modes/` folder and default configuration files.
5. **Verify** permissions in `plugin.yml` or your permissions plugin (LuckPerms, etc.):
   - `deathswap.play` (Default: true)
   - `deathswap.admin` (Default: op)

## API Compability
- **Events**:
  - `GameStartEvent`, `PlayerSwapEvent`, `GameEndEvent` signatures remain stable.
- **Commands**:
  - `/ds` subcommands structure is preserved.

## Future Deprecations
- **Legacy HUD**: The `CLEAN` UI mode might be deprecated in favor of a more modular resource-pack based UI in v2.0.
