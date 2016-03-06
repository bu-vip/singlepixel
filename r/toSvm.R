# Doug Roeper
# BU UROP
# 
# Converts sensor data file(s) and optitrack file(s) into SVM ready data points

rm(list = ls())

library(jsonlite)
library(ggplot2)
library(reshape2)
library(grid)
library(gridExtra)

source("sync.R")
source("features.R")

dataPath <- "/home/doug/Desktop/UROP/track2/data/"
sensorFiles <- c("take1.txt", "take2.txt", "take3.txt", "take4.txt", "take5.txt", "take6.txt", "take7.txt", "take8.txt")
optiFiles <- c("take1_opti.json", "take2_opti.json", "take3_opti.json", "take4_opti.json", "take5_opti.json", "take6_opti.json", "take7_opti.json", "take8_opti.json")
outputPrefixes <- c("take1-res-", "take2-res-", "take3-res-", "take4-res-", "take5-res-", "take6-res-", "take7-res-", "take8-res-")
outputFolder <- "/home/doug/Desktop/UROP/track2/r_out/"
sensorStartTimes <- c(1456450336081,1456450519513,1456450754459,1456450931828,1456452135556,1456452392004,1456452517247,1456452624509)
sensorEndTimes <- c(1456450385980,1456450581633,1456450823245,1456451051018,1456452244232,1456452462100,1456452586229,1456452676624)
optiStartTimes <- c(1690,1417,1689,1598,1546,1629,1482,1127)
optiEndTimes <- c(6674,7627,8551,13500,12417,8644,8381,6328)
trimStart <- c(5,5,5,5,5,5,5,5)
trimEnd <- c(50,50,50,50,50,75,50,50)
backgroundFile <- "background.txt"


# Config options
feature_derivative <- "deriv"
shouldGraph <- 1
graphWidthInch <- 8
graphHeightInch <- 6
graphWidthPx <- 1200
graphHeightPx <-900

# Constants
backgroundFileName <- paste(dataPath, backgroundFile, sep="")



# find the max and min of the optitrack data across all samples
optMinX <- 999999
optMaxX <- -999999
optMinZ <- 999999
optMaxZ <- -999999
for (i in 1:length(sensorFiles))
{
  optiFileName <- paste(dataPath, optiFiles[i], sep="")
  optiDataStartTime <- optiStartTimes[i]
  optiDataEndTime <- optiEndTimes[i]
  
  optiData <- fromJSON(readChar(optiFileName, file.info(optiFileName)$size))
  optiData <- optiData[optiData$frameIndex >= optiDataStartTime & optiData$frameIndex <= optiDataEndTime, ]
  
  optMinX <- pmin(min(optiData$x), optMinX)
  optMaxX <- pmax(max(optiData$x), optMaxX)
  optMinZ <- pmin(min(optiData$z), optMinZ)
  optMaxZ <- pmax(max(optiData$z), optMaxZ)
}


# calculate background sensor readings
backgroundData <- fromJSON(readChar(backgroundFileName, file.info(backgroundFileName)$size))
backgroundData$luminance <- features_calc_luminance(backgroundData$red, backgroundData$green, backgroundData$blue)
backgroundMean <- mean(backgroundData$luminance)

