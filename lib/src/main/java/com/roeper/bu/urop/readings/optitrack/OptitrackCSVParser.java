package com.roeper.bu.urop.readings.optitrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.roeper.bu.urop.readings.ReadingProvider;

public class OptitrackCSVParser implements ReadingProvider<OptitrackReading>
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

	public void start() throws Exception
	{
		bufferedReader = new BufferedReader(new FileReader(inputFile));
	}

	public boolean hasNext()
	{
		return next.isPresent();
	}

	public Optional<OptitrackReading> getReading()
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

	public void stop() throws Exception
	{
		if (bufferedReader != null)
		{
				// Always close files.
				bufferedReader.close();
		}
	}
}
