'use strict';

angular.module('myApp.configView', ['ngRoute', 'sensorService'])

.config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/configView', {
        templateUrl: 'configView/configView.html',
        controller: 'configViewCtrl'
    });
}])

.controller('configViewCtrl', ['$scope', 'SensorService', function ($scope, sensorService) {
    $scope.hostname = "localhost";
    $scope.port = "61614";
    $scope.clientId = "webConsole" + Math.floor((Math.random() * 100000) + 1);
    $scope.username = "admin";
    $scope.password = "password";
    $scope.connect = function () {
        if ($scope.hostname && $scope.port && $scope.clientId) {
            sensorService.connect($scope.hostname, $scope.port, $scope.clientId, $scope.username, $scope.password, $scope.prefix);
        }
    }
    
    $scope.disconnect = function () {
        sensorService.disconnect();
    }

    $scope.state = sensorService.state;
}]);