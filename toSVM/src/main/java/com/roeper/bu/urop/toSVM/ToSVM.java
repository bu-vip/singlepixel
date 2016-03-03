package com.roeper.bu.urop.toSVM;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.roeper.bu.urop.readings.sync.LinkedReading;

public class ToSVM
{
	public void start()
	{
		/*
		BufferedWriter xClass = null;
		BufferedWriter zClass = null;
		try
		{
			xClass = new BufferedWriter(new FileWriter(	outputXFile,
													false));
			zClass = new BufferedWriter(new FileWriter(	outputZFile,
														false));

			for (LinkedReading reading : linkedReadings)
			{
				xClass.write(reading.getSVMStringX());
				xClass.newLine();
				zClass.write(reading.getSVMStringZ());
				zClass.newLine();
			}

			xClass.flush();
			zClass.flush();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		finally
		{
			if (xClass != null)
				try
				{
					xClass.close();
					zClass.close();
				}
				catch (IOException ioe2)
				{
				}
		}
		*/
	}
}
