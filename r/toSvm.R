rm(list = ls())

library(jsonlite)
library(ggplot2)
library(reshape2)
library(grid)
library(gridExtra)

source("sync.R")
source("multiplot.R")

# Converts a input sensor data file and optitrack file into SVM ready data points

# setwd("/home/doug/Desktop/UROP/track1")
# sensorFiles <- c("take1_raw.txt", "take2_raw.txt", "take3_raw.txt")
# optiFiles <- c("take1_opti.json", "take2_opti.json", "take3_opti.json")
# outputPrefixes <- c("take1_res", "take2_res", "take3_res")
# sensorStartTimes <- c(1456351290828, 1456351427988, 1456351589187)
# sensorEndTimes <- c(1456351348197, 1456351498487, 1456351659988)
# optiStartTimes <- c(1813, 1346, 1220)
# optiEndTimes <- c(7552, 8398, 8302)
# trimStart <- c(5, 5, 5)
# trimEnd <- c(50, 50, 50)
# backgroundFileName <- "background.txt"

dataPath <- "/home/doug/Desktop/UROP/track2/"
sensorFiles <- c("take1.txt", "take2.txt", "take3.txt", "take4.txt", "take5.txt", "take6.txt", "take7.txt", "take8.txt")
optiFiles <- c("take1_opti.json", "take2_opti.json", "take3_opti.json", "take4_opti.json", "take5_opti.json", "take6_opti.json", "take7_opti.json", "take8_opti.json")
outputPrefixes <- c("take1-res-", "take2-res-", "take3-res-", "take4-res-", "take5-res-", "take6-res-", "take7-res-", "take8-res-")
sensorStartTimes <- c(1456450336081,1456450519513,1456450754459,1456450931828,1456452135556,1456452392004,1456452517247,1456452624509)
sensorEndTimes <- c(1456450385980,1456450581633,1456450823245,1456451051018,1456452244232,1456452462100,1456452586229,1456452676624)
optiStartTimes <- c(1690,1417,1689,1598,1546,1629,1482,1127)
optiEndTimes <- c(6674,7627,8551,13500,12417,8644,8381,6328)
trimStart <- c(5,5,5,5,5,5,5,5)
trimEnd <- c(50,50,50,50,50,75,50,50)
backgroundFileName <- paste(dataPath, "background.txt", sep="")


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
  outputName <- paste(dataPath, outputPrefixes[i], sep="")
  
  # load data
  sensData <- fromJSON(readChar(sensorFileName, file.info(sensorFileName)$size))
  optiData <- fromJSON(readChar(optiFileName, file.info(optiFileName)$size))
  
  # create another column of groupId + sensorId
  sensData$groupSensorId <- paste(sensData$groupId, "-", sensData$sensorId, sep="")
  uniqueSensors <- sort(unique(sensData$groupSensorId))
  
  # calculate luminance
  sensData$luminance <- features_calc_luminance(sensData$red, sensData$green, sensData$blue)
  
  # plot original data
  ggplot(sensData, aes(received, luminance)) + geom_line() + facet_grid(sensorId ~ .)
  ggsave(paste(outputName, "sensor_orig.png", sep=""), width = 8, height = 6)
  optiMelt <- melt(optiData[, c("frameIndex", "x", "y", "z")], id=c("frameIndex"))
  ggplot(data=optiMelt, aes(x=frameIndex, y=value, color=variable)) + geom_line()
  ggsave(paste(outputName, "opti_orig.png", sep=""), width = 8, height = 6)
  
  # background subtraction
  sensData$luminanceNorm <- (sensData$luminance - backgroundMean)
  
  # calculate class for optitrack data
  quantizeLevels <- 3
  optLevelSizeX <- (optMaxX - optMinX) / quantizeLevels
  optLevelSizeZ <- (optMaxZ - optMinZ) / quantizeLevels
  # ensure inside class boundaries
  optiData$class <- pmin(floor((optiData$x - optMinX) / optLevelSizeX), quantizeLevels - 1)
  optiData$class <- optiData$class + pmin(floor((optiData$z - optMinZ) / optLevelSizeZ), quantizeLevels - 1) * quantizeLevels
  
  # Sync optitrack and sensor data
  syncedData <- sync_sensor_optitrack(sensData, sensorDataStartTime, sensorDataEndTime, optiData, optiDataStartTime, optiDataEndTime, numberSamplesToRemoveStart, numberSamplesToRemoveEnd)
  
  # Plot synced data
  sync_plot_data(syncedData, uniqueSensors, paste(outputName, "synced.png", sep=""));
  
  # write data to json file
  jsonFile <- file(paste(outputName, "synced.json", sep=""))
  writeLines(toJSON(syncedData, pretty=FALSE), jsonFile)
  close(jsonFile)
  
  # write to csv
  write.csv(syncedData, file=paste(outputName, "synced.csv", sep=""))
}