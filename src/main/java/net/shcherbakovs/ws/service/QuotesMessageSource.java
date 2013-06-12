/**
 * 
 */
package net.shcherbakovs.ws.service;

import java.util.EventListener;

import net.shcherbakovs.ws.domain.Quote;

/**
 * @author "Sergey Shcherbakov"
 *
 */
public interface QuotesMessageSource {
	
	public interface Listener extends EventListener {
		void onQuoteMessage(Quote m);
	}

	void processMessage( Quote message );
	void subscribe(String symbol, Listener listener);
	void unsubscribe(Listener listener);
	void unsubscribe(String payload, Listener listener);
}
