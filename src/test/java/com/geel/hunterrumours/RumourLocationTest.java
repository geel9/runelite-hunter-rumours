package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Rumour;
import com.geel.hunterrumours.enums.RumourLocation;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import static org.junit.Assert.fail;

import org.junit.Test;

public class RumourLocationTest {

    private static String firstLocationInfo(Rumour rumour)
    {
        Map.Entry<String, java.util.List<RumourLocation>> location = RumourLocation
                .getGroupedLocationsForRumour(rumour)
                .findFirst()
                .orElseThrow(AssertionError::new);
        return RumourLocation.getLocationInfo(location).format();
    }

    @Test
    public void usesMethodSpecificLocationInformation()
    {
        assertEquals("4 birds", firstLocationInfo(Rumour.TROPICAL_WAGTAIL));
        assertEquals("20 creatures", firstLocationInfo(Rumour.WILD_KEBBIT));
        assertEquals("5 young trees", firstLocationInfo(Rumour.ORANGE_SALAMANDER));
        assertEquals("5 creatures", firstLocationInfo(Rumour.SPINED_LARUPIA));
        assertEquals("10 goats", firstLocationInfo(Rumour.WYRMSCRAIG_GOAT));
        assertEquals("11 creatures", firstLocationInfo(Rumour.EMBERTAILED_JERBOA));
        assertEquals("7 kebbits", firstLocationInfo(Rumour.SPOTTED_KEBBIT));
        assertEquals("7 butterflies", firstLocationInfo(Rumour.BLACK_WARLOCK));
        assertEquals("3 starting burrows", firstLocationInfo(Rumour.RAZOR_BACKED_KEBBIT));
        assertEquals("5 starting areas", firstLocationInfo(Rumour.HERBIBOAR));
    }

    @Test
    public void locationInformationNeverCallsMapPointsSpawns()
    {
        for (Rumour rumour : Rumour.values())
        {
            if (rumour != Rumour.NONE)
            {
                RumourLocation.getGroupedLocationsForRumour(rumour)
                        .map(RumourLocation::getLocationInfo)
                        .map(info -> info.format())
                        .forEach(info -> assertFalse(info.contains(" spawns")));
            }
        }
    }

    @Test
    public void rumourLocationTests() {
        for (RumourLocation location : RumourLocation.values()) {
            if (location.getLocationName() == null || location.getLocationName().isBlank()) {
                fail("Invalid location name for rumour location: " + location.name());
            }

            if (location.getRumour() == null || location.getRumour() == Rumour.NONE) {
                fail("You must set a rumour for rumour location: " + location.name());
            }

            if (location.getFairyRingCode() == null) {
                fail("Invalid fairy ring code for rumour location: " + location.name());
            }

            if (location.getWorldPoint() == null) {
                fail("You must set a world point for rumour location: " + location.name());
            }
        }
    }
}
