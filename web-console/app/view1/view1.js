'use strict';

angular.module('myApp.view1', ['ngRoute', 'sensorService'])

.config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/view1', {
        templateUrl: 'view1/view1.html',
        controller: 'View1Ctrl'
    });
}])

.controller('View1Ctrl', ['$scope', 'SensorService', function ($scope, sensorService) {
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

    $scope.connected = sensorService.isConnected();
    sensorService.addOnConnectListener('view1', function () {
        $scope.$apply(function () {
            $scope.connected = true;
        });
    });

    sensorService.addOnDisconnectListener('view1', function () {
        $scope.$apply(function () {
            $scope.connected = false;
        });
    });

}]);