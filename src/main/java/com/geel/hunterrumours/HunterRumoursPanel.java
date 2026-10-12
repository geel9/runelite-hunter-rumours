package com.geel.hunterrumours;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** Hosts blocklists and preferred locations under one toolbar button. */
public class HunterRumoursPanel extends PluginPanel
{
    @Inject
    public HunterRumoursPanel(BlocklistPanel blocklistPanel,
                              PreferredLocationPreferencesPanel locationsPanel)
    {
        super(false);
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Blocklists", blocklistPanel);
        tabs.addTab("Locations", locationsPanel);
        add(tabs, BorderLayout.CENTER);
    }
}
