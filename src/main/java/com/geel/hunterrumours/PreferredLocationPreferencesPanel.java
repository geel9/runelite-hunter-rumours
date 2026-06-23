package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Creature;
import com.geel.hunterrumours.enums.RumourLocation;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.EnumMap;
import java.util.Map;

/** Sidebar editor for the hidden per-creature preferred location settings. */
public class PreferredLocationPreferencesPanel extends PluginPanel
{
    private static final Color GOLD = new Color(214, 163, 62);
    private static final Color MUTED_TEXT = new Color(170, 170, 170);
    private static final Color SCROLL_TRACK = new Color(31, 32, 34);
    private static final Color SCROLL_THUMB = new Color(91, 94, 97);
    private static final Color SCROLL_THUMB_HOVER = new Color(118, 121, 124);
    private static final Color SELECT_BACKGROUND = new Color(42, 44, 46);
    private static final Color SELECT_BACKGROUND_HOVER = new Color(49, 52, 54);
    private static final Color SELECT_BORDER = new Color(76, 79, 82);
    private static final Color SELECT_BORDER_ACTIVE = new Color(170, 128, 49);
    private static final Color SELECTED_ROW = new Color(64, 82, 67);

    private final PreferredLocationPreferences preferences;
    private final ItemManager itemManager;
    private final JPanel cards = new JPanel();
    private final JLabel resultCount = new JLabel();
    private final JTextField search = new JTextField();
    private final Map<Creature, JPanel> creatureCards = new EnumMap<>(Creature.class);

