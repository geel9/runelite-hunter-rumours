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
                        .build(),
                LineComponent.builder()
                        .left("Back-To-Back")
                        .right(plugin.getBackToBackState().getNiceName())
                        .rightColor(BackToBackColor(plugin.getBackToBackState()))
                        .build(),
                LineComponent.builder().build()
                )
        );

        for (var hunter : plugin.getEnabledHunters()) {
            children.add(
                    LineComponent.builder()
                            .left(hunter.getCommonName())
                            .right(HunterRumoursPlugin.hunterRumours.get(hunter).getFullName())
                            .build()
            );
        }

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
