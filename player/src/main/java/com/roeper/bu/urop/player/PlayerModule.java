package com.roeper.bu.urop.player;

import java.io.File;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.roeper.bu.urop.lib.BrokerConfig;
import com.roeper.bu.urop.lib.ObjectReader;
import com.roeper.bu.urop.lib.SensorReading;

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
	public ObjectReader<SensorReading> getReader(ObjectMapper aMapper)
	{
		return new ObjectReader<SensorReading>(aMapper, new File(this.config.getSourceFilePath()), SensorReading.class);
	}

	@Provides
	@Named("topicPrefix")
	public String getTopicPrefix()
	{
		return this.config	.getBrokerConfig()
							.getTopicPrefix();
	}
}
