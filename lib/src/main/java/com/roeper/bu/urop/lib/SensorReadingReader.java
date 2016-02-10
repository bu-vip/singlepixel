package com.roeper.bu.urop.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class SensorReadingReader implements Iterator<SensorReading>
{
	public interface Factory
	{
		SensorReadingReader create(File aInputFile);
	}

	final Logger logger = LoggerFactory.getLogger(SensorReadingReader.class);
	private File inputFile;
	private ObjectMapper mapper;
	private long linesReadCount = 0;
	private BufferedReader bufferedReader;
	private Optional<SensorReading> next;

	@Inject
	protected SensorReadingReader(ObjectMapper aMapper, @Assisted File aInputFile)
	{
		this.mapper = aMapper;
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

	public SensorReading next()
	{
		SensorReading current = next.get();
		next = readNext();
		return current;
	}

	private Optional<SensorReading> readNext()
	{
		Optional<SensorReading> optRead = Optional.absent();
		try
		{
			// get next line
			String line = bufferedReader.readLine();
			if (line != null)
			{
				// parse line if read
				SensorReading reading = this.mapper.readValue(line, SensorReading.class);
				optRead = Optional.of(reading);
				linesReadCount++;
			}
		}
		catch (JsonParseException ea)
		{
			ea.printStackTrace();
			throw new RuntimeException("Invalid reading at line " + linesReadCount);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Error reading file");
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
