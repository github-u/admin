package com.platform.utils;

public class SecuritiesUtils {
	
	public static int getSecuritiesType(String securitiesCode) {
		return 	securitiesCode.startsWith("300") ? 0  : 
			securitiesCode.startsWith("000") ? 0  :
			securitiesCode.startsWith("001") ? 0  :
			securitiesCode.startsWith("002") ? 0  : 
			securitiesCode.startsWith("003") ? 0  : 
			securitiesCode.startsWith("600") ? 1  : 
			securitiesCode.startsWith("601") ? 1  : 
			securitiesCode.startsWith("603") ? 1  : 
			securitiesCode.startsWith("688") ? 1  : 
			-1;
	}
	
	public static String getTuShareSecuritiesCode(String securitiesCode) {
		return 	getSecuritiesType(securitiesCode) == 0 ? securitiesCode + ".SZ" :
				getSecuritiesType(securitiesCode) == 1 ? securitiesCode + ".SH" :
				securitiesCode + ".U";
	}
	
	public static String getEastMoneySecuritiesCode(String securitiesCode) {
		return 	getSecuritiesType(securitiesCode) + "." + securitiesCode;
	}
	
}
