clear all
close all
groupPrefix = 'oneVRest';
i = 1;

xPredicted = csvread(strcat(groupPrefix, num2str(i), '-testX.predicted'));
yPredicted = csvread(strcat(groupPrefix, num2str(i), '-testY.predicted'));
xActual = csvread(strcat(groupPrefix, num2str(i), '-testX.actual'));
yActual = csvread(strcat(groupPrefix, num2str(i), '-testY.actual'));
sensors = csvread(strcat(groupPrefix, num2str(i), '-testX.sensors'));

figure
axis([-1, 1 -1 1]);
h = scatter(xActual(1:971), yActual(1:971),'r');
grid on
box on
xlabel('X','FontSize',15,'FontWeight','bold'),ylabel('Y','FontSize',15,'FontWeight','bold')

% % print('walk_distrib', '-depsc');

%figure
%axis([-1, 1 -1 1]);
%scatter(xActual, yActual, 'r','linewidth',1);
%xlabel('X','FontSize',15,'FontWeight','bold'),ylabel('Y','FontSize',15,'FontWeight','bold')
% print('test_distrib', '-depsc');

tStart = 200;
tEnd   = 500;

figure
hold on;
axis([0 (tEnd - tStart) -1 1]);
plot(xPredicted(tStart:tEnd), 'Color', 'r','LineWidth',2);
plot(xActual(tStart:tEnd), 'Color', 'b','LineWidth',2);
xlabel('Frame','FontSize',15,'FontWeight','bold'),ylabel('X','FontSize',15,'FontWeight','bold')
AX = legend('Estimates','Ground Truth','FontSize',20,'FontWeight','bold')
LEG = findobj(AX,'type','text');
set(LEG,'FontSize',15,'FontWeight','bold')
hold off;
% print('predVActX', '-depsc');

figure
hold on;
axis([0 (tEnd - tStart) -1 1]);
plot(yPredicted(tStart:tEnd), 'Color', 'r','LineWidth',2);
plot(yActual(tStart:tEnd), 'Color', 'b','LineWidth',2);
xlabel('Frame','FontSize',15,'FontWeight','bold'),ylabel('Y','FontSize',15,'FontWeight','bold')
AX = legend('Estimates','Ground Truth','FontSize',20,'FontWeight','bold')
LEG = findobj(AX,'type','text');
set(LEG,'FontSize',15,'FontWeight','bold')
hold off;
% print('predVActY', '-depsc');
%%

colors = {[1.0 0 0] [0 1.0 0] [0 0 1.0] [1.0 0.5 0] [0.5 0 1.0] [0.0 0.5 0.5]};
for i = 1:6
  figure
  hold on;
  plot(sensors(tStart:tEnd, i), 'Color', colors{i}, 'LineWidth',1);
  axis([0 (tEnd - tStart) -1 1]);
  xlabel('Frame','FontSize',15,'FontWeight','bold');
  ylabel('Y','FontSize',15,'FontWeight','bold');
  title(cstrcat('Sensor ', num2str(i-1)));  
  hold off;
end
%print('rawSensors', '-depsc');

