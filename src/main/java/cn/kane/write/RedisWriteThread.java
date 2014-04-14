package cn.kane.write;

import redis.clients.jedis.Jedis;

public class RedisWriteThread implements Runnable{

//	private String serverIp = null ;
//	private int serverPort = -1 ;
//	private String password = null ;
	
	private Jedis client = null ;
	private final int MAX_SIZE = 1000000 ;
	
	public RedisWriteThread(String serverIp,int serverPort,String password) throws Exception{
		if(null == serverIp || serverPort<1 || serverPort > 65534){
			throw new Exception("serverIp or serverPort is wrong!");
		}
//		this.serverIp = serverIp ;
//		this.serverPort = serverPort ;
//		this.password = password ;
		client = new Jedis(serverIp, serverPort);
//		client.auth(password);
		System.out.println("-------------- INFO -------------");
		System.out.println(client.info());
	}
	
	public void run() {
		System.out.println(System.currentTimeMillis());
		for(int i = 1 ; i <= MAX_SIZE ; i++){
			client.set(i+"", i+"D");
//			client.del(i+"");
		}
		System.out.println(System.currentTimeMillis());
		System.out.println(client.info());
	}

	public static void main(String[] args){
		try {
			new Thread(new RedisWriteThread("127.0.0.1",6379,"kane")).run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
