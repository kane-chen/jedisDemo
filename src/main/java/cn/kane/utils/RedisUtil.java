package cn.kane.utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.Transaction;

public class RedisUtil {  
  
    // 数据源  
    private ShardedJedisPool shardedJedisPool;  
  
    /** 
     * 执行器，{@link com.futurefleet.framework.base.redis.RedisUtil}的辅助类， 
     * 它保证在执行操作之后释放数据源returnResource(jedis) 
     * @version V1.0 
     * @author fengjc 
     * @param <T> 
     */  
    abstract class Executor<T> {  
  
        ShardedJedis jedis = null;  
        ShardedJedisPool shardedJedisPool = null;  
  
        public Executor(ShardedJedisPool shardedJedisPool) {  
            this.shardedJedisPool = shardedJedisPool;  
            jedis = this.shardedJedisPool.getResource();  
        }  
  
        /** 
         * 回调 
         * @return 执行结果 
         */  
        abstract T execute();  
  
        /** 
         * 调用{@link #execute()}并返回执行结果 
         * 它保证在执行{@link #execute()}之后释放数据源returnResource(jedis) 
         * @return 执行结果 
         */  
        public T getResult() {  
            T result = null;  
            try {  
                result = execute();  
            } catch (Exception e) {  
                throw new RuntimeException("Redis execute exception", e);  
            } finally {  
                if (jedis != null) {  
                    shardedJedisPool.returnResource(jedis);  
                }  
            }  
            return result;  
        }  
    }  
  
