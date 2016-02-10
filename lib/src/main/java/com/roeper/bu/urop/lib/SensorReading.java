package com.roeper.bu.urop.lib;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SensorReading
{
	private String groupId;
	private String sensorId;
	private int red;
	private int green;
	private int blue;
	private int white;
	private int time1;
	private int time2;
	private Date received;

	protected SensorReading()
	{

	}

	public SensorReading(	String aGroupId,
							String aSensorId,
							int aRed,
							int aGreen,
							int aBlue,
							int aWhite,
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

	public int getRed()
	{
		return red;
	}

	public int getGreen()
	{
		return green;
	}

	public int getBlue()
	{
		return blue;
	}

	public int getWhite()
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
}
