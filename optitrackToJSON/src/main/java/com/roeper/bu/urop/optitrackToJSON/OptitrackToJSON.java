package com.roeper.bu.urop.optitrackToJSON;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.lib.OptitrackCSVParser;
import com.roeper.bu.urop.lib.OptitrackReading;

public class OptitrackToJSON
{
	public static void main(String args[]) throws Exception
	{
		if (args.length == 2)
		{
			ObjectMapper mapper = new ObjectMapper();
			OptitrackCSVParser parser = new OptitrackCSVParser(new File(args[0]));
			ObjectWriter<OptitrackReading> writer = new ObjectWriter<OptitrackReading>(	mapper,
																						new File(args[1]));
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
			this.optiDataParser.open();
			this.writer.open();

			logger.info("Converting file...");
			while (optiDataParser.hasNext())
			{
				OptitrackReading next = optiDataParser.next();
				writer.write(next);
			}
			this.optiDataParser.close();
			this.writer.close();

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

	public void stop()
	{
		this.optiDataParser.close();
		this.writer.close();
	}
}
