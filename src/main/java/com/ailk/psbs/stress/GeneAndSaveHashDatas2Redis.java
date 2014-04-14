package com.ailk.psbs.stress;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class GeneAndSaveHashDatas2Redis {

	private RedisClientsPool clientPool = null ;
	private int workThdSize = -1 ;
	private int recordsSize = -1 ;
	private boolean insertFlag = true ;
	private int recordSizePerTran = 1000 ;
	
	private CyclicBarrier cycBarrier = new CyclicBarrier(workThdSize+1);
	private static Logger logger = Logger.getLogger(GeneAndSaveHashDatas2Redis.class);
	
	public void insertRecords(){
		if(insertFlag){
			logger.info("try to geneAndSaveRecords with[records="+recordsSize+",workThdSize="+workThdSize+"]");
			geneAndSaveRecords() ;
			try {
				cycBarrier.await();
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			} catch (BrokenBarrierException e) {
				logger.error(e.getMessage());
			}
			logger.info("**********all records save done**********");
		}
	}
	
	private void geneAndSaveRecords(){
		final int rcdSizePerThd = recordsSize/workThdSize ;
		for(int i=0 ;i<workThdSize ;i++){
			final int thdIndex = i ;
			new Thread(new Runnable(){
				StoredDataStructUtils storedDatasUtils = new StoredDataStructUtils() ;
				public void run() {
					final Jedis client = clientPool.getJedis() ;
					long startMills = System.currentTimeMillis() ;
					int startIndex = thdIndex*rcdSizePerThd ;
					/**
					 * one key-value on one transaction
					 */
//					for(int m =0 ;m<rcdSizePerThd;m++){
//						int index = startIndex + m ;
//						String key = storedDatasUtils.geneKey(index) ;
//						if(null!=key){
//							Map<String,String> params = storedDatasUtils.geneParams(index) ;
//							client.hmset( key , params);
//						}
//					}
					
					
					/**
					 * recordSizePerTran key-values on one transaction
					 */
					Pipeline pipeLine = client.pipelined() ;
					for(int m=0; m<rcdSizePerThd;m++){
						int index = startIndex + m ;
						String key = storedDatasUtils.geneKey(index) ;
						if(null!=key){
							Map<String,String> params = storedDatasUtils.geneParams(index) ;
							pipeLine.hmset(key, params);
						}
						if(m%recordSizePerTran==(recordSizePerTran-1) || m == rcdSizePerThd-1)
							pipeLine.sync() ;
					}
					
					logger.info(Thread.currentThread().getName()+": cost >>>>> "+(System.currentTimeMillis()-startMills));
					
					try {
						cycBarrier.await();
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
					} catch (BrokenBarrierException e) {
						logger.error(e.getMessage());
					}
					clientPool.returnJedis(client) ;
				}
			},"Thd-"+i).start();
		}
		
	}
	

	public RedisClientsPool getClientPool() {
		return clientPool;
	}

	public void setClientPool(RedisClientsPool clientPool) {
		this.clientPool = clientPool;
	}
	
	public int getWorkThdSize() {
		return workThdSize;
	}

	public void setWorkThdSize(int workThdSize) {
		this.workThdSize = workThdSize;
	}

	public int getRecordsSize() {
		return recordsSize;
	}

	public void setRecordsSize(int recordsSize) {
		this.recordsSize = recordsSize;
	}

	public boolean isInsertFlag() {
		return insertFlag;
	}

	public void setInsertFlag(boolean insertFlag) {
		this.insertFlag = insertFlag;
	}

	
}
