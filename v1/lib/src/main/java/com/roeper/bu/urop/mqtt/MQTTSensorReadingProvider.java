package com.roeper.bu.urop.mqtt;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.roeper.bu.urop.readings.ReadingProvider;
import com.roeper.bu.urop.readings.sensor.SensorReading;

public class MQTTSensorReadingProvider implements ReadingProvider<SensorReading>, MqttCallback
{
	final Logger logger = LoggerFactory.getLogger(MQTTSensorReadingProvider.class);

	private final BlockingQueue<SensorReading> readings = new LinkedBlockingQueue<SensorReading>();
	private final MqttClient client;
	private final String topicPrefix;

	public MQTTSensorReadingProvider(MqttClient aClient, String aTopicPrefix)
	{
		this.client = aClient;
		this.topicPrefix = aTopicPrefix;
	}

	public void start() throws Exception
	{
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		client.connect(connOpts);
		client.setCallback(this);

		// subscribe to receive sensor readings
		String subscribeDest = this.topicPrefix + "/#";
		client.subscribe(subscribeDest);
	}

	public void stop() throws Exception
	{
		if (client.isConnected())
		{
			client.disconnect();
		}
	}

	public void connectionLost(Throwable arg0)
	{
		try
		{
			this.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void deliveryComplete(IMqttDeliveryToken arg0)
	{

	}

	public void messageArrived(String aTopic, MqttMessage message) throws Exception
	{
		String payload = new String(message.getPayload());
		// remove prefix
		String topic = aTopic.substring(this.topicPrefix.length());
		String[] levels = topic.split("/");

		// basic sanity checking
		if (levels.length == 5 && levels[1].equals("group") && levels[3].equals("sensor"))
		{
			// get sensor info from topic structure:
			// e.g. <prefix>/group/<id>/sensor/<id>
			String groupId = levels[2];
			String sensorId = levels[4];

			// more checking
			String[] readings = (payload).split(",");
			if (readings.length != 6)
			{
				// log invalid sensor reading
				logger.warn("Recevied an invalid sensor reading. Topic: {} Payload: {}", aTopic,
							payload);
			}
			else
			{
				try
				{
					double red = Double.parseDouble(readings[0].trim());
					double green = Double.parseDouble(readings[1].trim());
					double blue = Double.parseDouble(readings[2].trim());
					double white = Double.parseDouble(readings[3].trim());
					int time1 = Integer.parseInt(readings[4].trim());
					int time2 = Integer.parseInt(readings[5].trim());
					SensorReading reading = new SensorReading(	groupId, sensorId, red, green, blue,
																white, time1, time2, new Date());
					this.readings.put(reading);
				}
				catch (NumberFormatException e)
				{
					e.printStackTrace();
					// log invalid number
					logger.warn("An error occured processing a sensor reading. Topic: {} Payload: {}",
								aTopic, payload);
				}
			}
		}
		else
		{
			// log invalid topic
			logger.warn("Recevied an invalid topic. Topic: {} Payload: {}", aTopic, payload);
		}
	}

	public Optional<SensorReading> getReading()
	{
		return Optional.fromNullable(this.readings.poll());
	}
}
