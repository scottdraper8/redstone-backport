---
hide-toc: false
---

# Redstone Backport

Backports select **redstone-related** gameplay and tools from newer Minecraft Java releases onto
**1.20.1**, with matching behavior where practical. Supported loaders: **Fabric**, **Quilt**, and
**Forge**.

:::{important}
This project is still in **alpha**. Today it only ships a **1.20.1** backport target.

The roadmap is to cover redstone-relevant updates across **every** Java Edition release from the
current stable line **down to 1.16.5** (the baseline this matrix starts from). Older intermediate
versions are not separate distribution targets yet—the table tracks the full span of vanilla changes
this mod is meant to absorb over time.
:::

**Repository:**
[github.com/scottdraper8/squinchmods](https://github.com/scottdraper8/squinchmods)

(version-matrix)=

## Version Matrix

Each **Minecraft** row lists redstone-relevant changes in that release. The **Backported?** column
describe how much of that row’s **Redstone Changes** are planned to be backported or already
implemented. Possible values are *Not Planned*, *Planned*, *Implemented*, and *Partial*
(indicating that only some features from that update are implemented or planned).

:::{note}
Behavior is delivered with **mixins** and **loader-specific** hooks to match newer vanilla as closely
as possible, as opposed to other solutions like Forge Events that don't provide full parity.
:::
<!-- markdownlint-disable MD004 MD013 MD032 -->
<!-- MyST `list-table` rows start with `*`; nested bullets use `-`. Long cell lines are not practical
     to hard-wrap without breaking the directive. Line length is enforced elsewhere in this file. -->
:::{list-table}
:header-rows: 1
:class: align-top-table
* - Minecraft
  - Redstone Changes
  - Backported?
  - Notes
* - **1.17**
  - - **Lightning rod** — new block that outputs a redstone signal when struck by lightning; storm-driven and novelty inputs for circuits.
  - |
    **Planned**

    *(already implemented in 1.20.1, the current alpha backport target)*
  - **—**
* - **1.18**
  - - **Simulation distance** — separate from render distance; blocks, fluids, and entities outside it stop simulating, affecting farms and loaded-chunk machines.
  - |
    **Not Planned**

    *(already implemented in 1.20.1, the current alpha backport target)*
  - Attempting to write a distinct simulation engine in pre-1.18 versions would be extremely
    invasive and introduce extensive incompatibilities or unexpected behaviors with other mods.
* - **1.19**
  - - **Sculk / vibration** — sculk sensors and the vibration system (wool occlusion, categories, wireless-style detection).
  - |
    **Planned**

    *(already implemented in 1.20.1, the current alpha backport target)*
  - **—**
* - **1.19.4**
  - - **Jukebox automation** — comparator strength **15** while a disc plays; hoppers and droppers can load/unload discs.
    - **Sculk consistency fixes** — detection fixes for projectiles, hopper minecarts, and related events so vibration contraptions behave predictably.
  - |
    **Planned**

    *(already implemented in 1.20.1, the current alpha backport target)*
  - **—**
* - **1.20**
  - - **Chiseled bookshelf** — comparator reads book layout/stored pattern; hopper/dropper interaction.
    - **Calibrated sculk sensor** — filters vibration frequencies using a side redstone input.
    - **Amethyst resonance** — amethyst blocks can re-emit received vibration frequencies (wireless relay path).
    - **Sculk tuning** — timing, cooldown, output, and simplified vibration frequency rules for sensors.
  - |
    **Planned**

    *(already implemented in 1.20.1, the current alpha backport target)*
  - **—**
* - **1.20.2**
  - - **Sculk + bookshelf** — sensors detect chiseled bookshelf changes made via hoppers/droppers (bugfix-level parity with automation).
  - **Planned**
  - **—**
* - **1.20.3**
  - - **Decorated pots** — single-slot containers with hopper/dropper/minecart IO and comparator on fill level.
    - **`/tick` command** — freeze, step, query, and tick-rate control for testing (Mojang cited redstone debugging).
    - **Crafter (experimental toggle)** — preview only in this release, not the same as stable **1.21** Crafter.
  - **Partial**
  - {doc}`feature-reference/v1-20-3` documents **`/tick`** and tick managers; decorated pots and the **1.20.3** experimental crafter are **not** in the mod (use the **1.21** Crafter backport instead).
* - **1.20.4**
  - - **Decorated pot reliability** — fixes item loss after storage behavior landed in **1.20.3**.
  - **Planned**
  - Depends on vanilla pot storage; **N/A** until pots are backported.
* - **1.20.5**
  - - **Spawn chunks** — smaller default entity-ticking spawn area; **`spawnChunkRadius`** gamerule for farms relying on spawn loading.
    - **Hopper pickup rule** — hoppers skip picking up loose items when a full block is above (with defined exceptions); performance-oriented.
  - **Planned**
  - **—**
* - **1.21**
  - - **Crafter** — autocrafting on redstone pulse, ejection, disabled slots, hopper/dropper IO, comparator over recipe layout (0–9 style).
    - **Copper bulb** — toggles on a pulse; comparator **15** when lit; does not behave like a redstone block for dust routing.
    - **Copper doors & trapdoors** — redstone-openable like wood variants.
    - **Wind charges** — dispensers launch them; bursts can interact with doors, gates, buttons, levers, bells, candles, etc.
    - **Witch loot** — reliably drops **4–8** redstone dust (Looting applies); buff to witch-farm renewability.
    - **Piston audio** — quieter breaking/placement sounds only (no logic change).
  - **Partial**
  - {doc}`Crafter & witch drops <feature-reference/v1-21>` — crafter and witch loot are in; copper blocks, wind charges, and piston audio are **not** backported yet.
* - **1.21.2**
  - - **Redstone experiments** — optional world setting changing wire performance/update order (not default survival parity).
    - **Minecart experiment** — speed/`minecartMaxSpeed` and related rail behavior under optional toggle.
  - **Planned**
  - Experimental toggles, not shipped as default **1.20.1** behavior.
* - **1.21.4**
  - - **Creaking heart** — comparator output scales with distance to its linked creaking mob.
    - **Pale oak set** — buttons, pressure plates, doors, trapdoors (same redstone interactions as other wood sets).
  - **Planned**
  - **—**
* - **1.21.5**
  - - **Random ticks in simulation** — blocks in simulation distance (including some pearl-loaded cases) can receive random ticks; affects vanilla random-tick farms.
    - **Piston audio** — blocks broken by pistons play break sounds (feedback only).
  - **Partially planned**
  - Piston audio update is planned. Random ticks in simulation distance update is *not* planned for versions without a distinct simulation engine (pre-1.18). For version 1.18 and above, it will be included as a toggleable game rule (off by default).
* - **1.21.6**
  - - **Dispenser + shears** — can cut leashes on entities in front; related vibration tweaks.
  - **Planned**
  - **—**
* - **1.21.9**
  - - **Copper golem statue** — comparator strength depends on pose/state.
    - **Shelf** — redstone-powered swap between shelf storage and player hotbar.
  - **Planned**
  - **—**
* - **1.21.10**
  - - **Contraption fixes** — piston interactions with cobwebs/powder snow and related wind-charge/command edge cases.
  - **Planned**
  - **—**
:::

<!-- markdownlint-enable MD004 MD013 MD032 -->

## Documentation

- {doc}`feature-reference/index` — Per-version feature pages (**1.20.3** `/tick`, **1.21** crafter &
  witch drops).
- {doc}`developer/index` — License, contributing, and build instructions.

```{toctree}
:maxdepth: 3
:hidden:

feature-reference/index
developer/index
```
