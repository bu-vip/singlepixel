clear
graphics_toolkit('fltk');
more off;

groupPrefix = '/home/doug/Desktop/UROP/track5/6_results/results/oneVRestWalks';
i = 1;

xActual = csvread(strcat(groupPrefix, num2str(i), '-testX.actual'));
yActual = csvread(strcat(groupPrefix, num2str(i), '-testY.actual'));
xPredicted = csvread(strcat(groupPrefix, num2str(i), '-testX.predicted'));
yPredicted = csvread(strcat(groupPrefix, num2str(i), '-testY.predicted'));

% convert to movie using 'ffmpeg -i "%5d.png" -y output.mpeg'
mkdir('/tmp/UROP/');
mkdir('/tmp/UROP/octave/');
mkdir('/tmp/UROP/octave/output/');
set(0, 'defaultfigurevisible', 'off');
for i=1:rows(xPredicted)
  clf
  axis([-2, 2 -2 2]);
  rectangle('Position',[xPredicted(i) yPredicted(i) 0.1 0.1],'Curvature',[1,1], 'FaceColor', 'red');
  rectangle('Position',[xActual(i) yActual(i) 0.1 0.1],'Curvature',[1,1], 'FaceColor', 'blue');
  filename=sprintf('/tmp/UROP/octave/output/%05d.png',i);
  print(filename);
end