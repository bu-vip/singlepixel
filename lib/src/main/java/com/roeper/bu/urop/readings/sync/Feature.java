package com.roeper.bu.urop.readings.sync;

public class Feature
{
	private int classId;
	private int [] indexes;
	private double [] features;
	
	public Feature(int aClassId, int[] aIndexes, double [] aFeatures)
	{
		this.classId = aClassId;
		this.indexes = aIndexes;
		this.features = aFeatures;
	}

	public int getClassId()
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
