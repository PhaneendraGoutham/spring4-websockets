angular.module('quotesModule', ['webSocketService'])
.controller("SymbolsCtrl", function( $scope, $route, $routeParams, WebSocket ){
	$scope.symbols = [];
	
	WebSocket.connect('ws://' + window.location.host + '/spring4-websockets/quotes');

	$scope.addSymbol = function() {
		if($scope.symbol) {		
			var symbol = angular.uppercase($scope.symbol);
			if($.inArray(symbol, $scope.symbols) < 0 ) {
				$scope.symbols.push(symbol);
			}
			$scope.symbol = '';
		}
	};
	
	$scope.removeSymbol = function(sym) {
		$.each($scope.symbols, function(i){
		    if($scope.symbols[i] === sym) $scope.symbols.splice(i,1);
		});
	};
})
.directive('myChart', function ( WebSocket ) {
	return {
		link: function(scope, element, attrs) {
			
			var subscriber = function(packet) {
	        	var series = scope.chart.series[0];
	        	if(series.data.length > 0 && packet.timestamp < series.data[0].x) {
	        		series.setData([]);
	        	}
	        	var shift = series.data.length > 20;
	            series.addPoint([packet.timestamp, packet.close], true, shift);    	
		        //scope.chart.redraw();
			};

			var initChart = function() {
 				scope.chart.setTitle({ text: null });
 				scope.chart.yAxis[0].setTitle({ text: scope.symbol });
 				scope.chart.series[0].name = scope.symbol;
 				
 				WebSocket.subscribe(scope.symbol, subscriber);
 				scope.initialized = true;
			};
			
			attrs.$observe('id', function(value) {
	 			scope.chart = createChart(value);
	 			if(!scope.initialized) {
	 				initChart();
	 			}
			});
	 		
	 		attrs.$observe('myChart', function(value) {
	 			scope.symbol = value;
	 			if(!scope.initialized) {
	 				initChart();
	 			}
			});
	 		
	 		scope.$on("$destroy",function() {
                WebSocket.unsubscribe(subscriber);
            }); 
		}
	}
});


function createChart(divId, symbol) {
	return new Highcharts.Chart({
		chart : {
			renderTo: divId,
//			events : {
//				load : function() {
//					// set up the websocket connection
//					connect('ws://' + window.location.host + '/spring4-websockets/quotes');
//				}
//			}
		},
		
		colors : [
			'green'
		],
		
		legend : {
			enabled: false
		},
		
		xAxis : {
			type: 'datetime'
		},
		
		exporting: {
			enabled: false
		},
		
		series : [{
			data : []
		}]
	});
}
