package com.ailk.psbs.stress;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StoredDataStructUtils {

	private final Random random = new Random();
	
	private final String phoneNoPrex = "130";
	private final String[] paramNames = {"templetId","areaCode","userName","validates"};
	private final String userNamePrex = "user-" ;
	private final int templetAccounts = 100 ;
	private final int areaCodesAccounts = 20 ;
	private final int maxValidates = 90 ;
	
	public String geneKey(int index){
		String result = null ;
		if(index<10)
			result = phoneNoPrex + "0000000" + index ;
		else if(index<100)
			result = phoneNoPrex + "000000" + index ;
		else if(index<1000)
			result = phoneNoPrex + "00000" + index ;
		else if(index<10000)
			result = phoneNoPrex + "0000" + index ;
		else if(index<100000)
			result = phoneNoPrex + "000" + index ;
		else if(index<1000000)
			result = phoneNoPrex + "00" + index ;
		else if(index<10000000)
			result = phoneNoPrex + "0" + index ;
		else{
			//can not be null
		}
		
		return result ;
	}
	
	public Map<String,String> geneParams(int index){
		Map<String,String> result = new HashMap<String,String>(paramNames.length) ;
		for(int i=0;i<paramNames.length;i++){
			String value = null ;
			switch(i){
				case 0 : value = "Templet-"+random.nextInt(templetAccounts);break ;
				case 1 : value = "AreaCode-"+random.nextInt(areaCodesAccounts)+"";break ;
				case 2 : value = userNamePrex+index;break ;
				case 3 : value = random.nextInt(maxValidates)+"";break ;
			
			}
			result.put(paramNames[i], value);	
		}
		return result ;
	}
}
