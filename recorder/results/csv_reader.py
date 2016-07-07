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

def readCSV_white(fileName, numOfCameras = 12):
    N = 0
    white = [[] for j in range(numOfCameras)]
    try:
        f = open(fileName, "r")
        with f:
            reader = csv.reader(f)
            normalized = False
            for row in reader:
                if N != 0:
                    cameraId = 8*int(row[2])+int(row[5])
                    if (normalized or cameraId == 0) and cameraId < numOfCameras: # ignore results before normalization and higher camera IDs
                        normalized = True # normalize it
                        white[cameraId].append(float(row[8])) # convert value to float
                N = N+1
            for i in range(len(white)):
                if len(white[numOfCameras-1]) < len(white[i]):
                    white[i].pop()
            return white
    except Exception as e:
        print(e)
    return []

def readCSV_convert(fileName, numOfCameras = 12):
    N = 0
    white = [[] for j in range(numOfCameras)]
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
                        redValue = float(row[4])
                        greenValue = float(row[1])
                        blueValue = float(row[0])
                        whiteValue = 0.299*redValue + 0.587*greenValue + 0.114*blueValue
                        white[cameraId].append(whiteValue)
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

def collectData(mainDirectory = os.getcwd()):
    allData = dict() # store everything in a dictionary
    for subdir, dirs, files in os.walk(mainDirectory):
        gestureName = os.path.basename(subdir)
        if gestureName != "results":
            if not gestureName.startswith("_"):
                gestureList = list()
                for fileName in os.listdir(gestureName):
                    print(fileName)
                    gestureList.append(readCSV_convert(gestureName+"/"+fileName))
            allData[gestureName] = gestureList
    return allData
    

