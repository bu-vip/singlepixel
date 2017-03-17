package com.roeper.bu.urop.recorder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.roeper.bu.urop.lib.BrokerConfig;
import com.roeper.bu.urop.lib.ConfigReader;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.readings.ReadingProvider;
import com.roeper.bu.urop.readings.sensor.SensorReading;

public class Recorder
{
	public static void main(String args[]) throws Exception
	{
		if (args.length == 3)
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
																	args[1], args[2]);

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
			
			//wait for user to stop
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        System.out.print("Press enter to stop");
	        br.readLine();
	        recorder.stop();
		}
		else
		{
			System.out.println("Usage: <config-file> <output-dir> <format (JSON|CSV)>");
		}
	}

	final Logger logger = LoggerFactory.getLogger(Recorder.class);

	// Writer used to write the readings to a file
	private final ObjectWriter<SensorReading> writer;
	// Provider of sensor readings
	private final ReadingProvider<SensorReading> provider;
	// Used to notify the worker thread when it should stop
	private final AtomicBoolean shouldStop = new AtomicBoolean(false);
	private final Thread worker = (new Thread(new Worker()));

	@Inject
	protected Recorder(	ObjectWriter<SensorReading> aWriter,
						ReadingProvider<SensorReading> aReadingProvider)
	{
		this.writer = aWriter;
		this.provider = aReadingProvider;
	}

	public void start()
	{
		try
		{
			logger.info("Starting...");
			writer.start();
			provider.start();
			shouldStop.set(false);
			worker.start();
			
			logger.info("Recording...");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			this.stop();
		}
	}

	private class Worker implements Runnable
	{
		public void run()
		{
			while (!shouldStop.get())
			{
				// check for new readings
				Optional<SensorReading> reading = provider.getReading();
				if (reading.isPresent())
				{
					writer.write(reading.get());
				}
				else
				{
					// if no readings available, wait for a little bit
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException expected)
					{

					}
				}
			}
		}
	}

	public void stop()
	{
		logger.info("Stopping...");

		try
		{
			shouldStop.set(true);
			worker.join(5000);
			logger.info("Stopping MQTT...");
			provider.stop();
			logger.info("Closing file...");
			writer.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
