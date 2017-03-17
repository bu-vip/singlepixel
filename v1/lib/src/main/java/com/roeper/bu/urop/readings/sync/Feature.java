package com.roeper.bu.urop.readings.sync;

public class Feature
{
	private double classId;
	private int [] indexes;
	private double [] features;
	
	public Feature(double aClassId, int[] aIndexes, double [] aFeatures)
	{
		this.classId = aClassId;
		this.indexes = aIndexes;
		this.features = aFeatures;
	}

	public double getClassId()
	{
		return classId;
	}

	public int[] getIndexes()
	{
		return indexes;
	}

	public double[] getFeatures()
	{
		return features;
	}
}
