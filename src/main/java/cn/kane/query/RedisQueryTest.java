package cn.kane.query;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import redis.clients.jedis.Jedis;
import junit.framework.TestCase;

public class RedisQueryTest extends TestCase {

	static Jedis client = null ;
	
	static{
		client = new Jedis("127.0.0.1", 6379);
		System.out.println("-------------- INFO -------------");
		System.out.println(client.info());
	}
	
	public void testQuery(){
		Random random = new Random() ;
		for(int i = 0 ;i<500;i++){
			String queryKey = random.nextInt(1000000)+"";
			long startMills = System.currentTimeMillis() ;
			client.get(queryKey);
			System.out.println(System.currentTimeMillis() - startMills);
		}
	}
	
	public void testQueryMultiThd(){
		final Random random = new Random() ;
		ExecutorService exeThdPools = Executors.newFixedThreadPool(10);
		exeThdPools.execute(new Runnable(){
			public void run() {
				for(int i = 0 ;i<500;i++){
					String queryKey = random.nextInt(1000000)+"";
					long startMills = System.currentTimeMillis() ;
					client.get(queryKey);
					System.out.println(System.currentTimeMillis() - startMills);
				}
			}
		});
		
	}
	
	public  static void main(String[] args){
		
		for(int i=0 ;i<50;i++){
			new Thread(new Runnable(){
				Random random = new Random() ;
				Jedis client = new Jedis("127.0.0.1", 6379); 
				public void run(){
					for(int i = 0 ;i<1000;i++){
						String queryKey = random.nextInt(1000000)+"";
						long startMills = System.currentTimeMillis() ;
						client.get(queryKey);
						System.out.println(System.currentTimeMillis() - startMills);
					}
				}
			}).start();
		}
	}
}
