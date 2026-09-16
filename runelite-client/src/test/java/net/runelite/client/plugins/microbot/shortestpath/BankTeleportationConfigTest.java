package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.runelite.api.coords.WorldPoint;

import static org.junit.Assert.*;

public class BankTeleportationConfigTest {
    @Test
    public void existingDefaultsAndSerializedValuesArePreserved() {
        ShortestPathConfig config = new ShortestPathConfig() { };
        assertEquals(TeleportationItem.INVENTORY, config.useTeleportationItems());
        assertFalse(TeleportationItem.bankWalkingEnabled(config));
        assertEquals(TeleportationItem.NONE, TeleportationItem.valueOf("NONE"));
        assertEquals(TeleportationItem.INVENTORY, TeleportationItem.fromType("Inventory"));
        assertEquals(TeleportationItem.INVENTORY_NON_CONSUMABLE, TeleportationItem.fromType("Inventory (perm)"));
        assertEquals(TeleportationItem.INVENTORY_AND_BANK, TeleportationItem.fromType("Inventory + Bank"));
    }

    @Test
    public void newOptionEnablesBankWalkingWithoutTheAdvancedToggle() {
        ShortestPathConfig config = new ShortestPathConfig() {
            @Override public TeleportationItem useTeleportationItems() { return TeleportationItem.INVENTORY_AND_BANK; }
        };
        assertFalse(config.walkWithBankedTransports());
        assertTrue(TeleportationItem.bankWalkingEnabled(config));
    }

    @Test
    public void advancedBankWalkingStillWorksWithEveryTeleportSetting() {
        for (TeleportationItem option : TeleportationItem.values()) {
            ShortestPathConfig config = new ShortestPathConfig() {
                @Override public boolean walkWithBankedTransports() { return true; }
                @Override public TeleportationItem useTeleportationItems() { return option; }
            };
            assertTrue(TeleportationItem.bankWalkingEnabled(config));
        }
    }

    @Test
    public void bankEligibilityIsLimitedToTheLegAndTransportType() {
        PathfinderConfig planner = new PathfinderConfig(null, new HashMap<>(), List.of(), null, null);
        planner.setBankTeleportsOnly(true);
        for (TransportType type : TransportType.values()) assertFalse(planner.canUseBankFor(type));
        planner.setUseBankItems(true);
        for (TransportType type : TransportType.values()) {
            assertEquals(type.name(), type == TransportType.TELEPORTATION_ITEM
                    || type == TransportType.TELEPORTATION_SPELL, planner.canUseBankFor(type));
        }
        planner.setBankTeleportsOnly(false);
        for (TransportType type : TransportType.values()) assertTrue(planner.canUseBankFor(type));
        planner.setUseBankItems(false);
        for (TransportType type : TransportType.values()) assertFalse(planner.canUseBankFor(type));
    }

    @Test
    public void bankSnapshotCannotUnlockAnObstacleInTeleportOnlyMode() throws Exception {
        PathfinderConfig planner = new PathfinderConfig(null, new HashMap<>(), List.of(), null, null);
        Field carried = PathfinderConfig.class.getDeclaredField("refreshAvailableItemIds");
        Field bank = PathfinderConfig.class.getDeclaredField("refreshBankItemIds");
        carried.setAccessible(true);
        bank.setAccessible(true);
        carried.set(planner, Set.of());
        bank.set(planner, Set.of(123));
        Method usable = PathfinderConfig.class.getDeclaredMethod("hasRequiredItems", Transport.class);
        usable.setAccessible(true);
        Transport item = new Transport(new WorldPoint(3200, 3200, 0), "Item", TransportType.TELEPORTATION_ITEM,
                false, 20, Set.of(Set.of(123)));
        Transport obstacle = new Transport(new WorldPoint(3200, 3200, 0), "Obstacle", TransportType.TRANSPORT,
                false, 20, Set.of(Set.of(123)));
        planner.setBankTeleportsOnly(true);
        assertEquals(false, usable.invoke(planner, item));
        planner.setUseBankItems(true);
        assertEquals(true, usable.invoke(planner, item));
        assertEquals(false, usable.invoke(planner, obstacle));
        planner.setBankTeleportsOnly(false);
        assertEquals(true, usable.invoke(planner, obstacle));
    }
}
