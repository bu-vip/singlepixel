clear
graphics_toolkit('fltk');
more off;
pkg load statistics;

% Assumes that data is structured in the following format:
% dataDirectory
%   Directory "person 1"
%     person1Take1
%     person1Take2
%     ...
%   Directory "person 2"
%     person2Take1
%     ...
dataDirectory = 'data/track5/'

% select which sensors to use
% Sensor locations:
%   1 - Corner nearest door
%   2 - Middle door side
%   3 - Corner door side
%   4 - Middle back side
%   5 - Corner back side near windows
%   6 - Corner back side away from windows
%useSensors = [1, 2, 3, 4, 5, 6];
useSensors = [1, 3, 5, 6];

% Prefix for file names
%prefix = 'six';
prefix = 'four';

% helper functions

% concatenates all takes from a single person into 1 matrix
function [walkData] = concatAllTakes(data, personIndex)
    walkData = [];
    personData = data.(num2str(personIndex));
    for i = 1:personData.takeCount
        walkData = [walkData; personData.(num2str(i))];
    end
end

% concatenates the takes in the list and puts the rest in another
function [group, rest] = concatSplitTakes(data, personIndex, takeIndexes)
    group = [];
    rest = [];
    personData = data.(num2str(personIndex));
    for i = 1:personData.takeCount
        if any(i == takeIndexes)
            group = [group; personData.(num2str(i))];
        else
            rest = [rest; personData.(num2str(i))];
        end
    end
end

% splits the data set into labels and features - also makes features sparse
function [labelsX, labelsY, labelsC, features] = splitData(data, sensorsToUse)
    labelsX = data(:, [1]);
    labelsY = data(:, [2]);
    ftCut = data(:, 3:columns(data));
    features = sparse(ftCut(:, sensorsToUse));
    grid = 3;
    quantX = floor((labelsX .+ 1) .* grid ./ 2.01);
    quantY = floor((labelsY .+ 1) .* grid ./ 2.01) .* grid;
    labelsC = quantX .+ quantY;
end

% writes the training and test sets to files for SVC & SVR
function writeData(name, trainSet, testSet, sensorsToUse)
    [trainX, trainY, trainC, trainFeatures] = splitData(trainSet, sensorsToUse);
    [testX, testY, testC, testFeatures] = splitData(testSet, sensorsToUse);
    libsvmwrite(strcat(name, '-trainX.scaled'), trainX, trainFeatures);
    libsvmwrite(strcat(name, '-trainY.scaled'), trainY, trainFeatures);
    libsvmwrite(strcat(name, '-trainC.scaled'), trainC, trainFeatures);
    libsvmwrite(strcat(name, '-testX.scaled'), testX, testFeatures);
    libsvmwrite(strcat(name, '-testY.scaled'), testY, testFeatures);
    libsvmwrite(strcat(name, '-testC.scaled'), testC, testFeatures);
end

% Get files in directory
filelist = readdir (dataDirectory);
personCount = 0;
minTakes = 999999999;
for ii = 1:numel(filelist)
  % skip special files . and ..
  if (regexp (filelist{ii}, "^\\.\\.?$"))
    continue;
  end

  % keep track of the number of people
  personCount = personCount + 1;
  % load the files for the person
  personFiles = readdir(strcat(dataDirectory, filelist{ii}));
  takeCount = 0;
  clear personData
  for j = 1:numel(personFiles)
      % skip special files . and ..
      if (regexp (personFiles{j}, "^\\.\\.?$"))
        continue;
      end

     % load the data file
     take = csvread(strcat(dataDirectory, filelist{ii}, "/", personFiles{j}));
     % increment number of takes read for this person
     takeCount = takeCount + 1;
     % skip first row of CSV with labels
     personData.(num2str(takeCount)) = take(2:rows(take), :);
  end

  % keep track of the min number of takes across all people
  if (takeCount < minTakes)
      minTakes = takeCount;
  end
  % set number of takes this person has
  personData.takeCount = takeCount;
  % save person data
  data.(num2str(personCount)) = personData;
end

% figure out the min number of samples in a walk
minSamplesInWalk = 999999999;
for person = 1:personCount
    personData = data.(num2str(person));
    for take = 1:personData.takeCount
        samples = rows(personData.(num2str(take)));
        if (samples < minSamplesInWalk)
            minSamplesInWalk = samples;
        end
    end
end

% randomly trim walks so that all walks are sample length
for person = 1:personCount
    personData = data.(num2str(person));
    for take = 1:personData.takeCount
        takeData = personData.(num2str(take));
        % trim take data
        startIndex = randi([1, rows(takeData) - minSamplesInWalk + 1]);
        % update personData
        personData.(num2str(take)) = takeData(startIndex:(startIndex + minSamplesInWalk - 1), :);
    end
    % copy modified person data back into data
    data.(num2str(person)) = personData;
end

% generate data splits for 1vRest person wise
oddManOut = randperm(personCount);
for i = 1:personCount
    trainSet = [];
    testSet = [];
    for person = 1:personCount
        if (person == oddManOut(i))
            testSet = concatAllTakes(data, person);
        else
            trainSet = [trainSet; concatAllTakes(data, person)];
        end
    end
    % Rescale data
    trainMins = min(trainSet, [], 1);
    trainRange = max(trainSet, [], 1) - trainMins;
    trainSet = (trainSet - repmat(trainMins, size(trainMins, 1), 1)) ./ repmat(trainRange, size(trainMins, 1), 1);
    testSet = (testSet - repmat(trainMins, size(testSet, 1), 1)) ./ repmat(trainRange, size(testSet, 1), 1);
    % Bound incase test set has larger/smaller values
    testSet(testSet < 0) = 0;
    testSet(testSet > 1) = 1;
    % Write data
    writeData(strcat('output/', prefix, 'Public', num2str(i)), trainSet, testSet, useSensors);
    % Save ranges
    save(strcat('output/', prefix, 'Public', num2str(i), '-ranges.mat'), 'trainMins', 'trainRange');
end

% generate the data split with 3 walks for training and 1 for testing
combinations = combnk(1:minTakes, 3);
for i = 1:rows(combinations)
    trainSet = [];
    testSet = [];
    for person = 1:personCount
        [pTrain, pTest] = concatSplitTakes(data, person, combinations(i, :));
        trainSet = [trainSet; pTrain];
        testSet = [testSet; pTest];
    end
    % Rescale data
    trainMins = min(trainSet, [], 1);
    trainRange = max(trainSet, [], 1) - trainMins;
    trainSet = (trainSet - repmat(trainMins, size(trainMins, 1), 1)) ./ repmat(trainRange, size(trainMins, 1), 1);
    testSet = (testSet - repmat(trainMins, size(testSet, 1), 1)) ./ repmat(trainRange, size(testSet, 1), 1);
    % Bound incase test set has larger/smaller values
    testSet(testSet < 0) = 0;
    testSet(testSet > 1) = 1;
    % Write data
    writeData(strcat('output/', prefix, 'Private', num2str(i)), trainSet, testSet, useSensors);
    % Save ranges
    save(strcat('output/', prefix, 'Private', num2str(i), '-ranges.mat'), 'trainMins', 'trainRange');
end