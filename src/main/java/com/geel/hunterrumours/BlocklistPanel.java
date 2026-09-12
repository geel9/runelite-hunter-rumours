package com.geel.hunterrumours;

import com.geel.hunterrumours.blocklist.BlocklistPlan;
import com.geel.hunterrumours.blocklist.BlocklistPlanner;
import com.geel.hunterrumours.blocklist.BlocklistPreset;
import com.geel.hunterrumours.blocklist.CustomBlocklist;
import com.geel.hunterrumours.blocklist.CustomBlocklistStore;
import com.geel.hunterrumours.blocklist.HunterAssignments;
import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.EnumMap;
import java.util.Map;

/** Editor and live setup guide for preset and named custom blocklists. */
public class BlocklistPanel extends JPanel
{
    private static final String SELECTED_LIST_KEY = "blocklist.selected";
    private static final String SELECTED_HUNTER_KEY = "blocklist.selectedHunter";
    private static final Color MUTED = new Color(170, 170, 170);

    private enum Mode
    {
        PRESET("Preset"), CUSTOM("Custom");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private final CustomBlocklistStore store;
    private final ConfigManager configManager;
    private final BlocklistPlanner planner = new BlocklistPlanner();
    private final JComboBox<Mode> mode = new JComboBox<>(Mode.values());
    private final JComboBox<BlocklistPreset> preset = new JComboBox<>(BlocklistPreset.values());
    private final JComboBox<Hunter> presetHunter = new JComboBox<>();
    private final JComboBox<Hunter> activeHunter = new JComboBox<>(Hunter.allValues());
    private final JComboBox<CustomBlocklist> savedLists = new JComboBox<>();
    private final JPanel presetField = field("Preset", preset);
    private final JPanel presetHunterField = field("Active hunter", presetHunter);
    private final JTextField name = new JTextField();
    private final JPanel blockFields = verticalPanel();
    private final JPanel customEditor = verticalPanel();
    private final JPanel progress = verticalPanel();
    private final JLabel nextAction = new JLabel();
    private final Map<Hunter, JComboBox<RumourChoice>> blockChoices = new EnumMap<>(Hunter.class);
    private Map<Hunter, Rumour> observed = new EnumMap<>(Hunter.class);
    private boolean syncing;

    @Inject
    public BlocklistPanel(CustomBlocklistStore store, ConfigManager configManager)
    {
        this.store = store;
        this.configManager = configManager;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("BLOCKLIST HELPER");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
        add(title);
        add(text("Choose a preset or build a named list, then follow the next action."));
        add(gap());
        add(field("Mode", mode));
        add(presetField);
        add(presetHunterField);

        customEditor.add(field("Active hunter", activeHunter));
        customEditor.add(field("Saved list", savedLists));
        customEditor.add(field("Name", name));
        customEditor.add(blockFields);
        customEditor.add(buttonRow());
        add(customEditor);

        add(gap());
        JLabel progressTitle = new JLabel("CURRENT SETUP");
        progressTitle.setForeground(Color.WHITE);
        progressTitle.setFont(FontManager.getRunescapeBoldFont());
        add(progressTitle);
        add(progress);
        nextAction.setForeground(new Color(214, 163, 62));
        add(nextAction);

        installListeners();
        setComboRenderer(activeHunter);
        setComboRenderer(presetHunter);
        mode.setSelectedItem(Mode.PRESET);
        rebuildPresetHunters();
        rebuildMode();
    }

    public void refresh(Map<Hunter, Rumour> hunterRumours)
    {
        observed = new EnumMap<>(Hunter.class);
        if (hunterRumours != null)
        {
            observed.putAll(hunterRumours);
        }
        SwingUtilities.invokeLater(this::renderPlan);
    }

