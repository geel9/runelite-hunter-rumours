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

	public String format()
	{
		return metric.format(count);
	}
}
