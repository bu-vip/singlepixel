features_calc_luminance <- function(red, green, blue)
{
  return(0.2989 * red + 0.5870 * green + 0.1140 * blue)
}