package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.HunterRumoursConfig;
import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomBlocklistStoreTest
{
    @Test
    public void savesListsInSeparateHunterLibrariesAndDeletesThem()
    {
        Map<String, String> config = new HashMap<>();
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getConfiguration(eq(HunterRumoursConfig.GROUP), anyString()))
                .thenAnswer(invocation -> config.get(invocation.getArgument(1)));
        doAnswer(invocation -> {
            config.put(invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(configManager).setConfiguration(eq(HunterRumoursConfig.GROUP), anyString(), anyString());
        doAnswer(invocation -> {
            config.remove(invocation.getArgument(1));
            return null;
        }).when(configManager).unsetConfiguration(eq(HunterRumoursConfig.GROUP), anyString());

        CustomBlocklistStore store = new CustomBlocklistStore(configManager, new BlocklistCodec());
        Map<Hunter, Rumour> cervusBlocks = new EnumMap<>(Hunter.class);
        cervusBlocks.put(Hunter.NOVICE_GILMAN, Rumour.PYRE_FOX);
        CustomBlocklist cervus = CustomBlocklist.create("Fast", Hunter.ADEPT_CERVUS, cervusBlocks);
        Map<Hunter, Rumour> tecoBlocks = new EnumMap<>(Hunter.class);
        tecoBlocks.put(Hunter.NOVICE_GILMAN, Rumour.SUNLIGHT_ANTELOPE);
        CustomBlocklist teco = CustomBlocklist.create("Fast", Hunter.EXPERT_TECO, tecoBlocks);

        store.save(cervus);
        store.save(teco);

        assertEquals(1, store.list(Hunter.ADEPT_CERVUS).size());
        assertEquals(1, store.list(Hunter.EXPERT_TECO).size());
        store.delete(cervus);
        assertEquals(0, store.list(Hunter.ADEPT_CERVUS).size());
        assertEquals(1, store.list(Hunter.EXPERT_TECO).size());
    }
}
