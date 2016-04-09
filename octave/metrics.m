dataDirectory = '/home/doug/Desktop/UROP/track5/results/dataSets/'

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

function [meanDistance, stdDev] = calcDistance(actualX, actualY, predX, predY)
  xDistance = predX .- actualX;
  yDistance = predY .- actualY;
  distance = sqrt((xDistance .* xDistance) + (yDistance .* yDistance));
  meanDistance = mean(distance);
  stdDev = std(distance);
 end
  

function [maeX, mseX, maeY, mseY, meanDistance, stdDistance] = calcSVRMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange)
    xActual = [];
    yActual = [];
    xPredicted = [];
    yPredicted = [];
    for i = 1:groupCount
        xActual = [xActual; csvread(strcat(groupPrefix, num2str(i), '-testX.actual'))];
        yActual = [yActual; csvread(strcat(groupPrefix, num2str(i), '-testY.actual'))];
        xPredicted = [xPredicted; csvread(strcat(groupPrefix, num2str(i), '-testX.out'))];
        yPredicted = [yPredicted; csvread(strcat(groupPrefix, num2str(i), '-testY.out'))];
    end

    [maeX, mseX] = calcSVRMetrics(scale(xActual, xMin, xRange), scale(xPredicted, xMin, xRange));
    [maeY, mseY] = calcSVRMetrics(scale(yActual, yMin, yRange), scale(yPredicted, yMin, yRange));
    [meanDistance, stdDistance] = calcDistance(scale(xActual, xMin, xRange), scale(yActual, yMin, yRange), scale(xPredicted, xMin, xRange), scale(yPredicted, yMin, yRange));
end

function [ccr] = calcSVCMetricsForGroup(groupPrefix, groupCount)
    actual = [];
    predicted = [];
    for i = 1:groupCount
        actual = [actual; csvread(strcat(groupPrefix, num2str(i), '-testC.actual'))];
        predicted = [predicted; csvread(strcat(groupPrefix, num2str(i), '-testC.out'))];
    end

    diff = actual .- predicted;
    ccr = (rows(diff) - nnz(diff)) / rows(diff);
end

[hMAEX, hMSEX, hMAEY, hMSEY, hMD, hSTD] = calcSVRMetricsForGroup(strcat(dataDirectory, "halfHalf"), halfHalfCount, xMin, xRange, yMin, yRange);
[oMAEX, oMSEX, oMAEY, oMSEY, oMD, oSTD]= calcSVRMetricsForGroup(strcat(dataDirectory, "oneVRest"), oneVRestCount, xMin, xRange, yMin, yRange);

printf("2v2 MeanAbsoluteErrorX: %f MeanSquareErrorX: %f MeanAbsoluteErrorY: %f MeanSquareErrorY: %f MeanDistance: %f StdDevDistance: %f\n", hMAEX, hMSEX, hMAEY, hMSEY, hMD, hSTD);
printf("3v1 MeanAbsoluteErrorX: %f MeanSquareErrorX: %f MeanAbsoluteErrorY: %f MeanSquareErrorY: %f MeanDistance: %f StdDevDistance: %f\n", oMAEX, oMSEX, oMAEY, oMSEY, oMD, oSTD);


[hCCR] = calcSVCMetricsForGroup(strcat(dataDirectory, "halfHalf"), halfHalfCount);
[oCCR] = calcSVCMetricsForGroup(strcat(dataDirectory, "oneVRest"), oneVRestCount);

printf("2v2 CCR: %f\n", hCCR);
printf("3v1 CCR: %f\n", oCCR);



