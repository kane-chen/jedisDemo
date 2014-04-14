package cn.kane;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;

public class SimpleClientTest {

	private static Jedis client = null ;
	static{
		client = new Jedis("localhost",6379) ;
//		client.auth("kane");
		System.out.println("------------ INFO -----------");
		System.out.println(client.info());
	}

	
	static void writeString(){
		client.set("key1", "hello");
		System.out.println(client.get("key1"));
	}
	
	static void writeList(){
		System.out.println(client.llen("key"));
		String[] arr = new String[3];
		for(int i=0;i<3;i++){
			arr[i] = i+"D" ;
		}
		client.lpush("key", arr);
		System.out.println(client.llen("key"));
		client.ltrim("key", -1, 0);
//		client.lpush("list1", "val1","var2","var3");
//		System.out.println(client.llen("list1"));
//		System.out.println(client.lindex("list1", 0));
//		System.out.println(client.lpop("list1"));
//		System.out.println(client.lindex("list1", 0));
//		client.ltrim("list1", -1, 0);
//		client.lrem("list1", client.llen("list1"), "*") ;
//		System.out.println(client.llen("list1"));
	}
	
	static void hashTest(){
		String key = "person" ;
		String[] fields = {"id","name","pos"} ;
		String[] values = {"1","t-mac","sf"};
		Map<String,String> kvMap = new HashMap<String,String>() ;
		for(int i =0;i<fields.length;i++){
			kvMap.put(fields[i], values[i]);
		}
		client.hmset(key,kvMap);
		System.out.println(client.hgetAll(key));
		client.hdel(key, fields);
		System.out.println(client.hgetAll(key));
	}
	
	static void sortedSetTest(){
		String key = "sortedSet" ;
		Map<Double,String> sortedValues = new HashMap<Double,String>();
		sortedValues.put(new Double(1), "t-mac");
		sortedValues.put(new Double(7), "camel");
		sortedValues.put(new Double(3), "iven");
		sortedValues.put(new Double(2), "kidds");
		sortedValues.put(new Double(6), "james");
		
		client.zadd(key, sortedValues);
		System.out.println(client.zcount(key, 1, 3));
		System.out.println(client.zrange(key, 1, 3));
		System.out.println(client.zrangeByScoreWithScores(key, 1, 3));
		System.out.println(client.zremrangeByRank(key, 0, -1));
		System.out.println(client.zcard(key));
	}
	
	static void testFlushDB(){
		long startMills = System.currentTimeMillis() ;
		client.flushDB() ;
		System.out.println(System.currentTimeMillis()-startMills);
	}
	
	public static void main(String[] args){
		testFlushDB();
	}
	
}
