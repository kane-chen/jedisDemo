package cn.kane.write;

import java.util.Random;

import redis.clients.jedis.Jedis;

public class RedisSortedSetWritePerfTest {

	void multiWriteSingelSortedSet(int thdSize,int rcdSize,String host,int port){
		final String key = "multiOpOneSortedSet" ;
		final int rcdSizePerThd = rcdSize/thdSize ;
		for(int i=0;i<thdSize;i++){
			final Jedis client = new Jedis(host,port) ;
			final int startIndex = i*rcdSizePerThd ;
			new Thread(new Runnable(){
				public void run() {
					Random random =new Random();
					long startMills = System.currentTimeMillis() ;
					for(int i=0;i<rcdSizePerThd;i++){
						double score = random.nextInt(100) ;
						client.zadd(key, score, startIndex+i+"V");
					}
					System.out.println(Thread.currentThread().getName()+": cost "+(System.currentTimeMillis()-startMills));
				}
			},"THD-"+i).start();
		}
	}
	
	void queryPerfTest(){
		
	}
	public static void main(String[] args) {
		RedisSortedSetWritePerfTest sortedSetPerf = new RedisSortedSetWritePerfTest();
		sortedSetPerf.multiWriteSingelSortedSet(100, 100000, "localhost", 6379);//cost 5130~5280 mills
	}

}
