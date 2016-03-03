package com.roeper.bu.urop.readings.sync;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roeper.bu.urop.readings.optitrack.OptitrackReading;
import com.roeper.bu.urop.readings.sensor.SensorReading;

public class LinkedReading
{
	private OptitrackReading optitrackReading;
	private List<SensorReading> sensorReadings;
	
	protected LinkedReading()
	{
		
	}
	
	public LinkedReading(OptitrackReading aOptitrackReading, List<SensorReading> aSensorReadings)
	{
		this.optitrackReading = aOptitrackReading;
		this.sensorReadings = aSensorReadings;
	}

	public OptitrackReading getOptitrackReading()
	{
		return optitrackReading;
	}

	public List<SensorReading> getSensorReadings()
	{
		return sensorReadings;
	}
	
	@JsonIgnore
	public String getSVMString()
	{
		String svm = "";
		svm += optitrackReading.getX() + "," + optitrackReading.getY() + "," + optitrackReading.getZ();
		int index = 1;
		for (SensorReading reading : sensorReadings)
		{
			svm += index + ":" + reading.getWhite() + " ";
			index++;
		}
		return svm;
	}
	
	@JsonIgnore
	public String getSVMStringX()
	{
		String svm = "";
		svm += optitrackReading.getX() + " ";
		int index = 1;
		for (SensorReading reading : sensorReadings)
		{
			svm += index + ":" + reading.getWhite() + " ";
			index++;
		}
		return svm;
	}
	
	@JsonIgnore
	public String getSVMStringZ()
	{
		String svm = "";
		svm += optitrackReading.getZ() + " ";
		int index = 1;
		for (SensorReading reading : sensorReadings)
		{
			svm += index + ":" + reading.getWhite() + " ";
			index++;
		}
		return svm;
	}
}
