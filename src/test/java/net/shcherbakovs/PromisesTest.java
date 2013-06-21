package net.shcherbakovs;

import static org.junit.Assert.*;

import org.junit.Test;

import reactor.Fn;
import reactor.core.Promise;
import reactor.core.Promises;
import reactor.fn.Function;

public class PromisesTest {

	@Test
	public void promiseTest() throws InterruptedException {
		Promise<String> promise = Promises.<String>defer().sync().get();
		String result  = promise.get();
		assertNull(result);

		Promise<String> newPromise = promise.then(new Function<String, String>() {
			public String apply(String str) {
				return str.toUpperCase();
			}
		}, Fn.<Throwable>consumer(new Runnable() {
			public void run() {
				fail("Should not reach here");
			}
		}))
		.then(new Function<String, String>() {
			public String apply(String str) {
				return "<" + str + ">";
			}
		}, Fn.<Throwable>consumer(new Runnable() {
			public void run() {
				fail("Should not reach here");
			}
		}));
		
		promise.accept("hello");
		result = newPromise.await();
		assertEquals("<HELLO>", result);
	}
}
