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
			+ " 	redis.call('expire',KEYS[1],ARGV[1]); "
			+ "     return 1; "
			+ " else"	
			+ " 	if tonumber(current) <= tonumber(ARGV[2]) then "
			+ "     	return 1; "
			+ "		else "
			+ "			return -1; "
			+ "     end "
			+ " end ";
	public static void main(String[] args) throws InterruptedException {
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");
		JedisPool pool = (JedisPool)ac.getBean("jedisPool");
		Jedis jedis = pool.getResource();
		List<String> keys = new ArrayList<String>();
		keys.add("Test0001");
		List<String> vals = new ArrayList<String>();
		vals.add("10");
		vals.add("3");
		
		Object o = jedis.eval(LuaScript, keys, vals);
		System.out.println(o.getClass());
		System.out.println("result:" + o);
		
		while(true) {
			System.out.println("val: " + jedis.get("Test0001"));
			System.out.println("ttl: " + jedis.ttl("Test0001"));
			Thread.sleep(1000);
		}
		
		
	}
}
