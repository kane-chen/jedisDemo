package cn.kane.write;

import java.util.Random;

import redis.clients.jedis.Jedis;

public class RedisListPerfTest {

	/**
	 * INPUT: thd-10;rcd-100,000 >>> cost 22,000mills ; 16MB
	 * @param thdSize
	 * @param rcdSize
	 * @param host
	 * @param port
	 */
	public void writeList(int thdSize,int rcdSize,String host,int port){
		final int rcdSizePerThd = rcdSize/thdSize ;
		for(int i=0 ;i < thdSize ; i++){
			final Jedis client = new Jedis(host,port);
			final int startIndex = rcdSizePerThd*i ;
			new Thread(new Runnable(){
				public void run() {
					long startMills = System.currentTimeMillis() ;
					for(int j=0 ; j<rcdSizePerThd;j++){
						String key = startIndex + j + "K" ;
						final int rcdSizeInList = 10 ;
//						String[] emValues = new String[rcdSizeInList] ;
						for(int m = 0; m<rcdSizeInList; m++){
//							emValues[m] = key+"-"+m+"D";
							String emValue = key+"-"+m+"D";
							client.lpush(key, emValue);
						}
//						client.lpush(key, emValues) ;
					}
					System.out.println(Thread.currentThread().getName()+": cost "+(System.currentTimeMillis()-startMills));
				}
			},"THD-"+i).start();
		}
	}
	
	/**
	 * INPUT thdSize=100  :   rcdSize=100,000 >>> cost 3130~3160 mills ; rcdSize=1,000,000 >>> cost 24500~24800 mills
	 * @param thdSize
	 * @param rcdSize
	 * @param host
	 * @param port
	 */
	public void multiWriteSingelList(int thdSize,int rcdSize,String host,int port){
		final String key = "multiOpOneList" ;
		final int rcdSizePerThd = rcdSize/thdSize ;
		for(int i=0 ;i < thdSize ; i++){
			final Jedis client = new Jedis(host,port);
			final int startIndex = rcdSizePerThd*i ;
			new Thread(new Runnable(){
				public void run() {
					long startMills = System.currentTimeMillis() ;
					for(int j=0 ; j<rcdSizePerThd;j++){
						String emValue = key+"-"+(startIndex+rcdSizePerThd)+"D";
						client.lpush(key, emValue);
					}
					System.out.println(Thread.currentThread().getName()+": cost "+(System.currentTimeMillis()-startMills));
				}
			},"THD-"+i).start();
		}
	}
	/**
	 * INPUT: thd-100;rcd-100,000;queryTime-100 >>> query-time is n~160 mills
	 * @param thdSize
	 * @param rcdSize
	 * @param opSize
	 * @param host
	 * @param port
	 */
	public void queryList(int thdSize,int rcdSize,int opSize,String host,int port){
		for(int i=0;i<thdSize;i++){
			final Jedis client = new Jedis(host,port) ;
			final int queryTimes = opSize ;
			final int recordsSize = rcdSize ;
			new Thread(new Runnable(){
				Random random = new Random() ;
				public void run() {
					for(int m = 0;m<queryTimes;m++){
						String key = random.nextInt(recordsSize)+"K";
						long startMills = System.currentTimeMillis() ;
						client.lpop(key) ;
						System.out.println(Thread.currentThread().getName()+" : "+(System.currentTimeMillis()-startMills));
					}
				}
			},"THD-"+i).start();
		}
	}
	
	/**
	 * INPUT: rcd-1,000,000;queryTime-500  >>> thd-10 : 120~180mills;thd-100: 280~400 mills
	 * @param thdSize
	 * @param rcdSize
	 * @param opSize
	 * @param host
	 * @param port
	 */
	public void multiQuerySingelList(int thdSize,int rcdSize,int opSize,String host,int port){
		final String key = "multiOpOneList" ;
		for(int i=0;i<thdSize;i++){
			final Jedis client = new Jedis(host,port) ;
			final int queryTimes = opSize ;
			final int recordsSize = rcdSize ;
			new Thread(new Runnable(){
				Random random = new Random() ;
				public void run() {
					for(int m = 0;m<queryTimes;m++){
						int index = random.nextInt(recordsSize);
						long startMills = System.currentTimeMillis() ;
						client.lindex(key, index);
						System.out.println(Thread.currentThread().getName()+" : "+(System.currentTimeMillis()-startMills));
					}
				}
			},"THD-"+i).start();
		}
	}
	
	public static void main(String[] args){
		RedisListPerfTest test = new RedisListPerfTest() ;
//		test.writeList(10, 100000, "127.0.0.1", 6379) ;
//		test.queryList(100, 100000, 100, "127.0.0.1", 6379) ;
//		test.multiWriteSingelList(100, 1000000, "127.0.0.1", 6379) ;
		test.multiQuerySingelList(5, 1000000,500, "127.0.0.1", 6379) ;
	}
	
}
