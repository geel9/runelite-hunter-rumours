package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HunterRumoursPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(HunterRumoursPlugin.class);
        RuneLite.main(args);
    }

    @After
    public void resetRumours() {
        HunterRumoursPlugin.hunterRumours.replaceAll((hunter, rumour) -> Rumour.NONE);
    }

    @Test
    public void clearsShortestPathAfterInactivityTimeout() throws Exception {
        HunterRumoursPlugin plugin = new HunterRumoursPlugin();
        HunterRumoursConfig config = mock(HunterRumoursConfig.class);
        Client client = mock(Client.class);
        EventBus eventBus = mock(EventBus.class);

        setField(plugin, "config", config);
        setField(plugin, "client", client);
        setField(plugin, "eventBus", eventBus);
        setField(plugin, "latestInteractionTime", 100);
        setField(plugin, "shortestPathActive", true);

        plugin.currentHunter = Hunter.NOVICE_GILMAN;
        HunterRumoursPlugin.hunterRumours.put(Hunter.NOVICE_GILMAN, Rumour.RED_SALAMANDER);

        when(config.showInfoBox()).thenReturn(false);
        when(config.showWorldMapLocations()).thenReturn(false);
        when(config.useShortestPath()).thenReturn(true);
        when(config.shortestPathDisableTimer()).thenReturn(5);
        when(client.getTickCount()).thenReturn(601);

        plugin.onGameTick(new GameTick());

        verify(eventBus).post(any(PluginMessage.class));
        assertFalse((boolean) getField(plugin, "shortestPathActive"));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = HunterRumoursPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = HunterRumoursPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
