# Teleport items and runes from the bank

In Web Walker, select **Use teleportation items → Inventory + Bank**. The default remains Inventory.

The walker compares direct travel with travel to an accessible bank, withdrawing teleport items or spell runes, and continuing to the destination. The trip to the bank uses only carried equipment and inventory. Bank contents are eligible when calculating the leg from the bank.

- **Min. bank route savings (tiles)** still applies (default: 80).
- **Bank trip when cache unavailable** controls whether the walker visits a bank to learn its contents. If disabled, it uses the existing bank cache when available.
- **Use teleportation spells** must be enabled to consider spell runes. Magic level, spellbook and other spell requirements still apply. Inventory, rune pouch and equipped rune sources reduce the amount withdrawn; combination runes are resolved to actual bank stacks.
- **Walk with banked transports** remains the broader option for fares and other transport requirements. Inventory + Bank works with that toggle disabled and authorizes withdrawing teleport items and spell runes.
- Existing None, Inventory and Inventory (perm) choices retain their meanings. Selecting None still disables item teleports even with the broader bank option enabled.

At the bank the walker refreshes the route from live bank contents, checks quantities and inventory capacity, withdraws actual items rather than notes, and verifies the inventory increase. It does not deposit your items to make room. A failed withdrawal continues with carried items instead of repeatedly visiting the bank during the same walk. Any items already withdrawn remain in the inventory.

The transport catalogue still determines supported items, charged variants, requirements and destinations. Chronicle requires a known positive charge count; this feature does not guess unknown charges or recharge items. Cached bank contents can be stale, so the final route is recalculated after banking. The completed route preview is explicitly discarded after withdrawal, even when it has the same destination, so the walker uses the newly carried teleport supplies.

## Validation in game

1. With a supported charged teleport item only in the bank, select a sufficiently distant destination near its landing point. Confirm bank visit, one required withdrawal, teleport and arrival.
2. Repeat with the item already in inventory/equipment; confirm no unnecessary withdrawal.
3. Choose a destination where the bank detour does not meet the savings threshold; confirm direct travel.
4. With an inventory full of unrelated items, confirm no automatic deposits or repeated bank trips.
5. Compare bank cache bootstrap enabled/disabled before opening the bank.
6. Pause/clear during the bank leg; confirm no later withdrawal or continuation of the old destination.
7. At Lumbridge bank, with a Cowbell amulet and Varrock Teleport runes in the bank, enable teleportation spells and choose the Grand Exchange. Confirm the route compares both options, withdraws the selected supplies and teleports after closing the bank. Repeat with spell teleports disabled to exercise the item route.

Withdrawal verification uses the increase in item quantity, not occupied slots, so three air runes in one slot count as a complete withdrawal. A failed action logs the item ID, requested amount and observed quantity before continuing with carried items.

When a caller uses `walkUntil` and its interaction condition is satisfied, the bank wrapper reports arrival. Cancellation or logout remains `EXIT` without a duplicate bank-failure warning; the underlying walker logs the stop reason. An unreachable destination still reports failure. Finishing an old walk must not clear a replacement route.

See the focused tests `BankTeleportationConfigTest`, `TransportItemWithdrawalsTest`, `BankedTeleportRunesTest`, `BankSupplyWithdrawalTest`, `BankedWalkContinuationTest` and `BankedWalkOutcomeTest` for offline coverage. These do not replace live interaction validation.
