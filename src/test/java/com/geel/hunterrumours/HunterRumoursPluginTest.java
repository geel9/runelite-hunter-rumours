package com.geel.hunterrumours;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HunterRumoursPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(HunterRumoursPlugin.class);
        RuneLite.main(args);
    }
}