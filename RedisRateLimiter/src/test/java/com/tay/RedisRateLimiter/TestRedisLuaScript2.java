package com.tay.RedisRateLimiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class TestRedisLuaScript2 {
		
	private static final String LuaScript2 = " local current;"
			+ " redis.call('zadd',KEYS[1],ARGV[1],ARGV[2]);"
			+ "current = redis.call('zcount', KEYS[1], '-inf', '+inf');"
			+ " if tonumber(current) == 1 then "
			+ " redis.call('expire',KEYS[1],ARGV[3]) "
			+ " end ";
	public static void main(String[] args) throws InterruptedException {
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");
		JedisPool pool = (JedisPool)ac.getBean("jedisPool");
		Jedis jedis = pool.getResource();
		List<String> keys = new ArrayList<String>();
		keys.add("Test001");
		List<String> values = new ArrayList<String>();
		values.add("1000");
		values.add("value1");
		values.add("21");
		
		jedis.eval(LuaScript2, keys, values);
		
		while(true) {
			System.out.println(jedis.zcount("Test001", -1, 10000));
			System.out.println(jedis.ttl("Test001"));
			Thread.sleep(1000);
		}
		
		
		
//		jedis.zadd(keyNames[1], Long.parseLong(currentSecondIndex), currentSecondIndex);
//		if(jedis.zcount(keyNames[1], "-inf", "+inf") == 1) {
//			jedis.expire(keyNames[1], getExpire());
//		}
	}
}
