package com.geel.hunterrumours;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;

import java.util.Arrays;

import static com.geel.hunterrumours.Trap.SNARE;
import static com.geel.hunterrumours.Trap.BOX_TRAP;
import static com.geel.hunterrumours.Trap.DEADFALL;
import static com.geel.hunterrumours.Trap.NET_TRAP;
import static com.geel.hunterrumours.Trap.PIT;
import static com.geel.hunterrumours.Trap.FALCONRY;
import static com.geel.hunterrumours.Trap.BUTTERFLY;
import static com.geel.hunterrumours.Trap.NOOSE;

@AllArgsConstructor
public enum Rumour
{
	NONE(0, "Unknown", ItemID.COINS_995, BOX_TRAP, 0, new int[]{}, false, false, false, false),

	TROPICAL_WAGTAIL(NpcID.TROPICAL_WAGTAIL, "Tropical Wagtail", ItemID.TROPICAL_WAGTAIL, SNARE, 19, new int[]{95, 96}, true, false, false, false),
	WILD_KEBBIT(NpcID.WILD_KEBBIT, "Wild Kebbit", ItemID.KEBBIT_9962, DEADFALL, 23, new int[]{128}, true, false, false, false),
	SAPPHIRE_GLACIALIS(NpcID.SAPPHIRE_GLACIALIS, "Sapphire Glacialis", ItemID.BUTTERFLY_9971, BUTTERFLY, 25, new int[]{34}, true, false, false, false),

	SWAMP_LIZARD(NpcID.SWAMP_LIZARD, "Swamp Lizard", ItemID.SWAMP_LIZARD, NET_TRAP, 29, new int[]{152}, true, true, false, false),
	SPINED_LARUPIA(NpcID.SPINED_LARUPIA, "Spined Larupia", ItemID.LARUPIA_HAT, PIT, 31, new int[]{180}, true, true, false, false),
	BARB_TAILED_KEBBIT(NpcID.BARBTAILED_KEBBIT, "Barb-tailed Kebbit", ItemID.KEBBIT_9958, DEADFALL, 33, new int[]{168}, true, true, false, false),
	SNOWY_KNIGHT(NpcID.SNOWY_KNIGHT, "Snowy Knight", ItemID.BUTTERFLY_9972, BUTTERFLY, 35, new int[]{44}, true, true, false, false),
	PRICKLY_KEBBIT(NpcID.PRICKLY_KEBBIT, "Prickly Kebbit", ItemID.KEBBIT_9957, DEADFALL, 37, new int[]{204}, true, true, false, false),
	// TODO: Verify jerboa
	EMBERTAILED_JERBOA(NpcID.EMBERTAILED_JERBOA, "Embertailed Jerboa", ItemID.EMBERTAILED_JERBOA, BOX_TRAP, 39, new int[]{137}, true, true, false, false),
	HORNED_GRAAHK(NpcID.HORNED_GRAAHK, "Horned Graahk", ItemID.GRAAHK_HEADDRESS, PIT, 41, new int[]{240}, true, true, false, false),
	SPOTTED_KEBBIT(NpcID.SPOTTED_KEBBIT, "Spotted Kebbit", ItemID.KEBBIT_9960, FALCONRY, 43, new int[]{104}, true, true, false, false),
	BLACK_WARLOCK(NpcID.BLACK_WARLOCK, "Black Warlock", ItemID.BUTTERFLY_9973, BUTTERFLY, 45, new int[]{54}, true, true, false, false),

