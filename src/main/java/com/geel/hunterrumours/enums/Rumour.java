package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;

@AllArgsConstructor
public enum Rumour
{
	NONE(Creature.BARB_TAILED_KEBBIT, "Unknown", ItemID.COINS, false, false, false, false),

	TROPICAL_WAGTAIL(Creature.TROPICAL_WAGTAIL, "Tropical Wagtail", ItemID.HG_TAILFEATHER, true, false, false, false),
	WILD_KEBBIT(Creature.WILD_KEBBIT, "Wild Kebbit", ItemID.HG_KEBBITTUFT, true, false, false, false),
	SAPPHIRE_GLACIALIS(Creature.SAPPHIRE_GLACIALIS, "Sapphire Glacialis", ItemID.HG_BUTTERFLYWING_BLUE, true, false, false, false),

	SWAMP_LIZARD(Creature.SWAMP_LIZARD, "Swamp Lizard", ItemID.HG_LIZARDCLAW, true, true, false, false),
	SPINED_LARUPIA(Creature.SPINED_LARUPIA, "Spined Larupia", ItemID.HG_LARUPIA_EAR, true, true, false, false),
	BARB_TAILED_KEBBIT(Creature.BARB_TAILED_KEBBIT, "Barb-tailed Kebbit", ItemID.HG_KEBBITTUFT, true, true, false, false),
	SNOWY_KNIGHT(Creature.SNOWY_KNIGHT, "Snowy Knight", ItemID.HG_BUTTERFLYWING_WHITE, true, true, false, false),
	PRICKLY_KEBBIT(Creature.PRICKLY_KEBBIT, "Prickly Kebbit", ItemID.HG_KEBBITTUFT, true, true, false, false),
	// TODO: Verify jerboa
	EMBERTAILED_JERBOA(Creature.EMBERTAILED_JERBOA, "Embertailed Jerboa", ItemID.HG_JERBOATAIL_LARGE, true, true, false, false),
	HORNED_GRAAHK(Creature.HORNED_GRAAHK, "Horned Graahk", ItemID.HG_GRAAHK_HORN, true, true, false, false),
	SPOTTED_KEBBIT(Creature.SPOTTED_KEBBIT, "Spotted Kebbit", ItemID.HG_KEBBITTUFT, true, true, false, false),
	BLACK_WARLOCK(Creature.BLACK_WARLOCK, "Black Warlock", ItemID.HG_BUTTERFLYWING_BLACK, true, true, false, false),

	ORANGE_SALAMANDER(Creature.ORANGE_SALAMANDER, "Orange Salamander", ItemID.HG_LIZARDCLAW_ORANGE, true, true, true, false),
	RAZOR_BACKED_KEBBIT(Creature.RAZOR_BACKED_KEBBIT, "Razor-backed Kebbit", ItemID.HG_KEBBITTUFT, true, true, true, false), //TODO
	SABRE_TOOTHED_KEBBIT(Creature.SABRE_TOOTHED_KEBBIT, "Sabre-toothed Kebbit", ItemID.HG_KEBBITTUFT, true, true, true, false),
	GREY_CHINCHOMPA(Creature.GREY_CHINCHOMPA, "Grey Chinchompa", ItemID.HG_CHINCHOMPATUFT, true, true, true, false),
	SABRE_TOOTHED_KYATT(Creature.SABRE_TOOTHED_KYATT, "Sabre-toothed Kyatt", ItemID.HG_KYATTTOOTH, true, true, true, false),
	DARK_KEBBIT(Creature.DARK_KEBBIT, "Dark Kebbit", ItemID.HG_KEBBITTUFT, true, true, true, false),
	PYRE_FOX(Creature.PYRE_FOX, "Pyre Fox", ItemID.HG_FOXFLUFF, true, true, true, true),
	RED_SALAMANDER(Creature.RED_SALAMANDER, "Red Salamander", ItemID.HG_LIZARDCLAW_RED, true, false, true, true),
	WYRMSCRAIG_GOAT(Creature.WYRMSCRAIG_GOAT, "Wyrmscraig Goat", ItemID.HG_GOAT_PIT_HOOF, true, true, true, false),
	RED_CHINCHOMPA(Creature.RED_CHINCHOMPA, "Carnivorous Chinchompa", ItemID.HG_CHINCHOMPATUFT_RED, true, false, true, true),
	RED_CHINCHOMPA_2(Creature.RED_CHINCHOMPA, "Red Chinchompa", ItemID.HG_CHINCHOMPATUFT_RED, true, false, true, true),
	SUNLIGHT_MOTH(Creature.SUNLIGHT_MOTH, "Sunlight Moth", ItemID.HG_MOTHWING_SUN, true, false, false, true),
	DASHING_KEBBIT(Creature.DASHING_KEBBIT, "Dashing Kebbit", ItemID.HG_KEBBITTUFT,true, false, true, true),
	SUNLIGHT_ANTELOPE(Creature.SUNLIGHT_ANTELOPE, "Sunlight Antelope", ItemID.HG_ANTELOPEHOOF_SUN,true, false, true, true),
	MOONLIGHT_MOTH(Creature.MOONLIGHT_MOTH, "Moonlight Moth", ItemID.HG_MOTHWING_MOON,true, false, false, true),
	TECU_SALAMANDER(Creature.TECU_SALAMANDER, "Tecu Salamander", ItemID.HG_LIZARDCLAW_MOUNTAIN, true, false, false, true),
	HERBIBOAR(Creature.HERBIBOAR, "Herbiboar", ItemID.HG_HERBYTUFT, true, false, false, true),
	MOONLIGHT_ANTELOPE(Creature.MOONLIGHT_ANTELOPE, "Moonlight Antelope", ItemID.HG_ANTELOPEHOOF_MOON, true, false, false, true);

	@Getter
    private final Creature TargetCreature;

	@Getter
	private final String Name;

	@Getter
	private final int RumourItemID;

	@Getter
	private final boolean Novice;

	@Getter
	private final boolean Adept;

	@Getter
	private final boolean Expert;

	@Getter
	private final boolean Master;

	public static Rumour[] allValues()
	{
		return Arrays.stream(Rumour.values()).filter(rumour -> rumour.getTargetCreature().getHunterLevel() != 0).toArray(Rumour[]::new);
	}

	public Trap getTrap() {
	    return getTargetCreature().getTrap();
    }

	public String getFullName()
	{
		// Don't show the trap if it's NONE
		if (this == NONE)
		{
			return this.getName();
		}

		return this.getName() + " (" + this.getTargetCreature().getTrap().getName() + ")";
	}
}
