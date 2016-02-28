package com.roeper.bu.urop.svmLive;

import java.util.List;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

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
import com.roeper.bu.urop.lib.SensorReading;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

public class SVMLive implements MqttCallback
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

			SVMLiveModuleConfig config = new SVMLiveModuleConfig(brokerConfig, args[1]);

			Injector injector = Guice.createInjector(new SVMLiveModule(config));

			// create
			final SVMLive svMlive = injector.getInstance(SVMLive.class);

			// add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					svMlive.stop();
				}
			});

			// start
			svMlive.start();

			svMlive.recordBackground();
		}
		else
		{
			System.out.println("Usage: <config-file> <model-file>");
		}
	}

	private static final int BACKGROUND_LENGTH = 1000;

	final Logger logger = LoggerFactory.getLogger(SVMLive.class);

	private final MqttClient client;
	private final String topicPrefix;
	private final String modelFileName;
	private svm_model model;
	private boolean recordingBackground = false;
	private double backgroundMean;
	private double backgroundStddev;
	private List<SensorReading> readingsBuffer = new LinkedList<SensorReading>();
	private SensorReading[] svmBuffer = new SensorReading[6];

	@Inject
	protected SVMLive(	MqttClient aClient, @Named("topicPrefix") String aTopicPrefix,
						@Named("modelFile") String aModelFile)
	{
		this.client = aClient;
		this.topicPrefix = aTopicPrefix;
		this.modelFileName = aModelFile;
	}

	public void start()
	{
		try
		{
			model = svm.svm_load_model(modelFileName);
		}
		catch (IOException e)
		{

		}

		try
		{
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			client.connect(connOpts);
			logger.info("Successfully connected to broker.");
			client.setCallback(this);

			// subscribe to receive sensor readings
			String subscribeDest = this.topicPrefix + "/#";
			logger.info("Subscribing to: {}", subscribeDest);
			client.subscribe(subscribeDest);
		}
		catch (MqttException me)
		{
			logger.error("An error occured connecting to the broker");
			me.printStackTrace();
			this.stop();
		}
	}

	public void recordBackground()
	{
		logger.info("Recording background...");
		readingsBuffer.clear();
		backgroundMean = 0;
		backgroundStddev = 0;
		recordingBackground = true;
	}

	public void stop()
	{
		logger.info("Stopping...");

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
		arg0.printStackTrace();
		logger.info("Lost connection.");
		this.stop();
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
					processReading(reading);
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

	private void processReading(SensorReading aReading)
	{
		if (recordingBackground)
		{
			readingsBuffer.add(aReading);
			if (readingsBuffer.size() > BACKGROUND_LENGTH)
			{
				logger.info("Finished recording background");
				calculateBackground();
				readingsBuffer.clear();
				recordingBackground = false;
			}
		}
		else if (!recordingBackground)
		{
			svmBuffer[Integer.parseInt(aReading.getSensorId())] = aReading;

			boolean updated = true;
			for (int i = 0; i < svmBuffer.length; i++)
			{
				if (svmBuffer[i] == null)
				{
					updated = false;
					break;
				}
			}

			if (updated)
			{
				double res = predict(svmBuffer);
				System.out.println("Predicted: " + res);

				for (int i = 0; i < svmBuffer.length; i++)
				{
					svmBuffer[i] = null;
				}
			}
		}
	}

	private void calculateBackground()
	{
		backgroundMean = 0;
		backgroundStddev = 0;
		if (readingsBuffer.size() == 0)
			return;

		// calculate mean and stddev
		double[] data = new double[readingsBuffer.size()];
		int index = 0;
		for (SensorReading measure : readingsBuffer)
		{
			data[index] = measure.getLuminance();
			index++;
		}

		double sum = 0.0;
		for (double a : data)
			sum += a;
		backgroundMean = sum / data.length;
		logger.info("Background mean: {}", backgroundMean);

		double temp = 0;
		for (double a : data)
			temp += (backgroundMean - a) * (backgroundMean - a);
		backgroundStddev = Math.sqrt(temp / data.length);
		logger.info("Background stddev: {}", backgroundStddev);
	}

	private double predict(SensorReading[] aReadings)
	{
		svm_node[] data = new svm_node[6];
		for (int i = 0; i < data.length; i++)
		{
			data[i] = new svm_node();
			data[i].index = i;
			data[i].value = (aReadings[i].getLuminance() - backgroundMean) / backgroundStddev;
		}

		return svm.svm_predict(this.model, data);
	}
}
