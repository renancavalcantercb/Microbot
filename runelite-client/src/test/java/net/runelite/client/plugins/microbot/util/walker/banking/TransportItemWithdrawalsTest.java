package net.runelite.client.plugins.microbot.util.walker.banking;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransportItemWithdrawalsTest {
    private Transport teleport(boolean consumable, Integer... ids) {
        Transport transport = mock(Transport.class);
        when(transport.getType()).thenReturn(TransportType.TELEPORTATION_ITEM);
        when(transport.getItemIdRequirements()).thenReturn(Set.of(Set.of(ids)));
        when(transport.isConsumable()).thenReturn(consumable);
        return transport;
    }

    @Test
    public void selectsAnActuallyBankedChargeVariantAndOnlyOneItem() {
        assertEquals(Map.of(103, 1), TransportItemWithdrawals.teleports(
                List.of(teleport(true, 101, 102, 103)), Map.of(103, 8), Map.of(), Set.of()).get());
    }

    @Test
    public void carriedAndEquippedVariantsAvoidUnnecessaryWithdrawals() {
        Transport item = teleport(true, 101, 102);
        assertTrue(TransportItemWithdrawals.teleports(List.of(item), Map.of(101, 8), Map.of(102, 1), Set.of()).get().isEmpty());
        assertTrue(TransportItemWithdrawals.teleports(List.of(item), Map.of(101, 8), Map.of(), Set.of(102)).get().isEmpty());
    }

    @Test
    public void consumedTabletsAreSummedAndExistingInventoryIsSubtracted() {
        Transport tablet = teleport(true, 101);
        assertEquals(Map.of(101, 2), TransportItemWithdrawals.teleports(
                List.of(tablet, tablet, tablet), Map.of(101, 2), Map.of(101, 1), Set.of()).get());
        assertFalse(TransportItemWithdrawals.teleports(
                List.of(tablet, tablet, tablet), Map.of(101, 1), Map.of(101, 1), Set.of()).isPresent());
    }

    @Test
    public void permanentTeleportItemIsReused() {
        Transport cape = teleport(false, 101);
        assertEquals(Map.of(101, 1), TransportItemWithdrawals.teleports(
                List.of(cape, cape), Map.of(101, 1), Map.of(), Set.of()).get());
    }

    @Test
    public void missingAndPlaceholderOnlyStacksAreUnavailable() {
        assertFalse(TransportItemWithdrawals.teleports(
                List.of(teleport(true, 101)), Map.of(101, 0), Map.of(), Set.of()).isPresent());
        assertFalse(TransportItemWithdrawals.teleports(
                List.of(teleport(true, 101)), Map.of(102, 3), Map.of(), Set.of()).isPresent());
    }

    @Test
    public void revalidatingLiveContentsCanChooseAnotherChargeVariant() {
        List<Transport> route = List.of(teleport(true, 101, 102));
        assertEquals(Map.of(101, 1), TransportItemWithdrawals.teleports(route, Map.of(101, 1), Map.of(), Set.of()).get());
        assertEquals(Map.of(102, 1), TransportItemWithdrawals.teleports(route, Map.of(102, 1), Map.of(), Set.of()).get());
        assertFalse(TransportItemWithdrawals.teleports(route, Map.of(), Map.of(), Set.of()).isPresent());
    }

    @Test
    public void fullInventoryOnlyAcceptsAnExistingStack() {
        Map<Integer, Integer> withdrawal = Map.of(101, 2);
        Map<Integer, Integer> bank = Map.of(101, 5);
        assertTrue(TransportItemWithdrawals.availableAndFits(withdrawal, bank, Map.of(101, 1), Set.of(101), 0));
        assertFalse(TransportItemWithdrawals.availableAndFits(withdrawal, bank, Map.of(), Set.of(101), 0));
        assertFalse(TransportItemWithdrawals.availableAndFits(withdrawal, bank, Map.of(101, 1), Set.of(), 0));
    }

    @Test
    public void validatesWholeWithdrawalBeforeAnyItemsAreTaken() {
        assertFalse(TransportItemWithdrawals.availableAndFits(Map.of(101, 2), Map.of(101, 1), Map.of(), Set.of(), 28));
        assertFalse(TransportItemWithdrawals.availableAndFits(Map.of(101, 2, 102, 1),
                Map.of(101, 2, 102, 1), Map.of(), Set.of(), 2));
        assertTrue(TransportItemWithdrawals.availableAndFits(Map.of(101, 2, 102, 1),
                Map.of(101, 2, 102, 1), Map.of(), Set.of(101), 2));
    }

    @Test
    public void teleportOnlyPlanningDoesNotWithdrawRunesOrObstacleItems() {
        Transport spell = new Transport(new WorldPoint(3200, 3200, 0), "Spell",
                TransportType.TELEPORTATION_SPELL, false, 20, Set.of(Set.of(101)));
        assertTrue(TransportItemWithdrawals.teleports(List.of(spell), Map.of(101, 5), Map.of(), Set.of()).get().isEmpty());
    }
}
