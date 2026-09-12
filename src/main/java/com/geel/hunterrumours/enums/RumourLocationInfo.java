package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class RumourLocationInfo
{
	@Getter
	private final LocationMetric metric;

	@Getter
	private final int count;

	@Getter
	private final String note;

	public String format()
	{
		String value = metric.format(count);
		return note.isEmpty() ? value : value + ", " + note;
	}
}
