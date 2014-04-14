package cn.kane.write;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;

public class RedisHashWritePerfTest {

	/**
	 * Memory     used_memory:546340(500K) >>> used_memory:112622968(100M)
	 * @param thdSize
	 * @param rcdSize
	 * @param host
	 * @param port
	 */
	void writeHash(int thdSize,int rcdSize,String host,int port){
		final int rcdSizePerThd = rcdSize/thdSize ;
		for(int i=0 ;i<thdSize ;i++){
			final int thdIndex = i ;
			final Jedis client = new Jedis(host,port) ;
			final int fieldsSize = 5 ;
			new Thread(new Runnable(){
				public void run() {
					int startIndex = thdIndex*rcdSizePerThd ;
					Map<String,String> fieldsMap = new HashMap<String,String>(fieldsSize) ; 
					for(int m =0 ;m<rcdSizePerThd;m++){
						int index = startIndex + m ;
						fieldsMap.clear();
						long startMills = System.currentTimeMillis() ;
						for(int n=0;n<fieldsSize;n++){
							fieldsMap.put("F-"+ index + "-" +m, "V" + index + "-" +m);
						}
						client.hmset( "K"+index , fieldsMap);
						System.out.println(Thread.currentThread().getName()+": cost "+(System.currentTimeMillis()-startMills));
					}
				}
			},"Thd-"+i).start();
		}
		
	}
	
	void remHash(int thdSize,int rcdSize,String host,int port){
		final int rcdSizePerThd = rcdSize/thdSize ;
		for(int i=0 ;i<thdSize ;i++){
			final int thdIndex = i ;
			final Jedis client = new Jedis(host,port) ;
			final int fieldsSize = 5 ;
			new Thread(new Runnable(){
				public void run() {
					int startIndex = thdIndex*rcdSizePerThd ;
					String[] fields = new String[fieldsSize] ; 
					for(int m =0 ;m<rcdSizePerThd;m++){
						int index = startIndex + m ;
						long startMills = System.currentTimeMillis() ;
						for(int n=0;n<fieldsSize;n++){
							fields[n] = "F-"+ index + "-" +m;
						}
						client.hdel("K"+index , fields);
						System.out.println(Thread.currentThread().getName()+": cost "+(System.currentTimeMillis()-startMills));
					}
				}
			},"Thd-"+i).start();
		}
	}
	
	public static void main(String[] args){
		RedisHashWritePerfTest hashTest = new RedisHashWritePerfTest() ;
		Jedis client = new Jedis("localhost",6379);
		System.out.println("------------START--------------");
		client.info();
		System.out.println("--------------------------");
		
		hashTest.writeHash(100, 1000000, "localhost", 6379) ;
		System.out.println("--------------END--------------");
		client.info();
		System.out.println("--------------------------");
//		hashTest.remHash(100, 1000000, "localhost", 6379) ;
	}
	
}
