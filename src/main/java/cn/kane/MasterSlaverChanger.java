package cn.kane;

import redis.clients.jedis.Jedis;

public class MasterSlaverChanger {

	private Jedis master = null ;
	private Jedis slaver = null ;
	
	private String masterIp = null ;
	private int masterPort = -1 ;
	private String slaverIp = null ;
	private int slaverPort = -1 ;
	
	public MasterSlaverChanger(String masterIp,int masterPort,String slaverIp,int slaverPort){
		this.masterIp = masterIp ;
		this.masterPort = masterPort ;
		this.slaverIp = slaverIp ;
		this.slaverPort = slaverPort ;
		
		master = new Jedis(masterIp,masterPort) ;
//		master.auth("kane");
		slaver = new Jedis(slaverIp,slaverPort) ;
//		slaver.auth("kane");
	}
	
	public void addDatas(int thdSize,int rcdSize){
		for(int i=0;i<thdSize;i++){
			System.out.println(master.slaveofNoOne());
			final Jedis master = this.master ;
			final int rcdSizePerThd = rcdSize/thdSize ;
			final int startIndex = i*rcdSizePerThd;
			new Thread(new Runnable(){
				public void run() {
					long startMills = System.currentTimeMillis() ;
					for(int i=0 ;i<rcdSizePerThd;i++){
						master.set(startIndex+i+"", startIndex+i+"");
					}
					System.out.println("THD-"+(startIndex/rcdSizePerThd)+": cost "+(System.currentTimeMillis()-startMills));
				}
			}).start();
		}
	}
	
	public void changeMasterSlaver() throws InterruptedException{
		slaver = new Jedis(slaverIp,slaverPort) ;
		slaver.slaveof(masterIp, masterPort);
		long startMills = System.currentTimeMillis() ;
		slaver.sync();//send request[SYNC] >> master
		System.out.println("SYNC[MASTER>>SLAVER]:"+(System.currentTimeMillis()-startMills));
		Thread.sleep(1000);
		
		slaver = new Jedis(slaverIp,slaverPort) ;
		startMills = System.currentTimeMillis() ;
		slaver.slaveofNoOne();//release relation[master--slaver]
		System.out.println("Remove-slaver:"+(System.currentTimeMillis()-startMills));
		slaver = new Jedis(slaverIp,slaverPort) ;
		slaver.set("11","33333");
		
		master = new Jedis(masterIp,masterPort) ;
		startMills = System.currentTimeMillis() ;
		master.slaveof(slaverIp, slaverPort);
		master.sync();
		System.out.println("SYNC[slaver>>master]:"+(System.currentTimeMillis()-startMills));
		Thread.sleep(1000);
		
		master = new Jedis(masterIp,masterPort) ;
		startMills = System.currentTimeMillis() ;
		master.slaveofNoOne();
		System.out.println("Remove-slaver:"+(System.currentTimeMillis()-startMills));
		System.out.println(master.get("11"));
	}
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		MasterSlaverChanger appMain = new MasterSlaverChanger("10.1.252.189",56379,"10.1.252.189",56380);
//		appMain.addDatas(1, 10000);
		appMain.changeMasterSlaver();
	}

}
