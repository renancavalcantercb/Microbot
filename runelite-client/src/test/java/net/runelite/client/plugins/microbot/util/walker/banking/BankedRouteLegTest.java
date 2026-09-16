package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.TransportRouteAnalysis;
import net.runelite.client.plugins.microbot.util.walker.WebWalkLog;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BankedRouteLegTest {
    @Test
    public void bankItemsOnlyBecomeEligibleAfterTheBankAndSelectedTransportsSurviveRestore() {
        WorldPoint start = new WorldPoint(3200, 3200, 0);
        WorldPoint target = new WorldPoint(3300, 3300, 0);
        BankLocation bank = BankLocation.values()[0];
        WorldPoint bankTile = bank.getWorldPoint();
        List<WorldPoint> direct = List.of(start, target);
        List<WorldPoint> toBank = List.of(start, bankTile);
        List<WorldPoint> fromBank = List.of(bankTile, target);
        Transport item = new Transport(target, "Test teleport", TransportType.TELEPORTATION_ITEM,
                false, 20, Set.of(Set.of(101)));
        PathfinderConfig planner = spy(new PathfinderConfig(null, new HashMap<>(), List.of(), null, null));
        doNothing().when(planner).refresh();
        doNothing().when(planner).refresh(any(WorldPoint.class));
        try (MockedStatic<Rs2PathApi> api = mockStatic(Rs2PathApi.class);
             MockedStatic<Rs2Walker> walker = mockStatic(Rs2Walker.class);
             MockedStatic<Rs2Bank> banks = mockStatic(Rs2Bank.class);
             MockedStatic<WebWalkLog> logs = mockStatic(WebWalkLog.class)) {
            api.when(Rs2PathApi::getPathfinderConfig).thenReturn(planner);
            api.when(() -> Rs2PathApi.override("preferTransportToTarget", false)).thenReturn(false);
            banks.when(Rs2Bank::bankItems).thenReturn(List.of());
            banks.when(() -> Rs2Bank.getNearestBank(start)).thenAnswer(ignored -> {
                assertFalse(planner.isUseBankItems());
                return bank;
            });
            walker.when(() -> Rs2Walker.getWalkPath(start, target)).thenAnswer(ignored -> {
                assertFalse(planner.isUseBankItems());
                return direct;
            });
            walker.when(() -> Rs2Walker.getWalkPath(start, bankTile)).thenAnswer(ignored -> {
                assertFalse(planner.isUseBankItems());
                return toBank;
            });
            walker.when(() -> Rs2Walker.getWalkPath(bankTile, target)).thenAnswer(ignored -> {
                assertTrue(planner.isUseBankItems());
                return fromBank;
            });
            walker.when(() -> Rs2Walker.getTotalTilesFromPath(direct, target)).thenReturn(500);
            walker.when(() -> Rs2Walker.getTotalTilesFromPath(toBank, bankTile)).thenReturn(30);
            walker.when(() -> Rs2Walker.getTotalTilesFromPath(fromBank, target)).thenReturn(20);
            walker.when(() -> Rs2Walker.getTransportsForPath(fromBank, 0, TransportType.TELEPORTATION_SPELL, true))
                    .thenReturn(List.of(item));

            TransportRouteAnalysis result = Rs2WalkerBankingPlanner.compareRoutes(start, target);
            assertFalse(planner.isUseBankItems());
            assertEquals(bankTile, result.getBankLocation());
            assertEquals(List.of(item), result.getBankLegTransports());
            assertTrue(result.getTileSavings() >= 80);
            assertFalse(result.isDirectIsFaster());
        }
    }
}
