package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.magic.RuneFilter;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class BankedTeleportRunesTest {
    @Test
    public void varrockWithdrawsOnlyOneCast() {
        Map<Integer, Integer> bank = Map.of(Runes.AIR.getItemId(), 100,
                Runes.FIRE.getItemId(), 100, Runes.LAW.getItemId(), 100);
        assertEquals(Map.of(Runes.AIR.getItemId(), 3, Runes.FIRE.getItemId(), 1, Runes.LAW.getItemId(), 1),
                TransportItemWithdrawals.addRunes(Map.of(), Rs2Spells.VARROCK_TELEPORT.getRequiredRunes(), bank).get());
    }

    @Test
    public void combinationRunesResolveToActualWithdrawableIds() {
        Map<Integer, Integer> bank = Map.of(Runes.SMOKE.getItemId(), 3, Runes.LAW.getItemId(), 1);
        assertEquals(Map.of(Runes.SMOKE.getItemId(), 3, Runes.LAW.getItemId(), 1),
                TransportItemWithdrawals.addRunes(Map.of(), Rs2Spells.VARROCK_TELEPORT.getRequiredRunes(), bank).get());
    }

    @Test
    public void insufficientRunesRejectTheWholePlan() {
        Map<Integer, Integer> bank = Map.of(Runes.AIR.getItemId(), 3, Runes.FIRE.getItemId(), 1);
        assertFalse(TransportItemWithdrawals.addRunes(Map.of(), Rs2Spells.VARROCK_TELEPORT.getRequiredRunes(), bank).isPresent());
    }

    @Test
    public void preservesItemWithdrawalsAndUsesOnlyTheRuneDeficit() {
        assertEquals(Map.of(33104, 1, Runes.AIR.getItemId(), 1), TransportItemWithdrawals.addRunes(
                Map.of(33104, 1), Map.of(Runes.AIR, 1), Map.of(33104, 1, Runes.AIR.getItemId(), 30)).get());
    }

    @Test
    public void repeatedSpellsSumCostsBeforeSubtractingCarriedRunes() {
        Transport varrock = new Transport(new WorldPoint(3213, 3424, 0), "Varrock Teleport",
                TransportType.TELEPORTATION_SPELL, false, 20, Set.of());
        try (MockedStatic<Rs2Magic> magic = mockStatic(Rs2Magic.class, CALLS_REAL_METHODS)) {
            magic.when(() -> Rs2Magic.getRs2Spell("Varrock Teleport")).thenReturn(Rs2Spells.VARROCK_TELEPORT);
            magic.when(() -> Rs2Magic.getRunes(any(RuneFilter.class))).thenAnswer(invocation -> {
                RuneFilter filter = invocation.getArgument(0);
                assertFalse("bank is a withdrawal source, not already-carried runes", filter.isIncludeBank());
                assertTrue(filter.isIncludeEquipment());
                assertTrue(filter.isIncludeInventory());
                assertTrue(filter.isIncludeRunePouch());
                // Fire staff + 2 air runes in the pouch + one carried law rune.
                return new HashMap<>(Map.of(Runes.FIRE, Integer.MAX_VALUE, Runes.AIR, 2, Runes.LAW, 1));
            });
            assertEquals(Map.of(Runes.AIR, 4, Runes.LAW, 1),
                    Rs2WalkerBankingPlanner.getMissingSpellRunes(List.of(varrock, varrock)).get());
        }
    }

    @Test
    public void unknownSpellDoesNotProduceAFalseSuccessfulWithdrawalPlan() {
        Transport unknown = new Transport(new WorldPoint(3213, 3424, 0), "Unknown spell",
                TransportType.TELEPORTATION_SPELL, false, 20, Set.of());
        try (MockedStatic<Rs2Magic> magic = mockStatic(Rs2Magic.class)) {
            assertFalse(Rs2WalkerBankingPlanner.getMissingSpellRunes(List.of(unknown)).isPresent());
        }
    }
}
