package net.runelite.client.plugins.microbot.util.walker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BankSupplyWithdrawalTest {
    @Test
    public void threeAirRunesInOneSlotDoNotAbortFireAndLawWithdrawals() {
        assertWithdrawals(0, 3, true, true, List.of(556, 554, 563));
    }

    @Test
    public void addingToExistingStackChecksQuantityIncrease() {
        assertWithdrawals(10, 3, true, true, List.of(556, 554, 563));
    }

    @Test
    public void partialStackDoesNotPassJustBecauseTotalExceedsRequestedAmount() {
        assertWithdrawals(10, 2, true, false, List.of(556));
    }

    @Test
    public void rejectedWithdrawalStopsBeforeTheNextItem() {
        assertWithdrawals(0, 0, false, false, List.of(556));
    }

    private void assertWithdrawals(int initialAir, int receivedAir, boolean dispatched,
            boolean expected, List<Integer> expectedAttempts) {
        Map<Integer, Integer> quantities = new HashMap<>();
        quantities.put(556, initialAir);
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        plan.put(556, 3);
        plan.put(554, 1);
        plan.put(563, 1);
        List<Integer> attempts = new ArrayList<>();
        try (MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
             MockedStatic<Rs2Bank> bank = mockStatic(Rs2Bank.class);
             MockedStatic<Global> waits = mockStatic(Global.class)) {
            // Exercise the actual quantity/slot-count implementations against item snapshots.
            inventory.when(() -> Rs2Inventory.count(anyInt())).thenCallRealMethod();
            inventory.when(() -> Rs2Inventory.count(any(Predicate.class))).thenCallRealMethod();
            inventory.when(() -> Rs2Inventory.itemQuantity(anyInt())).thenCallRealMethod();
            inventory.when(() -> Rs2Inventory.itemQuantity(any(Predicate.class))).thenCallRealMethod();
            inventory.when(() -> Rs2Inventory.items(any(Predicate.class))).thenAnswer(invocation -> {
                Predicate<Rs2ItemModel> filter = invocation.getArgument(0);
                return quantities.entrySet().stream().filter(entry -> entry.getValue() > 0).map(entry -> {
                    Rs2ItemModel item = mock(Rs2ItemModel.class);
                    when(item.getId()).thenReturn(entry.getKey());
                    when(item.getQuantity()).thenReturn(entry.getValue());
                    return item;
                }).filter(filter);
            });
            bank.when(() -> Rs2Bank.withdrawX(any(Predicate.class), anyInt())).thenAnswer(invocation -> {
                Predicate<Rs2ItemModel> filter = invocation.getArgument(0);
                int amount = invocation.getArgument(1);
                for (int id : plan.keySet()) {
                    Rs2ItemModel row = mock(Rs2ItemModel.class);
                    when(row.getId()).thenReturn(id);
                    if (!filter.test(row)) continue;
                    attempts.add(id);
                    if (dispatched) quantities.merge(id, id == 556 ? receivedAir : amount, Integer::sum);
                    return dispatched;
                }
                throw new AssertionError("Withdrawal predicate did not select a planned bank row");
            });
            waits.when(() -> Global.sleepUntil(any(BooleanSupplier.class), anyInt()))
                    .thenAnswer(invocation -> ((BooleanSupplier) invocation.getArgument(0)).getAsBoolean());
            assertEquals(expected, Rs2Walker.withdrawBankSupplies(plan));
            assertEquals(expectedAttempts, attempts);
            if (expected) {
                assertEquals(initialAir + 3, Rs2Inventory.itemQuantity(556));
                assertEquals(1, Rs2Inventory.count(556));
                assertEquals(1, Rs2Inventory.itemQuantity(554));
                assertEquals(1, Rs2Inventory.itemQuantity(563));
            }
        }
    }
}
