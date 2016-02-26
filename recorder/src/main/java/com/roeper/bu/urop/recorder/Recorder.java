package com.roeper.bu.urop.recorder;

import java.util.Date;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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
import com.roeper.bu.urop.lib.BrokerConfig;
import com.roeper.bu.urop.lib.ConfigReader;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.lib.SensorReading;

public class Recorder implements MqttCallback
{
	public static void main(String args[]) throws Exception
	{
		if (args.length == 2)
		{
			// get the config
			BrokerConfig brokerConfig = null;
			ConfigReader<BrokerConfig> reader = new ConfigReader<BrokerConfig>(BrokerConfig.class);
			try
			{
				brokerConfig = reader.read(args[0]);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException("Error getting config");
			}

			RecorderModuleConfig config = new RecorderModuleConfig(	brokerConfig,
																	args[1]);

			Injector injector = Guice.createInjector(new RecorderModule(config));

			// create recorder
			final Recorder recorder = injector.getInstance(Recorder.class);

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
		else
		{
			System.out.println("Usage: <config-file> <output-dir>");
		}
	}

	final Logger logger = LoggerFactory.getLogger(Recorder.class);

	private final ObjectWriter<SensorReading> writer;
	private final MqttClient client;
	private final String topicPrefix;

	@Inject
	protected Recorder(	ObjectWriter<SensorReading> aWriter,
						MqttClient aClient,
						@Named("topicPrefix") String aTopicPrefix)
	{
		this.writer = aWriter;
		this.client = aClient;
		this.topicPrefix = aTopicPrefix;
	}

	public void start()
	{

		writer.open();

		try
		{
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			client.connect(connOpts);
			logger.info("Successfully connected to broker.");
			client.setCallback(this);

			// subscribe to receive sensor readings
			String subscribeDest = this.topicPrefix + "/#";
			logger.info("Subscribing to: {}",
						subscribeDest);
			client.subscribe(subscribeDest);
		}
		catch (MqttException me)
		{
			logger.error("An error occured connecting to the broker");
			me.printStackTrace();
			this.stop();
		}
	}

	public void stop()
	{
		logger.info("Stopping...");

		writer.close();

		try
		{
			if (client.isConnected())
			{
				client.disconnect();
			}
		}
		catch (MqttException e)
		{
			e.printStackTrace();
		}
	}

	public void connectionLost(Throwable arg0)
	{
		logger.info("Lost connection. All data collected will be saved");
		this.stop();
	}

	public void deliveryComplete(IMqttDeliveryToken arg0)
	{

	}

	public void messageArrived(	String aTopic,
								MqttMessage message) throws Exception
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
				logger.warn("Recevied an invalid sensor reading. Topic: {} Payload: {}",
							aTopic,
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
					SensorReading reading = new SensorReading(	groupId,
																sensorId,
																red,
																green,
																blue,
																white,
																time1,
																time2,
																new Date());
					writer.write(reading);
				}
				catch (NumberFormatException e)
				{
					e.printStackTrace();
					// log invalid number
					logger.warn("An error occured processing a sensor reading. Topic: {} Payload: {}",
								aTopic,
								payload);
				}
			}
		}
		else
		{
			// log invalid topic
			logger.warn("Recevied an invalid topic. Topic: {} Payload: {}",
						aTopic,
						payload);
		}
	}
}
