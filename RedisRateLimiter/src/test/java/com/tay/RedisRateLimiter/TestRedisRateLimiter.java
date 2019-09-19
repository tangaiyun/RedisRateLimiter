package com.tay.RedisRateLimiter;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import redis.clients.jedis.JedisPool;

public class TestRedisRateLimiter {
	private static final MetricRegistry metrics = new MetricRegistry();
	private static ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).build();
	private static final Meter requests = metrics.meter(MetricRegistry.name(TestRedisRateLimiter.class, "success"));
	private Timer timer = metrics.timer(MetricRegistry.name(TestRedisRateLimiter.class, "totalRequest"));

	@Test
	public void testRedisRateLimit() throws InterruptedException {
		reporter.start(10, TimeUnit.SECONDS);
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");

		Runnable runnable = () -> {
			JedisPool pool = (JedisPool) ac.getBean("jedisPool");
			RedisRateLimiter limiter = new RedisRateLimiter(pool, TimeUnit.MINUTES, 120);
			while (true) {
				boolean flag = false;
				Context context = timer.time();
				if(limiter.acquire("testMKey1")) {
					flag = true;
				}
				context.stop();
				if (flag) {
					requests.mark();
				}
				try {
					Thread.sleep(1);
				}
				catch(Exception e) {

				}
			}
		};
		int threadCount = 10;
		for(int i = 0; i < threadCount; i++ ) {
			Thread t = new Thread(runnable);
			t.start();
		}

		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
	}
}
