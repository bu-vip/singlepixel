package com.roeper.bu.urop.readings;

import com.google.common.base.Optional;
import com.roeper.bu.urop.lib.ObjectReader;

public class FileReadingProvider<T> implements ReadingProvider<T>
{	
	private final ObjectReader<T> reader;
	
	public FileReadingProvider(ObjectReader<T> aReader)
	{
		this.reader = aReader;
	}
	
	public void start() throws Exception
	{
		reader.start();
	}

	public void stop() throws Exception
	{
		reader.stop();
	}

	public Optional<T> getReading()
	{
		Optional<T> optional = Optional.absent();
		if (reader.hasNext())
		{
			optional = Optional.of(reader.next());
		}
		
		return optional;
	}
}
