package com.roeper.bu.urop.lib;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SensorReading
{
	private String groupId;
	private String sensorId;
	private double red;
	private double green;
	private double blue;
	private double white;
	private int time1;
	private int time2;
	private Date received;

	protected SensorReading()
	{

	}

	public SensorReading(	String aGroupId,
							String aSensorId,
							double aRed,
							double aGreen,
							double aBlue,
							double aWhite,
							int aTime1,
							int aTime2,
							Date aReceived)
	{
		this.groupId = aGroupId;
		this.sensorId = aSensorId;
		this.red = aRed;
		this.green = aGreen;
		this.blue = aBlue;
		this.white = aWhite;
		this.time1 = aTime1;
		this.time2 = aTime2;
		this.received = aReceived;
	}

	public String getGroupId()
	{
		return groupId;
	}

	public String getSensorId()
	{
		return sensorId;
	}

	public double getRed()
	{
		return red;
	}

	public double getGreen()
	{
		return green;
	}

	public double getBlue()
	{
		return blue;
	}

	public double getWhite()
	{
		return white;
	}

	public int getTime1()
	{
		return time1;
	}

	public int getTime2()
	{
		return time2;
	}
	
	public Date getReceived()
	{
		return received;
	}

	@JsonIgnore
	public String getPayload()
	{
		return red + ", " + green + ", " + blue + ", " + white + ", " + time1 + ", " + time2;
	}
	
	@JsonIgnore
	public double getLuminance()
	{
		return 0.2989 * red + 0.587 * green + 0.114 * blue;
	}
}
