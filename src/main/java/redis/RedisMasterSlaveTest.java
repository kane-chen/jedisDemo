package redis;  
   
import java.util.List;  
import java.util.UUID;  
   
import redis.clients.jedis.Jedis;  
   
/** 
* @author Andy 
*/  
public class RedisMasterSlaveTest {  
  
    private static final String HOST = "";  
    private static final int PORT = 0;  
  
    /** 
     * 添加测试数据 
     */  
     static void setData(Jedis jedis) {  
  
        for (int i = 0; i < 100; i++) {  
            final String a = UUID.randomUUID().toString();  
            jedis.set(a, a);  
        }  
    }  
  
    /** 
     * dbsize 数据库key总数 
     */  
     static long getDBSize(Jedis jedis) {  
        return jedis.dbSize();  
    }  
  
    /** 
     * 查询持久化策略 
     */  
     static List<String> getSaveConfig(Jedis jedis) {  
        return jedis.configGet("save");  
    }  
  
    /** 
     * 设置持久化策略 
     */  
     static String setSaveConfig(Jedis jedis) {  
        String celue_1 = "800 1";  
        String celue_2 = "400 2";  
        return jedis.configSet("save", celue_1 + " " + celue_2);  
    }  
  
    /** 
     * 阻塞IO后持久化数据然后关闭redis (shutdown) 
     */  
     static String shutdown(Jedis jedis) {  
        return jedis.shutdown();  
    }  
  
    /** 
     * 将此redis设置为master主库 
     */  
     static String slaveofNoOne(Jedis jedis) {  
        return jedis.slaveofNoOne();  
    }  
  
    /** 
     * 将此redis根据host/port设置为slaveof从库 
     */  
     static String slaveof(Jedis jedis) {  
        return jedis.slaveof(HOST, PORT);  
    }  
  
    /** 
     * 查询redis的info信息 
     */  
     static String info(Jedis jedis) {  
        return jedis.info();  
    }  
  
    /** 
     * select? 
     */  
     static String select(Jedis jedis) {  
        return jedis.select(1);  
    }  
  
} 