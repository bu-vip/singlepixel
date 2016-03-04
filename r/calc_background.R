rm(list = ls())

library(jsonlite)

setwd("/home/doug/Desktop/")

backgroundName <- paste("background.txt", sep="")
dataName <- paste("take1_raw.txt", sep="")

# calculate background
backgroundData <- fromJSON(readChar(backgroundName, file.info(backgroundName)$size))
backgroundData$luminance <- 0.2989 * backgroundData$red + 0.5870 * backgroundData$green + 0.1140 * backgroundData$blue
backgroundMean <- mean(backgroundData$luminance)
backgroundStdev <- sd(backgroundData$luminance)