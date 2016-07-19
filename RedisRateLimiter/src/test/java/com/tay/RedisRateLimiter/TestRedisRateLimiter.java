package com.tay.RedisRateLimiter;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.JedisPool;

public class TestRedisRateLimiter {
    private static final MetricRegistry metrics = new MetricRegistry();
    private static ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).build();
    private static final Meter requests = metrics.meter(MetricRegistry.name(TestRedisRateLimiter.class, "request"));

	@Test
	public void testRedisRateLimit() throws InterruptedException {
		reporter.start(3, TimeUnit.SECONDS);
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");
		JedisPool pool = (JedisPool)ac.getBean("jedisPool");
		RedisRateLimiter limiter = new RedisRateLimiter(pool, TimeUnit.SECONDS, 120);
		while(true) {
			
			boolean flag = true;
			try {
				limiter.acquire("testKey");
				
			} catch (RateExceedException e) {
				flag = false;
			}
			Thread.sleep(1);
			if(flag) {
				requests.mark();
			}
		}
		
	}
}
