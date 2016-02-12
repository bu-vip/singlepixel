package com.roeper.bu.urop.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OptitrackCSVParser
{
	final Logger logger = LoggerFactory.getLogger(OptitrackCSVParser.class);
	private File inputFile;
	private long linesReadCount = 0;
	private BufferedReader bufferedReader;
	private Optional<OptitrackReading> next;

	public OptitrackCSVParser(File aInputFile)
	{
		this.inputFile = aInputFile;
	}

	public void open() throws FileNotFoundException
	{
		bufferedReader = new BufferedReader(new FileReader(inputFile));
		next = readNext();
	}

	public boolean hasNext()
	{
		return next.isPresent();
	}

	public OptitrackReading next()
	{
		OptitrackReading current = next.get();
		next = readNext();
		return current;
	}

	private Optional<OptitrackReading> readNext()
	{
		Optional<OptitrackReading> optRead = Optional.absent();
		try
		{
			// get next line
			String line = bufferedReader.readLine();
			if (line != null)
			{
				String[] values = line.split(",");
				if (values.length > 0)
				{
					int frameIndex = Integer.parseInt(values[1].trim());
					double timestamp = Double.parseDouble(values[2].trim());
					int id = Integer.parseInt(values[4].trim());
					int markerCount = Integer.parseInt(values[6].trim());

					float xSum = 0;
					float ySum = 0;
					float zSum = 0;
					for (int i = 0; i < markerCount; i++)
					{
						xSum += Float.parseFloat(values[7 + (i * 4) + 0].trim());
						ySum += Float.parseFloat(values[7 + (i * 4) + 1].trim());
						zSum += Float.parseFloat(values[7 + (i * 4) + 2].trim());
					}

					// parse line if read
					OptitrackReading reading = new OptitrackReading(frameIndex,
																	timestamp,
																	id,
																	xSum / markerCount,
																	ySum / markerCount,
																	zSum / markerCount);
					optRead = Optional.of(reading);
				}
				linesReadCount++;

			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Error reading file on line: " + linesReadCount);
		}

		return optRead;
	}

	public void remove()
	{
		// do nothing
	}

	public void close()
	{
		if (bufferedReader != null)
		{
			try
			{
				// Always close files.
				bufferedReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