    @Inject
    PreferredLocationPreferencesPanel(PreferredLocationPreferences preferences, ItemManager itemManager)
    {
        this.preferences = preferences;
        this.itemManager = itemManager;
        setBorder(new EmptyBorder(0, 6, 0, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        configureScrolling();

        add(createHero());
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(createSearch());
        add(Box.createRigidArea(new Dimension(0, 4)));
        resultCount.setFont(FontManager.getRunescapeSmallFont());
        resultCount.setForeground(MUTED_TEXT);
        resultCount.setBorder(new EmptyBorder(0, 4, 3, 0));
        resultCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(resultCount);

        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setOpaque(false);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(cards);

        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e) { rebuild(); }
            @Override public void removeUpdate(DocumentEvent e) { rebuild(); }
            @Override public void changedUpdate(DocumentEvent e) { rebuild(); }
        });
        SwingUtilities.invokeLater(this::rebuild);
    }

    private void configureScrolling()
    {
        getScrollPane().setBorder(BorderFactory.createEmptyBorder());
        getScrollPane().setBackground(ColorScheme.DARK_GRAY_COLOR);
        getScrollPane().setViewportBorder(null);
        getScrollPane().getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JScrollBar scrollBar = getScrollPane().getVerticalScrollBar();
        scrollBar.setPreferredSize(new Dimension(10, 0));
        scrollBar.setUnitIncrement(48);
        scrollBar.setBlockIncrement(240);
        scrollBar.setUI(new PreferredLocationScrollBarUI());
    }

    static BufferedImage createNavigationIcon()
    {
        BufferedImage image = ImageUtil.loadImageResource(
                PreferredLocationPreferencesPanel.class, "/util/Basic_quetzal_whistle.png");
        return ImageUtil.resizeImage(image, 16, 16);
    }

    private JPanel createHero()
    {
        JPanel hero = new JPanel()
        {
            @Override protected void paintComponent(Graphics graphics)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(50, 83, 58), getWidth(), getHeight(), new Color(35, 36, 38)));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(13, 12, 12, 12));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel("HUNTER RUMOUR LOCATIONS");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 14f));
        JLabel subtitle = new JLabel(
                "<html><div style='width: 165px'>Choose where to hunt each creature.<br /><br />"
                        + "This will change where 'Auto-Scroll to Fairy Ring' scrolls to and "
                        + "'use shortest path plugin' paths to.</div></html>");
        subtitle.setForeground(new Color(205, 215, 207));
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        hero.add(title);
        hero.add(subtitle);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, hero.getPreferredSize().height));
        return hero;
    }

    private JPanel createSearch()
    {
        JPanel wrapper = new JPanel(new BorderLayout(7, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 2, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("Search");
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(GOLD);
        label.setToolTipText("Filter creatures or locations");

        search.setToolTipText("Filter creatures or locations");
        search.getAccessibleContext().setAccessibleName("Search creatures or locations");
        search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        search.setForeground(Color.WHITE);
        search.setCaretColor(Color.WHITE);
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR), new EmptyBorder(5, 7, 5, 7)));
        wrapper.add(label, BorderLayout.WEST);
        wrapper.add(search, BorderLayout.CENTER);
        return wrapper;
    }

    private void rebuild()
    {
        String query = search.getText().trim().toLowerCase(Locale.ENGLISH);
        cards.removeAll();
        int shown = 0;
        for (Creature creature : preferences.getCreatures())
        {
            String name = preferences.getDisplayName(creature);
            List<RumourLocation> locations = preferences.getLocations(creature);
            boolean matches = name.toLowerCase(Locale.ENGLISH).contains(query)
                    || locations.stream().anyMatch(location -> location.getLocationName().toLowerCase(Locale.ENGLISH).contains(query));
            if (!matches)
            {
                continue;
            }
            if (shown++ > 0)
            {
                cards.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            cards.add(creatureCards.computeIfAbsent(creature,
                    ignored -> createCreatureCard(creature, name, locations)));
        }
        resultCount.setText(shown + (shown == 1 ? " creature" : " creatures"));
        Dimension contentSize = cards.getPreferredSize();
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, contentSize.height));
        cards.revalidate();
        cards.repaint();
        revalidate();
        getWrappedPanel().revalidate();
        getScrollPane().revalidate();

        SwingUtilities.invokeLater(() ->
        {
            JScrollBar scrollBar = getScrollPane().getVerticalScrollBar();
            int maximumValue = Math.max(scrollBar.getMinimum(),
                    scrollBar.getMaximum() - scrollBar.getVisibleAmount());
            if (scrollBar.getValue() > maximumValue)
            {
                scrollBar.setValue(maximumValue);
            }
        });
    }

    private JPanel createCreatureCard(Creature creature, String name, List<RumourLocation> locations)
    {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(62, 63, 65)), new EmptyBorder(9, 9, 9, 9)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel image = new JLabel();
        image.setPreferredSize(new Dimension(36, 36));
        AsyncBufferedImage itemImage = itemManager.getImage(creature.getItemId());
        itemImage.addTo(image);
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setOpaque(false);
        JLabel title = new JLabel(name);
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont());
        JLabel detail = new JLabel(locations.size() + (locations.size() == 1 ? " location" : " locations"));
        detail.setForeground(MUTED_TEXT);
        detail.setFont(FontManager.getRunescapeSmallFont());
        identity.add(title);
        identity.add(detail);
        JPanel heading = new JPanel(new BorderLayout(7, 0));
        heading.setOpaque(false);
        heading.add(image, BorderLayout.WEST);
        heading.add(identity, BorderLayout.CENTER);
        card.add(heading, BorderLayout.NORTH);

        JComboBox<RumourLocation> choice = new JComboBox<>(locations.toArray(new RumourLocation[0]));
        choice.setUI(new PreferredLocationComboBoxUI());
        choice.setOpaque(false);
        choice.setBackground(SELECT_BACKGROUND);
        choice.setForeground(Color.WHITE);
        choice.setFont(FontManager.getRunescapeSmallFont());
        choice.setBorder(new EmptyBorder(1, 1, 1, 1));
        choice.setMaximumRowCount(8);
        choice.setPreferredSize(new Dimension(0, 30));
        choice.setRenderer((list, value, index, selected, focused) ->
        {
            String suffix = value.getFairyRingCode().length() == 3 ? " (" + value.getFairyRingCode() + ")" : "";
            JLabel label = new JLabel(value.getLocationName() + suffix);
            label.setOpaque(index >= 0);
            label.setFont(index < 0 ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
            label.setBorder(new EmptyBorder(index < 0 ? 5 : 7, 9, index < 0 ? 5 : 7, 7));
            label.setBackground(index < 0
                    ? new Color(0, 0, 0, 0)
                    : selected ? SELECTED_ROW : SELECT_BACKGROUND);
            label.setForeground(selected && index >= 0 ? new Color(238, 218, 176) : Color.WHITE);
            return label;
        });
        choice.setSelectedItem(preferences.getPreferredLocation(creature));
        choice.addActionListener(event ->
        {
            RumourLocation selected = (RumourLocation) choice.getSelectedItem();
            if (selected != null)
            {
                preferences.setPreferredLocation(creature, selected);
            }
        });
        card.add(choice, BorderLayout.SOUTH);
        return card;
    }

    private static class PreferredLocationComboBoxUI extends BasicComboBoxUI
    {
        private boolean hovered;

        @Override
        protected void installListeners()
        {
            super.installListeners();
            comboBox.addMouseListener(new MouseAdapter()
            {
                @Override public void mouseEntered(MouseEvent event)
                {
                    hovered = true;
                    comboBox.repaint();
                }

                @Override public void mouseExited(MouseEvent event)
                {
                    hovered = false;
                    comboBox.repaint();
                }
            });
        }

        @Override
        public void paint(Graphics graphics, JComponent component)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered ? SELECT_BACKGROUND_HOVER : SELECT_BACKGROUND);
            g.fillRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 8, 8);
            g.setColor(comboBox.hasFocus() || comboBox.isPopupVisible() ? SELECT_BORDER_ACTIVE : SELECT_BORDER);
            g.drawRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 8, 8);
            g.dispose();
            super.paint(graphics, component);
        }

        @Override
        public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus)
        {
            // The rounded background is painted once by paint(); BasicComboBoxUI's default is an opaque rectangle.
        }

        @Override
        protected JButton createArrowButton()
        {
            JButton button = new JButton()
            {
                @Override
                protected void paintComponent(Graphics graphics)
                {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(getModel().isRollover() ? Color.WHITE : GOLD);
                    int centerX = getWidth() / 2;
                    int centerY = getHeight() / 2;
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(centerX - 4, centerY - 2, centerX, centerY + 2);
                    g.drawLine(centerX, centerY + 2, centerX + 4, centerY - 2);
                    g.dispose();
                }
            };
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setRolloverEnabled(true);
            button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            return button;
        }

        @Override
        protected ComboPopup createPopup()
        {
            return new BasicComboPopup(comboBox)
            {
                @Override
                protected void configurePopup()
                {
                    super.configurePopup();
                    setBorder(BorderFactory.createLineBorder(SELECT_BORDER_ACTIVE));
                }

                @Override
                protected void configureList()
                {
                    super.configureList();
                    list.setBackground(SELECT_BACKGROUND);
                    list.setSelectionBackground(SELECTED_ROW);
                    list.setSelectionForeground(Color.WHITE);
                    list.setFixedCellHeight(30);
                }

                @Override
                protected JScrollPane createScroller()
                {
                    JScrollPane scroller = super.createScroller();
                    scroller.setBorder(BorderFactory.createEmptyBorder());
                    scroller.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
                    scroller.getVerticalScrollBar().setUnitIncrement(30);
                    scroller.getVerticalScrollBar().setUI(new PreferredLocationScrollBarUI());
                    return scroller;
                }
            };
        }
    }

    private static class PreferredLocationScrollBarUI extends BasicScrollBarUI
    {
        @Override
        protected void configureScrollBarColors()
        {
            trackColor = SCROLL_TRACK;
            thumbColor = SCROLL_THUMB;
            thumbDarkShadowColor = SCROLL_THUMB;
            thumbHighlightColor = SCROLL_THUMB;
            thumbLightShadowColor = SCROLL_THUMB;
        }

        @Override
        protected JButton createDecreaseButton(int orientation)
        {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation)
        {
            return createZeroButton();
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds)
        {
            graphics.setColor(SCROLL_TRACK);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds)
        {
            if (bounds.isEmpty() || !scrollbar.isEnabled())
            {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isThumbRollover() ? SCROLL_THUMB_HOVER : SCROLL_THUMB);
            g.fillRoundRect(bounds.x + 2, bounds.y + 2, Math.max(4, bounds.width - 4),
                    Math.max(8, bounds.height - 4), 6, 6);
            g.dispose();
        }

        private static JButton createZeroButton()
        {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            return button;
        }
    }
}
