# RedisRateLimiter

Redis Based Rate limiter

support control level: Seconds, Minutes,  Hours,  Days

usage:

please set the configuration of redis,change the content fo redis.properties as following:

#change the redis host as your real environment
redis.host=192.168.150.7
redis.port=6379
#redis.password=
redis.timeout=2000
  
redis.maxIdle=300
redis.minIdle=100
redis.maxTotal=600
redis.testOnBorrow=true
redis.testOnReturn=true


detail api usage:

please refer to: https://github.com/tangaiyun/RedisRateLimiter/blob/master/RedisRateLimiter/src/test/java/com/tay/RedisRateLimiter/TestRedisRateLimiter.java
