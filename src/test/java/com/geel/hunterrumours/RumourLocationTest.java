package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Rumour;
import com.geel.hunterrumours.enums.RumourLocation;

import static org.junit.Assert.fail;

import org.junit.Test;

public class RumourLocationTest {

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
