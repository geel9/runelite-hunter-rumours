package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Rumour;
import com.geel.hunterrumours.enums.RumourLocation;
import com.geel.hunterrumours.enums.Trap;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;

import javax.annotation.Nonnull;
import java.awt.*;

public class RumourInfoBox extends InfoBox {
    private final HunterRumoursPlugin plugin;

    public RumourInfoBox(
            Rumour rumour,
            @Nonnull HunterRumoursPlugin plugin,
            @Nonnull ItemManager itemManager,
            @Nonnull RuneLiteConfig runeLiteConfig
    ) {
        super(itemManager.getImage(rumour.getTargetCreature().getItemId()), plugin);
        this.plugin = plugin;

        // MacOS (and perhaps other platforms) cannot display the bullet point character we use.
        final char bulletPointCharacter;
        if (runeLiteConfig.tooltipFontType().getFont().canDisplay('•')) {
            bulletPointCharacter = '•';
        } else {
            bulletPointCharacter = '-';
        }

        final var locations = RumourLocation.getGroupedLocationsForRumour(rumour);
        final StringBuilder locationsStringBuilder = new StringBuilder();

        locations.forEach(entry -> {
            RumourLocation rumourLocation = entry.getValue().get(0);

            locationsStringBuilder.append("</br>  ").append(bulletPointCharacter).append(' ').append(entry.getKey()).append(" (");
            if (!rumourLocation.getFairyRingCode().equals("")) {
                locationsStringBuilder.append(rumourLocation.getFairyRingCode()).append(", ");
            }
            locationsStringBuilder.append(entry.getValue().size()).append(" spawns)");
        });

        final Trap trap = rumour.getTargetCreature().getTrap();
        final int pityThreshold = trap.calculatePityRateForItems(plugin.getHunterKitItems());
        String hasFinishedRumourText = plugin.getIsCurrentHunterRumourCompleted() ? "Yes" : "No";

        this.setTooltip(
                ColorUtil.wrapWithColorTag("Rumour: ", Color.YELLOW) + rumour.getFullName() + "</br>" +
                        ColorUtil.wrapWithColorTag("Finished: ", Color.YELLOW) + hasFinishedRumourText + "</br>" +
                        ColorUtil.wrapWithColorTag("Item: ", Color.YELLOW) + itemManager.getItemComposition(rumour.getRumourItemID()).getName() + "</br>" +
                        ColorUtil.wrapWithColorTag("Caught: ", Color.YELLOW) + plugin.getCaughtRumourCreatures() + " / " + pityThreshold + "</br>" +
                        ColorUtil.wrapWithColorTag("Locations:", Color.YELLOW) + locationsStringBuilder
        );
    }

    @Override
    public String getText() {
        if (plugin.getIsCurrentHunterRumourCompleted()) {
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
        HunterRumoursConfig config = plugin.getConfig();
        if (plugin.getIsCurrentHunterRumourCompleted()) {
            return config.completedRumourInfoBoxTextColor();
        } else if (!showNumUntilPity()) {
            return config.defaultInfoBoxTextColor();
        } else {
            final Rumour currentRumour = plugin.getCurrentRumour();
            if (currentRumour == Rumour.NONE) {
                return config.defaultInfoBoxTextColor();
            }
            final int caughtCreatures = plugin.getCaughtRumourCreatures();
            final int pityThreshold = currentRumour.getTrap().calculatePityRateForItems(plugin.getHunterKitItems());
            final float percentage = (float) caughtCreatures / pityThreshold * 100f;
            if (percentage >= 75) {
                return config.unluckyRateInfoBoxTextColor();
            } else if (percentage >= 50) {
                return config.normalRateInfoBoxTextColor();
            } else {
                return config.luckyRateInfoBoxTextColor();
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