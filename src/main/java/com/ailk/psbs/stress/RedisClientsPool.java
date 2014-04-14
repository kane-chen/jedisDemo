package com.ailk.psbs.stress;
import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClientsPool {
   
	private JedisPool pool = null ;
  
	private String host = "127.0.0.1";
    private int port = 6379;
    private int timeout = 0;
    private int maxActive = 500;
    private int maxIdle = 200;
    private long maxWait = 1000;

    private static Logger logger = Logger.getLogger(RedisClientsPool.class);

    public void initPool() {
        logger.info("Init Redis Pool");
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxActive(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMaxWait(maxWait);
        config.setTestOnBorrow(false);
        pool = new JedisPool(config, host, port, timeout);// 线程数量限制，IP地址，端口，超时时间
    }

    public Jedis getJedis() {
        if (pool == null)
            initPool();
        return pool.getResource();
    }
    
    public void returnJedis(Jedis jedis) {
        if (jedis != null)
            pool.returnResource(jedis);
    }
    
    public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getMaxActive() {
		return maxActive;
	}

	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}

	public int getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	public long getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

}