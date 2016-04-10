dataDirectory = '/home/doug/Desktop/UROP/track5/results/results/'

halfHalfCount = 6;
oneVRestCount = 4;
xMin = 0.047699;
yMin = -1.216962;
xRange = 2.376123;
yRange = 2.724559;

function [scaled] = scale(data, min, range)
    scaled = ((data .+ 1) ./ 2) .* range .+ min;
end

function [meanAbsError, meanSqError] = calcSVRMetrics(actual, predicted)
    difference = actual - predicted;
    squares = difference .* difference;
    meanAbsError = mean(abs(difference));
    meanSqError = sum(squares) / rows(squares);
end

function [meanDistance, stdDev, confidence] = calcDistance(actualX, actualY, predX, predY)
  xDistance = predX .- actualX;
  yDistance = predY .- actualY;
  distance = sqrt((xDistance .* xDistance) + (yDistance .* yDistance));
  meanDistance = mean(distance);
  stdDev = std(distance);
  confidence = stdDev / sqrt(rows(actualX));
 end
  

function [maeX, mseX, maeY, mseY, meanDistance, stdDistance, confidence] = calcSVRMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange)
    xActual = [];
    yActual = [];
    xPredicted = [];
    yPredicted = [];
    for i = 1:groupCount
        xActual = [xActual; csvread(strcat(groupPrefix, num2str(i), '-testX.actual'))];
        yActual = [yActual; csvread(strcat(groupPrefix, num2str(i), '-testY.actual'))];
        xPredicted = [xPredicted; csvread(strcat(groupPrefix, num2str(i), '-testX.predicted'))];
        yPredicted = [yPredicted; csvread(strcat(groupPrefix, num2str(i), '-testY.predicted'))];
    end

    [maeX, mseX] = calcSVRMetrics(scale(xActual, xMin, xRange), scale(xPredicted, xMin, xRange));
    [maeY, mseY] = calcSVRMetrics(scale(yActual, yMin, yRange), scale(yPredicted, yMin, yRange));
    [meanDistance, stdDistance, confidence] = calcDistance(scale(xActual, xMin, xRange), scale(yActual, yMin, yRange), scale(xPredicted, xMin, xRange), scale(yPredicted, yMin, yRange));
end

function [ccr] = calcSVCMetricsForGroup(groupPrefix, groupCount)
    actual = [];
    predicted = [];
    for i = 1:groupCount
        actual = [actual; csvread(strcat(groupPrefix, num2str(i), '-testC.actual'))];
        predicted = [predicted; csvread(strcat(groupPrefix, num2str(i), '-testC.predicted'))];
    end

    diff = actual .- predicted;
    ccr = (rows(diff) - nnz(diff)) / rows(diff);
end

function calcMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange)
  [maeX, mseX, maeY, mseY, meanDistance, meanStd, confidence] = calcSVRMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange);
  [ccr] = calcSVCMetricsForGroup(groupPrefix, groupCount);
  
  printf(groupPrefix);
  printf("\nMean Absolute Error X: %f", maeX);
  printf("\nMean Square Error X: %f", mseX);
  printf("\nMean Absolute Error Y: %f", maeY);
  printf("\nMean Square Error Y: %f", mseY);
  printf("\nMean Distance: %f", meanDistance);
  printf("\nStd Dev Distance: %f +- %f", meanStd, confidence);
  printf("\nCCR: %f\n", ccr);
end


calcMetricsForGroup(strcat(dataDirectory, "oneVRest"), oneVRestCount, xMin, xRange, yMin, yRange);
calcMetricsForGroup(strcat(dataDirectory, "oneVRestWalks"), oneVRestCount, xMin, xRange, yMin, yRange);
