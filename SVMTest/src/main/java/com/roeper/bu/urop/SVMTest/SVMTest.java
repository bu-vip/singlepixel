package com.roeper.bu.urop.SVMTest;

import java.io.File;
import java.io.IOException;

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
		String filename = "/home/doug/Desktop/deriv.model";
		FileFeatureProvider provider = new FileFeatureProvider(new File("/home/doug/Desktop/deriv_test.scale"));
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
			int[][] confusionStats = new int[model.label.length][model.label.length];
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
				
				confusionStats[feature.getClassId()][(int)predicted]++;
			}
			
			logger.info("Correct percent: {}", ((float)correct / total));
			
			logger.info("Confusion:");
			for (int i = 0; i < confusionStats.length; i++)
			{
				String forClass = "";
				for (int j = 0; j < confusionStats.length; j++)
				{
					forClass += confusionStats[i][j] + ",";
				}
				System.out.println("[" + forClass + "],");
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
