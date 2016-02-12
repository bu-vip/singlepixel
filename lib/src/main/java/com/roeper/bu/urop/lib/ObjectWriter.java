package com.roeper.bu.urop.lib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectWriter<T>
{
	final Logger logger = LoggerFactory.getLogger(ObjectWriter.class);
	private ObjectMapper mapper;
	private File destination;
	private List<T> buffer = new LinkedList<T>();
	private LinkedBlockingQueue<List<T>> writeJobs = new LinkedBlockingQueue<List<T>>();
	private int bufferSize = 100;
	private AtomicBoolean done = new AtomicBoolean(false);

	public ObjectWriter(ObjectMapper aMapper, File aDestination)
	{
		this.mapper = aMapper;
		this.destination = aDestination;
	}

	public void open()
	{
		(new Thread(new WriteReadingsWorker())).start();
	}

	public void write(T aReading)
	{
		buffer.add(aReading);

		if (buffer.size() > bufferSize)
		{
			List<T> toWrite = this.buffer;
			this.buffer = new LinkedList<T>();
			writeJobs.add(toWrite);
		}
	}

	public void close()
	{
		if (!done.get())
		{
			if (this.buffer != null)
			{
				List<T> toWrite = this.buffer;
				this.buffer = null;
				writeJobs.add(toWrite);
			}
			done.set(true);
		}
	}

	private class WriteReadingsWorker implements Runnable
	{

		public void run()
		{

			while (!done.get() || !writeJobs.isEmpty())
			{
				BufferedWriter bw = null;
				try
				{
					List<T> readings = writeJobs.take();
					// APPEND MODE SET HERE
					bw = new BufferedWriter(new FileWriter(destination, true));

					for (T reading : readings)
					{
						String toWrite = mapper.writeValueAsString(reading);
						bw.write(toWrite);
						bw.newLine();
					}

					bw.flush();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
				finally
				{
					if (bw != null)
						try
						{
							bw.close();
						}
						catch (IOException ioe2)
						{
						}
				}
			}
		}
	}
}