	ORANGE_SALAMANDER(NpcID.ORANGE_SALAMANDER, "Orange Salamander", ItemID.ORANGE_SALAMANDER, NET_TRAP, 47, new int[]{224}, true, true, true, false),
	RAZOR_BACKED_KEBBIT(0, "Razor-backed Kebbit", ItemID.KEBBIT_9961, NOOSE, 49, new int[]{348}, true, true, true, false), //TODO
	SABRE_TOOTHED_KEBBIT(NpcID.SABRETOOTHED_KEBBIT, "Sabre-toothed Kebbit", ItemID.KEBBIT_9959, DEADFALL, 51, new int[]{200}, true, true, true, false),
	GREY_CHINCHOMPA(NpcID.CHINCHOMPA, "Grey Chinchompa", ItemID.CHINCHOMPA, BOX_TRAP, 53, new int[]{198, 199}, true, true, true, false),
	SABRE_TOOTHED_KYATT(NpcID.SABRETOOTHED_KYATT, "Sabre-toothed Kyatt", ItemID.KYATT_HAT, PIT, 53, new int[]{300}, true, true, true, false),
	DARK_KEBBIT(NpcID.DARK_KEBBIT, "Dark Kebbit", ItemID.KEBBIT_9963, FALCONRY, 57, new int[]{132}, true, true, true, false),
	PYRE_FOX(NpcID.PYRE_FOX, "Pyre Fox", ItemID.PYRE_FOX, DEADFALL, 57, new int[]{222}, true, true, true, true),
	RED_SALAMANDER(NpcID.RED_SALAMANDER, "Red Salamander", ItemID.RED_SALAMANDER, NET_TRAP, 59, new int[]{272}, true, false, true, true),
	RED_CHINCHOMPA(NpcID.CARNIVOROUS_CHINCHOMPA, "Carnivorous Chinchompa", ItemID.RED_CHINCHOMPA, BOX_TRAP, 63, new int[]{265}, true, false, true, true),
	RED_CHINCHOMPA_2(NpcID.CARNIVOROUS_CHINCHOMPA, "Red Chinchompa", ItemID.RED_CHINCHOMPA, BOX_TRAP, 63, new int[]{265}, true, false, true, true),
	SUNLIGHT_MOTH(NpcID.SUNLIGHT_MOTH, "Sunlight Moth", ItemID.SUNLIGHT_MOTH, BUTTERFLY, 65, new int[]{74}, true, false, false, true),
	DASHING_KEBBIT(NpcID.DASHING_KEBBIT, "Dashing Kebbit", ItemID.KEBBIT_9964, FALCONRY, 69,  new int[]{156},true, false, true, true),
	SUNLIGHT_ANTELOPE(NpcID.SUNLIGHT_ANTELOPE, "Sunlight Antelope", ItemID.SUNLIGHT_ANTELOPE, PIT, 72,  new int[]{380},true, false, true, true),
	MOONLIGHT_MOTH(NpcID.MOONLIGHT_MOTH, "Moonlight Moth", ItemID.MOONLIGHT_MOTH, BUTTERFLY, 75,  new int[]{84},true, false, false, true),
	TECU_SALAMANDER(NpcID.TECU_SALAMANDER, "Tecu Salamander", ItemID.TECU_SALAMANDER, NET_TRAP, 79, new int[]{344}, true, false, false, true),
	HERBIBOAR(NpcID.HERBIBOAR, "Herbiboar", ItemID.HERBIBOAR, NOOSE, 80, new int[]{204}, true, false, false, true),
	MOONLIGHT_ANTELOPE(NpcID.MOONLIGHT_ANTELOPE, "Moonlight Antelope", ItemID.MOONLIGHT_ANTELOPE, PIT, 91, new int[]{450}, true, false, false, true);

	@Getter
	private final int NpcId;

	@Getter
	private final String Name;

	@Getter
	private final int ItemId;

	@Getter
	private final Trap Trap;

	@Getter
	private final int HunterLevel;

	@Getter
	private final int[] PossibleXpDrops;

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
		return Arrays.stream(Rumour.values()).filter(rumour -> rumour.HunterLevel != 0).toArray(Rumour[]::new);
	}

	public String getFullName()
	{
		// Don't show the trap if it's NONE
		if (this == NONE)
		{
			return this.getName();
		}

		return this.getName() + " (" + this.getTrap().getName() + ")";
	}
}
