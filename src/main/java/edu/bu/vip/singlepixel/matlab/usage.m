% Example usage of MQTT Sensor Java -> Matlab Interface
% Note: Jar must be compiled for proper Java version

% Configure java
javaclasspath('-dynamic');
javaaddpath('./matlabmqtt_deploy.jar');
% javaaddpath(fullfile(matlabroot, 'matlabmqtt_deploy.jar'));
p = javaclasspath

% Create the reader
topicPrefix = '';
hostname = 'tcp://test.mosquitto.org:1883';
numReadings = 12;
reader = javaObject('edu.bu.vip.singlepixel.matlab.MatlabMqtt', topicPrefix, hostname, numReadings);

% Start the reader
reader.start();

% Run forever
while 1
    % Get a map containing the past readings for each sensor
    readings = reader.getReadings();
    pause(1)
    % TODO: Break out of loop somehow
end

% Stop the reader
reader.stop();