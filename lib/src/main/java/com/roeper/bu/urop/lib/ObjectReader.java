package com.roeper.bu.urop.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class ObjectReader<T> implements Iterator<T>, Service
{
	final Logger logger = LoggerFactory.getLogger(ObjectReader.class);
	private File inputFile;
	private ObjectMapper mapper;
	private long linesReadCount = 0;
	private BufferedReader bufferedReader;
	private Optional<T> next;
	private Class<T> classVar;

	public ObjectReader(ObjectMapper aMapper, File aInputFile, Class<T> aClassVar) 
	{
		this.mapper = aMapper;
		this.inputFile = aInputFile;
		this.classVar = aClassVar;
	}
	
	public void start() throws Exception
	{
		bufferedReader = new BufferedReader(new FileReader(inputFile));
		next = readNext();
	}

	public boolean hasNext()
	{
		return next.isPresent();
	}

	public T next()
	{
		T current = next.get();
		next = readNext();
		return current;
	}

	private Optional<T> readNext()
	{
		Optional<T> optRead = Optional.absent();
		try
		{
			// get next line
			String line;
			while  ((line = bufferedReader.readLine()) != null)
			{
				// parse line if read
				T reading = this.mapper.readValue(line, classVar);
				optRead = Optional.of(reading);
				linesReadCount++;
				
				if (optRead.isPresent())
				{
					break;
				}
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

	public void stop() throws Exception
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
