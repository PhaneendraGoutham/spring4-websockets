package net.shcherbakovs;

import static reactor.fn.Functions.$;

import org.junit.Assert;
import org.junit.Test;

import reactor.R;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;

public class ReactorTest {
	
	private Reactor reactor = R.reactor().get();
	private Long testValue = 0L;

	@Test
	public void test() {
		
		reactor.on($("hello"), new Consumer<Event<Long>>(){
			public void accept(Event<Long> t) {
				Assert.assertEquals(42L, t.getData().longValue());
				testValue = t.getData();
			}
		});
		
		reactor.notify("hello", Event.wrap(Long.valueOf(42L)));
		Assert.assertEquals(42L, testValue.longValue());
	}

}
