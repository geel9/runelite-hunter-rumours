package com.geel.hunterrumours;

import net.runelite.api.NPC;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import org.junit.Test;

import java.awt.Color;
import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class HunterRumoursPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(HunterRumoursPlugin.class);
        RuneLite.main(args);
    }

    @Test
    public void buildsSelectedHunterNpcHighlightStyles() {
        HighlightedNpc highlight = HunterRumoursPlugin.buildHunterNpcHighlight(
                mock(NPC.class),
                Color.CYAN,
                EnumSet.of(
                        HunterRumoursConfig.HighlightType.OUTLINE,
                        HunterRumoursConfig.HighlightType.TRUE_TILE
                )
        );

        assertTrue(highlight.isOutline());
        assertTrue(highlight.isTrueTile());
        assertFalse(highlight.isTile());
        assertFalse(highlight.isHull());
    }

    @Test
    public void buildsClickboxAndTileIndependently() {
        HighlightedNpc highlight = HunterRumoursPlugin.buildHunterNpcHighlight(
                mock(NPC.class),
                Color.CYAN,
                EnumSet.of(
                        HunterRumoursConfig.HighlightType.CLICKBOX,
                        HunterRumoursConfig.HighlightType.TILE
                )
        );

        assertTrue(highlight.isHull());
        assertTrue(highlight.isTile());
        assertFalse(highlight.isOutline());
        assertFalse(highlight.isTrueTile());
    }
}
