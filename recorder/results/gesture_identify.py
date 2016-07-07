from csv_reader import readCSV_white, readCSV_convert, graph_plots
from scipy import interpolate
import numpy as np

numOfCameras = 12
thrsh = 1e-10

stand_results = readCSV_convert("results-stand0.txt", numOfCameras)
stand_rgb = readCSV_convert("results-stand0.txt", numOfCameras)
spin_results = readCSV_convert("results-spin.txt", numOfCameras)
write_results = readCSV_convert("results-write.txt", numOfCameras)

no_results = readCSV_convert("results-nothing.txt", numOfCameras)

identifyTest = readCSV_convert("test-result.txt", numOfCameras)
identification_dict = {
    'standing': stand_results,
    'spinning': spin_results,
    'writing': write_results,
    'nothing': no_results
    }

def cutData(funct): # Input a function and the derivative will be found where
    # the data is to be cut off.
    
    

def identify_gesture(test, trainingData=identification_dict):
    if isinstance(test, str):
        test_read = readCSV_convert(test, numOfCameras)
        test_result = interpolate_result(normalize(test_read))
    else:
        test_result = interpolate_result(normalize(test))
    interp_data = dict() # store interpreted data in a dictionary
    data_sum = dict()
    for name, dataset in trainingData.items(): # dataset
        interp_data[name] = interpolate_result(normalize(dataset))
        data_sum[name] = 0.0
    
    for name, func in interp_data.items():
        for n in np.linspace(0., 1., 200):
            func_values = np.array(func(n)) # result of training data function
            test_values = np.array(test_result(n)) # result of test function
            diff = (func_values - test_values) # get difference
            diff_sum = np.sum(np.square(diff)) # get sum
            data_sum[name] += diff_sum
    print(data_sum) # test function to show the differences
    return min(data_sum, key=data_sum.get)
                
def normalize(inputList):
    averages = []
    for i in range(len(inputList)):
        if (np.std(inputList[i]) > thrsh): # floating point error handling/close to 0
            averages.append((inputList[i]-np.mean(inputList[i]))/np.std(inputList[i]))
        else:
            averages.append(inputList[i]-np.mean(inputList[i])) 
    return averages
    
def interpolate_result(result_list): # Returns interpolation function normalized from 0 to 1
    x_axis = np.linspace(0., 1., len(result_list[0]))
    result_function = interpolate.interp1d(x_axis, result_list)
    return result_function
    
