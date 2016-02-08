package com.roeper.bu.urop.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roeper.bu.urop.lib.BrokerConfig;

public class PlayerModuleConfig
{
	@JsonProperty("broker")
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