    /** 
     * 删除模糊匹配的key 
     * @param likeKey 模糊匹配的key 
     * @return 删除成功的条数 
     */  
    public long delKeysLike(final String likeKey) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                Collection<Jedis> jedisC = jedis.getAllShards();  
                Iterator<Jedis> iter = jedisC.iterator();  
                long count = 0;  
                while (iter.hasNext()) {  
                    Jedis _jedis = iter.next();  
                    Set<String> keys = _jedis.keys(likeKey + "*");  
                    String[] strKeys = new String[keys.size()];  
                    keys.toArray(strKeys);  
                    count += _jedis.del(strKeys);  
                }  
                return count;  
            }  
        }.getResult();  
    }  
  
    /** 
     * 删除 
     * @param key 匹配的key 
     * @return 删除成功的条数 
     */  
    public Long delByKey(final String key) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                return jedis.del(key);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。 
     * 在 Redis 中，带有生存时间的 key 被称为『可挥发』(volatile)的。 
     * @param key 
     * @param expire 生命周期，单位为秒 
     * @return 1: 设置成功 0: 已经超时或key不存在 
     */  
    public Long expire(final String key, final int expire) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                return jedis.expire(key, expire);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 一个跨jvm的id生成器，利用了redis原子性操作的特点 
     * @param key id的key 
     * @return 返回生成的Id 
     */  
    public long getID(final String key) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                long id = jedis.incr(key);  
                if ((id + 75807) == Long.MAX_VALUE) {  
                    // 避免溢出，重置，getSet命令之前允许incr插队，75807就是预留的插队空间  
                    jedis.getSet(key, "0");  
                }  
                return id;  
            }  
        }.getResult();  
    }  
  
    /* ======================================Strings====================================== */  
  
    /** 
     * 将字符串值 value 关联到 key 。 
     * 如果 key 已经持有其他值， setString 就覆写旧值，无视类型。 
     * 对于某个原本带有生存时间（TTL）的键来说， 当 setString 成功在这个键上执行时， 这个键原有的 TTL 将被清除。 
     * 时间复杂度：O(1) 
     * @param key 
     * @param value 
     * @return 在设置操作成功完成时，才返回 OK 。 
     */  
    public String setString(final String key, final String value) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                return jedis.set(key, value);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 将值 value 关联到 key ，并将 key 的生存时间设为 expire (以秒为单位)。 
     * 如果 key 已经存在， 将覆写旧值。 
     * 类似于以下两个命令: 
     * SET key value 
     * EXPIRE key expire # 设置生存时间 
     * 不同之处是， {@link #setString(String, String, int)} 是一个原子性(atomic)操作，关联值和设置生存时间两个动作会在同一时间内完成，在 Redis 用作缓存时，非常实用。 
     * 时间复杂度：O(1) 
     * @param key 
     * @param value 
     * @param expire 
     * @return 设置成功时返回 OK 。当 expire 参数不合法时，返回一个错误。 
     */  
    public String setString(final String key, final String value, final int expire) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                return jedis.setex(key, expire, value);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 将 key 的值设为 value ，当且仅当 key 不存在。若给定的 key 已经存在，则 setStringIfNotExists 不做任何动作。 
     * 时间复杂度：O(1) 
     * @param key 
     * @param value 
     * @return 设置成功，返回 1 。设置失败，返回 0 。 
     */  
    public Long setStringIfNotExists(final String key, final String value) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                return jedis.setnx(key, value);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回 key 所关联的字符串值。如果 key 不存在那么返回特殊值 nil 。 
     * 假如 key 储存的值不是字符串类型，返回一个错误，因为 getString 只能用于处理字符串值。 
     * 时间复杂度: O(1) 
     * @param key 
     * @return 当 key 不存在时，返回 nil ，否则，返回 key 的值。如果 key 不是字符串类型，那么返回一个错误。 
     */  
    public String getString(final String key) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                return jedis.get(key);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的 {@link #setString(String, String)} 
     * @param pairs 键值对数组{数组第一个元素为key，第二个元素为value} 
     * @return 操作状态的集合 
     */  
    public List<Object> batchSetString(final List<Pair<String, String>> pairs) {  
        return new Executor<List<Object>>(shardedJedisPool) {  
  
            @Override  
            List<Object> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                for (Pair<String, String> pair : pairs) {  
                    pipeline.set(pair.getKey(), pair.getValue());  
                }  
                return pipeline.syncAndReturnAll();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的 {@link #getString(String)} 
     * @param keys 
     * @return value的集合 
     */  
    public List<String> batchGetString(final List<String> keys) {  
        return new Executor<List<String>>(shardedJedisPool) {  
  
            @Override  
            List<String> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                List<String> result = new ArrayList<String>(keys.size());  
                List<Response<String>> responses = new ArrayList<Response<String>>(keys.size());  
                for (String key : keys) {  
                    responses.add(pipeline.get(key));  
                }  
                pipeline.sync();  
                for (Response<String> resp : responses) {  
                    result.add(resp.get());  
                }  
                return result;  
            }  
        }.getResult();  
    }  
  
    /* ======================================Hashes====================================== */  
  
    /** 
     * 将哈希表 key 中的域 field 的值设为 value 。 
     * 如果 key 不存在，一个新的哈希表被创建并进行 hashSet 操作。 
     * 如果域 field 已经存在于哈希表中，旧值将被覆盖。 
     * 时间复杂度: O(1) 
     * @param key 
     * @param field 
     * @param value 
     * @return 如果 field 是哈希表中的一个新建域，并且值设置成功，返回 1 。如果哈希表中域 field 已经存在且旧值已被新值覆盖，返回 0 。 
     */  
    public Long hashSet(final String key, final String field, final String value) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                return jedis.hset(key, field, value);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 将哈希表 key 中的域 field 的值设为 value 。 
     * 如果 key 不存在，一个新的哈希表被创建并进行 hashSet 操作。 
     * 如果域 field 已经存在于哈希表中，旧值将被覆盖。 
     * @param key 
     * @param field 
     * @param value 
     * @param expire 生命周期，单位为秒 
     * @return 如果 field 是哈希表中的一个新建域，并且值设置成功，返回 1 。如果哈希表中域 field 已经存在且旧值已被新值覆盖，返回 0 。 
     */  
    public Long hashSet(final String key, final String field, final String value, final int expire) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                Pipeline pipeline = jedis.getShard(key).pipelined();  
                Response<Long> result = pipeline.hset(key, field, value);  
                pipeline.expire(key, expire);  
                pipeline.sync();  
                return result.get();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回哈希表 key 中给定域 field 的值。 
     * 时间复杂度:O(1) 
     * @param key 
     * @param field 
     * @return 给定域的值。当给定域不存在或是给定 key 不存在时，返回 nil 。 
     */  
    public String hashGet(final String key, final String field) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                return jedis.hget(key, field);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回哈希表 key 中给定域 field 的值。 如果哈希表 key 存在，同时设置这个 key 的生存时间 
     * @param key 
     * @param field 
     * @param expire 生命周期，单位为秒 
     * @return 给定域的值。当给定域不存在或是给定 key 不存在时，返回 nil 。 
     */  
    public String hashGet(final String key, final String field, final int expire) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                Pipeline pipeline = jedis.getShard(key).pipelined();  
                Response<String> result = pipeline.hget(key, field);  
                pipeline.expire(key, expire);  
                pipeline.sync();  
                return result.get();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中。 
     * 时间复杂度: O(N) (N为fields的数量) 
     * @param key 
     * @param hash 
     * @return 如果命令执行成功，返回 OK 。当 key 不是哈希表(hash)类型时，返回一个错误。 
     */  
    public String hashMultipleSet(final String key, final Map<String, String> hash) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                return jedis.hmset(key, hash);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中。同时设置这个 key 的生存时间 
     * @param key 
     * @param hash 
     * @param expire 生命周期，单位为秒 
     * @return 如果命令执行成功，返回 OK 。当 key 不是哈希表(hash)类型时，返回一个错误。 
     */  
    public String hashMultipleSet(final String key, final Map<String, String> hash, final int expire) {  
        return new Executor<String>(shardedJedisPool) {  
  
            @Override  
            String execute() {  
                Pipeline pipeline = jedis.getShard(key).pipelined();  
                Response<String> result = pipeline.hmset(key, hash);  
                pipeline.expire(key, expire);  
                pipeline.sync();  
                return result.get();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回哈希表 key 中，一个或多个给定域的值。如果给定的域不存在于哈希表，那么返回一个 nil 值。 
     * 时间复杂度: O(N) (N为fields的数量) 
     * @param key 
     * @param fields 
     * @return 一个包含多个给定域的关联值的表，表值的排列顺序和给定域参数的请求顺序一样。 
     */  
    public List<String> hashMultipleGet(final String key, final String... fields) {  
        return new Executor<List<String>>(shardedJedisPool) {  
  
            @Override  
            List<String> execute() {  
                return jedis.hmget(key, fields);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回哈希表 key 中，一个或多个给定域的值。如果给定的域不存在于哈希表，那么返回一个 nil 值。 
     * 同时设置这个 key 的生存时间 
     * @param key 
     * @param fields 
     * @param expire 生命周期，单位为秒 
     * @return 一个包含多个给定域的关联值的表，表值的排列顺序和给定域参数的请求顺序一样。 
     */  
    public List<String> hashMultipleGet(final String key, final int expire, final String... fields) {  
        return new Executor<List<String>>(shardedJedisPool) {  
  
            @Override  
            List<String> execute() {  
                Pipeline pipeline = jedis.getShard(key).pipelined();  
                Response<List<String>> result = pipeline.hmget(key, fields);  
                pipeline.expire(key, expire);  
                pipeline.sync();  
                return result.get();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的{@link #hashMultipleSet(String, Map)}，在管道中执行 
     * @param pairs 多个hash的多个field 
     * @return 操作状态的集合 
     */  
    public List<Object> batchHashMultipleSet(final List<Pair<String, Map<String, String>>> pairs) {  
        return new Executor<List<Object>>(shardedJedisPool) {  
  
            @Override  
            List<Object> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                for (Pair<String, Map<String, String>> pair : pairs) {  
                    pipeline.hmset(pair.getKey(), pair.getValue());  
                }  
                return pipeline.syncAndReturnAll();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的{@link #hashMultipleSet(String, Map)}，在管道中执行 
     * @param data Map<String, Map<String, String>>格式的数据 
     * @return 操作状态的集合 
     */  
    public List<Object> batchHashMultipleSet(final Map<String, Map<String, String>> data) {  
        return new Executor<List<Object>>(shardedJedisPool) {  
  
            @Override  
            List<Object> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                for (Map.Entry<String, Map<String, String>> iter : data.entrySet()) {  
                    pipeline.hmset(iter.getKey(), iter.getValue());  
                }  
                return pipeline.syncAndReturnAll();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的{@link #hashMultipleGet(String, String...)}，在管道中执行 
     * @param pairs 多个hash的多个field 
     * @return 执行结果的集合 
     */  
    public List<List<String>> batchHashMultipleGet(final List<Pair<String, String[]>> pairs) {  
        return new Executor<List<List<String>>>(shardedJedisPool) {  
  
            @Override  
            List<List<String>> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                List<List<String>> result = new ArrayList<List<String>>(pairs.size());  
                List<Response<List<String>>> responses = new ArrayList<Response<List<String>>>(pairs.size());  
                for (Pair<String, String[]> pair : pairs) {  
                    responses.add(pipeline.hmget(pair.getKey(), pair.getValue()));  
                }  
                pipeline.sync();  
                for (Response<List<String>> resp : responses) {  
                    result.add(resp.get());  
                }  
                return result;  
            }  
        }.getResult();  
  
    }  
  
    /** 
     * 返回哈希表 key 中，所有的域和值。在返回值里，紧跟每个域名(field name)之后是域的值(value)，所以返回值的长度是哈希表大小的两倍。 
     * 时间复杂度: O(N) 
     * @param key 
     * @return 以列表形式返回哈希表的域和域的值。若 key 不存在，返回空列表。 
     */  
    public Map<String, String> hashGetAll(final String key) {  
        return new Executor<Map<String, String>>(shardedJedisPool) {  
  
            @Override  
            Map<String, String> execute() {  
                return jedis.hgetAll(key);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回哈希表 key 中，所有的域和值。在返回值里，紧跟每个域名(field name)之后是域的值(value)，所以返回值的长度是哈希表大小的两倍。 
     * 同时设置这个 key 的生存时间 
     * @param key 
     * @param expire 生命周期，单位为秒 
     * @return 以列表形式返回哈希表的域和域的值。若 key 不存在，返回空列表。 
     */  
    public Map<String, String> hashGetAll(final String key, final int expire) {  
        return new Executor<Map<String, String>>(shardedJedisPool) {  
  
            @Override  
            Map<String, String> execute() {  
                Pipeline pipeline = jedis.getShard(key).pipelined();  
                Response<Map<String, String>> result = pipeline.hgetAll(key);  
                pipeline.expire(key, expire);  
                pipeline.sync();  
                return result.get();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的{@link #hashMultipleGet(String, String...)} 
     * @param keys 
     * @return 执行结果的集合 
     */  
    public List<Map<String, String>> batchHashGetAll(final List<String> keys) {  
        return new Executor<List<Map<String, String>>>(shardedJedisPool) {  
  
            @Override  
            List<Map<String, String>> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                List<Map<String, String>> result = new ArrayList<Map<String,String>>(keys.size());  
                List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String,String>>>(keys.size());  
                for (String key : keys) {  
                    responses.add(pipeline.hgetAll(key));  
                }  
                pipeline.sync();  
                for (Response<Map<String, String>> resp : responses) {  
                    result.add(resp.get());  
                }  
                return result;  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的{@link #hashMultipleGet(String, String...)}，与{@link #batchHashGetAll(List)}不同的是，返回值为Map类型 
     * @param keys 
     * @return 多个hash的所有filed和value 
     */  
    public Map<String, Map<String, String>> batchHashGetAllForMap(final List<String> keys) {  
        return new Executor<Map<String, Map<String, String>>>(shardedJedisPool) {  
  
            @Override  
            Map<String, Map<String, String>> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                Map<String, Map<String, String>> result = new HashMap<String, Map<String,String>>();  
                List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String,String>>>(keys.size());  
                for (String key : keys) {  
                    responses.add(pipeline.hgetAll(key));  
                }  
                pipeline.sync();  
                for (int i = 0; i < keys.size(); ++i) {  
                    result.put(keys.get(i), responses.get(i).get());  
                }  
                return result;  
            }  
        }.getResult();  
    }  
  
    /* ======================================List====================================== */  
  
    /** 
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)。 
     * @param key 
     * @param value 
     * @return 执行 listPushTail 操作后，表的长度 
     */  
    public Long listPushTail(final String key, final String value) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                return jedis.rpush(key, value);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 将一个或多个值 value 插入到列表 key 的表头, 当列表大于指定长度是就对列表进行修剪(trim) 
     * @param key 
     * @param value 
     * @param size 链表超过这个长度就修剪元素 
     * @return 执行 listPushHeadAndTrim 命令后，列表的长度。 
     */  
    public Long listPushHeadAndTrim(final String key, final String value, final long size) {  
        return new Executor<Long>(shardedJedisPool) {  
  
            @Override  
            Long execute() {  
                Pipeline pipeline = jedis.getShard(key).pipelined();  
                Response<Long> result = pipeline.lpush(key, value);  
                // 修剪列表元素, 如果 size - 1 比 end 下标还要大，Redis将 size 的值设置为 end 。  
                pipeline.ltrim(key, 0, size - 1);  
                pipeline.sync();  
                return result.get();  
            }  
        }.getResult();  
    }  
  
    /** 
     * 批量的{@link #listPushTail(String, String)}，以锁的方式实现 
     * @param key 
     * @param values 
     * @param delOld 如果key存在，是否删除它。true 删除；false: 不删除，只是在行尾追加 
     * @return 执行结果的集合 
     */  
    public List<Object> batchListPushTail(final String key, final List<String> values, final boolean delOld) {  
        return new Executor<List<Object>>(shardedJedisPool) {  
  
            @Override  
            List<Object> execute() {  
                List<Object> status = null;  
                if (delOld) {  
                    RedisLock lock = new RedisLock(key, shardedJedisPool);  
                    lock.lock();  
                    try {  
                        Pipeline pipeline = jedis.getShard(key).pipelined();  
                        pipeline.del(key);  
                        for (String value : values) {  
                            pipeline.rpush(key, value);  
                        }  
                        status = pipeline.syncAndReturnAll();  
                    } finally {  
                        lock.unlock();  
                    }  
                } else {  
                    Pipeline pipeline = jedis.getShard(key).pipelined();  
                    for (String value : values) {  
                        pipeline.rpush(key, value);  
                    }  
                    status = pipeline.syncAndReturnAll();  
                }  
                return status;  
            }  
        }.getResult();  
    }  
  
    /** 
     * 同{@link #batchListPushTail(String, List, boolean)},不同的是利用redis的事务特性来实现 
     * @param key 
     * @param values 
     * @return null 
     */  
    public Object updateListInTransaction(final String key, final List<String> values) {  
        return new Executor<Object>(shardedJedisPool) {  
  
            @Override  
            Object execute() {  
                Transaction transaction = jedis.getShard(key).multi();  
                transaction.del(key);  
                for (String value : values) {  
                    transaction.rpush(key, value);  
                }  
                transaction.exec();  
                return null;  
            }  
        }.getResult();  
    }  
  
    /** 
     * 在key对应list的尾部部添加字符串元素,如果key存在，什么也不做 
     * @param key 
     * @param values 
     */  
    public void insertListIfNotExists(final String key, final List<String> values) {  
        new Executor<Object>(shardedJedisPool) {  
  
            @Override  
            Object execute() {  
                RedisLock lock = new RedisLock(key, shardedJedisPool);  
                lock.lock();  
                try {  
                    if (!jedis.exists(key)) {  
                        Pipeline pipeline = jedis.getShard(key).pipelined();  
                        for (String value : values) {  
                            pipeline.rpush(key, value);  
                        }  
                        pipeline.sync();  
                    }  
                } finally {  
                    lock.unlock();  
                }  
                return null;  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回list所有元素，下标从0开始，负值表示从后面计算，-1表示倒数第一个元素，key不存在返回空列表 
     * @param key 
     * @return list所有元素 
     */  
    public List<String> listGetAll(final String key) {  
        return new Executor<List<String>>(shardedJedisPool) {  
  
            @Override  
            List<String> execute() {  
                return jedis.lrange(key, 0, -1);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 返回指定区间内的元素，下标从0开始，负值表示从后面计算，-1表示倒数第一个元素，key不存在返回空列表 
     * @param key 
     * @param beginIndex 下标开始索引（包含） 
     * @param endIndex 下标结束索引（不包含） 
     * @return 指定区间内的元素 
     */  
    public List<String> listRange(final String key, final long beginIndex, final long endIndex) {  
        return new Executor<List<String>>(shardedJedisPool) {  
  
            @Override  
            List<String> execute() {  
                return jedis.lrange(key, beginIndex, endIndex - 1);  
            }  
        }.getResult();  
    }  
  
    /** 
     * 一次获得多个链表的数据 
     * @param keys 
     * @return 执行结果 
     */  
    public Map<String, List<String>> batchGetAllList(final List<String> keys) {  
        return new Executor<Map<String, List<String>>>(shardedJedisPool) {  
  
            @Override  
            Map<String, List<String>> execute() {  
                ShardedJedisPipeline pipeline = jedis.pipelined();  
                Map<String, List<String>> result = new HashMap<String, List<String>>();  
                List<Response<List<String>>> responses = new ArrayList<Response<List<String>>>(keys.size());  
                for (String key : keys) {  
                    responses.add(pipeline.lrange(key, 0, -1));  
                }  
                pipeline.sync();  
                for (int i = 0; i < keys.size(); ++i) {  
                    result.put(keys.get(i), responses.get(i).get());  
                }  
                return result;  
            }  
        }.getResult();  
    }  
  
    /** 
     * 设置数据源 
     * @param shardedJedisPool 数据源 
     */  
    public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {  
        this.shardedJedisPool = shardedJedisPool;  
    }  
  
    /** 
     * 构造Pair键值对 
     * @param key key 
     * @param value value 
     * @return 键值对 
     */  
    public <K, V> Pair<K, V> makePair(K key, V value) {  
        return new Pair<K, V>(key, value);  
    }  
  
    /** 
     * 键值对 
     * @version V1.0 
     * @author fengjc 
     * @param <K> key 
     * @param <V> value 
     */  
    public class Pair<K, V> {  
  
        private K key;  
        private V value;  
  
        public Pair(K key, V value) {  
            this.key = key;  
            this.value = value;  
        }  
  
        public K getKey() {  
            return key;  
        }  
  
        public void setKey(K key) {  
            this.key = key;  
        }  
  
        public V getValue() {  
            return value;  
        }  
  
        public void setValue(V value) {  
            this.value = value;  
        }  
    }  
}  

/**
 * Spring配置文件：
 */
// 
//<?xml version="1.0" encoding="UTF-8" ?>  
//<beans xmlns="http://www.springframework.org/schema/beans"  
//    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:p="http://www.springframework.org/schema/p"  
//    xmlns:context="http://www.springframework.org/schema/context"  
//    xsi:schemaLocation="http://www.springframework.org/schema/beans  
//    http://www.springframework.org/schema/beans/spring-beans-3.0.xsd  
//    http://www.springframework.org/schema/context  
//    http://www.springframework.org/schema/context/spring-context-3.0.xsd">  
//  
//    <!-- POOL配置 -->  
//    <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">  
//        <property name="maxActive" value="${redis.jedisPoolConfig.maxActive}" />  
//        <property name="maxIdle" value="${redis.jedisPoolConfig.maxIdle}" />  
//        <property name="maxWait" value="${redis.jedisPoolConfig.maxWait}" />  
//        <property name="testOnBorrow" value="${redis.jedisPoolConfig.testOnBorrow}" />  
//    </bean>  
//  
//    <!-- jedis shard信息配置 -->  
//    <bean id="jedis.shardInfoCache1" class="redis.clients.jedis.JedisShardInfo">  
//        <constructor-arg index="0" value="${redis.jedis.shardInfoCache1.host}" />  
//        <constructor-arg index="1"  type="int" value="${redis.jedis.shardInfoCache1.port}" />  
//    </bean>  
//    <bean id="jedis.shardInfoCache2" class="redis.clients.jedis.JedisShardInfo">  
//        <constructor-arg index="0" value="${redis.jedis.shardInfoCache2.host}" />  
//        <constructor-arg index="1"  type="int" value="${redis.jedis.shardInfoCache2.port}" />  
//    </bean>  
//  
//    <!-- jedis shard pool配置 -->  
//    <bean id="shardedJedisPoolCache" class="redis.clients.jedis.ShardedJedisPool">  
//        <constructor-arg index="0" ref="jedisPoolConfig" />  
//        <constructor-arg index="1">  
//            <list>  
//                <ref bean="jedis.shardInfoCache1" />  
//                <ref bean="jedis.shardInfoCache2" />  
//            </list>  
//        </constructor-arg>  
//    </bean>  
//  
//    <bean id="redisCache" class="com.**.RedisUtil">  
//        <property name="shardedJedisPool" ref="shardedJedisPoolCache" />  
//    </bean>  
//</beans>  