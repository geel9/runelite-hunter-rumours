package com.geel.hunterrumours;

import static org.junit.Assert.fail;
import org.junit.Test;

public class TrapTest
{
	@Test
	public void trapTests()
	{
		for (final Trap trap : Trap.values())
		{
			// Check if trap has a name
			if (trap.getName().equals(""))
			{
				fail("Trap should have a name");
			}

			// Check if trap has an item id
			if (trap.getItemId() <= 0)
			{
				fail("Trap (" + trap.getName() + ") should have an item id set (>0)");
			}

			// Pity threshold should be > 0
			if (trap.getPityThreshold() <= 0)
			{
				fail("Trap (" + trap.getName() + ") should have a pity threshold > 0");
			}
		}
	}
}
