package com.roeper.bu.urop.recorder;

import com.roeper.bu.urop.lib.BrokerConfig;

public class RecorderModuleConfig {

	private BrokerConfig brokerConfig;
	
	private String destinationFolder;
	
	protected RecorderModuleConfig()
	{
		
	}

	public RecorderModuleConfig(BrokerConfig aBrokerConfig, String aDestinationFolder)
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
