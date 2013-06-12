angular.module('webSocketService',[])
	.factory('WebSocket', function() {
		
		var socket;
		var subscribers = {};
		
		return {
			subscribe : function(symbol, messageCallback) {
				console.log('Subscribing to '+symbol);
				if(!subscribers[symbol]) {
					subscribers[symbol] = [];
				}
				subscribers[symbol].push(messageCallback);
				
				socket.send(symbol);
			},
			
			unsubscribe : function(messageCallback) {
				$.each(subscribers, function(symbol, list){
					var i = $.inArray(messageCallback, list);
					if( i>=0 ) {
						list.splice(i, 1);
					}
					if(list.length == 0) {
						console.log('Unsubscribing from '+symbol);
						socket.send('-'+symbol);
					}
				});
			},
			
			connect : function(host) {
			    if ('WebSocket' in window) {
			        socket = new WebSocket(host);
			    } else if ('MozWebSocket' in window) {
			        socket = new MozWebSocket(host);
			    } else {
			        console.log('Error: WebSocket is not supported by this browser.');
			        return;
			    }

			    socket.onopen = function () {
			        // Socket open.. start the game loop.
			        console.log('Info: WebSocket connection opened.');
			        setInterval(function() {
			            // Prevent server read timeout.
			            socket.send('.');
			        }, 5000);
			    };

			    socket.onclose = function () {
			        console.log('Info: WebSocket closed.');
			    };

			    socket.onmessage = function (message) {
			        //console.log('Message: ' + message.data);
			        // _Potential_ security hole, consider using json lib to parse data in production.
			        var packet = eval('(' + message.data + ')');
			        
			        var symbol = packet.symbol;
			        
			        $.each(subscribers[symbol], function(ind, subscriber){			        	
			        	subscriber(packet);
			        });
			    };
			}
		};
	});