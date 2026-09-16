package net.runelite.client.plugins.microbot.util.walker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BankedWalkOutcomeTest {
    private final WorldPoint target = new WorldPoint(3002, 3372, 0);

    @Test
    public void satisfiedInteractionConditionIsArrivalBeforeFailureReporting() throws Exception {
        assertOutcome(target, true, WalkerState.EXIT, WalkerState.ARRIVED, false, false);
    }

    @Test
    public void manualCancellationDoesNotReportFailureOrClearAReplacementRoute() throws Exception {
        assertOutcome(null, false, WalkerState.EXIT, WalkerState.EXIT, false, false);
    }

    @Test
    public void unsatisfiedConditionDoesNotTurnCancellationIntoArrival() throws Exception {
        assertOutcome(target, false, WalkerState.EXIT, WalkerState.EXIT, false, false);
    }

    @Test
    public void completionForAnotherDestinationDoesNotCompleteThisWalk() throws Exception {
        assertOutcome(new WorldPoint(3200, 3200, 0), true,
                WalkerState.EXIT, WalkerState.EXIT, false, false);
    }

    @Test
    public void unreachableDestinationStillReportsFailureAndClearsItsRoute() throws Exception {
        assertOutcome(null, false, WalkerState.UNREACHABLE, WalkerState.UNREACHABLE, true, true);
    }

    @Test
    public void movingRemainsInProgress() throws Exception {
        assertOutcome(null, false, WalkerState.MOVING, WalkerState.MOVING, false, false);
    }

    @Test
    public void unreachableOldDestinationDoesNotClearAReplacementRoute() throws Exception {
        assertOutcome(null, false, WalkerState.UNREACHABLE, WalkerState.UNREACHABLE, true, false);
    }

    @SuppressWarnings("unchecked")
    private void assertOutcome(WorldPoint completionTarget, boolean met, WalkerState state,
                               WalkerState expected, boolean failure, boolean cleared) throws Exception {
        Field contextField = Rs2Walker.class.getDeclaredField("walkCompletionContext");
        contextField.setAccessible(true);
        ThreadLocal<Object> context = (ThreadLocal<Object>) contextField.get(null);
        Object previousContext = context.get();
        WorldPoint previousTarget = Rs2Walker.currentTarget;
        try (MockedStatic<WebWalkLog> logs = mockStatic(WebWalkLog.class);
             MockedStatic<Rs2Walker> walker = mockStatic(Rs2Walker.class)) {
            // Preserve the captured completion state from isWalkCancelled, without a game client.
            if (completionTarget != null) {
                Class<?> type = Class.forName(Rs2Walker.class.getName() + "$WalkCompletionContext");
                Constructor<?> constructor = type.getDeclaredConstructor(WorldPoint.class, BooleanSupplier.class);
                constructor.setAccessible(true);
                Object captured = constructor.newInstance(completionTarget, (BooleanSupplier) () -> {
                    throw new AssertionError("Reporting must not re-evaluate the caller's condition");
                });
                Field metField = type.getDeclaredField("met");
                metField.setAccessible(true);
                metField.setBoolean(captured, met);
                context.set(captured);
            } else {
                context.remove();
            }
            Rs2Walker.currentTarget = cleared ? target : new WorldPoint(3100, 3100, 0);
            walker.when(() -> Rs2Walker.finishBankedDirectWalk(target, state)).thenCallRealMethod();

            assertEquals(expected, Rs2Walker.finishBankedDirectWalk(target, state));
            logs.verify(() -> WebWalkLog.bankWalkFailed(target, state), times(failure ? 1 : 0));
            walker.verify(() -> Rs2Walker.setTarget(isNull(), anyString()), times(cleared ? 1 : 0));
        } finally {
            Rs2Walker.currentTarget = previousTarget;
            if (previousContext == null) context.remove(); else context.set(previousContext);
        }
    }
}
