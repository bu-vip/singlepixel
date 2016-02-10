'use strict';

angular.module('myApp.view2', ['ngRoute', 'sensorService'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/view2', {
    templateUrl: 'view2/view2.html',
    controller: 'View2Ctrl'
  });
}])

.controller('View2Ctrl', ['$scope', 'SensorService', function($scope, SensorService) {
    $scope.groups = SensorService.groups;
}]);