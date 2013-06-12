/**
 * 
 */
package net.shcherbakovs.ws.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import net.shcherbakovs.ws.domain.Quote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author "Sergey Shcherbakov"
 *
 */
@Component
public class QuotesPublisher implements QuotesMessageSource {
	private static final Logger log = LoggerFactory.getLogger(QuotesPublisher.class);
	
	private Map<String, Set<QuotesMessageSource.Listener>> symbolListeners = 
			new ConcurrentHashMap<String, Set<QuotesMessageSource.Listener>>();

	public void processMessage( Quote message ) {
		Set<QuotesMessageSource.Listener> listeners = symbolListeners.get(message.getSymbol());
		if(listeners != null) {
			for(QuotesMessageSource.Listener listener : listeners) {
				listener.onQuoteMessage(message);
			}
		}
	}

	public void subscribe(String symbol, Listener listener) {
		Set<QuotesMessageSource.Listener> listeners = symbolListeners.get(symbol);
		if( listeners == null ) {
			listeners = new CopyOnWriteArraySet<QuotesMessageSource.Listener>();
			symbolListeners.put(symbol, listeners);
		}
		listeners.add(listener);
		log.debug("Subscribed {}", symbol);
	}

	public void unsubscribe(String symbol, Listener listener) {
		Set<QuotesMessageSource.Listener> listeners = symbolListeners.get(symbol);
		if( listeners != null ) {
			listeners.remove(listener); 
		}
	}

	public void unsubscribe(Listener listener) {
		for(Map.Entry<String, Set<QuotesMessageSource.Listener>> entry : symbolListeners.entrySet()) {
			entry.getValue().remove(listener);
		}
	}
}
