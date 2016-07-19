package com.tay.RedisRateLimiter;

import java.util.concurrent.TimeUnit;

public class RateExceedException extends Exception {
	
	private static final long serialVersionUID = 3490773153161502813L;
	private String key;
	private TimeUnit timeUnit;
	private int permitsPerSecond;
	public RateExceedException(String key, TimeUnit timeUnit, int permitsPerSecond) {
		this.key = key;
		this.setTimeUnit(timeUnit);
		this.permitsPerSecond = permitsPerSecond;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public int getPermitsPerSecond() {
		return permitsPerSecond;
	}
	public void setPermitsPerSecond(int permitsPerSecond) {
		this.permitsPerSecond = permitsPerSecond;
	}
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}
	@Override
	public String toString() {
		return "RateExceedException [key=" + key + ", timeUnit=" + timeUnit + ", permitsPerSecond=" + permitsPerSecond
				+ "]";
	}
	
}
