package com.roeper.bu.urop.optitrack;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.roeper.bu.urop.lib.ObjectWriter;
import com.roeper.bu.urop.lib.OptitrackCSVParser;
import com.roeper.bu.urop.lib.OptitrackReading;

public class OptitrackCSVConverterModule extends AbstractModule
{
	private OptitrackCSVConverterConfig config;
	
	public OptitrackCSVConverterModule(OptitrackCSVConverterConfig aConfig)
	{
		this.config = aConfig;
	}
	
	@Override
	protected void configure()
	{

	}
	
	@Provides
	public OptitrackCSVParser getParser()
	{
		return new OptitrackCSVParser(new File(config.getInputFile()));
	}
	
	@Provides
	public ObjectWriter<OptitrackReading> getWriter(ObjectMapper aMapper)
	{
		// create the reading writer
		return new ObjectWriter<OptitrackReading>(aMapper, new File(config.getOutputFile()));
	}

}
