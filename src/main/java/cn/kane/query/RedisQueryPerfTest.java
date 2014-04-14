package cn.kane.query;

import java.util.Random;

import redis.clients.jedis.Jedis;

public class RedisQueryPerfTest {

	private static String host ="127.0.0.1" ;
	private static int port = 6379 ;
	
	/**
	 * query : total=1,000,000  thd:1 >> 0~1 ; 100 >> 2~4 ; 1000 >> 40~50 ;
	 * @param thdSize
	 * @param queryTimes
	 * @param host
	 * @param port
	 */
	public void multiQueryThread(int thdSize, int queryTimes, String host ,int port){
		final String hostIp = host ;
		final int serverPort = port ;
		final int qTimes = queryTimes ;
		final int rcdSize = 1000000 ;
		
		for(int i =0 ;i< thdSize ; i++){
			new Thread(new Runnable(){
				public void run() {
					Jedis client = new Jedis(hostIp,serverPort);
					Random random = new Random() ;
					String thdName = Thread.currentThread().getName() ;
					for(int i=0 ; i<qTimes ; i++){
						String key = random.nextInt(rcdSize)+"" ;
						long startMills = System.currentTimeMillis() ;
						client.get(key);
						System.out.println(thdName+":"+(System.currentTimeMillis()-startMills));
					}
				}
			},"THD-"+i).start() ;
		}
	}
	
	public static void main(String[] args){
		RedisQueryPerfTest queryPerf = new RedisQueryPerfTest() ;
		queryPerf.multiQueryThread(100, 1000, host, port);
	}
}
