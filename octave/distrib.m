clear
graphics_toolkit('fltk');
more off;

groupPrefix = '/home/doug/Desktop/UROP/track5/results/dataSets/halfHalf';
i = 1;

xActual = csvread(strcat(groupPrefix, num2str(i), '-trainX.actual'));
yActual = csvread(strcat(groupPrefix, num2str(i), '-trainY.actual'));

set(0, 'defaultfigurevisible', 'off');
clf
axis([-2, 2 -2 2]);
for i=1:rows(xActual)
  rectangle('Position', [xActual(i) yActual(i) 0.01 0.01], 'Curvature',[1,1], 'FaceColor', 'red');
  if (mod(i, 100) == 0)
    i
   end
end

print('/home/doug/Desktop/train_distrib.png');