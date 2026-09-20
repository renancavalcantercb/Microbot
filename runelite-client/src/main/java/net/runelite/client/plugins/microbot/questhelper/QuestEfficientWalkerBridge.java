package net.runelite.client.plugins.microbot.questhelper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic reflection bridge from Quest Helper to Efficient Walker.
 *
 * Provides intelligent routing using Efficient Walker when available, with
 * automatic fallback to Microbot's built-in Web Walker (Rs2Walker) when
 * Efficient Walker reports BLOCKED or cannot service the route (e.g. boats/ferries).
 */
public class QuestEfficientWalkerBridge
{
    private static final Logger log = LoggerFactory.getLogger(QuestEfficientWalkerBridge.class);
    private static final String EW_PLUGIN_CLASS = "net.runelite.client.plugins.microbot.efficientwalker.EfficientWalkerPlugin";

    private Object walkerInstance;
    private Method walkToMethod;
    private Method cancelMethod;
    private Method getStatusMethod;
    private Method getPlanningFailureMethod;
    private boolean initialized;

    // Track active target and destinations that EW cannot route to
    private volatile WorldPoint activeTarget;
    private final Map<WorldPoint, Long> blockedTargets = new ConcurrentHashMap<>();
    private static final long BLOCKED_CACHE_DURATION_MS = 120_000L;

    public boolean isAvailable()
    {
        init();
        return walkerInstance != null;
    }

    private synchronized void init()
    {
        if (initialized && walkerInstance != null)
        {
            return;
        }
        try
        {
            Plugin ewPlugin = Microbot.getPlugin(EW_PLUGIN_CLASS);
            if (ewPlugin == null && Microbot.getPluginManager() != null)
            {
                for (Plugin p : Microbot.getPluginManager().getPlugins())
                {
                    if (p.getClass().getName().equals(EW_PLUGIN_CLASS))
                    {
                        ewPlugin = p;
                        break;
                    }
                }
            }
            if (ewPlugin != null && Microbot.getPluginManager().isPluginEnabled(ewPlugin))
            {
                Method getWalker = ewPlugin.getClass().getMethod("getWalker");
                walkerInstance = getWalker.invoke(ewPlugin);
                if (walkerInstance != null)
                {
                    walkToMethod = walkerInstance.getClass().getMethod("walkTo", WorldPoint.class);
                    cancelMethod = walkerInstance.getClass().getMethod("cancel");
                    getStatusMethod = walkerInstance.getClass().getMethod("getStatus");
                    getPlanningFailureMethod = walkerInstance.getClass().getMethod("getPlanningFailure");
                    initialized = true;
                    log.info("Quest Helper EW: Successfully connected to Efficient Walker");
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("Quest Helper EW: Failed to connect to Efficient Walker: {}", ex.getMessage());
            walkerInstance = null;
        }
    }

    public boolean walkTo(WorldPoint destination)
    {
        return walkTo(destination, 0, null);
    }

    public boolean walkTo(WorldPoint destination, int distance)
    {
        return walkTo(destination, distance, null);
    }

    public boolean walkTo(WorldPoint destination, int distance, java.util.function.BooleanSupplier isRunning)
    {
        init();
        if (walkerInstance == null || walkToMethod == null)
        {
            return invokeRs2Walker(destination, distance);
        }

        try
        {
            // If already within requested distance, arrival confirmed
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc != null && playerLoc.getPlane() == destination.getPlane()
                    && distance > 0 && playerLoc.distanceTo(destination) <= distance)
            {
                cancel();
                return true;
            }

            // Check if this destination was recently confirmed BLOCKED by EW
            long now = System.currentTimeMillis();
            Long blockedUntil = blockedTargets.get(destination);
            if (blockedUntil != null && now < blockedUntil)
            {
                return invokeRs2Walker(destination, distance);
            }

            // Submit destination to EW
            activeTarget = destination;
            boolean accepted = (Boolean) walkToMethod.invoke(walkerInstance, destination);
            if (!accepted)
            {
                String failure = getPlanningFailure();
                log.warn("Quest Helper EW: Efficient Walker did not accept route to {} (failure: {}), falling back to Rs2Walker", destination, failure);
                blockedTargets.put(destination, now + BLOCKED_CACHE_DURATION_MS);
                activeTarget = null;
                return invokeRs2Walker(destination, distance);
            }

            // Phase 1: Wait for planning to resolve (up to 4000ms)
            long planningDeadline = now + 4000;
            String status = getStatus();
            while (System.currentTimeMillis() < planningDeadline && ("PLANNING".equalsIgnoreCase(status) || "IDLE".equalsIgnoreCase(status)))
            {
                if (isRunning != null && !isRunning.getAsBoolean())
                {
                    cancel();
                    return false;
                }
                playerLoc = Rs2Player.getWorldLocation();
                if (playerLoc != null && playerLoc.getPlane() == destination.getPlane() && distance > 0 && playerLoc.distanceTo(destination) <= distance)
                {
                    cancel();
                    return true;
                }
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException ignored)
                {
                    Thread.currentThread().interrupt();
                    cancel();
                    return false;
                }
                status = getStatus();
            }

            // Check if planning resulted in BLOCKED or timed out
            if ("BLOCKED".equalsIgnoreCase(status) || "PLANNING".equalsIgnoreCase(status))
            {
                String failure = getPlanningFailure();
                log.warn("Quest Helper EW: Route to {} blocked/unsupported (status: {}, failure: {}), falling back to Rs2Walker", destination, status, failure);
                blockedTargets.put(destination, System.currentTimeMillis() + BLOCKED_CACHE_DURATION_MS);
                cancel();
                return invokeRs2Walker(destination, distance);
            }

            // Phase 2: Active walking loop - track until arrival, dialogue, or failure
            while (isRunning == null || isRunning.getAsBoolean())
            {
                playerLoc = Rs2Player.getWorldLocation();
                if (playerLoc != null && playerLoc.getPlane() == destination.getPlane()
                        && playerLoc.distanceTo(destination) <= Math.max(distance, 1))
                {
                    cancel();
                    return true;
                }

                if (net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue.isInDialogue())
                {
                    cancel();
                    return true;
                }

                status = getStatus();
                if ("ARRIVED".equalsIgnoreCase(status))
                {
                    cancel();
                    return true;
                }
                if ("BLOCKED".equalsIgnoreCase(status))
                {
                    String failure = getPlanningFailure();
                    log.warn("Quest Helper EW: Walk interrupted/blocked (failure: {}), falling back to Rs2Walker", failure);
                    blockedTargets.put(destination, System.currentTimeMillis() + BLOCKED_CACHE_DURATION_MS);
                    cancel();
                    return invokeRs2Walker(destination, distance);
                }
                if ("IDLE".equalsIgnoreCase(status))
                {
                    if (playerLoc != null && playerLoc.getPlane() == destination.getPlane()
                            && playerLoc.distanceTo(destination) <= Math.max(distance, 2))
                    {
                        cancel();
                        return true;
                    }
                    log.warn("Quest Helper EW: Transitioned to IDLE before arrival, falling back to Rs2Walker");
                    return invokeRs2Walker(destination, distance);
                }

                try
                {
                    Thread.sleep(150);
                }
                catch (InterruptedException ignored)
                {
                    Thread.currentThread().interrupt();
                    cancel();
                    return false;
                }
            }

            cancel();
            return false;
        }
        catch (Exception ex)
        {
            log.warn("Quest Helper EW: Error during walkTo, falling back to Rs2Walker", ex);
            cancel();
            return invokeRs2Walker(destination, distance);
        }
    }

