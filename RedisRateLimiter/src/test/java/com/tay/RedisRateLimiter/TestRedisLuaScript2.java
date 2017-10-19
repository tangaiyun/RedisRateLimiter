package com.tay.RedisRateLimiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class TestRedisLuaScript2 {
		
	private static final String LuaScript2 = 
			  " local currentSectionCount;"
			+ " local previosSectionCount;"
			+ " local totalCountInPeriod;"
			
			+ " currentSectionCount = redis.call('zcount', KEYS[2], '-inf', '+inf');"
			+ " previosSectionCount = redis.call('zcount', KEYS[1], ARGV[3], '+inf');"
			+ " totalCountInPeriod = tonumber(currentSectionCount)+tonumber(previosSectionCount);"
			+ " if totalCountInPeriod < tonumber(ARGV[5]) then " 
			+ " 	redis.call('zadd',KEYS[2],ARGV[1],ARGV[2]);"
			+ "		if tonumber(currentSectionCount) == 0 then "
			+ "			redis.call('expire',KEYS[2],ARGV[4]); "
			+ "		end "
			+ "     return totalCountInPeriod"
			+ "	else "
			+ " 	return totalCountInPeriod"	 	
			+ " end ";
	public static void main(String[] args) throws InterruptedException {
		ApplicationContext ac = new ClassPathXmlApplicationContext("root-context.xml");
		JedisPool pool = (JedisPool)ac.getBean("jedisPool");
		Jedis jedis = pool.getResource();
		List<String> jedisTime = jedis.time();
		String currentScore = jedisTime.get(0);
		String currentVal = jedisTime.get(0)+jedisTime.get(1)+RandomStringUtils.randomAlphanumeric(4);
		String[] keyNames =  getKeyNames(jedis, "Test002", TimeUnit.MINUTES);
		String previousSectionBeginScore = (Long.parseLong(currentScore) - getPeriodSecond(TimeUnit.MINUTES)) + "";
		String expires = getExpire(TimeUnit.MINUTES)+"";
		List<String> keys = new ArrayList<String>();
		keys.add(keyNames[0]);
		keys.add(keyNames[1]);
		List<String> values = new ArrayList<String>();
		values.add(currentScore);
		values.add(currentVal);
		values.add(previousSectionBeginScore);
		values.add(expires);
		values.add("10");
		
		System.out.println("evalval:"+jedis.eval(LuaScript2, keys, values));
		
		int count = 1;
		
		while(true) {
			Thread.sleep(1000);
			jedisTime = jedis.time();
			currentScore = jedisTime.get(0);
			currentVal = jedisTime.get(0)+jedisTime.get(1)+RandomStringUtils.randomAlphanumeric(4);
			keyNames =  getKeyNames(jedis, "Test002", TimeUnit.MINUTES);
			previousSectionBeginScore = (Long.parseLong(currentScore) - getPeriodSecond(TimeUnit.MINUTES)) + "";
			expires = getExpire(TimeUnit.MINUTES)+"";
			keys = new ArrayList<String>();
			keys.add(keyNames[0]);
			keys.add(keyNames[1]);
			values = new ArrayList<String>();
			values.add(currentScore);
			values.add(currentVal);
			values.add(previousSectionBeginScore);
			values.add(expires);
			values.add("10");
			
			System.out.println("evalval:"+jedis.eval(LuaScript2, keys, values));
			System.out.println("key: "+keyNames[1] +"expire: " + jedis.ttl(keyNames[1]));
			count++;
			System.out.println();
			System.out.println("---------------------------------------------");
//			System.out.println("total count: " + count);
		}
		
		
		
//		jedis.zadd(keyNames[1], Long.parseLong(currentSecondIndex), currentSecondIndex);
//		if(jedis.zcount(keyNames[1], "-inf", "+inf") == 1) {
//			jedis.expire(keyNames[1], getExpire());
//		}
	}
	
	private static String[] getKeyNames(Jedis jedis, String keyPrefix, TimeUnit timeUnit) {
		String[] keyNames = null;
		if (timeUnit == TimeUnit.MINUTES) {
			long index = Long.parseLong(jedis.time().get(0)) / 60;
			String keyName1 = keyPrefix + ":" + (index - 1);
			String keyName2 = keyPrefix + ":" + index;
			keyNames = new String[] { keyName1, keyName2 };
		} else if (timeUnit == TimeUnit.HOURS) {
			long index = Long.parseLong(jedis.time().get(0)) / 3600;
			String keyName1 = keyPrefix + ":" + (index - 1);
			String keyName2 = keyPrefix + ":" + index;
			keyNames = new String[] { keyName1, keyName2 };
		} else if (timeUnit == TimeUnit.DAYS) {
			long index = Long.parseLong(jedis.time().get(0)) / (3600 * 24);
			String keyName1 = keyPrefix + ":" + (index - 1);
			String keyName2 = keyPrefix + ":" + index;
			keyNames = new String[] { keyName1, keyName2 };
		} else {
			throw new java.lang.IllegalArgumentException("Don't support this TimeUnit: " + timeUnit);
		}
		return keyNames;
	}
	
	private static int getPeriodSecond(TimeUnit timeUnit) {
		if (timeUnit == TimeUnit.MINUTES) {
			return 60;
		} else if (timeUnit == TimeUnit.HOURS) {
			return 3600;
		} else if (timeUnit == TimeUnit.DAYS) {
			return 24*3600;
		} else {
			throw new java.lang.IllegalArgumentException("Don't support this TimeUnit: " + timeUnit);
		}
	}
	private static int getExpire(TimeUnit timeUnit) {
		int expire = 0;
		if (timeUnit == TimeUnit.SECONDS) {
			expire = 10;
		} else if (timeUnit == TimeUnit.MINUTES) {
			expire = 2 * 60 + 10;
		} else if (timeUnit == TimeUnit.HOURS) {
			expire = 2 * 3600 + 10;
		} else if (timeUnit == TimeUnit.DAYS) {
			expire = 2 * 3600 * 24 + 10;
		} else {
			throw new java.lang.IllegalArgumentException("Don't support this TimeUnit: " + timeUnit);
		}
		return expire;
	}
}
