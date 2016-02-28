package com.roeper.bu.urop.svmLive;

import com.roeper.bu.urop.lib.BrokerConfig;

public class SVMLiveModuleConfig {

	private BrokerConfig brokerConfig;
	
	private String modelFile;
	
	protected SVMLiveModuleConfig()
	{
		
	}

	public SVMLiveModuleConfig(BrokerConfig aBrokerConfig, String aDestinationFolder)
	{
		this.brokerConfig = aBrokerConfig;
		this.modelFile = aDestinationFolder;
	}
	
	public BrokerConfig getBrokerConfig() {
		return brokerConfig;
	}

	public String getModelFile() {
		return modelFile;
	}
}
