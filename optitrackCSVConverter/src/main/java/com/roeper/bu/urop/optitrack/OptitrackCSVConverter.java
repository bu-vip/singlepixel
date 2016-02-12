package com.roeper.bu.urop.optitrack;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.lib.OptitrackCSVParser;
import com.roeper.bu.urop.lib.OptitrackReading;

public class OptitrackCSVConverter
{
	public static void main(String args[]) throws Exception
	{
		if (args.length == 2)
		{
			OptitrackCSVConverterConfig config = new OptitrackCSVConverterConfig(	args[0],
																					args[1]);

			Injector injector = Guice.createInjector(new OptitrackCSVConverterModule(config));

			// create recorder
			final OptitrackCSVConverter converter = injector.getInstance(OptitrackCSVConverter.class);

			// add shutdown hook
			Runtime	.getRuntime()
					.addShutdownHook(new Thread()
					{
						@Override
						public void run()
						{
							converter.stop();
						}
					});

			// start recorder
			converter.convert();
		}
		else
		{
			System.out.println("Usage: <input-file> <output-file>");
		}
	}

	final Logger logger = LoggerFactory.getLogger(OptitrackCSVConverter.class);
	private final OptitrackCSVParser parser;
	private final ObjectWriter<OptitrackReading> writer;

	@Inject
	protected OptitrackCSVConverter(OptitrackCSVParser aParser,
									ObjectWriter<OptitrackReading> aWriter)
	{
		this.parser = aParser;
		this.writer = aWriter;
	}

	public void convert()
	{
		try
		{
			this.writer.open();
			this.parser.open();

			logger.info("Converting...");
			while (this.parser.hasNext())
			{
				OptitrackReading next = this.parser.next();
				this.writer.write(next);
			}
			logger.info("Done");
		}
		catch (FileNotFoundException ea)
		{
			logger.error("The input file was not found");
			ea.printStackTrace();
		}
		finally
		{
			this.stop();
		}
	}

	public void stop()
	{
		this.writer.close();
		this.parser.close();
	}
}