    private void installListeners()
    {
        mode.addActionListener(event -> rebuildMode());
        preset.addActionListener(event -> {
            rebuildPresetHunters();
            renderPlan();
        });
        presetHunter.addActionListener(event -> renderPlan());
        activeHunter.addActionListener(event -> {
            if (!syncing)
            {
                configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP,
                        SELECTED_HUNTER_KEY, activeHunter.getSelectedItem());
                reloadLibrary(null);
                rebuildBlockFields(null);
                renderPlan();
            }
        });
        savedLists.addActionListener(event -> {
            if (!syncing)
            {
                loadSelectedList();
            }
        });
    }

    private JPanel buttonRow()
    {
        JPanel buttons = new JPanel(new GridLayout(0, 2, 4, 4));
        buttons.setOpaque(false);
        addButton(buttons, "New", event -> newList());
        addButton(buttons, "Save", event -> saveList());
        addButton(buttons, "Save copy", event -> saveCopy());
        addButton(buttons, "Delete", event -> deleteList());
        addButton(buttons, "Import", event -> importList());
        addButton(buttons, "Export", event -> exportList());
        return buttons;
    }

    private void rebuildMode()
    {
        boolean custom = mode.getSelectedItem() == Mode.CUSTOM;
        presetField.setVisible(!custom);
        presetHunterField.setVisible(!custom);
        customEditor.setVisible(custom);
        if (custom)
        {
            restoreActiveHunter();
            reloadLibrary(selectedListId());
            if (savedLists.getSelectedItem() == null)
            {
                rebuildBlockFields(null);
            }
        }
        revalidate();
        repaint();
        renderPlan();
    }

    private void reloadLibrary(String selectId)
    {
        syncing = true;
        savedLists.removeAllItems();
        Hunter hunter = (Hunter) activeHunter.getSelectedItem();
        CustomBlocklist selected = null;
        for (CustomBlocklist list : store.list(hunter))
        {
            savedLists.addItem(list);
            if (list.getId().equals(selectId))
            {
                selected = list;
            }
        }
        savedLists.setSelectedItem(selected);
        syncing = false;
        if (selected != null)
        {
            loadList(selected);
        }
    }

