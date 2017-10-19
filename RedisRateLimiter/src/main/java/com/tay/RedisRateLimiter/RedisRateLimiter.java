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
	private static final String LUA_SECOND_SCRIPT = " local current; "
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
	private static final String LUA_PERIOD_SCRIPT =   " local currentSectionCount;"
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
			+ "     return 1"
			+ "	else "
			+ " 	return -1"	 	
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
					
						List<String> keys = new ArrayList<String>();
						keys.add(keyName);
						List<String> argvs = new ArrayList<String>();
						argvs.add(getExpire() + "");
						argvs.add(permitsPerUnit + "");
						Long val = (Long)jedis.eval(LUA_SECOND_SCRIPT, keys, argvs);
						rtv = (val > 0);
					
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
		String currentScore = jedisTime.get(0);
		//不用UUID是因为UUID是36个字符比较长，下面方法只有20位，而且冲突可能性已很少
		String currentVal = jedisTime.get(0)+jedisTime.get(1)+RandomStringUtils.randomAlphanumeric(4);
		String previousSectionBeginScore = (Long.parseLong(currentScore) - getPeriodSecond()) + "";
		String expires = getExpire()+"";
		List<String> keys = new ArrayList<String>();
		keys.add(keyNames[0]);
		keys.add(keyNames[1]);
		List<String> argvs = new ArrayList<String>();
		argvs.add(currentScore);
		argvs.add(currentVal);
		argvs.add(previousSectionBeginScore);
		argvs.add(expires);
		argvs.add(permitsPerUnit+"");
		Long val = (Long)jedis.eval(LUA_PERIOD_SCRIPT, keys, argvs);
		return (val > 0);
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
	
	private int getPeriodSecond() {
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

}
