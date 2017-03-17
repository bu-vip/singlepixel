package com.roeper.bu.urop.lib;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Writes objects to file in JSON. File contains an array of JSON objects.
 * 
 * @author doug
 *
 * @param <T>
 *            - The type of object to write.
 */
public class ObjectWriter<T> implements Service
{
	public enum Format
	{
		JSON, CSV
	}

	final Logger logger = LoggerFactory.getLogger(ObjectWriter.class);
	private File destination;
	private Format format;
	private JsonGenerator writer;
	private Class<T> classVar;

	public ObjectWriter(File aDestination,
						Format aFormat, Class<T> aClassVar)
	{
		this.destination = aDestination;
		this.format = aFormat;
		this.classVar = aClassVar;
	}

	public void start() throws Exception
	{
		if (this.format == Format.JSON)
		{
			JsonFactory f = new JsonFactory();
			this.writer = f.createGenerator(this.destination,
											JsonEncoding.UTF8);
			this.writer.setCodec(new ObjectMapper());
			this.writer.writeStartArray();
		}
		else if (this.format == Format.CSV)
		{
			CsvFactory factory = new CsvFactory();
			CsvGenerator generator = factory.createGenerator(	this.destination,
																JsonEncoding.UTF8);
			CsvMapper mapper = new CsvMapper();
			CsvSchema schema = (mapper.schemaFor(this.classVar)).withHeader();
			generator.setSchema(schema);
			generator.setCodec(mapper);
			this.writer = generator;
		}
		else
		{
			throw new RuntimeException("Unknown format");
		}
	}

	public void write(T aReading)
	{
		try
		{
			this.writer.writeObject(aReading);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void stop() throws Exception
	{
		if (this.format == Format.JSON)
		{
			try
			{
				this.writer.writeEndArray();
			}
			catch (JsonGenerationException expected)
			{
				//TODO figure out why this happens
			}
		}
		else if (this.format == Format.CSV)
		{

		}
		else
		{
			throw new RuntimeException("Unknown format type");
		}
		this.writer.close();
	}
}
