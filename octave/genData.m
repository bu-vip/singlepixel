clear
graphics_toolkit('fltk');
more off;

% helper functions

% concatenates all takes from a single person into 1 matrix
function [walkData] = concatAllTakes(data, personIndex)
    walkData = [];
    personData = data.(num2str(personIndex));
    for i = 1:personData.takeCount
        walkData = [walkData; personData.(num2str(i))];
    end
end

% splits the data set into labels and features - also makes features sparse
function [labelsX, labelsY, labelsC, features] = splitData(data)
    labelsX = data(:, [1]);
    labelsY = data(:, [2]);
    features = sparse(data(:, 3:columns(data)));
    grid = 3;
    quantX = floor((labelsX .+ 1) .* grid ./ 2.01);
    quantY = floor((labelsY .+ 1) .* grid ./ 2.01) .* grid;
    labelsC = quantX .+ quantY;
end

% writes the training and test sets to files for SVC & SVR
function writeData(name, trainSet, testSet)
    [trainX, trainY, trainC, trainFeatures] = splitData(trainSet);
    [testX, testY, testC, testFeatures] = splitData(testSet);
    libsvmwrite(strcat(name, '-trainX.scaled'), trainX, trainFeatures);
    libsvmwrite(strcat(name, '-trainY.scaled'), trainY, trainFeatures);
    libsvmwrite(strcat(name, '-trainC.scaled'), trainC, trainFeatures);
    libsvmwrite(strcat(name, '-testX.scaled'), testX, testFeatures);
    libsvmwrite(strcat(name, '-testY.scaled'), testY, testFeatures);
    libsvmwrite(strcat(name, '-testC.scaled'), testC, testFeatures);
end

% Assumes that data is structured in the following format:
% dataDirectory
%   Directory "person 1"
%     person1Take1
%     person1Take2
%     ...
%   Directory "person 2"
%     person2Take1
%     ...
dataDirectory = '/home/doug/Desktop/track5-synced/'

% Get files in directory
filelist = readdir (dataDirectory);
personCount = 0;
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

% calculate the min and range of the each take
minimums = [];
maxs = [];
for person = 1:personCount
    personData = data.(num2str(person));
    for take = 1:personData.takeCount
        takeData = personData.(num2str(take));
        takeMins = min(takeData, [], 1);
        takeMaxs = max(takeData, [], 1);
        minimums = [minimums; takeMins];
        maxs = [maxs; takeMaxs];
    end
end
% calculate the global min and range
globalMin = min(minimums, [], 1);
globalRange = max(maxs, [], 1) - globalMin;

% scale the takes between [-1, 1] for SVM purposes
for person = 1:personCount
    personData = data.(num2str(person));
    for take = 1:personData.takeCount
        takeData = personData.(num2str(take));
        takeData = (takeData - repmat(globalMin, size(takeData, 1), 1)) ./ repmat(globalRange, size(takeData, 1), 1);
        takeData = (takeData .* 2) .- 1;
        personData.(num2str(take)) = takeData;
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
    writeData(strcat('output/oneVRest', num2str(i)), trainSet, testSet);
end
