# Compute the luminance based on a RGB value
features_calc_luminance <- function(red, green, blue)
{
  return(0.2989 * red + 0.5870 * green + 0.1140 * blue)
}

# Compute discete derivative
features_calc_derivative <- function(value)
{
  # sobel operator
  h <- c(-1, 0, 1)
  derivative <- convolve(value, h, type="open")
  return(derivative)
}

features_gen_gaussian_kernel <- function(radius) {
  width <- ceiling(radius) #kernel will have a middle cell, and width on either side
  sigma <- radius / 3
  kernel <- vector(length=(width * 2 + 1))
  
  #normalisation constant makes sure total of matrix is 1
  norm <- 1.0 / (sqrt(2 * pi) * sigma)
  
  #the bit you divide x^2 by in the exponential
  coeff <- 2 * sigma * sigma
  
  total <- 0
  for (x in -width:width)
  {
    val <- norm * exp(-1 * x * x / coeff)
    kernel[x + width + 1] <- val
    total <- total + val
  }
  
  kernel <- kernel / total
  
  return(kernel)
}

features_gaussian_smooth <- function(radius, values)
{
  h <- features_gen_gaussian_kernel(radius)
  smoothed <- convolve(values, h, type="open")
  return(smoothed)
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

# Applys a function to all sensor signals individually
features_apply_sensorwise_arg <- function(sensorData, sensorNames, func, inVar, outVar, arg1)
{
  for (sensorName in sensorNames)
  {
    inputName <- paste(sensorName, inVar, sep="")
    resultName <- paste(sensorName, outVar, sep="")
    input <- sensorData[, inputName]
    sensorData[resultName] <- func(arg1, input)[1:length(input)]
  }
  
  return(sensorData)
}