sync_sensor_optitrack <- function(sensorData, sensorStartTime, sensorEndTime, optitrackData, optiStartTime, optiEndTime, trimBeginning, trimEnd)
{
  library(signal)
  
  # trim data
  optitrackData <- optitrackData[optitrackData$frameIndex >= optiDataStartTime & optitrackData$frameIndex <= optiDataEndTime, ]
  sensorData <- sensData[sensorData$received >= sensorDataStartTime & sensorData$received <= sensorDataEndTime, ]
  
  #get unique sensors
  uniqueSensors <- sort(unique(sensorData$groupSensorId))
  minReadingCount <- 9999999999
  for (groupSensorId in uniqueSensors)
  {
    # get data only for current sensor
    curSensorData <- sensorData[sensorData$groupSensorId == groupSensorId, ];
    
    # check for min reading size
    readingCount <- nrow(curSensorData)
    if (readingCount < minReadingCount)
    {
      minReadingCount <- readingCount
    }
  }
  
  # resample the optitrack data
  optSampled <- resample(optitrackData$class, minReadingCount, nrow(optitrackData))
  
  # create a dataframe with all synced data
  syncedData <- data.frame(t=seq(1, minReadingCount, by=1))
  syncedData$class <- round(optSampled)
  for (groupSensorId in uniqueSensors)
  {
    # resize sensor signals to be the same size
    trimmedsensorData <- sensorData[sensorData$groupSensorId == groupSensorId, ][seq(1, minReadingCount, by=1), ]
    syncedData[groupSensorId] <- trimmedsensorData$luminanceNorm
  }
  
  # trim from end
  syncedData <- syncedData[-seq(0, trimBeginning, by=1), ]
  syncedData <- syncedData[-seq(nrow(syncedData) - trimEnd + 1, nrow(syncedData) + 1, by=1), ]

  return(syncedData)
}

# save graph to png specified by output name
sync_plot_data <- function(syncedData, sensorNames, outputName)
{
  # create sensor data plot
  meltedSensor <- melt(syncedData[, c(sensorNames, "t")], id=c("t"))
  sensorPlot <- ggplot(data=meltedSensor, aes(x=t, y=value, color=variable)) + geom_line()
  
  # create class plot
  meltedClass <- melt(syncedData[, c("class", "t")], id=c("t"))
  classPlot <- ggplot(data=meltedClass, aes(x=t, y=value, color=variable)) + geom_line()
  
  # save to png
  png(outputName, width=1200, height=900)
  grid.arrange(sensorPlot, classPlot, ncol=1)
  dev.off()
}