package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;

public class BlocklistCodecTest
{
    @Test
    public void exportImportRoundTripCreatesANewIdentity()
    {
        Map<Hunter, Rumour> blocks = new EnumMap<>(Hunter.class);
        blocks.put(Hunter.NOVICE_GILMAN, Rumour.PYRE_FOX);
        blocks.put(Hunter.ADEPT_ORNUS, Rumour.SABRE_TOOTHED_KEBBIT);
        CustomBlocklist original = CustomBlocklist.create("Cervus list", Hunter.ADEPT_CERVUS, blocks);

        BlocklistCodec codec = new BlocklistCodec();
        String exported = codec.exportList(original);
        CustomBlocklist imported = codec.importList(exported);

        assertFalse(exported.contains(original.getId()));
        assertNotEquals(original.getId(), imported.getId());
        assertEquals(original.getName(), imported.getName());
        assertEquals(original.getBlocks(), imported.getBlocks());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownFields()
    {
        new BlocklistCodec().importList("{\"format\":\"hunter-rumours-blocklist\",\"version\":1,"
                + "\"name\":\"x\",\"activeHunter\":\"ADEPT_CERVUS\",\"blocks\":{},\"surprise\":true}");
    }
}
