package com.tay.RedisRateLimiter;

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
	private static final Meter requests = metrics.meter(MetricRegistry.name(TestRedisRateLimiter.class, "request"));
	private Timer timer = metrics.timer(MetricRegistry.name(TestRedisRateLimiter.class, "response-timer"));

	@Test
	public void testRedisRateLimit() throws InterruptedException {
		reporter.start(3, TimeUnit.SECONDS);
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");
		JedisPool pool = (JedisPool) ac.getBean("jedisPool");
		RedisRateLimiter limiter = new RedisRateLimiter(pool, TimeUnit.MINUTES, 300);
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
			Thread.sleep(1);
		}

	}
}
