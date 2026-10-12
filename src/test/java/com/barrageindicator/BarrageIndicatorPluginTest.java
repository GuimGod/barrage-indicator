package com.barrageindicator;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BarrageIndicatorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BarrageIndicatorPlugin.class);
		RuneLite.main(args);
	}
}
