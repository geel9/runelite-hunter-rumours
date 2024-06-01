package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Rumour;
import com.geel.hunterrumours.enums.RumourLocation;
import com.geel.hunterrumours.enums.Trap;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class RumourInfoBox extends InfoBox {
    private final HunterRumoursPlugin plugin;

    public RumourInfoBox(Rumour rumour, @Nonnull HunterRumoursPlugin plugin, @Nonnull ItemManager itemManager) {
        super(itemManager.getImage(rumour.getTargetCreature().getItemId()), plugin);
        this.plugin = plugin;
        this.setTooltip(getTooltipText(rumour, itemManager));
    }

    String getTooltipText(Rumour rumour, ItemManager itemManager) {
        final Map<String, List<RumourLocation>> locations = RumourLocation.getGroupedLocationsForRumour(rumour);
        final StringBuilder sb = new StringBuilder();

        locations.keySet().forEach(locationName -> {
            var keyedLocations = locations.get(locationName);
            RumourLocation rumourLocation = keyedLocations.get(0);

            sb.append("</br>  • ").append(locationName).append(" (");
            if (!rumourLocation.getFairyRingCode().equals("")) {
                sb.append(rumourLocation.getFairyRingCode()).append(", ");
            }
            sb.append(keyedLocations.size()).append(" spawns)");
        });

        final Trap trap = rumour.getTargetCreature().getTrap();
        final int pityThreshold = trap.calculatePityRateForItems(plugin.getHunterKitItems());
        String hasFinishedRumourText = plugin.getHunterRumourState() ? "Yes" : "No";
        String tooltipText = ColorUtil.wrapWithColorTag("Rumour: ", Color.YELLOW) + rumour.getFullName() + "</br>" +
                ColorUtil.wrapWithColorTag("Finished: ", Color.YELLOW) + hasFinishedRumourText + "</br>" +
                ColorUtil.wrapWithColorTag("Item: ", Color.YELLOW) + itemManager.getItemComposition(rumour.getRumourItemID()).getName() + "</br>" +
                ColorUtil.wrapWithColorTag("Caught: ", Color.YELLOW) + plugin.getCaughtRumourCreatures() + " / " + pityThreshold + "</br>" +
                ColorUtil.wrapWithColorTag("Locations:", Color.YELLOW) + sb;
        return tooltipText;
    }

    @Override
    public String getText() {
        if (plugin.getHunterRumourState()) {
            return "Done";
        }

        final Rumour currentRumour = plugin.getCurrentRumour();
        if (currentRumour != Rumour.NONE && showNumUntilPity()) {
            final int pityThreshold = currentRumour.getTrap().calculatePityRateForItems(plugin.getHunterKitItems());
            return String.valueOf(pityThreshold - plugin.getCaughtRumourCreatures());
        }

        return "";
    }

    @Override
    public Color getTextColor() {
        if (plugin.getHunterRumourState()) {
            return Color.GREEN;
        } else if (!showNumUntilPity()) {
            return Color.WHITE;
        } else {
            final Rumour currentRumour = plugin.getCurrentRumour();
            if (currentRumour == Rumour.NONE) {
                return Color.WHITE;
            }
            final int caughtCreatures = plugin.getCaughtRumourCreatures();
            final int pityThreshold = currentRumour.getTrap().calculatePityRateForItems(plugin.getHunterKitItems());
            final float percentage = (float) caughtCreatures / pityThreshold * 100f;
            if (percentage >= 75) {
                return Color.ORANGE.brighter();
            } else if (percentage >= 50) {
                return Color.ORANGE.darker();
            } else {
                return Color.RED;
            }
        }
    }

    private boolean showNumUntilPity() {
        if (this.plugin == null || this.plugin.getConfig() == null) {
            return false;
        }

        return this.plugin.getConfig().showCatchesRemainingUntilPity();
    }
}