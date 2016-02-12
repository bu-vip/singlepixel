package com.roeper.bu.urop.player;

import com.roeper.bu.urop.lib.BrokerConfig;

public class PlayerModuleConfig
{
	private BrokerConfig brokerConfig;
	private String sourceFilePath;

	protected PlayerModuleConfig()
	{

	}

	public PlayerModuleConfig(	BrokerConfig aBrokerConfig,
								String aSourceFilePath)
	{
		this.brokerConfig = aBrokerConfig;
		this.sourceFilePath = aSourceFilePath;
	}

	public BrokerConfig getBrokerConfig()
	{
		return brokerConfig;
	}

	public String getSourceFilePath()
	{
		return sourceFilePath;
	}
}