    private void loadSelectedList()
    {
        CustomBlocklist selected = (CustomBlocklist) savedLists.getSelectedItem();
        if (selected == null)
        {
            return;
        }
        loadList(selected);
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, SELECTED_LIST_KEY, selected.getId());
    }

    private void loadList(CustomBlocklist list)
    {
        syncing = true;
        activeHunter.setSelectedItem(list.getActiveHunter());
        name.setText(list.getName());
        rebuildBlockFields(list.getBlocks());
        syncing = false;
        renderPlan();
    }

    private void rebuildBlockFields(Map<Hunter, Rumour> selections)
    {
        blockFields.removeAll();
        blockChoices.clear();
        Hunter active = (Hunter) activeHunter.getSelectedItem();
        for (Hunter holder : HunterAssignments.getBlockHolders(active))
        {
            JComboBox<RumourChoice> choice = new JComboBox<>();
            choice.addItem(RumourChoice.none());
            for (Rumour rumour : HunterAssignments.getValidBlocks(active, holder))
            {
                choice.addItem(new RumourChoice(rumour));
            }
            if (selections != null && selections.containsKey(holder))
            {
                choice.setSelectedItem(new RumourChoice(selections.get(holder)));
            }
            choice.addActionListener(event -> {
                if (!syncing)
                {
                    removeDuplicateSelection(holder, choice);
                    renderPlan();
                }
            });
            blockChoices.put(holder, choice);
            blockFields.add(field(holder.getCommonName(), choice));
        }
        blockFields.revalidate();
        blockFields.repaint();
    }

    private void newList()
    {
        savedLists.setSelectedItem(null);
        name.setText("");
        rebuildBlockFields(null);
        renderPlan();
    }

    private void saveList()
    {
        try
        {
            CustomBlocklist selected = (CustomBlocklist) savedLists.getSelectedItem();
            CustomBlocklist list = selected == null
                    ? CustomBlocklist.create(name.getText(), (Hunter) activeHunter.getSelectedItem(), selectedBlocks())
                    : selected.edited(name.getText(), selectedBlocks());
            store.save(list);
            configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, SELECTED_LIST_KEY, list.getId());
            reloadLibrary(list.getId());
        }
        catch (IllegalArgumentException ex)
        {
            showError(ex.getMessage());
        }
    }

    private void saveCopy()
    {
        CustomBlocklist selected = (CustomBlocklist) savedLists.getSelectedItem();
        if (selected == null)
        {
            saveList();
            return;
        }
        name.setText(selected.getName() + " copy");
        savedLists.setSelectedItem(null);
        saveList();
    }

    private void deleteList()
    {
        CustomBlocklist selected = (CustomBlocklist) savedLists.getSelectedItem();
        if (selected == null)
        {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete '" + selected.getName() + "'?",
                "Delete blocklist", JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.OK_OPTION)
        {
            store.delete(selected);
            reloadLibrary(null);
            newList();
        }
    }

    private void exportList()
    {
        CustomBlocklist selected = (CustomBlocklist) savedLists.getSelectedItem();
        if (selected == null)
        {
            showError("Save the blocklist before exporting it");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(store.exportList(selected)), null);
        JOptionPane.showMessageDialog(this, "Copied '" + selected.getName() + "' to the clipboard.");
    }

    private void importList()
    {
        JTextArea input = new JTextArea(12, 34);
        int answer = JOptionPane.showConfirmDialog(this, new JScrollPane(input),
                "Paste a blocklist", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION)
        {
            return;
        }
        try
        {
            CustomBlocklist imported = store.importList(input.getText());
            String preview = "Name: " + imported.getName() + "\nActive hunter: "
                    + imported.getActiveHunter().getCommonName() + "\nBlocks: " + imported.getBlocks().size();
            if (JOptionPane.showConfirmDialog(this, preview, "Import blocklist",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            {
                return;
            }
            store.save(imported);
            mode.setSelectedItem(Mode.CUSTOM);
            activeHunter.setSelectedItem(imported.getActiveHunter());
            reloadLibrary(imported.getId());
        }
        catch (IllegalArgumentException ex)
        {
            showError(ex.getMessage());
        }
    }

    private Map<Hunter, Rumour> selectedBlocks()
    {
        Map<Hunter, Rumour> blocks = new EnumMap<>(Hunter.class);
        for (Map.Entry<Hunter, JComboBox<RumourChoice>> entry : blockChoices.entrySet())
        {
            RumourChoice choice = (RumourChoice) entry.getValue().getSelectedItem();
            if (choice != null && choice.rumour != null)
            {
                blocks.put(entry.getKey(), choice.rumour);
            }
        }
        return blocks;
    }

    private void removeDuplicateSelection(Hunter changedHolder, JComboBox<RumourChoice> changed)
    {
        RumourChoice selected = (RumourChoice) changed.getSelectedItem();
        if (selected == null || selected.rumour == null)
        {
            return;
        }
        syncing = true;
        for (Map.Entry<Hunter, JComboBox<RumourChoice>> entry : blockChoices.entrySet())
        {
            if (entry.getKey() != changedHolder && selected.equals(entry.getValue().getSelectedItem()))
            {
                entry.getValue().setSelectedIndex(0);
            }
        }
        syncing = false;
    }

    private void rebuildPresetHunters()
    {
        Hunter previous = (Hunter) presetHunter.getSelectedItem();
        presetHunter.removeAllItems();
        BlocklistPreset selected = (BlocklistPreset) preset.getSelectedItem();
        if (selected == null)
        {
            return;
        }
        for (Hunter hunter : selected.getActiveHunters())
        {
            presetHunter.addItem(hunter);
        }
        if (selected.getActiveHunters().contains(previous))
        {
            presetHunter.setSelectedItem(previous);
        }
    }

    private CustomBlocklist currentList()
    {
        try
        {
            if (mode.getSelectedItem() == Mode.PRESET)
            {
                BlocklistPreset selected = (BlocklistPreset) preset.getSelectedItem();
                Hunter hunter = (Hunter) presetHunter.getSelectedItem();
                return selected == null || hunter == null ? null : selected.resolve(hunter, observed);
            }
            CustomBlocklist selected = (CustomBlocklist) savedLists.getSelectedItem();
            if (selected != null)
            {
                return selected.edited(name.getText(), selectedBlocks());
            }
            if (name.getText().trim().isEmpty() || selectedBlocks().isEmpty())
            {
                return null;
            }
            return CustomBlocklist.create(name.getText(), (Hunter) activeHunter.getSelectedItem(), selectedBlocks());
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    private void renderPlan()
    {
        progress.removeAll();
        CustomBlocklist list = currentList();
        if (list == null)
        {
            nextAction.setText("Create or choose a blocklist.");
        }
        else
        {
            BlocklistPlan plan = planner.plan(list, observed);
            for (BlocklistPlan.Row row : plan.getRows())
            {
                if (row.getStatus() == BlocklistPlan.Row.Status.ACTIVE)
                {
                    progress.add(text(row.getHunter().getCommonName() + ": active hunter"));
                }
                else
                {
                    progress.add(text(row.getHunter().getCommonName() + ": " + row.getDesired().getName()
                            + " [" + row.getStatus().name().toLowerCase().replace('_', ' ') + "]"));
                }
            }
            nextAction.setText("<html>" + plan.getNextAction() + "</html>");
        }
        progress.revalidate();
        progress.repaint();
    }

    private String selectedListId()
    {
        try
        {
            return configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP,
                    SELECTED_LIST_KEY, String.class);
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }

    private void restoreActiveHunter()
    {
        try
        {
            Hunter selected = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP,
                    SELECTED_HUNTER_KEY, Hunter.class);
            if (selected != null && selected != Hunter.NONE)
            {
                syncing = true;
                activeHunter.setSelectedItem(selected);
                syncing = false;
            }
        }
        catch (RuntimeException ignored)
        {
            // No logged-in profile yet.
        }
    }

    private void showError(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Blocklist helper", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel field(String labelText, Component input)
    {
        JPanel field = new JPanel(new BorderLayout(4, 2));
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(2, 0, 2, 0));
        JLabel label = new JLabel(labelText);
        label.setForeground(MUTED);
        label.setFont(FontManager.getRunescapeSmallFont());
        field.add(label, BorderLayout.NORTH);
        field.add(input, BorderLayout.CENTER);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return field;
    }

    private static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private static JLabel text(String value)
    {
        JLabel label = new JLabel("<html>" + value + "</html>");
        label.setForeground(MUTED);
        label.setFont(FontManager.getRunescapeSmallFont());
        return label;
    }

    private static Component gap()
    {
        return Box.createRigidArea(new Dimension(0, 8));
    }

    private static void addButton(JPanel panel, String text, java.awt.event.ActionListener listener)
    {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
    }

    private static void setComboRenderer(JComboBox<Hunter> combo)
    {
        combo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                           boolean selected, boolean focus)
            {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Hunter)
                {
                    setText(((Hunter) value).getCommonName());
                }
                return this;
            }
        });
    }

    private static final class RumourChoice
    {
        private final Rumour rumour;
        private RumourChoice(Rumour rumour) { this.rumour = rumour; }
        private static RumourChoice none() { return new RumourChoice(null); }
        @Override public String toString() { return rumour == null ? "None" : rumour.getName(); }
        @Override public boolean equals(Object other) {
            return other instanceof RumourChoice && ((RumourChoice) other).rumour == rumour;
        }
        @Override public int hashCode() { return rumour == null ? 0 : rumour.hashCode(); }
    }
}