    public void cancel()
    {
        activeTarget = null;
        if (walkerInstance != null && cancelMethod != null)
        {
            try
            {
                cancelMethod.invoke(walkerInstance);
            }
            catch (Exception ex)
            {
                log.warn("Quest Helper EW: Error calling cancel on Efficient Walker", ex);
            }
        }
    }

    public void clearBlockedCache()
    {
        blockedTargets.clear();
        activeTarget = null;
    }

    public String getStatus()
    {
        if (walkerInstance != null && getStatusMethod != null)
        {
            try
            {
                Object status = getStatusMethod.invoke(walkerInstance);
                return status != null ? status.toString() : "IDLE";
            }
            catch (Exception ex)
            {
                return "ERROR";
            }
        }
        return isAvailable() ? "IDLE" : "UNAVAILABLE";
    }

    public String getPlanningFailure()
    {
        if (walkerInstance != null && getPlanningFailureMethod != null)
        {
            try
            {
                Object failure = getPlanningFailureMethod.invoke(walkerInstance);
                return failure != null ? failure.toString() : "NONE";
            }
            catch (Exception ex)
            {
                return "NONE";
            }
        }
        return "NONE";
    }

    private boolean invokeRs2Walker(WorldPoint destination, int distance)
    {
        activeTarget = null;
        log.info("Quest Helper EW: Routing via Rs2Walker to {} (dist: {})", destination, distance);
        return distance > 0 ? Rs2Walker.walkTo(destination, distance) : Rs2Walker.walkTo(destination);
    }
}
