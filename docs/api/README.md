# API Reference

Hand-maintained per-class references for the Microbot helper API
(`runelite-client/src/main/java/net/runelite/client/plugins/microbot/util`).

Each page lists the class overview and its public method signatures. They are a
reading aid, not the source of truth — when a page and the code disagree, the code
wins. For an offline signature lookup against the live source, use
`./microbot-cli ct <method>` (see [MICROBOT_CLI.md](../MICROBOT_CLI.md)).

**Before using any of these to read game entities**, read the
[Queryable API guide](../../runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md):
caches and queryables must never be instantiated directly, and terminal helpers
that resolve live names or widget text must run on the client thread.

## Player & Combat

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Player](Rs2Player.md) | `util/player` | Local player state, health, prayer, run energy, animations |
| [Rs2Pvp](Rs2Pvp.md) | `util/player` | Wilderness level and PvP range checks |
| [Rs2Combat](Rs2Combat.md) | `util/combat` | Attack style, special attack, auto-retaliate |
| [Rs2Prayer](Rs2Prayer.md) | `util/prayer` | Prayer toggling and book state |
| [Rs2Antiban](Rs2Antiban.md) | `util/antiban` | Antiban activity/timing profiles |

## Items & Storage

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Inventory](Rs2Inventory.md) | `util/inventory` | Inventory queries, interaction, drop logic |
| [Rs2Bank](Rs2Bank.md) | `util/bank` | Bank open/deposit/withdraw and tab handling |
| [Rs2DepositBox](Rs2DepositBox.md) | `util/depositbox` | Deposit box interaction |
| [Rs2Equipment](Rs2Equipment.md) | `util/equipment` | Worn equipment queries and un/equipping |
| [Rs2GroundItem](Rs2GroundItem.md) | `util/grounditem` | Ground item lookup and looting |
| [Rs2Shop](Rs2Shop.md) | `util/shop` | Shop interfaces, buy/sell |
| [Rs2GrandExchange](Rs2GrandExchange.md) | `util/grandexchange` | GE offers, collect, abort |
| [Rs2RunePouch](Rs2RunePouch.md) | `util/inventory` | Rune pouch contents and refills |
| [Rs2Gembag](Rs2Gembag.md) | `util/inventory` | Gem bag contents |
| [Rs2Food](Rs2Food.md) | `util/misc` | Food identification and eating |

See also the [items entity guide](../entity-guides/items.md) for the footguns these
helpers do not protect you from.

## World & Entities

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Npc](Rs2Npc.md) | `util/npc` | NPC lookup, interaction, attackability |
| [Rs2GameObject](Rs2GameObject.md) | `util/gameobject` | Objects, walls, decorations, ground objects |
| [Rs2Cannon](Rs2Cannon.md) | `util/gameobject` | Dwarf multicannon setup and refill |
| [Rs2Tile](Rs2Tile.md) | `util/tile` | Tile state and reachability helpers |
| [Rs2WorldPoint](Rs2WorldPoint.md) | `util/coords` | World/local/scene coordinate conversion |

## Movement & Camera

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Walker](Rs2Walker.md) | `util/walker` | Web walking, transports, obstacle handling |
| [Rs2MiniMap](Rs2MiniMap.md) | `util/walker` | Minimap projection and walk clicks |
| [Rs2Camera](Rs2Camera.md) | `util/camera` | Camera pitch/yaw and turn-to-target |

See also the [movement entity guide](../entity-guides/movement.md) and the walker
notes in [walker-audit.md](../walker-audit.md).

## Interfaces & Input

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Widget](Rs2Widget.md) | `util/widget` | Widget lookup, text matching, clicking |
| [Rs2Tab](Rs2Tab.md) | `util/tabs` | Sidebar tab switching |
| [Rs2Dialogue](Rs2Dialogue.md) | `util/dialogues` | NPC dialogue progression and option select |
| [Rs2Keyboard](Rs2Keyboard.md) | `util/keyboard` | Key press/type helpers |
| [Mouse](Mouse.md) | `util/mouse` | Abstract mouse interface (click, move, drag, scroll) |
| [NewMenuEntry](NewMenuEntry.md) | `util/menu` | Fluent builder for synthetic menu entries |
| [MouseMacroRecorder](MouseMacroRecorder.md) | `mouserecorder` | Plugin that records mouse/menu events to JSON |

Widget IDs shift between game updates — see
[Settings & Widget Debugging](../../runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/settings/AGENTS.md).

## Magic & Skilling

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Magic](Rs2Magic.md) | `util/magic` | Casting, spellbook state, teleports |
| [Rs2Spells](Rs2Spells.md) | `util/magic` | Spell definitions and requirements |
| [Rs2Farming](Rs2Farming.md) | `util/farming` | Patch state and farming actions |
| [Rs2HuntKit](Rs2HuntKit.md) | `util/huntkit` | Hunter kit contents and traps |

## Client & Configuration

| Class | Package | Covers |
|-------|---------|--------|
| [Rs2Settings](Rs2Settings.md) | `util/settings` | In-game settings, varbits, toggles |
| [Rs2Reflection](Rs2Reflection.md) | `util/reflection` | Reflective client access |
| [Rs2LeaguesTransport](Rs2LeaguesTransport.md) | `util/leaguetransport` | League relic transport unlocks |

## Coverage

These 37 pages cover the helpers scripts call most. `util/` contains roughly 90
`Rs2*` types in total; models, enums, data holders (`Rs2NpcModel`, `Rs2ItemModel`,
`Rs2Potion`, the `Rs2Door*`/`Rs2Walker*` internals, …) are documented in source
Javadoc only. Do not assume a missing page means a missing helper — check
`util/` or `./microbot-cli ct <method>` first.

When you add or rename a page here, add it to the matching table above and keep
its `## [Back](README.md)` link intact.
