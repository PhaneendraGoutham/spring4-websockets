package net.shcherbakovs.ws.handler;

import static reactor.fn.Functions.$;

import java.io.IOException;

import net.shcherbakovs.ws.domain.Quote;
import net.shcherbakovs.ws.service.QuotesMessageSource;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QuotesWebSocketHandler extends TextWebSocketHandlerAdapter implements QuotesMessageSource.Listener {
	private static final Logger log = LoggerFactory.getLogger(QuotesWebSocketHandler.class);
	
	@Autowired
	private ObjectMapper objectMapper;

//	@Autowired
//	private QuotesMessageSource quotesMessageSource;

	@Autowired
	private Reactor reactor;
	private Registration<Consumer<Event<Quote>>> reg = null;
	
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
    			//quotesMessageSource.unsubscribe(payload, this);
    			if( reg != null ) {
    				reg.cancel();
    			}
    			log.debug("Unsubscribed {}", payload);
    		}
    		else {
    			//quotesMessageSource.subscribe(payload, this);
    			reg = reactor.on($(payload), new Consumer<Event<Quote>>(){
    				public void accept(Event<Quote> event) {
    					onQuoteMessage(event.getData());
    				}
    			});
    			log.debug("Subscribed {}", payload);
    		}
		}
    }


    @Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    	log.debug("Session {} closed: {}", session, status);
    	//quotesMessageSource.unsubscribe(this);
    	if( reg != null ) {
    		reg.cancel();
    	}
    }


	public void onQuoteMessage(Quote msg) {
		try {
	    	log.trace("Sending message: {}", msg);
			String json = objectMapper.writeValueAsString(msg);
			session.sendMessage(new TextMessage(json));
		} 
		catch (JsonProcessingException ex) {
			log.error(String.format("Failed to dispatch the message %s to the session %s:", msg, session), ex);
		}
		catch (IOException ex) {
			log.error(String.format("Failed to dispatch the message %s to the session %s:", msg, session), ex);
		}
	}

}
