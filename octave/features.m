clear
graphics_toolkit('fltk');
more off;

dataDirectory = 'data/track4/'
outPrefix = 'inorder-raw'
svmParams = '-m 10000 -s 3 -q'
featureDerivative = 0
if (featureDerivative == 1)
  featureDerivConv = [-1, 0, 1]
end

doGridSearch = 0;
if (doGridSearch == 1)
  gridMinC = -15
  gridMaxC = 15
  gridStepC = 1
  gridMinG = -15
  gridMaxG = 0
  gridStepG = 1
end

bestcX = 3.0518e-05;
bestcY = 3.0518e-05;
bestgX = 3.0518e-05;
bestgY = 3.0518e-05;

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
%groupDist = [11, 13, 7, 8, 2, 12, 10, 3, 4, 1, 6, 9, 5]; %randperm(fileCount);
groupDist = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13];
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

% separate labels and features
groupTrainLabelX = groupTrain(:, [1]);
groupTrainLabelY = groupTrain(:, [2]);
groupTrainFeatures = groupTrain(:, 3:columns(groupTrain));
groupTestLabelX = groupTest(:, [1]);
groupTestLabelY = groupTest(:, [2]);
groupTestFeatures = groupTest(:, 3:columns(groupTest));

libsvmwrite(strcat('output/', outPrefix, '-trainX.scaled'), groupTrainLabelX, sparse(groupTrainFeatures));
libsvmwrite(strcat('output/', outPrefix, '-trainY.scaled'), groupTrainLabelY, sparse(groupTrainFeatures));
libsvmwrite(strcat('output/', outPrefix, '-testX.scaled'), groupTestLabelX, sparse(groupTestFeatures));
libsvmwrite(strcat('output/', outPrefix, '-testY.scaled'), groupTestLabelY, sparse(groupTestFeatures));

%if (doGridSearch == 1)
%function [bestc, bestg] = gridSearch(svmParams, labels, features, minLogC, maxLogC, stepLogC, minLogG, maxLogG, stepLogG)
%  bestcv = 999999;
%  bestc = 99999;
%  bestg = 99999;
%  for log2c = minLogC:stepLogC:maxLogC
%    for log2g = minLogG:stepLogG:maxLogG
%      cmd = [svmParams, ' -v 5 -c ', num2str(2^log2c), ' -g ', num2str(2^log2g)];
%      cv = svmtrain(labels, features, cmd);
%      if (cv < bestcv)
%        bestcv = cv; bestc = 2^log2c; bestg = 2^log2g;
%      end
%      fprintf('%g %g %g (best c=%g, g=%g, rate=%g)\n', log2c, log2g, cv, bestc, bestg, bestcv);
%    end
%  end
%end
%  % parameter tuning
%  [bestcX, bestgX] = gridSearch(svmParams, groupTrainLabelX, groupTrainFeatures, gridMinC, gridMaxC, gridStepC, gridMinG, gridMaxG, gridStepG);
%  [bestcY, bestgY] = gridSearch(svmParams, groupTrainLabelY, groupTrainFeatures, gridMinC, gridMaxC, gridStepC, gridMinG, gridMaxG, gridStepG);
%end

% train model and test
%function [predicted, model] = trainAndTestModel(svmParams, c, g, trainLabels, trainFeatures, testLabels, testFeatures)
%  cmd = [svmParams, ' -c ', num2str(c), ' -g ', num2str(g)];
%  model = svmtrain(trainLabels, trainFeatures, cmd);
%  [predicted] = svmpredict(testLabels, testFeatures, model);
%end

%fprintf('-----X-----\n');
%[predictedX, modelX] = trainAndTestModel(svmParams, bestcX, bestgX, groupTrainLabelX, groupTrainFeatures, groupTestLabelX, groupTestFeatures);
%fprintf('-----Y-----\n');
%[predictedY, modelY] = trainAndTestModel(svmParams, bestcY, bestgY, groupTrainLabelY, groupTrainFeatures, groupTestLabelY, groupTestFeatures);
%
%std(predictedX)
%std(predictedY)


% convert to movie using 'ffmpeg -i "%5d.png" -y output.mpeg'
%if (makeMovie == 1)
%  mkdir('/tmp/UROP/');
%  mkdir('/tmp/UROP/octave/');
%  mkdir('/tmp/UROP/octave/output/');
%  set(0, 'defaultfigurevisible', 'off');
%  for i=1:rows(predictedX)
%    clf
%    axis([-2, 2 -2 2]);
%    rectangle('Position',[predictedX(i) predictedY(i) 0.1 0.1],'Curvature',[1,1], 'FaceColor', 'green');
%    rectangle('Position',[groupTestLabelX(i) groupTestLabelY(i) 0.1 0.1],'Curvature',[1,1], 'FaceColor', 'red');
%    filename=sprintf('/tmp/UROP/octave/output/%05d.png',i);
%    print(filename);
%  end
%end