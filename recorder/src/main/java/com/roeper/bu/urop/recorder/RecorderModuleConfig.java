package com.roeper.bu.urop.recorder;

import com.roeper.bu.urop.lib.BrokerConfig;

public class RecorderModuleConfig
{
	private BrokerConfig brokerConfig;
	private String destinationFolder;
	private String outputFormat;

	protected RecorderModuleConfig()
	{

	}

	public RecorderModuleConfig(BrokerConfig aBrokerConfig,
								String aDestinationFolder,
								String aOutputFormat)
	{
		this.brokerConfig = aBrokerConfig;
		this.destinationFolder = aDestinationFolder;
		this.outputFormat = aOutputFormat;
	}

	public BrokerConfig getBrokerConfig()
	{
		return brokerConfig;
	}

	public String getDestinationFolder()
	{
		return destinationFolder;
	}

	public String getOutputFormat()
	{
		return outputFormat;
	}
}
