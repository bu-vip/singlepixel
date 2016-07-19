from csv_reader import *
from scipy import interpolate
from sklearn import neighbors
import numpy as np

numOfCameras = 6
signalDataPoints = 30
thrsh = 1e-10
n_neighbors = 5
method = 'diff'

def interp_all(trainingData):
    interp_data = dict() # store interpreted data in a dictionary
    for name, allSets in trainingData.items(): # dataset
        interp_data[name] = list()
        for dataset in allSets:
            dataFunction = interpolate_result(dataset)
            interpDataSet = list()
            for n in range(signalDataPoints):
                interpDataSet.append(dataFunction(n))
            data_onedim = np.reshape(interpDataSet, numOfCameras*signalDataPoints, -1)
            interp_data[name].append(data_onedim)
    return interp_data # dictionary containing interpolated functions

# Structure of data: dictionary
# 1st dimension: name of the gesture
# 2nd dimension: the nth result reading
# 3rd dimension: the sensor ID
# 4th dimension: the reading at time t
    
def split_data(storage_dict):
    keys = list()
    values = list()
    for key in storage_dict.keys():
        for light_data in storage_dict[key]:
            keys.append(key)
            values.append(light_data)
    return (keys, values)
                
def normalize(inputList):
    averages = []
    for i in range(len(inputList)):
        if (np.std(inputList[i]) > thrsh): # floating point error handling/close to 0
            averages.append((inputList[i]-np.mean(inputList[i]))/np.std(inputList[i]))
        else:
            averages.append(inputList[i]-np.mean(inputList[i])) 
    return averages
    
def interpolate_result(result_list): # Returns interpolation function normalized from 0 to 1
    x_axis = np.linspace(0., signalDataPoints, len(result_list[0]))
    result_function = interpolate.interp1d(x_axis, result_list, kind='cubic')
    return result_function
    
identification_dict = collect_data(numOfCameras = numOfCameras, method=method)
interp_data = interp_all(identification_dict)

def identify_gesture(test, train_functions = interp_data):
    # interpolate function to test
    if isinstance(test, str):
        test_read = readCSV_white(test, numOfCameras, method=method)
        test_result = interpolate_result(normalize(test_read))
    else:
        test_result = interpolate_result(normalize(test))
    test_list = list()
    for n in range(signalDataPoints):
        test_list.append(test_result(n))
    test_list = np.reshape(test_list, numOfCameras*signalDataPoints, -1)
    test_list = np.array(test_list).reshape((1, -1))
    # test_list = np.reshape(test_list, -1, 1)
    # maxValues = dict.fromkeys(train_functions.keys(), 0)
    clf = neighbors.KNeighborsClassifier(n_neighbors, weights='distance')
    keys, values = split_data(train_functions)
    clf.fit(values, keys)
    print(clf.kneighbors(test_list, n_neighbors))
    return clf.predict(test_list)
