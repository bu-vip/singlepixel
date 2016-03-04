#sensorName <- readline("Enter the sensor data filename: ")
sensorName <- "take1_raw.txt"
sensorData <- fromJSON(readChar(sensorName, file.info(sensorName)$size))
sensorData$luminance <- 0.2989 * sensorData$red + 0.5870 * sensorData$green + 0.1140 * sensorData$blue
ggplot(sensorData, aes(received, luminance)) + geom_line() + facet_grid(sensorId ~ .)