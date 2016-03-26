package com.roeper.bu.urop.readings.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.roeper.bu.urop.readings.optitrack.OptitrackCSVParser;

public class FileFeatureProvider
{
	final Logger logger = LoggerFactory.getLogger(OptitrackCSVParser.class);
	private File inputFile;
	private long linesReadCount = 0;
	private BufferedReader bufferedReader;

	public FileFeatureProvider(File aInputFile)
	{
		this.inputFile = aInputFile;
	}

	public void start() throws Exception
	{
		bufferedReader = new BufferedReader(new FileReader(inputFile));
	}

	public Optional<Feature> getFeature()
	{
		Optional<Feature> optRead = Optional.absent();
		try
		{
			// get next line
			String line = bufferedReader.readLine();
			if (line != null)
			{
				String[] values = line.split(" ");
				if (values.length > 1)
				{
					double classId = Double.parseDouble(values[0]);
					
					int indexes[] = new int[values.length - 1];
					double features[] = new double[values.length - 1];
					for (int i = 1; i < values.length; i++)
					{
						String[] vals = values[i].split(":");
						indexes[i - 1] = Integer.parseInt(vals[0]);
						features[i - 1] = Double.parseDouble(vals[1]);
					}
					
					optRead = Optional.of(new Feature(classId, indexes, features));
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
