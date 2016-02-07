package com.roeper.bu.urop.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ConfigReader<T>
{	
	private final Class<T> classType;
	
	public ConfigReader( Class<T> aClass)
	{
		this.classType = aClass;
	}
	
	public T read(String aFilePath) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		T config = mapper.readValue(new File(aFilePath), this.classType);
		return config;
	}
	
	public T read(InputStream aSource) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		T config = mapper.readValue(aSource, this.classType);
		return config;
	}
}
