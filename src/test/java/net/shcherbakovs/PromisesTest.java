package net.shcherbakovs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import reactor.core.Promise;
import reactor.core.Promises;

public class PromisesTest {

	@Test
	public void promiseTest() {
		Promise<String> promise = Promises.<String>defer().sync().get();
		String result  = promise.get();
		assertNull(result);
		
		promise.accept("hello");
		result = promise.get();
		assertEquals("hello", result);
		
	}
}
