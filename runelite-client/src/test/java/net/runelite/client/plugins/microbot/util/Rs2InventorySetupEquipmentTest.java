package net.runelite.client.plugins.microbot.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetupsItem;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetupsStackCompareID;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class Rs2InventorySetupEquipmentTest {
    private MockedStatic<Microbot> microbot;
    private MockedStatic<Rs2Bank> bank;
    private MockedStatic<Rs2Inventory> inventory;
    private MockedStatic<Rs2Equipment> equipment;
    private MockedStatic<Global> global;
    private final List<String> actions = new ArrayList<>();
    private final Set<Integer> inventoryIds = new HashSet<>();
    private final Set<String> equippedNames = new HashSet<>();
    private Rs2InventorySetup loader;
    private ScheduledFuture<?> scheduler;
    private int failedWithdrawalId = -1;

    @Before
    public void setUp() {
        microbot = mockStatic(Microbot.class);
        bank = mockStatic(Rs2Bank.class);
        inventory = mockStatic(Rs2Inventory.class);
        equipment = mockStatic(Rs2Equipment.class);
        global = mockStatic(Global.class);
        InventorySetup setup = mock(InventorySetup.class);
        when(setup.getEquipment()).thenReturn(Arrays.asList(
                row(100, "Helmet", 1, false), row(200, "Arrows", 50, false),
                InventorySetupsItem.getDummyItem()));
        scheduler = mock(ScheduledFuture.class);
        loader = new Rs2InventorySetup(setup, scheduler);

        bank.when(Rs2Bank::isOpen).thenReturn(true);
        bank.when(() -> Rs2Bank.setWithdrawAs(false)).thenReturn(true);
        bank.when(() -> Rs2Bank.verifyBankMirrorAfterOpen(anyBoolean(), anyInt())).thenReturn(true);
        bank.when(() -> Rs2Bank.hasItem(anyInt())).thenReturn(true);
        bank.when(() -> Rs2Bank.withdrawOne(anyInt())).thenAnswer(call -> withdraw(call.getArgument(0)));
        bank.when(() -> Rs2Bank.withdrawX(anyInt(), anyInt())).thenAnswer(call -> withdraw(call.getArgument(0)));
        bank.when(() -> Rs2Bank.wearItem(anyInt())).thenAnswer(call -> {
            int id = call.getArgument(0);
            actions.add("equip " + id);
            if (!inventoryIds.remove(id)) return false;
            equippedNames.add(id == 100 ? "Helmet" : "Arrows");
            return true;
        });
        bank.when(Rs2Bank::depositAll).thenAnswer(call -> {
            actions.add("deposit");
            inventoryIds.clear();
            return true;
        });
        bank.when(() -> Rs2Bank.syncBankInventoryAfterChange(anyInt())).thenReturn(true);
        inventory.when(Rs2Inventory::items).thenAnswer(call -> Stream.empty());
        inventory.when(Rs2Inventory::emptySlotCount).thenReturn(28);
        inventory.when(() -> Rs2Inventory.hasItem(anyInt())).thenAnswer(call -> inventoryIds.contains(call.getArgument(0)));
        equipment.when(() -> Rs2Equipment.isWearing(anyString())).thenAnswer(call -> equippedNames.contains(call.getArgument(0)));
        equipment.when(() -> Rs2Equipment.isWearing(anyString(), anyBoolean())).thenAnswer(call -> equippedNames.contains(call.getArgument(0)));
        global.when(() -> Global.sleepUntil(any(BooleanSupplier.class), anyInt()))
                .thenAnswer(call -> ((BooleanSupplier) call.getArgument(0)).getAsBoolean());
    }

    @After
    public void tearDown() {
        global.close();
        equipment.close();
        inventory.close();
        bank.close();
        microbot.close();
    }

    @Test
    public void withdrawsWholeBatchBeforeEquippingIncludingAmmunition() {
        assertTrue(loader.loadEquipment(false));
        assertEquals(Arrays.asList("withdraw 100", "withdraw 200", "equip 100", "equip 200"), actions);
        bank.verify(() -> Rs2Bank.withdrawX(200, 50));
        bank.verify(() -> Rs2Bank.setWithdrawAs(false), times(2));
    }

    @Test
    public void delaysEquippingGearAlreadyInInventoryUntilWithdrawalsFinish() {
        inventoryIds.add(100);
        assertTrue(loader.loadEquipment(false));
        assertEquals(Arrays.asList("withdraw 200", "equip 100", "equip 200"), actions);
    }

    @Test
    public void keepsGearAlreadyEquipped() {
        equippedNames.add("Helmet");
        assertTrue(loader.loadEquipment(false));
        assertEquals(Arrays.asList("withdraw 200", "equip 200"), actions);
    }

    @Test
    public void freesRoomForWholeBatchEvenWhenInventoryIsNotFull() {
        inventory.when(Rs2Inventory::emptySlotCount).thenReturn(1);
        assertTrue(loader.loadEquipment(false));
        assertEquals(Arrays.asList("deposit", "withdraw 100", "withdraw 200", "equip 100", "equip 200"), actions);
    }

    @Test
    public void failedWithdrawalDoesNotStartEquipping() {
        failedWithdrawalId = 200;
        assertFalse(loader.loadEquipment(false));
        assertEquals(Arrays.asList("withdraw 100"), actions);
    }

    @Test
    public void cancellationBetweenPhasesDoesNotStartEquipping() {
        when(scheduler.isCancelled()).thenReturn(false, false, true);
        assertFalse(loader.loadEquipment(false));
        assertEquals(Arrays.asList("withdraw 100", "withdraw 200"), actions);
    }

    private boolean withdraw(int id) {
        if (id == failedWithdrawalId) return false;
        actions.add("withdraw " + id);
        inventoryIds.add(id);
        return true;
    }

    private static InventorySetupsItem row(int id, String name, int quantity, boolean fuzzy) {
        return new InventorySetupsItem(id, name, quantity, fuzzy, InventorySetupsStackCompareID.None, false, -1);
    }
}
