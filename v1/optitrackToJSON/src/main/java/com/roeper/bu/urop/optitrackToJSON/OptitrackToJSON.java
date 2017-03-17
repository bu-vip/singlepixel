package com.roeper.bu.urop.optitrackToJSON;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.lib.ObjectWriter.Format;
import com.roeper.bu.urop.readings.optitrack.OptitrackCSVParser;
import com.roeper.bu.urop.readings.optitrack.OptitrackReading;

public class OptitrackToJSON
{
	public static void main(String args[]) throws Exception
	{
		if (args.length == 2)
		{
			OptitrackCSVParser parser = new OptitrackCSVParser(new File(args[0]));
			ObjectWriter<OptitrackReading> writer = new ObjectWriter<OptitrackReading>(new File(args[1]), Format.JSON, OptitrackReading.class);
			final OptitrackToJSON converter = new OptitrackToJSON(parser, writer);

			// add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					converter.stop();
				}
			});

			converter.start();
		}
		else
		{
			System.out.println("Usage: <opti-file> <out-file>");
			System.out.print("This program requires the optitrack file be trimmed to only rigidbody");
			System.out.println(" frame data. You can trim a file with: cat FILE.csv | egrep -e \"rigidbody,[0-9]\" > trimmed.csv");
		}
	}

	final Logger logger = LoggerFactory.getLogger(OptitrackToJSON.class);
	private final OptitrackCSVParser optiDataParser;
	private final ObjectWriter<OptitrackReading> writer;

	public OptitrackToJSON(	OptitrackCSVParser aOptiParser,
							ObjectWriter<OptitrackReading> aLinkedWriter)
	{
		this.optiDataParser = aOptiParser;
		this.writer = aLinkedWriter;
	}

	public void start()
	{
		try
		{
			logger.info("Opening file...");
			this.optiDataParser.start();
			this.writer.start();

			logger.info("Converting file...");
			Optional<OptitrackReading> optRead = Optional.absent();
			while ((optRead = optiDataParser.getReading()).isPresent())
			{
				writer.write(optRead.get());
			}
			this.optiDataParser.stop();
			this.writer.stop();

			logger.info("Done.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			this.stop();
		}
	}

	public void stop()
	{
		try
		{
			this.optiDataParser.stop();
			this.writer.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
