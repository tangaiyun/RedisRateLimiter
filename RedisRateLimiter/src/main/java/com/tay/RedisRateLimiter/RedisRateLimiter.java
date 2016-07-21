package com.tay.RedisRateLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis based Rate limiter
 * 
 * @author Aiyun Tang <aiyun.tang@gmail.com>
 */
 
public class RedisRateLimiter {
	private JedisPool jedisPool;
	private TimeUnit timeUnit;
	private int permitsPerUnit;
	private static final String LuaSecondsScript = " local current; " 
			+ " current = redis.call('incr',KEYS[1]); "
			+ " if tonumber(current) == 1 then " 
			+ " redis.call('expire',KEYS[1],ARGV[1]) " 
			+ " end ";
	private static final String LuaPeriodScript = " local current;"
			+ " redis.call('zadd',KEYS[1],ARGV[1],ARGV[2]);"
			+ "current = redis.call('zcount', KEYS[1], '-inf', '+inf');"
			+ " if tonumber(current) == 1 then "
			+ " redis.call('expire',KEYS[1],ARGV[3]) "
			+ " end ";

	public RedisRateLimiter(JedisPool jedisPool, TimeUnit timeUnit, int permitsPerUnit) {
		this.jedisPool = jedisPool;
		this.timeUnit = timeUnit;
		this.permitsPerUnit = permitsPerUnit;
	}

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public int getPermitsPerSecond() {
		return permitsPerUnit;
	}

	public boolean acquire(String keyPrefix){
		boolean rtv = false;
		if (jedisPool != null) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				if (timeUnit == TimeUnit.SECONDS) {
					String keyName = getKeyName(jedis, keyPrefix);
					String current = jedis.get(keyName);
					if ((current != null) && (Integer.parseInt(current) >= permitsPerUnit)) {
						rtv = false;
					} else {
						List<String> keys = new ArrayList<String>();
						keys.add(keyName);
						List<String> argvs = new ArrayList<String>();
						argvs.add(getExpire() + "");
						jedis.eval(LuaSecondsScript, keys, argvs);
						rtv = true;
					}
				} else if (timeUnit == TimeUnit.MINUTES) {
					rtv = doPeriod(jedis, keyPrefix, 60);
				} else if (timeUnit == TimeUnit.HOURS) {
					rtv = doPeriod(jedis, keyPrefix, 3600);
				} else if (timeUnit == TimeUnit.DAYS) {
					rtv = doPeriod(jedis, keyPrefix, 3600*24);
				}
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
		}
		return rtv;
	}
	private boolean doPeriod(Jedis jedis, String keyPrefix, int period) {
		String[] keyNames = getKeyNames(jedis, keyPrefix);
		//返回2个，第1个是秒计数10位，第2个是微秒6位
		List<String> jedisTime = jedis.time(); 
		String currentSecondIndex = jedisTime.get(0);
		String previousSecondIndex = (Long.parseLong(currentSecondIndex) - period) + "";

		long currentCount = jedis.zcount(keyNames[0], previousSecondIndex, currentSecondIndex)
				+ jedis.zcount(keyNames[1], previousSecondIndex, currentSecondIndex);
		
		if(currentCount >= permitsPerUnit) {
			return false;
		}
		else {
			List<String> keys = new ArrayList<String>();
			keys.add(keyNames[1]);
			List<String> argvs = new ArrayList<String>();
			argvs.add(currentSecondIndex);
			//不用UUID是因为UUID是36个字符比较长，下面方法只有20位，而且冲突可能性已很少
			argvs.add(jedisTime.get(0)+jedisTime.get(1)+RandomStringUtils.randomAlphanumeric(4));
			argvs.add(getExpire()+"");
			jedis.eval(LuaPeriodScript, keys, argvs);
			return true;
		}
	}
	private String getKeyName(Jedis jedis, String keyPrefix) {
		String keyName = null;
		if (timeUnit == TimeUnit.SECONDS) {
			keyName = keyPrefix + ":" + jedis.time().get(0);
		} else if (timeUnit == TimeUnit.MINUTES) {
			keyName = keyPrefix + ":" + Long.parseLong(jedis.time().get(0)) / 60;
		} else if (timeUnit == TimeUnit.HOURS) {
			keyName = keyPrefix + ":" + Long.parseLong(jedis.time().get(0)) / 3600;
		} else if (timeUnit == TimeUnit.DAYS) {
			keyName = keyPrefix + ":" + Long.parseLong(jedis.time().get(0)) / (3600 * 24);
		} else {
			throw new java.lang.IllegalArgumentException("Don't support this TimeUnit: " + timeUnit);
		}
		return keyName;
	}

	private String[] getKeyNames(Jedis jedis, String keyPrefix) {
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

	private int getExpire() {
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
