package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CustomBlocklistTest
{
    @Test
    public void createsAndEditsAValidCervusList()
    {
        Map<Hunter, Rumour> blocks = new EnumMap<>(Hunter.class);
        blocks.put(Hunter.NOVICE_GILMAN, Rumour.PYRE_FOX);
        blocks.put(Hunter.ADEPT_ORNUS, Rumour.SABRE_TOOTHED_KEBBIT);

        CustomBlocklist list = CustomBlocklist.create("No deadfalls", Hunter.ADEPT_CERVUS, blocks);
        CustomBlocklist renamed = list.edited("Cervus fast list", blocks);

        assertEquals(list.getId(), renamed.getId());
        assertEquals("Cervus fast list", renamed.getName());
        assertEquals(Rumour.PYRE_FOX, renamed.getBlocks().get(Hunter.NOVICE_GILMAN));
        assertNotEquals(list.getId(), list.copy("Copy").getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRumourTheActiveHunterCannotReceive()
    {
        Map<Hunter, Rumour> blocks = new EnumMap<>(Hunter.class);
        blocks.put(Hunter.ADEPT_ORNUS, Rumour.RED_SALAMANDER);
        CustomBlocklist.create("Invalid", Hunter.ADEPT_CERVUS, blocks);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateBlocks()
    {
        Map<Hunter, Rumour> blocks = new EnumMap<>(Hunter.class);
        blocks.put(Hunter.NOVICE_GILMAN, Rumour.PYRE_FOX);
        blocks.put(Hunter.ADEPT_ORNUS, Rumour.PYRE_FOX);
        CustomBlocklist.create("Invalid", Hunter.ADEPT_CERVUS, blocks);
    }
}
