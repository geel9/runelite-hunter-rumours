package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

import java.util.Arrays;

@AllArgsConstructor
public enum Rumour
{
	NONE(Creature.BARB_TAILED_KEBBIT, "Unknown", ItemID.COINS_995, false, false, false, false),

	TROPICAL_WAGTAIL(Creature.TROPICAL_WAGTAIL, "Tropical Wagtail", ItemID.TAILFEATHERS, true, false, false, false),
	WILD_KEBBIT(Creature.WILD_KEBBIT, "Wild Kebbit", ItemID.KEBBITY_TUFT, true, false, false, false),
	SAPPHIRE_GLACIALIS(Creature.SAPPHIRE_GLACIALIS, "Sapphire Glacialis", ItemID.BLUE_BUTTERFLY_WING, true, false, false, false),

	SWAMP_LIZARD(Creature.SWAMP_LIZARD, "Swamp Lizard", ItemID.SWAMP_LIZARD_CLAW, true, true, false, false),
	SPINED_LARUPIA(Creature.SPINED_LARUPIA, "Spined Larupia", ItemID.LARUPIA_EAR, true, true, false, false),
	BARB_TAILED_KEBBIT(Creature.BARB_TAILED_KEBBIT, "Barb-tailed Kebbit", ItemID.KEBBITY_TUFT, true, true, false, false),
	SNOWY_KNIGHT(Creature.SNOWY_KNIGHT, "Snowy Knight", ItemID.WHITE_BUTTERFLY_WING, true, true, false, false),
	PRICKLY_KEBBIT(Creature.PRICKLY_KEBBIT, "Prickly Kebbit", ItemID.KEBBITY_TUFT, true, true, false, false),
	// TODO: Verify jerboa
	EMBERTAILED_JERBOA(Creature.EMBERTAILED_JERBOA, "Embertailed Jerboa", ItemID.LARGE_JERBOA_TAIL, true, true, false, false),
	HORNED_GRAAHK(Creature.HORNED_GRAAHK, "Horned Graahk", ItemID.GRAAHK_HORN_SPUR, true, true, false, false),
	SPOTTED_KEBBIT(Creature.SPOTTED_KEBBIT, "Spotted Kebbit", ItemID.KEBBITY_TUFT, true, true, false, false),
	BLACK_WARLOCK(Creature.BLACK_WARLOCK, "Black Warlock", ItemID.BLACK_BUTTERFLY_WING, true, true, false, false),

	ORANGE_SALAMANDER(Creature.ORANGE_SALAMANDER, "Orange Salamander", ItemID.ORANGE_SALAMANDER_CLAW, true, true, true, false),
	RAZOR_BACKED_KEBBIT(Creature.RAZOR_BACKED_KEBBIT, "Razor-backed Kebbit", ItemID.KEBBITY_TUFT, true, true, true, false), //TODO
	SABRE_TOOTHED_KEBBIT(Creature.SABRE_TOOTHED_KEBBIT, "Sabre-toothed Kebbit", ItemID.KEBBITY_TUFT, true, true, true, false),
	GREY_CHINCHOMPA(Creature.GREY_CHINCHOMPA, "Grey Chinchompa", ItemID.CHINCHOMPA_TUFT, true, true, true, false),
	SABRE_TOOTHED_KYATT(Creature.SABRE_TOOTHED_KYATT, "Sabre-toothed Kyatt", ItemID.KYATT_TOOTH_CHIP, true, true, true, false),
	DARK_KEBBIT(Creature.DARK_KEBBIT, "Dark Kebbit", ItemID.KEBBITY_TUFT, true, true, true, false),
	PYRE_FOX(Creature.PYRE_FOX, "Pyre Fox", ItemID.FOX_FLUFF, true, true, true, true),
	RED_SALAMANDER(Creature.RED_SALAMANDER, "Red Salamander", ItemID.RED_SALAMANDER_CLAW, true, false, true, true),
	RED_CHINCHOMPA(Creature.RED_CHINCHOMPA, "Carnivorous Chinchompa", ItemID.RED_CHINCHOMPA_TUFT, true, false, true, true),
	RED_CHINCHOMPA_2(Creature.RED_CHINCHOMPA, "Red Chinchompa", ItemID.RED_CHINCHOMPA_TUFT, true, false, true, true),
	SUNLIGHT_MOTH(Creature.SUNLIGHT_MOTH, "Sunlight Moth", ItemID.SUNLIGHT_MOTH_WING, true, false, false, true),
	DASHING_KEBBIT(Creature.DASHING_KEBBIT, "Dashing Kebbit", ItemID.KEBBITY_TUFT,true, false, true, true),
	SUNLIGHT_ANTELOPE(Creature.SUNLIGHT_ANTELOPE, "Sunlight Antelope", ItemID.ANTELOPE_HOOF_SHARD,true, false, true, true),
	MOONLIGHT_MOTH(Creature.MOONLIGHT_MOTH, "Moonlight Moth", ItemID.MOONLIGHT_MOTH_WING,true, false, false, true),
	TECU_SALAMANDER(Creature.TECU_SALAMANDER, "Tecu Salamander", ItemID.SALAMANDER_CLAW, true, false, false, true),
	HERBIBOAR(Creature.HERBIBOAR, "Herbiboar", ItemID.HERBY_TUFT, true, false, false, true),
	MOONLIGHT_ANTELOPE(Creature.MOONLIGHT_ANTELOPE, "Moonlight Antelope", ItemID.ANTELOPE_HOOF_SHARD_29241, true, false, false, true);

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
