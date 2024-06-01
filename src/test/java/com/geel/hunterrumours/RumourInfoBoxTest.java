package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Rumour;
import com.google.inject.Guice;

import com.google.inject.testing.fieldbinder.BoundFieldModule;

import java.awt.*;

import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RumourInfoBoxTest {

    @Mock
    private HunterRumoursPlugin plugin;

    @Mock
    private ItemManager itemManager;

    @Mock
    private RumourInfoBox rumourInfoBox;

    @Before
    public void setUp() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
        plugin = mock(HunterRumoursPlugin.class);
        itemManager = mock(ItemManager.class);

        setupInfoBoxTextTests();
    }

    private void setupInfoBoxTextTests() {
        ItemComposition embertailedJerboaMock = mock(ItemComposition.class);
        when(embertailedJerboaMock.getName()).thenReturn("jerboa");
        when(itemManager.getItemComposition(Rumour.EMBERTAILED_JERBOA.getRumourItemID())).thenReturn(embertailedJerboaMock);
        when(plugin.getConfig()).thenReturn(mock(HunterRumoursConfig.class));

        rumourInfoBox = new RumourInfoBox(Rumour.EMBERTAILED_JERBOA, plugin, itemManager);
    }

    @Test
    public void infoBox_getText_Tests() {
        // Check done text based on rumour state
        when(plugin.getHunterRumourState()).thenReturn(true);

        if (!rumourInfoBox.getText().equals("Done")) {
            fail("The rumour info box getText is messed up, whenever we set the getHunterRumourState, it should return \"Done\"");
        }

        // Set the rumour state to false for our next few tests
        when(plugin.getHunterRumourState()).thenReturn(false);

        // Check text for Rumour.NONE
        when(plugin.getCurrentRumour()).thenReturn(Rumour.NONE);
        if (!rumourInfoBox.getText().equals("")) {
            fail("The rumour info box getText is messed up, whenever we set the plugin.getCurrentRumour to Rumour.NONE, it should return \"\"");
        }

        // Check text if you have the embertailed jerboa rumour (or any other one)
        when(plugin.getCurrentRumour()).thenReturn(Rumour.EMBERTAILED_JERBOA);

        // First do the check when the user disabled showing the pity rate numbers
        when(plugin.getConfig().showCatchesRemainingUntilPity()).thenReturn(false);
        if (!rumourInfoBox.getText().equals("")) {
            fail("The rumour info box getText is messed up, whenever we set the plugin.getCurrentRumour to any rumour other than Rumour.NONE, but have the 'showCatchesRemainingUntilPity' config set to false, it should return \"\"");
        }

        // Check if the infobox text shows the correct remaining catches until pity rate
        when(plugin.getConfig().showCatchesRemainingUntilPity()).thenReturn(true);
        when(plugin.getHunterKitItems()).thenReturn(0);
        when(plugin.getCaughtRumourCreatures()).thenReturn(10);
        String text = String.valueOf(Rumour.EMBERTAILED_JERBOA.getTrap().getPityThreshold() - plugin.getCaughtRumourCreatures());
        if (!rumourInfoBox.getText().equals(text)) {
            fail("The rumour info box getText is messed up, whenever we set the plugin.getCurrentRumour to any rumour other than Rumour.NONE and we have the 'showCatchesRemainingUntilPity' config set to true it should return the remaining catches.");
        }
    }

    @Test
    public void infoBox_getTextColor_Tests() {
        // First set the rumour state to completed
        when(plugin.getHunterRumourState()).thenReturn(true);

        // Completed color test
        if (colorsArentEqual(rumourInfoBox.getTextColor(), Color.GREEN)) {
            fail("The rumour infobox getTextColor method should return 'Color.GREEN' when the rumour state is completed.");
        }

        // Set rumour state to not completed to test the remaining colors
        when(plugin.getHunterRumourState()).thenReturn(false);

        // Set showCatchesRemainingUntilPity to false to test default text color
        when(plugin.getConfig().showCatchesRemainingUntilPity()).thenReturn(false);

        // Default text color test
        if (colorsArentEqual(rumourInfoBox.getTextColor(), Color.WHITE)) {
            fail("The rumour infobox getTextColor method should return 'Color.WHITE' when the 'showCatchesRemainingUntilPity' config is set to false.");
        }

        // Set showCatchesRemainingUntilPity to true to test the remaining colors
        when(plugin.getConfig().showCatchesRemainingUntilPity()).thenReturn(true);

        // Set current rumour to Rumour.NONE
        when(plugin.getCurrentRumour()).thenReturn(Rumour.NONE);

        // Rumour.NONE color test
        if (colorsArentEqual(rumourInfoBox.getTextColor(), Color.WHITE)) {
            fail("The rumour infobox getTextColor method should return 'Color.WHITE' when the current rumour is Rumour.NONE");
        }

        // Set current rumour to any other 'real' rumour
        when(plugin.getCurrentRumour()).thenReturn(Rumour.EMBERTAILED_JERBOA);

        // Set caught rumours to 0 to test 'lucky' color
        when(plugin.getCaughtRumourCreatures()).thenReturn(0);
        if (colorsArentEqual(rumourInfoBox.getTextColor(), Color.RED)) {
            fail("The rumour infobox getTextColor method should return 'Color.RED' when the caught creatures are < 50% of the pity rate");
        }

        // Set caught rumours to 51 to test 'normal' color
        when(plugin.getCaughtRumourCreatures()).thenReturn(51);
        if (colorsArentEqual(rumourInfoBox.getTextColor(), Color.ORANGE.darker())) {
            fail("The rumour infobox getTextColor method should return 'Color.ORANGE.darker()' when the caught creatures are >= 50% and < 75% of the pity rate");
        }

        // Set caught rumours to 80 to test 'unlucky' color
        when(plugin.getCaughtRumourCreatures()).thenReturn(80);
        if (colorsArentEqual(rumourInfoBox.getTextColor(), Color.ORANGE.brighter())) {
            fail("The rumour infobox getTextColor method should return 'Color.ORANGE.brighter()' when the caught creatures are >= 75% of the pity rate");
        }
    }

    private boolean colorsArentEqual(Color color1, Color color2) {
        return color1.getRed() != color2.getRed() || color1.getGreen() != color2.getGreen() || color1.getBlue() != color2.getBlue();
    }
}
