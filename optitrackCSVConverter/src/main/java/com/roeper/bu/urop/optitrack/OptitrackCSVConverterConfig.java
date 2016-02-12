package com.roeper.bu.urop.optitrack;

public class OptitrackCSVConverterConfig
{
	private String inputFile;
	private String outputFile;
	
	protected OptitrackCSVConverterConfig()
	{
		
	}
	
	public OptitrackCSVConverterConfig(String aInputFile, String aOutputFile)
	{
		this.inputFile = aInputFile;
		this.outputFile = aOutputFile;
	}
	
	public String getInputFile()
	{
		return inputFile;
	}
	public String getOutputFile()
	{
		return outputFile;
	}
	
	
}
