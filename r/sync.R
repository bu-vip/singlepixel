sync_sensor_optitrack <- function(sensorData, sensorStartTime, sensorEndTime, optitrackData, optiStartTime, optiEndTime)
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
  #optSampled <- resample(optitrackData$class, minReadingCount, nrow(optitrackData))
  optSampledX <- resample(optitrackData$x, minReadingCount, nrow(optitrackData))
  optSampledY <- resample(optitrackData$y, minReadingCount, nrow(optitrackData))
  optSampledZ <- resample(optitrackData$z, minReadingCount, nrow(optitrackData))
  
  # create a dataframe with all synced data
  syncedData <- data.frame(t=seq(1, minReadingCount, by=1))
  #syncedData$class <- round(optSampled)
  syncedData$x <- optSampledX
  syncedData$z <- optSampledZ
  for (groupSensorId in uniqueSensors)
  {
    # resize sensor signals to be the same size
    trimmedsensorData <- sensorData[sensorData$groupSensorId == groupSensorId, ][seq(1, minReadingCount, by=1), ]
    syncedData[groupSensorId] <- trimmedsensorData$luminance
  }

  return(syncedData)
}