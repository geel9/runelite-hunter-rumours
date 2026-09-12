package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum LocationMetric
{
	BIRDS("bird", "birds"),
	CREATURES("creature", "creatures"),
	GOATS("goat", "goats"),
	KEBBITS("kebbit", "kebbits"),
	BUTTERFLIES("butterfly", "butterflies"),
	MOTHS("moth", "moths"),
	YOUNG_TREES("young tree", "young trees"),
	TRACK_STARTS("starting burrow", "starting burrows"),
	STARTING_AREAS("starting area", "starting areas");

	@Getter
	private final String singular;

	@Getter
	private final String plural;

	public String format(int count)
	{
		return count + " " + (count == 1 ? singular : plural);
	}
}
