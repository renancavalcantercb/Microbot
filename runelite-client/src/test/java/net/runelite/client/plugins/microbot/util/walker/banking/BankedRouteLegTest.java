package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.Rs2RouteRequest;
import net.runelite.client.plugins.microbot.util.walker.Rs2RouteResult;
import net.runelite.client.plugins.microbot.util.walker.Rs2RouteStep;
import net.runelite.client.plugins.microbot.util.walker.Rs2RouteTermination;
import net.runelite.client.plugins.microbot.util.walker.Rs2TransportEdge;
import net.runelite.client.plugins.microbot.util.walker.Rs2TransportType;
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

        Rs2TransportEdge edge = mock(Rs2TransportEdge.class);
        when(edge.getType()).thenReturn(Rs2TransportType.TELEPORTATION_ITEM);
        when(edge.getOrigin()).thenReturn(bankTile);
        when(edge.getDestination()).thenReturn(target);
        Rs2RouteStep fromBankStep = mock(Rs2RouteStep.class);
        when(fromBankStep.getFrom()).thenReturn(bankTile);
        when(fromBankStep.getTo()).thenReturn(target);
        when(fromBankStep.isTransport()).thenReturn(true);
        when(fromBankStep.getTransport()).thenReturn(Optional.of(edge));

        Rs2RouteStep directStep = mock(Rs2RouteStep.class);
        when(directStep.getFrom()).thenReturn(start);
        when(directStep.getTo()).thenReturn(target);
        Rs2RouteStep toBankStep = mock(Rs2RouteStep.class);
        when(toBankStep.getFrom()).thenReturn(start);
        when(toBankStep.getTo()).thenReturn(bankTile);

        Rs2RouteResult directRoute = mock(Rs2RouteResult.class);
        when(directRoute.getPath()).thenReturn(direct);
        when(directRoute.getSteps()).thenReturn(List.of(directStep));
        when(directRoute.getTerminationReason()).thenReturn(Rs2RouteTermination.TARGET_REACHED);
        when(directRoute.isTargetReached(0)).thenReturn(true);

        Rs2RouteResult toBankRoute = mock(Rs2RouteResult.class);
        when(toBankRoute.getPath()).thenReturn(toBank);
        when(toBankRoute.getSteps()).thenReturn(List.of(toBankStep));
        when(toBankRoute.getTerminationReason()).thenReturn(Rs2RouteTermination.TARGET_REACHED);
        when(toBankRoute.isTargetReached(0)).thenReturn(true);

        Rs2RouteResult fromBankRoute = mock(Rs2RouteResult.class);
        when(fromBankRoute.getPath()).thenReturn(fromBank);
        when(fromBankRoute.getSteps()).thenReturn(List.of(fromBankStep));
        when(fromBankRoute.getTransportSteps()).thenReturn(List.of(fromBankStep));
        when(fromBankRoute.getTerminationReason()).thenReturn(Rs2RouteTermination.TARGET_REACHED);
        when(fromBankRoute.isTargetReached(0)).thenReturn(true);

        try (MockedStatic<Rs2PathApi> api = mockStatic(Rs2PathApi.class);
             MockedStatic<Rs2Walker> walker = mockStatic(Rs2Walker.class);
             MockedStatic<Rs2Bank> banks = mockStatic(Rs2Bank.class);
             MockedStatic<WebWalkLog> logs = mockStatic(WebWalkLog.class)) {
            api.when(() -> Rs2PathApi.plan(any())).thenAnswer(invocation -> {
                Rs2RouteRequest request = invocation.getArgument(0);
                if (request.getPurpose() == Rs2RouteRequest.Purpose.BANK_ROUTE_TO_BANK) {
                    return toBankRoute;
                }
                if (request.getPurpose() == Rs2RouteRequest.Purpose.BANK_ROUTE_FROM_BANK) {
                    return fromBankRoute;
                }
                return directRoute;
            });
            api.when(() -> Rs2PathApi.override("preferTransportToTarget", false)).thenReturn(false);
            banks.when(Rs2Bank::bankItems).thenReturn(List.of());
            banks.when(() -> Rs2Bank.getNearestBank(start)).thenReturn(bank);
            walker.when(() -> Rs2Walker.getTotalTilesFromPath(direct, target)).thenReturn(500);
            walker.when(() -> Rs2Walker.getTotalTilesFromPath(toBank, bankTile)).thenReturn(30);
            walker.when(() -> Rs2Walker.getTotalTilesFromPath(fromBank, target)).thenReturn(20);

            TransportRouteAnalysis result = Rs2WalkerBankingPlanner.compareRoutes(start, target);
            assertEquals(bankTile, result.getBankLocation());
            assertEquals(bankTile, result.getBankLocation());
            assertEquals(List.of(fromBankStep), result.getRouteFromBankSteps());
            assertTrue(result.getTileSavings() >= 80);
            assertFalse(result.isDirectIsFaster());
        }
    }
}
