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
  

function [maeX, mseX, maeY, mseY, meanDistance, stdDistance, confidence, qCCR, qConf] = calcSVRMetricsForGroup(groupPrefix, groupCount, scalePrefix)
    xActual = [];
    yActual = [];
    cActual = [];
    xPredicted = [];
    yPredicted = [];
    cPredicted = [];
    for i = 1:groupCount
        % get scaling params
        load(strcat(scalePrefix, num2str(i), '-ranges.mat'), 'trainMins', 'trainRange');
        %load data
        testXActual = csvread(strcat(groupPrefix, num2str(i), '-testX.actual'));
        testYActual = csvread(strcat(groupPrefix, num2str(i), '-testY.actual'));
        testXPredicted = csvread(strcat(groupPrefix, num2str(i), '-testX.predicted'));
        testYPredicted = csvread(strcat(groupPrefix, num2str(i), '-testY.predicted'));
        
        % scale and add to dataset
        xActual = [xActual; scale(testXActual, trainMins(1), trainRange(1))];
        yActual = [yActual; scale(testYActual, trainMins(2), trainRange(2))];
        cActual = [cActual; csvread(strcat(groupPrefix, num2str(i), '-testC.actual'))];
        xPredicted = [xPredicted; scale(testXPredicted, trainMins(1), trainRange(1))];
        yPredicted = [yPredicted; scale(testYPredicted, trainMins(2), trainRange(2))];
        
        %calc the quantized svr
        grid = 3;
        quantX = floor((testXPredicted .+ 1) .* grid ./ 2.01);
        quantY = floor((testYPredicted .+ 1) .* grid ./ 2.01) .* grid;
        cPredicted = [cPredicted; (quantX .+ quantY)];
    end
    
    cPredicted(cPredicted >= grid * grid) = grid * grid - 1;
    cPredicted(cPredicted < 0) = 0;
    
    % calc metrics
    [maeX, mseX] = calcSVRMetrics(xActual, xPredicted);
    [maeY, mseY] = calcSVRMetrics(yActual, yPredicted);
    [meanDistance, stdDistance, confidence] = calcDistance(xActual, yActual, xPredicted, yPredicted);
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

function calcMetricsForGroup(groupPrefix, groupCount, scalePrefix)
  [maeX, mseX, maeY, mseY, meanDistance, meanStd, confidence, qCCR, qConf] = calcSVRMetricsForGroup(groupPrefix, groupCount, scalePrefix);
  [ccr, conf] = calcSVCMetricsForGroup(groupPrefix, groupCount);
  
  printf(groupPrefix);
  printf("\nMean Absolute Error X: %.4f", maeX);
  printf("\nMean Square Error X: %.4f", mseX);
  printf("\nMean Absolute Error Y: %.4f", maeY);
  printf("\nMean Square Error Y: %.4f", mseY);
  printf("\nMean Distance: %.4f +- %.4f", meanDistance, confidence);
  printf("\nStd Dev Distance: %.4f", meanStd);
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

dataDirectory = '/home/doug/Desktop/rescaled/'
calcMetricsForGroup(strcat(dataDirectory, "results/", "sixPrivate"), 4, strcat(dataDirectory, "dataSets/", "sixPrivate"));
calcMetricsForGroup(strcat(dataDirectory, "results/", "sixPublic"), 4, strcat(dataDirectory, "dataSets/", "sixPublic"));
calcMetricsForGroup(strcat(dataDirectory, "results/", "fourPrivate"), 4, strcat(dataDirectory, "dataSets/", "fourPrivate"));
calcMetricsForGroup(strcat(dataDirectory, "results/", "fourPublic"), 4, strcat(dataDirectory, "dataSets/", "fourPublic"));
