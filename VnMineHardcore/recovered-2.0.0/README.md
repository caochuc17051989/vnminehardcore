# Recovered 2.0.0 Source

This directory was reconstructed from `target/VnMineHardcore-2.0.0.jar`.

- Decompiled Java files were generated from the JAR with Vineflower 1.12.0.
- `pom.xml` and the 38 packaged resources were extracted from the JAR. `plugin.yml` is the filtered runtime copy from the artifact.
- A few local variable names and generic type arguments were adjusted so the decompiled source compiles; behavior should be checked against the JAR before release.
- Bytecode comparison against the current project build found 26 of 30 top-level classes identical to the 2.0.0 artifact. Twenty-four matching files were synchronized from current source to restore their original formatting and comments.
- In `ConfigManager`, `DeathRenameManager`, and `MercenaryManager`, 33, 3, and 375 methods respectively were restored from current source only where bytecode matched and the signature paired uniquely. Different and ambiguous methods retain the 2.0.0 decompilation.
- `BossEventManager`, `DeathPenaltyManager`, and `DisasterManager` were preserved as-is because they contain local edits.
- Comments previously restored into the preserved/decompiled files are references from current source, not original 2.0.0 comments; they may be stale where behavior changed.
- Decompiled source is not the original source text. Original comments, formatting, and some local names cannot be recovered from bytecode.

Validation: `mvn -f VnMineHardcore/recovered-2.0.0/pom.xml compile`