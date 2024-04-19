package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.BackToBackState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static java.util.Arrays.asList;

@Slf4j
class HunterRumoursOverlay extends OverlayPanel {
    private final Client client;
    private final HunterRumoursPlugin plugin;
    private final HunterRumoursConfig config;

    @Inject
    private HunterRumoursOverlay(Client client, HunterRumoursPlugin plugin, HunterRumoursConfig config) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isInBurrows() || !config.showOverlay())
            return null;

        var children = panelComponent.getChildren();

        children.addAll(asList(
                TitleComponent.builder()
                        .text("Hunter Rumours")
                        .build(),
                LineComponent.builder()
                        .left("Current Hunter")
                        .right(plugin.currentHunter.getCommonName())
                        .build(),
                LineComponent.builder()
                        .left("Current Rumour")
                        .right(plugin.getCurrentRumour().getFullName())
                        .build()
                )
        );

        // Add back-to-back state if enabled
        if (plugin.getConfig().guildOverlayShowBackToBackState()) {
            children.add(LineComponent.builder()
                    .left("Back-To-Back")
                    .right(plugin.getBackToBackState().getNiceName())
                    .rightColor(BackToBackColor(plugin.getBackToBackState()))
                    .build()
            );
        }

        // Add list of hunters if enabled
        if (plugin.getConfig().guildOverlayListHunters()) {
            var enabledHunters = plugin.getEnabledHunters();

            // If any hunters to add, add a spacing line before listing them
            if (enabledHunters.length > 0) {
                children.add(LineComponent.builder().build());
            }

            // List all hunters
            for (var hunter : enabledHunters) {
                children.add(
                        LineComponent.builder()
                                .left(hunter.getCommonName())
                                .right(HunterRumoursPlugin.hunterRumours.get(hunter).getFullName())
                                .build()
                );
            }

        }

        // Hack to figure out ~~~roughly~~~ how big the panel should be
        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth("Current Rumour      Razor-backed Kebbit (Deadfall)"), 0));

        return super.render(graphics);
    }

    private Color BackToBackColor(BackToBackState state) {
        switch (state) {
            case UNKNOWN:
                return Color.YELLOW;
            case ENABLED:
                return Color.GREEN;
            case DISABLED:
                return Color.RED;
            default:
                return Color.PINK;
        }
    }

}
