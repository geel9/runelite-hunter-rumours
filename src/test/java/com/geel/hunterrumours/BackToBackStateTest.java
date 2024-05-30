package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.BackToBackState;

import static org.junit.Assert.fail;

import org.junit.Test;

public class BackToBackStateTest {
    @Test
    public void backToBackStateTests() {
        for (BackToBackState state : BackToBackState.values()) {
            if (state.getNiceName() == null || state.getNiceName().isBlank()) {
                fail("Invalid name set for back-to-back state: " + state.name());
            }
        }
    }
}
