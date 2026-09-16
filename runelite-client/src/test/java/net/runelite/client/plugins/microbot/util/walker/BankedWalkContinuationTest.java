package net.runelite.client.plugins.microbot.util.walker;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.shortestpath.TeleportationItem;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.banking.BankedWalkPlan;
import net.runelite.client.plugins.microbot.util.walker.state.WalkerRouteState;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankedWalkContinuationTest {
    private final WorldPoint target = new WorldPoint(3200, 3200, 0);

    private WalkerRouteState route() throws Exception {
        Field field = Rs2Walker.class.getDeclaredField("routeState");
        field.setAccessible(true);
        return (WalkerRouteState) field.get(null);
    }

    @Test
    public void bankWithdrawalDiscardsCompletedWalkingPreviewForTheSameDestination() {
        Pathfinder walkingPreview = mock(Pathfinder.class);
        when(walkingPreview.isDone()).thenReturn(true);
        when(walkingPreview.getTargets()).thenReturn(Set.of(target));
        when(walkingPreview.getPath()).thenReturn(List.of(new WorldPoint(3208, 3220, 2), target));
        Future<?> previousTask = mock(Future.class);
        try (MockedStatic<Rs2PathApi> api = mockStatic(Rs2PathApi.class)) {
            api.when(Rs2PathApi::getPathfinderMutex).thenReturn(new Object());
            api.when(Rs2PathApi::getPathfinder).thenReturn(walkingPreview);
            api.when(Rs2PathApi::getPathfinderFuture).thenReturn(previousTask);
            // This preview previously passed hasCurrentPath and was reused despite the new inventory.
            assertTrue(walkingPreview.isDone() && walkingPreview.getTargets().contains(target));
            Rs2Walker.invalidatePathAfterBanking();
            verify(walkingPreview).cancel();
            verify(previousTask).cancel(true);
            api.verify(() -> Rs2PathApi.setPathfinder(null));
            api.verify(() -> Rs2PathApi.setPathfinderFuture(null));
        }
    }

    @Test
    public void movingKeepsSelectedBankAndRestoresPlanningScope() throws Exception {
        assertMovingRetainsPlan(new WorldPoint(3100, 3100, 0));
    }

    @Test
    public void movingAfterBankFailureDoesNotStartAnotherBankDetour() throws Exception {
        assertMovingRetainsPlan(null);
    }

    private void assertMovingRetainsPlan(WorldPoint bank) throws Exception {
        ShortestPathConfig previous = Rs2Walker.config;
        WalkerRouteState route = route();
        BankedWalkPlan plan = new BankedWalkPlan(target, bank, List.of());
        PathfinderConfig planner = new PathfinderConfig(null, new HashMap<>(), List.of(), null, null);
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
             MockedStatic<Rs2PathApi> api = mockStatic(Rs2PathApi.class);
             MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
             MockedStatic<Rs2Bank> banks = mockStatic(Rs2Bank.class)) {
            api.when(Rs2PathApi::getPathfinderConfig).thenReturn(planner);
            Rs2Walker.config = new ShortestPathConfig() {
                @Override public TeleportationItem useTeleportationItems() { return TeleportationItem.INVENTORY_AND_BANK; }
            };
            route.bankedWalkPlan = plan;
            // A temporarily unavailable player snapshot leaves the selected leg in progress.
            assertEquals(WalkerState.MOVING, Rs2Walker.walkWithBankedTransportsAndState(target, 10, false));
            assertSame(plan, route.bankedWalkPlan);
            assertFalse(planner.isUseBankItems());
            assertFalse(planner.isBankTeleportsOnly());
            banks.verifyNoInteractions();
        } finally {
            route.bankedWalkPlan = null;
            Rs2Walker.config = previous;
        }
    }

    @Test
    public void clearingRouteDiscardsDetourAndBootstrap() throws Exception {
        WalkerRouteState route = route();
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
             MockedStatic<Rs2PathApi> api = mockStatic(Rs2PathApi.class)) {
            api.when(Rs2PathApi::getPathfinderMutex).thenReturn(new Object());
            route.bankedWalkPlan = new BankedWalkPlan(target, target, List.of());
            route.bankBootstrapLocation = target;
            route.bankBootstrapTarget = target;
            Rs2Walker.clearWalkingRoute("test:cancel-bank-detour");
            assertNull(route.bankedWalkPlan);
            assertNull(route.bankBootstrapLocation);
            assertNull(route.bankBootstrapTarget);
        }
    }
}
