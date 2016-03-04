package com.roeper.bu.urop.lib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes objects to file in JSON. File contains an array of JSON objects.
 * 
 * @author doug
 *
 * @param <T>
 *            - The type of object to write.
 */
public class ObjectWriter<T> implements Service
{
	final Logger logger = LoggerFactory.getLogger(ObjectWriter.class);
	private ObjectMapper mapper;
	private File destination;
	private final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
	private final Thread worker = (new Thread(new WriteReadingsWorker()));
	private final AtomicBoolean shouldStop = new AtomicBoolean(false);

	public ObjectWriter(ObjectMapper aMapper,
						File aDestination)
	{
		this.mapper = aMapper;
		this.destination = aDestination;
	}

	public void start() throws Exception
	{
		worker.start();
	}

	public void write(T aReading)
	{
		shouldStop.set(false);
		buffer.add(aReading);
	}

	public void stop() throws Exception
	{
		shouldStop.set(true);
		this.worker.join(5000);
	}

	private class WriteReadingsWorker implements Runnable
	{

		public void run()
		{
			BufferedWriter bw = null;
			RandomAccessFile randomAccessWriter = null;
			try
			{
				// creates a new file
				bw = new BufferedWriter(new FileWriter(	destination,
														false));
				// write [ for the start of the array
				bw.write("[");
				bw.flush();

				while (!shouldStop.get() || !buffer.isEmpty())
				{
					T reading = buffer.take();
					String toWrite = mapper.writeValueAsString(reading);
					bw.write(toWrite + ",");
					bw.newLine();
					bw.flush();
				}
				// close buffered writer
				bw.close();
				bw = null;

				// create a random access writer to overwrite the end of the
				// file
				randomAccessWriter = new RandomAccessFile(	destination,
															"rw");
				// Set write pointer to the end of the file
				randomAccessWriter.seek(randomAccessWriter.length() - 2);
				// overwrite the last "," with a "]"
				randomAccessWriter.write(']');
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
				throw new RuntimeException("Error writing file");
			}
			finally
			{
				if (bw != null)
				{
					try
					{
						bw.close();
					}
					catch (IOException ioe2)
					{
					}
				}
				if (randomAccessWriter != null)
				{
					try
					{
						randomAccessWriter.close();
					}
					catch (IOException ioe2)
					{
					}
				}
			}
		}

	}
}
