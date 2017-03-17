package com.roeper.bu.urop.lib;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class ObjectReader<T> implements Iterator<T>, Service
{
	final Logger logger = LoggerFactory.getLogger(ObjectReader.class);
	private File inputFile;
	private Optional<T> next;
	private Class<T> classVar;
	private ObjectMapper mapper;
	private JsonParser reader;

	public ObjectReader(ObjectMapper aMapper, File aInputFile, Class<T> aClassVar) 
	{
		this.mapper = aMapper;
		this.inputFile = aInputFile;
		this.classVar = aClassVar;
	}
	
	public void start() throws Exception
	{
		JsonFactory f = new JsonFactory();
		this.reader = f.createParser(this.inputFile);
		// advance stream to START_ARRAY first:
		this.reader.nextToken();

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
			// and then each time, advance to opening START_OBJECT
			if (this.reader.nextToken() == JsonToken.START_OBJECT) {
				T next = mapper.readValue(this.reader, classVar);
			   // process
			   // after binding, stream points to closing END_OBJECT
				optRead = Optional.of(next);
			}
		}
		catch (JsonParseException ea)
		{
			ea.printStackTrace();
			throw new RuntimeException("Invalid reading");
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
		this.reader.close();
	}
}
