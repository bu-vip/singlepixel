package com.roeper.bu.urop.recorder;

import org.apache.activemq.broker.BrokerService;
import org.junit.After;
import org.junit.Before;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.roeper.bu.urop.lib.ConfigReader;

public class RecorderTest
{
	private BrokerService broker;
	private Recorder recorder;
	private Injector injector;
	private RecorderModuleConfig config;

	@Before
	public void setUp() throws Exception
	{
		if (config == null)
		{
			ConfigReader<RecorderModuleConfig> reader = new ConfigReader<RecorderModuleConfig>(RecorderModuleConfig.class);
			try
			{
				config = reader.read(getClass().getResourceAsStream("/test_config.yml"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException("Error getting config");
			}
		}
		
		injector = Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				
			}
		});
		
		broker = new BrokerService();

		// configure the broker
		broker.addConnector("mqtt://localhost:1883");

		broker.start();
	}
	
	//TODO
	// -no broker
	// -broker disconnects during recording
	// -basic recording
	
	@After
	public void stopBroker() throws Exception
	{
		broker.stop();
		injector = null;
	}
}
