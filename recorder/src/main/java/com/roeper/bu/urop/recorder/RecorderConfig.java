package com.roeper.bu.urop.recorder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roeper.bu.urop.lib.BrokerConfig;

public class RecorderConfig {
	@JsonProperty("broker")
	private BrokerConfig brokerConfig;
	
	private String destinationFolder;
	
	protected RecorderConfig()
	{
		
	}

	public RecorderConfig(BrokerConfig aBrokerConfig, String aDestinationFolder)
	{
		this.brokerConfig = aBrokerConfig;
		this.destinationFolder = aDestinationFolder;
	}
	
	public BrokerConfig getBrokerConfig() {
		return brokerConfig;
	}

	public String getDestinationFolder() {
		return destinationFolder;
	}
}
