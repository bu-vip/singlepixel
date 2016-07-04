from csv_reader import readCSV_white, graph_plots
from scipy import interpolate
import numpy as np

numOfCameras = 12

stand_results = readCSV_white("results-stand.txt", numOfCameras)
spin_results = readCSV_white("results-spin.txt", numOfCameras)
write_results = readCSV_white("results-write.txt", numOfCameras)

no_results = readCSV_white("results-nothing.txt", numOfCameras)

identifyTest = readCSV_white("test-result.txt", numOfCameras)
identification_dict = {
    'standing': stand_results,
    'spinning': spin_results,
    'writing': write_results,
    'nothing': no_results
    }

def identify_gesture(test, trainingData=identification_dict):
    if isinstance(test, str):
        test_read = readCSV_white(test, numOfCameras)
        test_result = interpolate_result(test_read)
    else:
        test_result = interpolate_result(test)
    interp_data = dict() # store interpreted data in a dictionary
    data_sum = dict()
    for name, dataset in trainingData.items(): # dataset
        interp_data[name] = interpolate_result(dataset)
        data_sum[name] = 0.0
    
    for name, func in interp_data.items():
        for n in np.linspace(0., 1., 200):
            func_values = np.array(func(n))
            test_values = np.array(test_result(n))
            diff = (func_values - test_values)
            diff_sum = np.sum(np.square(diff))
            data_sum[name] += diff_sum
    print(data_sum) # test function to show the differences
    return min(data_sum, key=data_sum.get)
                
def listAverage(inputList):
    averages = []
    for i in range(len(inputList)):
        averages.append(np.mean(inputList[i]))
    return averages
    
def interpolate_result(result_list): # Returns interpolation function normalized from 0 to 1
    x_axis = np.linspace(0., 1., len(result_list[0]))
    result_function = interpolate.interp1d(x_axis, result_list)
    return result_function
    
