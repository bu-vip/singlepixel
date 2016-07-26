# Import CSV to read
import csv
import os

import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import numpy as np

# Output is a list of values for cameras for blue, green, and red
# from the recorder.

def readCSV(fileName, numOfCameras = 12):
    N = 0
    blue, red, green = ([[] for j in range(numOfCameras)] for i in range(3))
    try:
        f = open(fileName, "r")
        with f:
            reader = csv.reader(f)
            normalized = False
            for row in reader:
                if N != 0:
                    cameraId = 8*int(row[2])+int(row[5])
                    if (normalized or cameraId == 0) and cameraId < numOfCameras: # ignore results before normalization
                        normalized = True # normalize it
                        blue[cameraId].append(float(row[0])) # convert value to float
                        green[cameraId].append(float(row[1]))
                        red[cameraId].append(float(row[4]))
                N = N+1
            for i in range(len(blue)):
                if len(blue[numOfCameras-1]) < len(blue[i]):
                    red[i].pop()
                    green[i].pop()
                    blue[i].pop()
            return red, green, blue
    except Exception as e:
        print(e)
    return [], [], []

# for running program
#if __name__ == "__main__": 
#    main()

def readCSV_white(fileName, numOfCameras = 12, method='value'):
    N = 0
    white = [[] for j in range(numOfCameras)]
    try:
        f = open(fileName, "r")
        with f:
            reader = csv.reader(f)
            normalized = False
            prevValue = list()
            for row in reader:
                if N != 0:
                    cameraId = 8*int(row[2])+int(row[5])
                    if (normalized or cameraId == 0) and cameraId < numOfCameras: # ignore results before normalization and higher camera IDs
                        normalized = True # normalize it
                        if method == 'value':
                            listContent = float(row[8])
                        elif method == 'diff':
                            if len(white[cameraId]) == 0:
                                listContent = 0
                                prevValue.append(float(row[8]))
                            else:
                                listContent = float(row[8])-prevValue[cameraId]
                            prevValue[cameraId] = float(row[8]) # set previous value to current value
                        white[cameraId].append(listContent) # convert value to float
                N = N+1
            for i in range(len(white)):
                if len(white[numOfCameras-1]) < len(white[i]):
                    white[i].pop()
            return white
    except Exception as e:
        print(e)
    return []

def graph_plots(plots):
    colors = iter(cm.rainbow(np.linspace(0, 1, len(plots))))
    patches = list()
    for i in range(len(plots)):
        inputColor = next(colors)
        patches.append(mpatches.Patch(color=inputColor, label="Camera "+str(i)))
        plt.scatter(range(len(plots[i])), plots[i], color=inputColor)
    plt.legend(handles=patches)
    plt.show()

def plot_signal_details(plot_oneD):
    patches = list()
    label = "mean: " + str(np.mean(plot_oneD))+ "\nstd: "+str(np.std(plot_oneD))
    patches.append(mpatches.Patch(label=label))
    plt.scatter(range(len(plot_oneD)), plot_oneD)
    plt.xlim([0, len(plot_oneD)])
    plt.legend(handles=patches)
    plt.show()

def collect_data(mainDirectory = os.getcwd(), numOfCameras = 12, method='value', excluding = (None, None)):
    allData = dict() # store everything in a dictionary
    M = 0
    notWrittenName = ""
    for subdir, dirs, files in os.walk(mainDirectory):
        gestureName = os.path.basename(subdir)
        if gestureName != "results":
            if not gestureName.startswith("_"): # ignore all folders starting with _
                N = 0
                gestureList = list()
                for fileName in os.listdir(gestureName):
                    if (M, N) != excluding:
                        gestureList.append(readCSV_white(gestureName+"/"+fileName, numOfCameras=numOfCameras, method=method))
                    else:
                        notWrittenName = gestureName+"/"+fileName
                    N = N+1
                M = M+1
                allData[gestureName] = gestureList
    print("without", notWrittenName)
    return allData, notWrittenName

# Structure of data: dictionary
# 1st dimension: name of the gesture
# 2nd dimension: the nth result reading
# 3rd dimension: the sensor ID
# 4th dimension: the reading at time t
