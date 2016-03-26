clear
%graphics_toolkit('fltk');
more off;

dataDirectory = 'data/track2/'
svmParams = '-m 3000 -s 3 -q'
featureDerivative = 0
featureDerivConv = [-1, 0, 1]
gridMinC = -15
gridMaxC = 15
gridStepC = 1
gridMinG = -15
gridMaxG = 0
gridStepG = 1

bestcX = 3.0518e-05;
bestcY = 3.0518e-05;
bestgX = 3.0518e-05;
bestgY = 3.0518e-05;
doGridSearch = 1;

makeMovie = 1;


% Get files in directory
filelist = readdir (dataDirectory);
fileCount = 0;
for ii = 1:numel(filelist)
  % skip special files . and ..
  if (regexp (filelist{ii}, "^\\.\\.?$"))
    continue;
  end

  fileCount = fileCount + 1;
  % load the data file
  fileData = csvread(strcat(dataDirectory, filelist{ii}));
  % skip first row with labels
  data.(num2str(fileCount)) = fileData(2:rows(fileData), :);
  
end


% calc features
sensorCount = columns(data.('1')) - 2;
if featureDerivative == 1
  for i = 1:fileCount
    fileData = data.(num2str(i));
    newData = zeros(rows(fileData), sensorCount);
    for sensor = 1:sensorCount
      sensorData = fileData(:, [2+sensor]);
      newData(:, [sensor]) = conv(sensorData, featureDerivConv, 'same');
    end
    data.(num2str(i)) = [fileData, newData];
  end
end



% create 2 groups of data
groupDist = randperm(fileCount);
groupTrain = [];
groupTest = [];
for i = 1:fileCount
  fileIndex = groupDist(i);
  if (i < fileCount / 2)
    groupTrain = [groupTrain; data.(num2str(fileIndex))];
  else
    groupTest = [groupTest; data.(num2str(fileIndex))];
  end
end


% scale input data
minimums = min(groupTrain, [], 1);
ranges = max(groupTrain, [], 1) - minimums;
groupTrain = (groupTrain - repmat(minimums, size(groupTrain, 1), 1)) ./ repmat(ranges, size(groupTrain, 1), 1);
groupTrain = (groupTrain .* 2) .- 1;
groupTest = (groupTest - repmat(minimums, size(groupTest, 1), 1)) ./ repmat(ranges, size(groupTest, 1), 1);
groupTest = (groupTest .* 2) .- 1;

function [bestc, bestg] = gridSearch(svmParams, labels, features, minLogC, maxLogC, stepLogC, minLogG, maxLogG, stepLogG)
  bestcv = 999999;
  bestc = 99999;
  bestg = 99999;
  for log2c = minLogC:stepLogC:maxLogC
    for log2g = minLogG:stepLogG:maxLogG
      cmd = [svmParams, ' -v 5 -c ', num2str(2^log2c), ' -g ', num2str(2^log2g)];
      cv = svmtrain(labels, features, cmd);
      if (cv < bestcv)
        bestcv = cv; bestc = 2^log2c; bestg = 2^log2g;
      end
      fprintf('%g %g %g (best c=%g, g=%g, rate=%g)\n', log2c, log2g, cv, bestc, bestg, bestcv);
    end
  end
end

function [predicted, model] = trainAndTestModel(svmParams, c, g, trainLabels, trainFeatures, testLabels, testFeatures)
  cmd = [svmParams, ' -c ', num2str(c), ' -g ', num2str(g)];
  model = svmtrain(trainLabels, trainFeatures, cmd);
  [predicted] = svmpredict(testLabels, testFeatures, model);
end

groupTrainLabelX = groupTrain(:, [1]);
groupTrainLabelY = groupTrain(:, [2]);
groupTrainFeatures = groupTrain(:, 3:columns(groupTrain));
if (doGridSearch == 1)
  % parameter tuning
  [bestcX, bestgX] = gridSearch(svmParams, groupTrainLabelX, groupTrainFeatures, gridMinC, gridMaxC, gridStepC, gridMinG, gridMaxG, gridStepG);
  [bestcY, bestgY] = gridSearch(svmParams, groupTrainLabelY, groupTrainFeatures, gridMinC, gridMaxC, gridStepC, gridMinG, gridMaxG, gridStepG);
end

% train model and test
groupTestLabelX = groupTest(:, [1]);
groupTestLabelY = groupTest(:, [2]);
groupTestFeatures = groupTest(:, 3:columns(groupTest));
fprintf('-----X-----\n');
[predictedX, modelX] = trainAndTestModel(svmParams, bestcX, bestgX, groupTrainLabelX, groupTrainFeatures, groupTestLabelX, groupTestFeatures);
fprintf('-----Y-----\n');
[predictedY, modelY] = trainAndTestModel(svmParams, bestcY, bestgY, groupTrainLabelY, groupTrainFeatures, groupTestLabelY, groupTestFeatures);

std(predictedX)
std(predictedY)


% convert to movie using 'ffmpeg -i "%5d.png" -y output.mpeg'
if (makeMovie == 1)
  mkdir('/tmp/UROP/');
  mkdir('/tmp/UROP/octave/');
  mkdir('/tmp/UROP/octave/output/');
  set(0, 'defaultfigurevisible', 'off');
  for i=1:rows(predictedX)
    clf
    axis([-2, 2 -2 2]);
    rectangle('Position',[predictedX(i) predictedY(i) 0.1 0.1],'Curvature',[1,1], 'FaceColor', 'green');
    rectangle('Position',[groupTestLabelX(i) groupTestLabelY(i) 0.1 0.1],'Curvature',[1,1], 'FaceColor', 'red');
    filename=sprintf('/tmp/UROP/octave/output/%05d.png',i);
    print(filename);
  end
end




%%syncedData = csvread('full.csv');
%%
%%sensorCount = (columns(syncedData)-1);
%%
%%for index = 1:sensorCount
%%  subplot(sensorCount, 1, index)
%%  % first row is class
%%  plot(syncedData(:,[index + 1]));
%%end
%
%%for index = 1:sensorCount
%%  subplot(sensorCount, 1, index)
%%  % first row is class
%%  freqs = fft(syncedData(:,[index + 1]));
%%  plot(abs(freqs));
%%end
%
%classData = syncedData(:,[1]);
%
%sobel = [-1, 0, 1];
%featureData = zeros(rows(syncedData), sensorCount * 2);
%for index = 1:sensorCount
%  featureData(:, [index * 2 - 1]) = syncedData(:, [index + 1]);
%  featureData(:, [index * 2 + 0]) = conv(syncedData(:, [index + 1]), sobel, 'same');
%end
%
%windowSize = 3;
%featureCount = sensorCount * 2;
%
%result = zeros(rows(featureData) - windowSize, 1 + featureCount * windowSize);
%result(:,[1]) = classData(1:(rows(featureData) - windowSize), [1]);
%resultIndex = 2;
%for index = 1:windowSize
%  for feature = 1:featureCount
%    result(:,[resultIndex]) = featureData(index:rows(result)+index-1, [feature]);
%    resultIndex = resultIndex + 1;
%  end
%end
%
%csvwrite('windowed.csv', result);
%
%% low pass Butterworth filter with cutoff pi*Wc radians - choose the order of the filter n and cut-off frequency Wc to suit
%%[b,a] = butter(n, Wc)
%%filtered_data = filter(b,a,original_data);