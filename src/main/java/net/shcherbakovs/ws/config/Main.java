package net.shcherbakovs.ws.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
		ctx.start();
		ctx.close();
	}

}
