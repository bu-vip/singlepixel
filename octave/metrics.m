%
test=0;

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
 
function [ccr, confusion] = calcCCR(actualC, predictedC)
  correct = 0;
  grid = 3;
  confusion = zeros(grid * grid, grid * grid);
  for i = 1:rows(actualC)
    % CCR
    if actualC(i) == predictedC(i)
      correct = correct + 1;
    end
    % confusion
    confusion(actualC(i) + 1, predictedC(i) + 1) = confusion(actualC(i) + 1, predictedC(i) + 1) + 1;
  end
  ccr = correct / rows(actualC);
end
  

function [maeX, mseX, maeY, mseY, meanDistance, stdDistance, confidence, qCCR, qConf] = calcSVRMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange)
    xActual = [];
    yActual = [];
    cActual = [];
    xPredicted = [];
    yPredicted = [];
    for i = 1:groupCount
        xActual = [xActual; csvread(strcat(groupPrefix, num2str(i), '-testX.actual'))];
        yActual = [yActual; csvread(strcat(groupPrefix, num2str(i), '-testY.actual'))];
        cActual = [cActual; csvread(strcat(groupPrefix, num2str(i), '-testC.actual'))];
        xPredicted = [xPredicted; csvread(strcat(groupPrefix, num2str(i), '-testX.predicted'))];
        yPredicted = [yPredicted; csvread(strcat(groupPrefix, num2str(i), '-testY.predicted'))];
    end

    [maeX, mseX] = calcSVRMetrics(scale(xActual, xMin, xRange), scale(xPredicted, xMin, xRange));
    [maeY, mseY] = calcSVRMetrics(scale(yActual, yMin, yRange), scale(yPredicted, yMin, yRange));
    [meanDistance, stdDistance, confidence] = calcDistance(scale(xActual, xMin, xRange), scale(yActual, yMin, yRange), scale(xPredicted, xMin, xRange), scale(yPredicted, yMin, yRange));
        
    grid = 3;
    quantX = max(min(floor((xPredicted .+ 1) .* grid ./ 2.01), grid), 0);
    quantY = max(min(floor((yPredicted .+ 1) .* grid ./ 2.01), grid), 0) .* grid;
    cPredicted = quantX .+ quantY;
    [qCCR, qConf] = calcCCR(cActual, cPredicted);
end

function [ccr, conf] = calcSVCMetricsForGroup(groupPrefix, groupCount)
    actual = [];
    predicted = [];
    for i = 1:groupCount
        actual = [actual; csvread(strcat(groupPrefix, num2str(i), '-testC.actual'))];
        predicted = [predicted; csvread(strcat(groupPrefix, num2str(i), '-testC.predicted'))];
    end

    [ccr, conf] = calcCCR(actual, predicted);
end

function calcMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange)
  [maeX, mseX, maeY, mseY, meanDistance, meanStd, confidence, qCCR, qConf] = calcSVRMetricsForGroup(groupPrefix, groupCount, xMin, xRange, yMin, yRange);
  [ccr, conf] = calcSVCMetricsForGroup(groupPrefix, groupCount);
  
  printf(groupPrefix);
  printf("\nMean Absolute Error X: %.4f", maeX);
  printf("\nMean Square Error X: %.4f", mseX);
  printf("\nMean Absolute Error Y: %.4f", maeY);
  printf("\nMean Square Error Y: %.4f", mseY);
  printf("\nMean Distance: %.4f", meanDistance);
  printf("\nStd Dev Distance: %.4f +- %.4f", meanStd, confidence);
  printf("\nQuantized SVR CCR: %.4f\n", qCCR);
  qConf
  printf("\nCCR: %.4f\n", ccr);
  conf
  printf("\n");
% latex
%  printf(groupPrefix);
%  printf(" & \\multirow{2}{*}{%.4f} ", maeX);
%  printf("& \\multirow{2}{*}{%.4f} ", mseX);
%  printf("& \\multirow{2}{*}{%.4f} ", maeY);
%  printf("& \\multirow{2}{*}{%.4f} ", mseY);
%  printf("& \\multirow{2}{*}{%.4f} ", meanDistance);
%  printf("& \\multirow{2}{*}{%.4f $\\pm$ %.4f} ", meanStd, confidence);
%  printf("& \\multirow{2}{*}{%.4f} \n", ccr);
%  printf(groupPrefix);
%  printf(" & %.4f} ", maeX);
%  printf("& %.4f} ", mseX);
%  printf("& %.4f} ", maeY);
%  printf("& %.4f} ", mseY);
%  printf("& %.4f} ", meanDistance);
%  printf("& %.4f $\\pm$ %.4f ", meanStd, confidence);
%  printf("& %.4f \n", ccr);
end

dataDirectory = '/home/doug/Desktop/UROP/track5/6_results/'
load(strcat(dataDirectory, "dataSets/",'ranges.mat'), 'globalMin', 'globalRange');
calcMetricsForGroup(strcat(dataDirectory, "results/", "oneVRestWalks"), 4, globalMin(1), globalRange(1), globalMin(2), globalRange(2));
calcMetricsForGroup(strcat(dataDirectory, "results/", "oneVRest"), 4, globalMin(1), globalRange(1), globalMin(2), globalRange(2));

dataDirectory = '/home/doug/Desktop/UROP/track5/4_results/'
load(strcat(dataDirectory, "dataSets/",'fourCornersranges.mat'), 'globalMin', 'globalRange');
calcMetricsForGroup(strcat(dataDirectory, "results/", "fourCornersprivate"), 4, globalMin(1), globalRange(1), globalMin(2), globalRange(2));
calcMetricsForGroup(strcat(dataDirectory, "results/", "fourCornerspublic"), 4, globalMin(1), globalRange(1), globalMin(2), globalRange(2));
