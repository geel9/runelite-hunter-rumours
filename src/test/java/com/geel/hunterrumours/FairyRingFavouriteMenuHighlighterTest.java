package com.geel.hunterrumours;

import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FairyRingFavouriteMenuHighlighterTest {
    @Test
    public void highlightsOnlyMatchingCodeInFavouritesSubmenu() {
        MenuEntry parent = entry("Favourites", "Fairy ring");
        MenuEntry matching = entry("DKS", "Fairy ring");
        when(matching.getOption()).thenReturn("DKS", "<col=00ff00>DKS");
        MenuEntry other = entry("AKS", "Fairy ring");
        Menu submenu = mock(Menu.class);
        when(parent.getSubMenu()).thenReturn(submenu);
        when(submenu.getMenuEntries()).thenReturn(new MenuEntry[]{matching, other});

        assertTrue(FairyRingFavouriteMenuHighlighter.highlight(new MenuEntry[]{parent}, "DKS"));
        assertTrue(FairyRingFavouriteMenuHighlighter.highlight(new MenuEntry[]{parent}, "DKS"));

        verify(matching, times(2)).setOption("<col=00ff00>DKS");
        verify(matching, never()).setTarget(anyString());
        verify(other, never()).setOption(anyString());
        verify(parent, never()).setTarget(anyString());
    }

    @Test
    public void ignoresMatchingCodeOutsideFavouritesSubmenu() {
        MenuEntry matching = entry("DKS", "Fairy ring");

        assertFalse(FairyRingFavouriteMenuHighlighter.highlight(new MenuEntry[]{matching}, "DKS"));
        verify(matching, never()).setOption(anyString());
        verify(matching, never()).setTarget(anyString());
    }

    private static MenuEntry entry(String option, String target) {
        MenuEntry entry = mock(MenuEntry.class);
        when(entry.getOption()).thenReturn(option);
        when(entry.getTarget()).thenReturn(target);
        return entry;
    }
}
