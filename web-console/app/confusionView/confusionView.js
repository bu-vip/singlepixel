'use strict';

angular.module('myApp.confusionView', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/confusionView', {
    templateUrl: 'confusionView/confusionView.html',
    controller: 'confusionViewCtrl'
  });
}])

.controller('confusionViewCtrl', ['$scope', function($scope) {
    $scope.gridWidth = 3;
    $scope.gridHeight = 3;

    function ConfusionGrid(aId, aConfusionCounts) {
      this.id = aId;
      this.counts = aConfusionCounts;
      this.percents = [];
      this.colors = [];

      //get total counts
      this.total = this.counts.reduce(add, 0);
      function add(a, b) { return a + b; }

      //calculate percents and colors for each count
      for (var i = 0; i < aConfusionCounts.length; i++)
      {
        var percent = (this.counts[i] / this.total).toFixed(2);
        this.percents.push(percent);
        //calculate color
        var color = "";
        if (i != this.id) {
          //red for wrong
          color = "rgba(" + Math.floor(255);
          color += ", " + Math.floor(0);
          color += ", " + Math.floor(0);
          color += ", " + (Math.sqrt(percent)) + ")";
        }
        else {
          //green for correct
          color = "rgba(" + Math.floor(0);
          color += ", " + Math.floor(255);
          color += ", " + Math.floor(0);
          color += ", " + percent + ")";
        }
        this.colors.push(color)
      }
    }

    var confusionMatrix = [
      [305,58,0,8,0,0,0,0,0,],
      [7,355,28,0,9,0,0,0,0,],
      [1,10,452,5,2,3,0,0,0,],
      [6,2,2,271,6,6,5,0,0,],
      [0,6,1,7,216,10,0,1,0,],
      [0,0,6,2,3,336,4,2,12,],
      [0,0,0,16,5,3,345,14,3,],
      [0,0,0,0,13,1,5,335,14,],
      [0,0,0,0,0,8,0,6,559,]
    ];

    $scope.confusionData = [];
    for (var i = 0; i < confusionMatrix.length; i++)
    {
      $scope.confusionData.push(new ConfusionGrid(i, confusionMatrix[i]))
    }


    // Extended answer: http://stackoverflow.com/questions/11873570/angularjs-for-loop-with-numbers-ranges/17124017#17124017
    // By caching the function result, it can become orders of magnitudes more efficient (depending on how big the range is)
    // jsPerf: http://jsperf.com/memoizer-range/9
    $scope.range = (function() {
    	var cache = {};
    	return function(min, max, step) {
    		var isCacheUseful = (max - min) > 70;
    		var cacheKey;

    		if (isCacheUseful) {
    			cacheKey = max + ',' + min + ',' + step;

    			if (cache[cacheKey]) {
    				return cache[cacheKey];
    			}
    		}

    		var _range = [];
    		step = step || 1;
    		for (var i = min; i <= max; i += step) {
    			_range.push(i);
    		}

    		if (isCacheUseful) {
    			cache[cacheKey] = _range;
    		}

    		return _range;
    	};
    })();
}]);
