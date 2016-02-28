package com.roeper.bu.urop.svmLive;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.roeper.bu.urop.lib.BrokerConfig;

public class SVMLiveModule extends AbstractModule
{
	private SVMLiveModuleConfig config;

	public SVMLiveModule(SVMLiveModuleConfig aConfig)
	{
		this.config = aConfig;
	}

	@Override
	protected void configure()
	{
		bind(SVMLiveModuleConfig.class).toInstance(this.config);
	}

	@Provides
	public MqttClient getClient()
	{
		MqttClient client = null;
		try
		{
			MemoryPersistence persistence = new MemoryPersistence();
			BrokerConfig brokerConfig = this.config.getBrokerConfig();
			String clientId = "svm-live-" + (int) (Math.random() * 99999);
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
	@Named("topicPrefix")
	public String getTopicPrefix()
	{
		return this.config.getBrokerConfig().getTopicPrefix();
	}
	
	@Provides
	@Named("modelFile")
	public String getModelFile()
	{
		return this.config.getModelFile();
	}
}
