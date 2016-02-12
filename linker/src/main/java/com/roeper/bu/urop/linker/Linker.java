package com.roeper.bu.urop.linker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roeper.bu.urop.lib.LinkedReading;
import com.roeper.bu.urop.lib.ObjectReader;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.lib.OptitrackCSVParser;
import com.roeper.bu.urop.lib.OptitrackReading;
import com.roeper.bu.urop.lib.SensorReading;

public class Linker
{
	public static void main(String args[]) throws Exception
	{
		if (args.length == 7)
		{
			ObjectMapper mapper = new ObjectMapper();
			ObjectReader<SensorReading> reader = new ObjectReader<SensorReading>(	mapper,
																					new File(args[0]),
																					SensorReading.class);
			long sensorMin = Long.parseLong(args[1].trim());
			long sensorMax = Long.parseLong(args[2].trim());
			OptitrackCSVParser parser = new OptitrackCSVParser(new File(args[3]));
			long optiMin = Long.parseLong(args[4].trim());
			long optiMax = Long.parseLong(args[5].trim());
			ObjectWriter<LinkedReading> writer = new ObjectWriter<LinkedReading>(mapper, new File(args[6]));
			final Linker linker = new Linker(	reader,
													sensorMin,
													sensorMax,
													parser,
													optiMin,
													optiMax,
													writer);

			// add shutdown hook
			Runtime	.getRuntime()
					.addShutdownHook(new Thread()
					{
						@Override
						public void run()
						{
							linker.stop();
						}
					});

			linker.start();
		}
		else
		{
			System.out.print("Usage: <sensor-file> <sensor-time-min> <sensor-time-max>");
			System.out.println(" <opti-file> <opti-frame-min> <opti-frame-max> <output-file>");
		}
	}

	final Logger logger = LoggerFactory.getLogger(Linker.class);
	private final ObjectReader<SensorReading> sensorDataReader;
	private final long sensorMinTime;
	private final long sensorMaxTime;
	private final OptitrackCSVParser optiDataParser;
	private final long optiMinTime;
	private final long optiMaxTime;
	private final ObjectWriter<LinkedReading> linkedWriter;

	public Linker(	ObjectReader<SensorReading> aSensorReader,
						long aSensorMin,
						long aSensorMax,
						OptitrackCSVParser aOptiParser,
						long aOptiMin,
						long aOptiMax,
						ObjectWriter<LinkedReading> aLinkedWriter)
	{
		this.sensorDataReader = aSensorReader;
		this.sensorMinTime = aSensorMin;
		this.sensorMaxTime = (aSensorMax == -1 ? Long.MAX_VALUE : aSensorMax);
		this.optiDataParser = aOptiParser;
		this.optiMinTime = aOptiMin;
		this.optiMaxTime = (aOptiMax == -1 ? Long.MAX_VALUE : aOptiMax);
		this.linkedWriter = aLinkedWriter;
	}

	public void start()
	{
		try
		{
			// load & trim sensor data from file
			logger.info("Loading and trimming sensor streams...");
			Hashtable<String, List<SensorReading>> trimmedSensorData = getSensorStreams();

			// load & trim optitrack data from csv
			logger.info("Loading and trimming optitrack stream...");
			List<OptitrackReading> trimmedOptiData = getOptiStream();

			// link readings
			logger.info("Linking streams...");
			List<LinkedReading> linkedReadings = linkReadings(	trimmedSensorData,
																trimmedOptiData);

			// write the new data to a file
			logger.info("Saving linked stream...");
			this.linkedWriter.open();
			for (LinkedReading reading : linkedReadings)
			{
				this.linkedWriter.write(reading);
			}
			this.linkedWriter.close();
			
			logger.info("Done.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			this.stop();
		}
	}

	private Hashtable<String, List<SensorReading>> getSensorStreams() throws IOException
	{
		sensorDataReader.open();
		Hashtable<String, List<SensorReading>> trimmedSensorData = new Hashtable<String, List<SensorReading>>();
		while (sensorDataReader.hasNext())
		{
			SensorReading next = sensorDataReader.next();
			long timestamp = next.getReceived().getTime();
			if (timestamp > this.sensorMinTime && timestamp < this.sensorMaxTime)
			{
				String key = next.getGroupId() + "-" + next.getSensorId();
				if (!trimmedSensorData.containsKey(key))
				{
					trimmedSensorData.put(	key,
											new ArrayList<SensorReading>());
				}
				trimmedSensorData	.get(key)
									.add(next);
			}
		}
		// close
		sensorDataReader.close();
		return trimmedSensorData;
	}

	private List<OptitrackReading> getOptiStream() throws IOException
	{
		optiDataParser.open();
		List<OptitrackReading> trimmedOptiData = new ArrayList<OptitrackReading>();
		while (optiDataParser.hasNext())
		{
			OptitrackReading next = optiDataParser.next();
			long timestamp = next.getFrameIndex();
			if (timestamp > this.optiMinTime && timestamp < this.optiMaxTime)
			{
				trimmedOptiData.add(next);
			}
		}
		this.optiDataParser.close();
		return trimmedOptiData;
	}

	private List<LinkedReading> linkReadings(	Hashtable<String, List<SensorReading>> aSensorStreams,
												List<OptitrackReading> aOptiReadings)
	{
		// determine step size of re-sampling to make signals same length
		Hashtable<String, Float> stepSizes = new Hashtable<String, Float>();
		Hashtable<String, Float> locations = new Hashtable<String, Float>();
		for (String key : aSensorStreams.keySet())
		{
			float stepSize = (aSensorStreams.get(key).size() - 1) / (float) aOptiReadings.size();
			stepSizes.put(	key,
							stepSize);
			locations.put(	key,
							(float) 0);
		}

		// re-sample signals and created the linked readings
		List<LinkedReading> linkedReadings = new LinkedList<LinkedReading>();
		for (int i = 0; i < aOptiReadings.size(); i++)
		{
			OptitrackReading optitrackReading = aOptiReadings.get(i);
			List<SensorReading> readings = new LinkedList<SensorReading>();
			for (String key : stepSizes.keySet())
			{
				float stepSize = stepSizes.get(key);
				float location = locations.get(key);
				SensorReading reading = aSensorStreams	.get(key)
														.get((int) location);
				readings.add(reading);
				locations.put(	key,
								location + stepSize);
			}
			linkedReadings.add(new LinkedReading(	optitrackReading,
													readings));
		}

		return linkedReadings;
	}

	public void stop()
	{
		this.sensorDataReader.close();
		this.optiDataParser.close();
		this.linkedWriter.close();
	}
}
