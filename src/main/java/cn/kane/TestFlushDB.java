package cn.kane;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import redis.clients.jedis.Jedis;

public class TestFlushDB {

	private static String host = "localhost" ;
	private static int port = 6379 ;
	
	private static int thdSize = 500 ;
	private static int queryTimes = 50 ;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final CyclicBarrier barrier = new CyclicBarrier(thdSize+1) ;
		
		for(int i=0;i<thdSize;i++){
			new Thread(new Runnable(){
				Jedis client = new Jedis(host,port) ;
				Random random = new Random() ;
				public void run() {
					try {
						barrier.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
					for(int i=0;i<queryTimes;i++){
						String key = random.nextInt(1000000)+"";
						long startMills = System.currentTimeMillis() ;
						System.out.println(Thread.currentThread().getName()+" : "+(System.currentTimeMillis()-startMills)+" - " + client.get(key));
					}
				}
			},"THD-"+i).start();
		}
		try {
			barrier.await();
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	
		
		new Jedis(host,port).flushDB() ;
		System.out.println("---------------FLUSH-------------");
	}

}
