# Compute the luminance based on a RGB value
features_calc_luminance <- function(red, green, blue)
{
  return(0.2989 * red + 0.5870 * green + 0.1140 * blue)
}

# Compute discete derivative
features_calc_derivative <- function(value)
{
  # sobel operator
  h <- c(1, 0, -1)
  derivative <- convolve(value, h, type="open")
  return(derivative)
}

# Applys a function to all sensor signals individually
features_apply_sensorwise <- function(sensorData, sensorNames, func, inVar, outVar)
{
  for (sensorName in sensorNames)
  {
    inputName <- paste(sensorName, inVar, sep="")
    resultName <- paste(sensorName, outVar, sep="")
    input <- sensorData[, inputName]
    sensorData[resultName] <- func(input)[1:length(input)]
  }
  
  return(sensorData)
}