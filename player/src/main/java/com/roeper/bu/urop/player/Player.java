package com.roeper.bu.urop.player;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.roeper.bu.urop.lib.ConfigReader;
import com.roeper.bu.urop.lib.SensorReading;
import com.roeper.bu.urop.lib.SensorReadingReader;

public class Player
{
	public static void main(String args[]) throws Exception
	{
		String configFile = "config.yml";
		if (args.length == 1)
		{
			configFile = args[0];
		}

		// get the config
		PlayerModuleConfig config = null;
		ConfigReader<PlayerModuleConfig> reader = new ConfigReader<PlayerModuleConfig>(PlayerModuleConfig.class);
		try
		{
			config = reader.read(configFile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Error getting config");
		}

		Injector injector = Guice.createInjector(new PlayerModule(config));

		// create recorder
		final Player recorder = injector.getInstance(Player.class);

		// add shutdown hook
		Runtime	.getRuntime()
				.addShutdownHook(new Thread()
				{
					@Override
					public void run()
					{
						recorder.stop();
					}
				});

		// start recorder
		recorder.start();
	}

	final Logger logger = LoggerFactory.getLogger(Player.class);
	private SensorReadingReader reader;
	private MqttClient client;
	private String topicPrefix;

	@Inject
	protected Player(	SensorReadingReader aReader,
						MqttClient aClient,
						@Named("topicPrefix") String aTopicPrefix)
	{
		this.reader = aReader;
		this.client = aClient;
		this.topicPrefix = aTopicPrefix;
	}

	public void start()
	{
		this.reader.open();

		try
		{
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			client.connect(connOpts);
			logger.info("Successfully connected to broker.");

			//TODO replay with time
			while (this.reader.hasNext())
			{
				SensorReading next = this.reader.next();
				String topic = this.topicPrefix + "/group/" + next.getGroupId() + "/sensor/" + next.getSensorId();
				String payload = next.getPayload();
				MqttMessage message = new MqttMessage(payload.getBytes());
				message.setQos(2);
				client.publish(topic, message);
			}
		}
		catch (MqttException me)
		{
			logger.error("An error occured connecting to the broker");
			me.printStackTrace();
		}
	}

	public void stop()
	{
		this.reader.close();

		try
		{
			this.client.disconnect();
		}
		catch (MqttException e)
		{
			e.printStackTrace();
		}
	}
}
