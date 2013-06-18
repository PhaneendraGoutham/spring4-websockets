package net.shcherbakovs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static reactor.fn.Functions.$;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import reactor.Fn;
import reactor.R;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.Stream;
import reactor.filter.RoundRobinFilter;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Function;
import reactor.fn.Supplier;
import reactor.fn.dispatch.SynchronousDispatcher;
import reactor.fn.routing.ConsumerFilteringEventRouter;
import reactor.fn.selector.Selector;
import reactor.fn.tuples.Tuple2;

public class ReactorTest {
	
	private Environment testEnv;
	private Reactor reactor = R.reactor().get();
	private Long testValue = 0L;
	
	@Before
	public void setUp() {
		testEnv = new Environment();
		data = "";
		t = null;
		count = 0;
		d1 = d2 = d3 = d4 = false;
	}

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

	@Test
	public void reacotrConfiguredWithRreactor() {
		Reactor reactor = R.reactor().sync().get();
		assertEquals(reactor.getDispatcher(), SynchronousDispatcher.INSTANCE);
		
		Reactor r2 = R.reactor().roundRobinEventRouting().get();
		assertTrue(r2.getEventRouter() instanceof ConsumerFilteringEventRouter);
		assertTrue(((ConsumerFilteringEventRouter)r2.getEventRouter()).getFilter() instanceof RoundRobinFilter);
	}
	
	@Test
	public void reactorRegisteredWithEnvironment() {
		Reactor r1 = R.reactor().using(testEnv).register().get();
		Reactor r2 = testEnv.find(r1.getId());
		
		assertTrue(r1 == r2);
		assertEquals(r1, r2);
		
		r1 = R.reactor().using(testEnv).register("testReactor").get();
		r2 = testEnv.find("testReactor");
		
		assertTrue(r1 == r2);
		assertEquals(r1, r2);
	}
	
	private String data;
	private Thread t;
	private String completion;
	
	@Test
	public void reactorCanDispatchEvents() {
		Reactor reactor = R.reactor().sync().get();
		
		reactor.on($("test"), new Consumer<Event<String>>() {
			public void accept(Event<String> ev) {
				data = ev.getData();
				t = Thread.currentThread();
			}
		});
		
		reactor.notify("test", Event.wrap("Hello, world!"));
		assertEquals("Hello, world!", data);
		assertTrue(Thread.currentThread() == t);
		
		reactor.notify("test", Event.wrap("Hello, World!"), new Consumer<Event<String>>() {
			public void accept(Event<String> it) {
				completion = it.getData();
			}
		});
		assertEquals("Hello, World!", completion);
		
		reactor.notify("test", new Supplier<Event<String>>() {
			public Event<String> get() {
				return Event.wrap("Hello, world!!");
			}
		});
		assertEquals("Hello, world!!", data);
		
		reactor.on(new Consumer<Event<String>>(){
			public void accept(Event<String> t) {
				data = t.getData();
			}
		});
		data = "";
		reactor.notify(Event.wrap("Hello, world!!!"));
		assertEquals("Hello, world!!!", data);
		
		reactor.notify(new Supplier<Event<?>>() {
			public Event<?> get() {
				return Event.wrap("Hello, world!!..");
			}
		});
		assertEquals("Hello, world!!..", data);
		
		data = "something";
		reactor.notify("test");
		Assert.assertNull(data);

	}
	
	@Test
	public void reactorCanSendReceive() {
		Reactor reactor = R.reactor().sync().get();
		Selector sel = $("hello");
		Tuple2<Selector, Object> replyTo = $();
		
		reactor.on(replyTo.getT1(), new Consumer<Event<String>>(){
			public void accept(Event<String> ev) {
				data = ev.getData();
			}
		});
		
		reactor.receive(sel, Fn.function(new Callable<Object>(){
			public Object call() throws Exception {
				return "Hello, world!";
			}
		}));
		
		reactor.send("hello", Event.wrap("Hello,  world!", replyTo.getT2()));
		assertEquals("Hello, world!", data);
	}
	
	@Test
	public void reactorMapsRepliesToStream() throws InterruptedException {
		Reactor r = R.reactor().sync().get();
		
		Stream<List<String>> s = r.map($("hello"), new Function<Event<String>,String>(){
			public String apply(Event<String> ev) {
				return ev.getData().toUpperCase();
			}
		})
		.reduce()
		.setExpectedAcceptCount(1L);
		
		r.notify("hello", Event.wrap("Mutabor"));
		
		List<String> result = s.await();
		
		assertEquals(1, result.size());
		assertEquals("MUTABOR", result.get(0));
	}
	
	private int count = 0;
	
	@Test
	public void reactorCanComposeRepliesToStream() {
		Reactor r = R.reactor().sync().get();
		
		r.receive($("test"), new Function<Event<String>,Integer>(){
			public Integer apply(Event<String> ev) {
				return Integer.parseInt(ev.getData());
			}
		});
		
		Stream<String> s = r.compose("test", Event.wrap("1"));
		assertEquals(1, s.get());
		
		r.receive($("test"), new Function<Event<String>, Integer>(){
			public Integer apply(Event<String> ev) {
				return Integer.parseInt(ev.getData()) * 10;
			}
		});
		
		s = r.compose("test", Event.wrap("1"));
		
		List<String> reducedList = s.reduce().get();
		assertEquals(2, reducedList.size());
		assertEquals(1, reducedList.get(0));
		assertEquals(10, reducedList.get(1));
		
		r.compose("test", Event.wrap("1"), new Consumer<Object>() {
			public void accept(Object t) {
				count++;
			}
		});
		
		assertEquals(2, count);
	}
	
	private boolean d1, d2, d3, d4;
	
	@Test
	public void reactorCanBeLinked() {
		
		Reactor r1 = R.reactor().sync().get();
		Reactor r2 = R.reactor().using(r1).link().get();
		Reactor r3 = R.reactor().using(r1).link().get();
		Reactor r4 = R.reactor().sync().get();

		r1.on($("test"), Fn.<Event<?>>consumer(new Runnable() {
			public void run() {
				d1 = true;
			}
		}));
		r2.on($("test"), Fn.<Event<?>>consumer(new Runnable() {
			public void run() {
				d2 = true;
			}
		}));
		r3.on($("test"), Fn.<Event<?>>consumer(new Runnable() {
			public void run() {
				d3 = true;
			}
		}));
		r4.on($("test"), Fn.<Event<?>>consumer(new Runnable() {
			public void run() {
				d4 = true;
			}
		}));
		
		r1.notify("test", Event.wrap("bob"));
		
		assertTrue(d1 && d2 && d3 && !d4);
		d1 = d2 = d3 = d4 = false;
		
		r2.link(r4);
		r1.notify("test", Event.wrap("bob"));
		
		assertTrue(d1 && d2 && d3 && d4);
		d1 = d2 = d3 = d4 = false;

		r1.unlink(r2);
		r1.notify("test", Event.wrap("bob"));
		
		assertTrue(d1 && !d2 && d3 && !d4);
		d1 = d2 = d3 = d4 = false;

		r2.notify("test", Event.wrap("bob"));
		assertTrue(!d1 && d2 && !d3 && d4);
		d1 = d2 = d3 = d4 = false;
	}
}
