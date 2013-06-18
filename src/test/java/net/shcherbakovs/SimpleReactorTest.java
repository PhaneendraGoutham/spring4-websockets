package net.shcherbakovs;

import static reactor.fn.Functions.$;

import static org.junit.Assert.*;
import org.junit.Test;

import reactor.R;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Supplier;

public class SimpleReactorTest {

	private String data;
	
	@Test
	public void test() {
		Reactor reactor = R.reactor().sync().get();
		
		reactor.on($("test"), new Consumer<Event<String>>() {
			public void accept(Event<String> ev) {
				data = ev.getData();
			}
		});

		reactor.notify("test", new Supplier<Event<String>>() {
			public Event<String> get() {
				return Event.wrap("Hello, world!");
			}
		});
		
		assertEquals("Hello, world!", data);
	}
}