# process all takes
for (i in 1:length(sensorFiles))
{
  # get properties
  sensorFileName <- paste(dataPath, sensorFiles[i], sep="")
  optiFileName <- paste(dataPath, optiFiles[i], sep="")
  optiDataStartTime <- optiStartTimes[i]
  optiDataEndTime <- optiEndTimes[i]
  sensorDataStartTime <- sensorStartTimes[i]
  sensorDataEndTime <- sensorEndTimes[i]
  numberSamplesToRemoveStart <- trimStart[i]
  numberSamplesToRemoveEnd <- trimEnd[i]
  outputName <- paste(outputFolder, outputPrefixes[i], sep="")
  
  ### LOAD DATA ###
  # load data
  sensData <- fromJSON(readChar(sensorFileName, file.info(sensorFileName)$size))
  optiData <- fromJSON(readChar(optiFileName, file.info(optiFileName)$size))
  
  ### UNIQUE SENSORS ###
  # Create a key to address each sensor individually
  # key = groupId + sensorId
  sensData$groupSensorId <- paste(sensData$groupId, "-", sensData$sensorId, sep="")
  uniqueSensors <- sort(unique(sensData$groupSensorId))
  
  ### LUMINANCE ###
  # calculate luminance
  sensData$luminance <- features_calc_luminance(sensData$red, sensData$green, sensData$blue)
  # background subtraction
  sensData$luminance <- (sensData$luminance - backgroundMean)
  
  ### CLASSES ###
  # calculate class for optitrack data
  quantizeLevels <- 3
  optLevelSizeX <- (optMaxX - optMinX) / quantizeLevels
  optLevelSizeZ <- (optMaxZ - optMinZ) / quantizeLevels
  # ensure inside class boundaries
  optiData$class <- pmin(floor((optiData$x - optMinX) / optLevelSizeX), quantizeLevels - 1)
  optiData$class <- optiData$class + pmin(floor((optiData$z - optMinZ) / optLevelSizeZ), quantizeLevels - 1) * quantizeLevels
  
  ### SYNC ###
  # Sync optitrack and sensor data
  syncedData <- sync_sensor_optitrack(sensData, sensorDataStartTime, sensorDataEndTime, optiData, optiDataStartTime, optiDataEndTime)
  
  ### TRANSFORMATIONS ###
  # Calculate additional features
  syncedData <- features_apply_sensorwise(syncedData, uniqueSensors, features_calc_derivative, "", feature_derivative)
  
  # trim data
  syncedData <- syncedData[-seq(0, numberSamplesToRemoveStart, by=1), ]
  syncedData <- syncedData[-seq(nrow(syncedData) - numberSamplesToRemoveEnd + 1, nrow(syncedData) + 1, by=1), ]
  
  ### GRAPHING ###
  if (shouldGraph == 1)
  {
    # plot original data
    ggplot(sensData, aes(received, luminance)) + geom_line() + facet_grid(sensorId ~ .)
    ggsave(paste(outputName, "sensor_orig.png", sep=""), width = graphWidthInch, height = graphHeightInch)
    optiMelt <- melt(optiData[, c("frameIndex", "x", "y", "z")], id=c("frameIndex"))
    ggplot(data=optiMelt, aes(x=frameIndex, y=value, color=variable)) + geom_line()
    ggsave(paste(outputName, "opti_orig.png", sep=""), width = graphWidthInch, height = graphHeightInch)
    
    # Plot synced data
    # create sensor data plot
    meltedSensor <- melt(syncedData[, c(uniqueSensors, "t")], id=c("t"))
    sensorPlot <- ggplot(data=meltedSensor, aes(x=t, y=value, color=variable)) + geom_line()
    # create sensor data plot
    meltedSensorDeriv <- melt(syncedData[, c(paste(uniqueSensors, feature_derivative, sep=""), "t")], id=c("t"))
    sensorDerivPlot <- ggplot(data=meltedSensorDeriv, aes(x=t, y=value, color=variable)) + geom_line()
    # create class plot
    meltedClass <- melt(syncedData[, c("class", "t")], id=c("t"))
    classPlot <- ggplot(data=meltedClass, aes(x=t, y=value, color=variable)) + geom_line()
    # save to png
    png(paste(outputName, "synced.png", sep=""), width=graphWidthPx, height=graphHeightPx)
    grid.arrange(sensorPlot, sensorDerivPlot, classPlot, ncol=1)
    dev.off()
  }
  
  ### OUTPUT ###
  # write data to json file
  jsonFile <- file(paste(outputName, "synced.json", sep=""))
  writeLines(toJSON(syncedData, pretty=FALSE), jsonFile)
  close(jsonFile)
  # write to csv
  write.csv(syncedData, file=paste(outputName, "synced.csv", sep=""))
}