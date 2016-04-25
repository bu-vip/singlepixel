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
[259,108,0,52,16,0,0,0,0],
[83,1361,168,12,121,15,0,0,0],
[0,246,798,0,6,90,0,0,0],
[78,12,0,1967,229,0,172,9,0],
[11,157,24,161,1661,262,33,138,47],
[0,6,104,1,191,1550,5,12,99],
[0,1,0,124,30,0,1402,228,0],
[0,0,0,4,109,34,154,1622,216],
[0,0,0,0,50,123,4,178,993]
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
