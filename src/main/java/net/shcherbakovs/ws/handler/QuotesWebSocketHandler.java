package net.shcherbakovs.ws.handler;

import static reactor.fn.Functions.$;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.shcherbakovs.ws.domain.Quote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;

import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.registry.Registration;

import com.fasterxml.jackson.databind.ObjectMapper;

public class QuotesWebSocketHandler extends TextWebSocketHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(QuotesWebSocketHandler.class);
	
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private Reactor reactor;
	private Map<String, Registration<Consumer<Event<Quote>>>> regMap = new HashMap<String, Registration<Consumer<Event<Quote>>>>();
	
	private WebSocketSession session;

    @Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    	log.debug("Incoming session: {}", session);
    	this.session = session;
    }

    @Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    	String payload = message.getPayload();
    	
    	if(!payload.equals(".")) {			// <- not a heart beat message
        	log.debug("{}: {}", session, payload);
    		if(payload.startsWith("-")) { 
				unsubscribe(payload);
    			log.debug("Unsubscribed {}", payload);
    		}
    		else {
    			regMap.put(payload, reactor.on($(payload), new Consumer<Event<Quote>>(){
    				public void accept(Event<Quote> event) {
    					onQuoteMessage(event.getData());
    				}
    			}));
    			log.debug("Subscribed {}", payload);
    		}
		}
    }

	private void unsubscribe(String payload) {
		Registration<Consumer<Event<Quote>>> registration = regMap.get(payload.substring(1));
		if( registration != null ) {
			log.debug("Session {} Unsubscribed from {}", session, payload);
			registration.cancel();
		}
	}

	private void unsubscribeAll() {
		for(Map.Entry<String, Registration<Consumer<Event<Quote>>>> entry : regMap.entrySet()) {
			log.debug("Session {} Unsubscribed from {}", session, entry.getKey());
    		entry.getValue().cancel();
    	}
	}

    @Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    	log.debug("Session {} closed: {}", session, status);
    	unsubscribeAll();
    }

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    	log.debug("Transport error: {} Cloding Session {}", exception.getMessage(), session);
    	unsubscribeAll();
	}

	public void onQuoteMessage(Quote msg) {
		try {
	    	log.trace("Sending message: {}", msg);
			String json = objectMapper.writeValueAsString(msg);
			session.sendMessage(new TextMessage(json));
		} 
		catch (IOException ex) {
			log.error(String.format("Failed to dispatch the message %s to the session %s:", msg, session), ex);
		}
		catch (IllegalArgumentException iae) {
			log.error(String.format("Failed to dispatch the message %s to the session %s:", msg, session), iae);
			unsubscribe(msg.getSymbol());
		}
	}

}
