clear
graphics_toolkit('fltk')

syncedData = csvread('full.csv');

sensorCount = (columns(syncedData)-1);

%for index = 1:sensorCount
%  subplot(sensorCount, 1, index)
%  % first row is class
%  plot(syncedData(:,[index + 1]));
%end

%for index = 1:sensorCount
%  subplot(sensorCount, 1, index)
%  % first row is class
%  freqs = fft(syncedData(:,[index + 1]));
%  plot(abs(freqs));
%end

classData = syncedData(:,[1]);

sobel = [-1, 0, 1];
featureData = zeros(rows(syncedData), sensorCount * 2);
for index = 1:sensorCount
  featureData(:, [index * 2 - 1]) = syncedData(:, [index + 1]);
  featureData(:, [index * 2 + 0]) = conv(syncedData(:, [index + 1]), sobel, 'same');
end

windowSize = 3;
featureCount = sensorCount * 2;

result = zeros(rows(featureData) - windowSize, 1 + featureCount * windowSize);
result(:,[1]) = classData(1:(rows(featureData) - windowSize), [1]);
resultIndex = 2;
for index = 1:windowSize
  for feature = 1:featureCount
    result(:,[resultIndex]) = featureData(index:rows(result)+index-1, [feature]);
    resultIndex = resultIndex + 1;
  end
end

csvwrite('windowed.csv', result);

% low pass Butterworth filter with cutoff pi*Wc radians - choose the order of the filter n and cut-off frequency Wc to suit
%[b,a] = butter(n, Wc)
%filtered_data = filter(b,a,original_data);