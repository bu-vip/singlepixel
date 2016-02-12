package com.roeper.bu.urop.lib;

public class OptitrackReading
{
	private int frameIndex;
	private double timestamp;
	private int bodyId;
	private float x;
	private float y;
	private float z;
	
	protected OptitrackReading()
	{
		
	}
	
	public OptitrackReading(int aFrameIndex, double aTimestamp, int aBodyId, float aX, float aY, float aZ)
	{
		this.frameIndex = aFrameIndex;
		this.timestamp = aTimestamp;
		this.bodyId = aBodyId;
		this.x = aX;
		this.y = aY;
		this.z = aZ;
	}

	public int getFrameIndex()
	{
		return frameIndex;
	}

	public double getTimestamp()
	{
		return timestamp;
	}

	public int getBodyId()
	{
		return bodyId;
	}

	public float getX()
	{
		return x;
	}

	public float getY()
	{
		return y;
	}
	
	public float getZ()
	{
		return z;
	}

}
