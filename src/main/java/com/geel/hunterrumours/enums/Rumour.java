package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

import java.util.Arrays;

@AllArgsConstructor
public enum Rumour
{
	NONE(Creature.BARB_TAILED_KEBBIT, "Unknown", ItemID.COINS_995, false, false, false, false),

	TROPICAL_WAGTAIL(Creature.TROPICAL_WAGTAIL, "Tropical Wagtail", ItemID.TROPICAL_WAGTAIL, true, false, false, false),
	WILD_KEBBIT(Creature.WILD_KEBBIT, "Wild Kebbit", ItemID.KEBBIT_9962, true, false, false, false),
	SAPPHIRE_GLACIALIS(Creature.SAPPHIRE_GLACIALIS, "Sapphire Glacialis", ItemID.BUTTERFLY_9971, true, false, false, false),

	SWAMP_LIZARD(Creature.SWAMP_LIZARD, "Swamp Lizard", ItemID.SWAMP_LIZARD, true, true, false, false),
	SPINED_LARUPIA(Creature.SPINED_LARUPIA, "Spined Larupia", ItemID.LARUPIA_HAT, true, true, false, false),
	BARB_TAILED_KEBBIT(Creature.BARB_TAILED_KEBBIT, "Barb-tailed Kebbit", ItemID.KEBBIT_9958, true, true, false, false),
	SNOWY_KNIGHT(Creature.SNOWY_KNIGHT, "Snowy Knight", ItemID.BUTTERFLY_9972, true, true, false, false),
	PRICKLY_KEBBIT(Creature.PRICKLY_KEBBIT, "Prickly Kebbit", ItemID.KEBBIT_9957, true, true, false, false),
	// TODO: Verify jerboa
	EMBERTAILED_JERBOA(Creature.EMBERTAILED_JERBOA, "Embertailed Jerboa", ItemID.EMBERTAILED_JERBOA, true, true, false, false),
	HORNED_GRAAHK(Creature.HORNED_GRAAHK, "Horned Graahk", ItemID.GRAAHK_HEADDRESS, true, true, false, false),
	SPOTTED_KEBBIT(Creature.SPOTTED_KEBBIT, "Spotted Kebbit", ItemID.KEBBIT_9960, true, true, false, false),
	BLACK_WARLOCK(Creature.BLACK_WARLOCK, "Black Warlock", ItemID.BUTTERFLY_9973, true, true, false, false),

	ORANGE_SALAMANDER(Creature.ORANGE_SALAMANDER, "Orange Salamander", ItemID.ORANGE_SALAMANDER, true, true, true, false),
	RAZOR_BACKED_KEBBIT(Creature.RAZOR_BACKED_KEBBIT, "Razor-backed Kebbit", ItemID.KEBBIT_9961, true, true, true, false), //TODO
	SABRE_TOOTHED_KEBBIT(Creature.SABRE_TOOTHED_KEBBIT, "Sabre-toothed Kebbit", ItemID.KEBBIT_9959, true, true, true, false),
	GREY_CHINCHOMPA(Creature.GREY_CHINCHOMPA, "Grey Chinchompa", ItemID.CHINCHOMPA, true, true, true, false),
	SABRE_TOOTHED_KYATT(Creature.SABRE_TOOTHED_KYATT, "Sabre-toothed Kyatt", ItemID.KYATT_HAT, true, true, true, false),
	DARK_KEBBIT(Creature.DARK_KEBBIT, "Dark Kebbit", ItemID.KEBBIT_9963, true, true, true, false),
	PYRE_FOX(Creature.PYRE_FOX, "Pyre Fox", ItemID.PYRE_FOX, true, true, true, true),
	RED_SALAMANDER(Creature.RED_SALAMANDER, "Red Salamander", ItemID.RED_SALAMANDER, true, false, true, true),
	RED_CHINCHOMPA(Creature.RED_CHINCHOMPA, "Carnivorous Chinchompa", ItemID.RED_CHINCHOMPA, true, false, true, true),
	RED_CHINCHOMPA_2(Creature.RED_CHINCHOMPA, "Red Chinchompa", ItemID.RED_CHINCHOMPA, true, false, true, true),
	SUNLIGHT_MOTH(Creature.SUNLIGHT_MOTH, "Sunlight Moth", ItemID.SUNLIGHT_MOTH, true, false, false, true),
	DASHING_KEBBIT(Creature.DASHING_KEBBIT, "Dashing Kebbit", ItemID.KEBBIT_9964,true, false, true, true),
	SUNLIGHT_ANTELOPE(Creature.SUNLIGHT_ANTELOPE, "Sunlight Antelope", ItemID.SUNLIGHT_ANTELOPE,true, false, true, true),
	MOONLIGHT_MOTH(Creature.MOONLIGHT_MOTH, "Moonlight Moth", ItemID.MOONLIGHT_MOTH,true, false, false, true),
	TECU_SALAMANDER(Creature.TECU_SALAMANDER, "Tecu Salamander", ItemID.TECU_SALAMANDER, true, false, false, true),
	HERBIBOAR(Creature.HERBIBOAR, "Herbiboar", ItemID.HERBIBOAR, true, false, false, true),
	MOONLIGHT_ANTELOPE(Creature.MOONLIGHT_ANTELOPE, "Moonlight Antelope", ItemID.MOONLIGHT_ANTELOPE, true, false, false, true);

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
