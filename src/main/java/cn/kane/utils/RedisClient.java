package cn.kane.utils;
import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {
    private static JedisPool pool = null ;
    private static String host = "127.0.0.1";
    private static int port = 6379;
    private static int timeout = 60 * 1000;

    private static int maxActive = 100;
    private static int maxIdle = 20;
    private static long maxWait = 1000;

    public static final int EXPIRE_TIME = 86400;

    private static Logger logger = Logger.getLogger(RedisClient.class);

    public static void initPool() {
        logger.info("Init Redis Pool");
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxActive(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMaxWait(maxWait);
        config.setTestOnBorrow(false);
        pool = new JedisPool(config, host, port, timeout);// 线程数量限制，IP地址，端口，超时时间
    }

    public static Jedis getJedis() {
        if (pool == null)
            initPool();
        return pool.getResource();
    }
    
    public static void returnJedis(Jedis jedis) {
        if (jedis != null)
            pool.returnResource(jedis);
    }
}