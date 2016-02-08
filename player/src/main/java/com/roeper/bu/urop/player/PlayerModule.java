package com.roeper.bu.urop.player;

import java.io.File;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.roeper.bu.urop.lib.BrokerConfig;
import com.roeper.bu.urop.lib.SensorReadingReader;

public class PlayerModule extends AbstractModule
{
	private PlayerModuleConfig config;

	public PlayerModule(PlayerModuleConfig aConfig)
	{
		this.config = aConfig;
	}

	@Override
	protected void configure()
	{
		bind(PlayerModuleConfig.class).toInstance(this.config);

		install(new FactoryModuleBuilder()	.implement(	SensorReadingReader.class,
		                                  	          SensorReadingReader.class)
											.build(SensorReadingReader.Factory.class));
	}

	@Provides
	public MqttClient getClient()
	{
		MqttClient client = null;
		try
		{
			MemoryPersistence persistence = new MemoryPersistence();
			BrokerConfig brokerConfig = this.config.getBrokerConfig();
			String clientId = "data-player"; //note non random client ID
			client = new MqttClient(brokerConfig.getHostname(),
									clientId,
									persistence);
		}
		catch (MqttException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Error creating MQTT client.");
		}

		return client;
	}
	
	@Provides
	public SensorReadingReader getReader(SensorReadingReader.Factory aReaderFactor)
	{
		return aReaderFactor.create(new File(this.config.getSourceFilePath()));
	}

	@Provides
	@Named("topicPrefix")
	public String getTopicPrefix()
	{
		return this.config	.getBrokerConfig()
							.getTopicPrefix();
	}
}
