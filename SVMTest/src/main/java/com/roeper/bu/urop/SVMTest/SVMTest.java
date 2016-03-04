package com.roeper.bu.urop.SVMTest;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.roeper.bu.urop.lib.Service;
import com.roeper.bu.urop.readings.sync.Feature;
import com.roeper.bu.urop.readings.sync.FileFeatureProvider;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

public class SVMTest implements Service
{
	public static void main(String args[]) throws Exception
	{
		String filename = "/home/doug/Desktop/track2_data/sub1.txt.model";
		FileFeatureProvider provider = new FileFeatureProvider(new File("/home/doug/Desktop/track2_data/sub2.txt.scale"));
		SVMTest test = new SVMTest(filename, provider);
		test.start();
		test.stop();
	}
	
	final Logger logger = LoggerFactory.getLogger(SVMTest.class);
	
	private svm_model model;
	private String modelFileName;
	private FileFeatureProvider provider;
	
	public SVMTest(String aFileName, FileFeatureProvider aProvider)
	{
		this.modelFileName = aFileName;
		this.provider = aProvider;
	}

	public void start() throws Exception
	{
		try
		{
			model = svm.svm_load_model(modelFileName);
			
			provider.start();
			
			int correct = 0;
			int total = 0;
			Optional<Feature> optFeature = Optional.absent();
			Hashtable<String, Integer> confusionStats = new Hashtable<String, Integer>();
			while ((optFeature = provider.getFeature()).isPresent())
			{
				Feature feature = optFeature.get();
				svm_node[] nodes = new svm_node[feature.getFeatures().length];
				for (int i = 0; i < nodes.length; i++)
				{
					nodes[i] = new svm_node();
					nodes[i].index = feature.getIndexes()[i];
					nodes[i].value = feature.getFeatures()[i];
				}
				
				double predicted = svm.svm_predict(this.model, nodes);
				
				if (feature.getClassId() == predicted)
				{
					correct++;
				}
				total++;
				
				
				String key = feature.getClassId() + "-" + (int)predicted;
				if (!confusionStats.containsKey(key))
				{
					confusionStats.put(key, 0);
				}
				confusionStats.put(key, confusionStats.get(key) + 1);
			}
			
			logger.info("Correct percent: {}", ((float)correct / total));
			
			logger.info("Confusion:");
			for (String key : confusionStats.keySet())
			{
				logger.info("{},{}", key, confusionStats.get(key));
			}
			
		}
		catch (IOException e)
		{

		}
	}

	public void stop() throws Exception
	{
		provider.stop();
	}
	
}
