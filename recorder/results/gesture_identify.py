from csv_reader import *
from scipy import interpolate
from sklearn import neighbors
import numpy as np

numOfCameras = 12
signalDataPoints = 50
thrsh = 1e-10
n_neighbors = 3
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
            data_onedim = normalize(data_onedim)
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
                
def normalize(oneList):
    if (np.std(oneList) > thrsh): # floating point error handling/close to 0
        averages = (oneList-np.mean(oneList))/np.std(oneList)
    else:
        averages = (oneList-np.mean(oneList))
    return averages
    
def interpolate_result(result_list): # Returns interpolation function normalized from 0 to 1
    x_axis = np.linspace(0., signalDataPoints, len(result_list[0]))
    result_function = interpolate.interp1d(x_axis, result_list, kind='cubic')
    return result_function

def identify_gesture(test, train_functions):
    # interpolate function to test
    if isinstance(test, str):
        test_read = readCSV_white(test, numOfCameras, method=method)
        test_result = interpolate_result(normalize(test_read))
    else:
        test_result = interpolate_result(normalize(test))
    test_list = list()
    for n in range(signalDataPoints):
        test_list.append(test_result(n))
    test_list = normalize(test_list)
    test_list = np.reshape(test_list, numOfCameras*signalDataPoints, -1)
    test_list = np.array(test_list).reshape((1, -1))
    # test_list = np.reshape(test_list, -1, 1)
    # maxValues = dict.fromkeys(train_functions.keys(), 0)
    clf = neighbors.KNeighborsClassifier(n_neighbors, weights='distance')
    keys, values = split_data(train_functions)
    clf.fit(values, keys)
    return clf.predict(test_list)

for i in range(30):
    identification_dict, missing = collect_data(numOfCameras = numOfCameras, method=method, excluding=(5, i))
    interp_data = interp_all(identification_dict)
    print(i, identify_gesture(missing, interp_data))

