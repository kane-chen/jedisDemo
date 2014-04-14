package cn.kane.write;

import redis.clients.jedis.Jedis;

public class RedisWritePerfTest {

	private static int recordSize = 1000000 ;
	private static int thdSize = 100 ;
	private static String host = "127.0.0.1" ;
	private static int port = 6379 ;
	
	/**
	 * RESULT: input 1,000,000 [d,d+D] >>> cost 40,000+ mills;88,769,592 byte
	 * @param recordSize
	 * @param host
	 * @param port
	 */
	public void singelWriteThread(int recordSize,String host,int port){
		Jedis client = new Jedis(host,port) ;
		System.out.println(client.info());
		long startMills = System.currentTimeMillis() ;
		for(int i=1;i<recordSize;i++){
			client.set(i+"", i+"D");
		}
		System.out.println("COST:"+(System.currentTimeMillis()-startMills));
		System.out.println(client.info());
	}
	
	/**
	 * RESULT: REMOVE 1,000,000 >>> cost 56506
	 * @param recordSize
	 * @param host
	 * @param port
	 */
	public void singelRemoveThread(int recordSize,String host,int port){
		Jedis client = new Jedis(host,port) ;
		System.out.println(client.info());
		long startMills = System.currentTimeMillis() ;
		for(int i=1;i<recordSize;i++){
			client.del(i+"");
		}
		System.out.println("COST:"+(System.currentTimeMillis()-startMills));
		System.out.println(client.info());
	}
	
	/**
	 * RESULT: input 1,000,000[d,d+D] with thd-100 >>> cost 23800~23900
	 * @param thdSize
	 * @param rcdSize
	 * @param host
	 * @param port
	 */
	public void multiWriteThread(int thdSize,int rcdSize,String host,int port){
		for(int i=0;i<thdSize;i++){
			final Jedis client = new Jedis(host, port) ;
			final int rcdSizePerThd = rcdSize/thdSize ;
			final int startIndex = i*rcdSizePerThd;
			new Thread(new Runnable(){
				public void run() {
					long startMills = System.currentTimeMillis() ;
					for(int i=0 ;i<rcdSizePerThd;i++){
						client.set(startIndex+i+"", startIndex+i+"D");
					}
					System.out.println("THD-"+(startIndex/rcdSizePerThd)+": cost "+(System.currentTimeMillis()-startMills));
				}
			}).start();
		}
	}
	
	/**
	 *  RESULT: remove 1,000,000[d] with thd-100 >>> cost 21290~21370
	 * @param thdSize
	 * @param rcdSize
	 * @param host
	 * @param port
	 */
	public void multiRemoveThread(int thdSize,int rcdSize,String host,int port){
		for(int i=0;i<thdSize;i++){
			final Jedis client = new Jedis(host,port) ;
			final int rcdSizePerThd = rcdSize/thdSize ;
			final int startIndex = i*rcdSizePerThd ;
			new Thread(new Runnable(){
				public void run() {
					long startMills = System.currentTimeMillis() ;
					for(int i=0 ;i<rcdSizePerThd;i++){
						client.del(startIndex+i+"");
					}
					System.out.println("THD-"+(startIndex/rcdSizePerThd)+": cost "+(System.currentTimeMillis()-startMills));
				}
			}).start();
		}
	}
	
	public static void main(String[] args){
		RedisWritePerfTest perfTest = new RedisWritePerfTest() ;
//		perfTest.singelWriteThread(recordSize, host, port);
//		perfTest.singelRemoveThread(recordSize, host, port);
		perfTest.multiWriteThread(thdSize,recordSize, host, port) ;
//		perfTest.multiRemoveThread(thdSize,recordSize, host, port) ;
	}
	
	
}
