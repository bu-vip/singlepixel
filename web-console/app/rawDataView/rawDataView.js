'use strict';

angular.module('myApp.rawDataView', ['ngRoute', 'sensorService'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/rawDataView', {
    templateUrl: 'rawDataView/rawDataView.html',
    controller: 'rawDataViewCtrl'
  });
}])

.controller('rawDataViewCtrl', ['$scope', 'SensorService', function($scope, SensorService) {
    $scope.groups = SensorService.groups;
}]);