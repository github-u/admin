package com.platform.utils;

import java.util.Random;

public class EastMoneyUtils {
	
	public static final String PUSH2HIS_SUFFIX = ".push2his.eastmoney.com";
	
	public static final String F10_SUFFIX = "f10.eastmoney.com";
	
	public static final String HTTP_PREFIX = "http://";
	
	public static final String KLINES_URL_PATH = "/api/qt/stock/kline/get";
	
	public static final String PROFIT_URL_PATH = "/NewFinanceAnalysis/lrbAjax";
	
	public static Random RANDOM = new Random();
	
	public static String getPush2HisUrl() {
		int n = RANDOM.nextInt(99);
		return HTTP_PREFIX + n +PUSH2HIS_SUFFIX;
	}
	
	public static String getF10Url() {
		return HTTP_PREFIX + F10_SUFFIX;
	}
	
	public static String getKlinesURLPath() {
		return getPush2HisUrl() + KLINES_URL_PATH;
	}
	
	public static String getProfitURLPath() {
		return getF10Url() + PROFIT_URL_PATH;
	}
	
}

