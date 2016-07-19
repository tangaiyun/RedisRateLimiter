package com.tay.RedisRateLimiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class TestRedisLuaScript1 {
	private static final String LuaScript = " local current; "
			+ " current = redis.call('incr',KEYS[1]); "
			+ " if tonumber(current) == 1 then "
			+ " redis.call('expire',KEYS[1],10) "
			+ " end ";
	public static void main(String[] args) throws InterruptedException {
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");
		JedisPool pool = (JedisPool)ac.getBean("jedisPool");
		Jedis jedis = pool.getResource();
		List<String> keys = new ArrayList<String>();
		keys.add("Test001");
		
		jedis.eval(LuaScript, keys, new ArrayList<String>());
		
		while(true) {
			System.out.println(jedis.get("Test001"));
			System.out.println(jedis.ttl("Test001"));
			Thread.sleep(1000);
		}
		
		
	}
}
