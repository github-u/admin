
package com.platform.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
//import com.google.common.util.concurrent.RateLimiter;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;
import com.platform.service.EastMoneyService;
import com.platform.service.SQLService.VelocityContextKey;
import com.platform.utils.IOUtils;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

public class EastMoneyServiceImpl implements EastMoneyService {

	public static final String PUSH2HIS_PREFIX = "http://";
	
	public static final String PUSH2HIS_SUFFIX = ".push2his.eastmoney.com";
	
	public static final String KLINES_URL_PATH = "/api/qt/stock/kline/get";
	
	public static Random RANDOM = new Random();
	
	public static String getPush2HisUrl() {
		int n = RANDOM.nextInt(99);
		return PUSH2HIS_PREFIX + n +PUSH2HIS_SUFFIX;
	}
	
	private static Logger logger = LoggerFactory.getLogger(EastMoneyServiceImpl.class);
	
	@Override
	public ResultSupport<Map<String, Object>> getKLinesOfMonth(String securitiesCode, String paramStart, String paramEnd) {
		
		ResultSupport<Map<String, Object>> ret = new ResultSupport<Map<String, Object>> ();
		
		String secId = 	securitiesCode.startsWith("300") ? "0." + securitiesCode : 
						securitiesCode.startsWith("000") ? "0." + securitiesCode :
						securitiesCode.startsWith("001") ? "0." + securitiesCode :
						securitiesCode.startsWith("002") ? "0." + securitiesCode : 
						securitiesCode.startsWith("003") ? "0." + securitiesCode : 
						securitiesCode.startsWith("600") ? "1." + securitiesCode : 
						securitiesCode.startsWith("601") ? "1." + securitiesCode : 
						securitiesCode.startsWith("603") ? "1." + securitiesCode : 
						securitiesCode.startsWith("688") ? "1." + securitiesCode : 
						"";
		
		Preconditions.checkArgument(!"".equals(secId), ResultCode.SECURITIES_CODE_ILLEGAL + ", " + securitiesCode);
		
		Map<String, String> param = Maps.newTreeMap();
		param.put("secId", secId);
		param.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
		param.put("fields1", "f1,f2,f3,f4,f5");
		param.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58");
		param.put("klt", "103");
		param.put("fqt", "1");
		param.put("beg", paramStart);
		param.put("end", paramEnd);
		param.put("smplmt", "460");
		param.put("_", String.valueOf(new Date().getTime()));
		
		AtomicBoolean firstParam = new AtomicBoolean(Boolean.TRUE);

		StringBuffer url = param.entrySet().stream().reduce(
				new StringBuffer(getPush2HisUrl() + KLINES_URL_PATH)
				, (s, e) -> {
					if(firstParam.get()) {
						s.append("?").append(e.getKey()).append("=").append(e.getValue());
						firstParam.compareAndSet(Boolean.TRUE, Boolean.FALSE);
					}else {
						s.append("&").append(e.getKey()).append("=").append(e.getValue());
					}
					return s;
				}, (a, b) -> {
					a.append(b);
					return a;
				});
		
		HttpGet httpGet = new HttpGet(url.toString());
        
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("title=" + "EastMoneyService"
                        + "$mode=" + "getKLinesOfMonth"
                        + "$errCode=" + ResultCode.HTTP_STATUS_ILLEGAL
                        + "$errMsg=" + JSON.toJSONString(response.getStatusLine()));
                return ret.fail(ResultCode.HTTP_STATUS_ILLEGAL, JSON.toJSONString(response.getStatusLine()));
            }
            
            String httpEntity = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            
            JSONObject jsonEastMoneyResult = JSON.parseObject(httpEntity);
            JSONObject jsonEastMoneyData = jsonEastMoneyResult.getJSONObject("data");
            
            return ret.success(jsonEastMoneyData);
            
        } catch (Exception e) {
            logger.error("title=" + "EastMoneyService"
                        + "$mode=" + "getKLinesOfMonth"
                        + "$errCode=" + ResultCode.HTTP_CLIENT_EXECUTE_EXCEPTION
                        + "$errMsg=", e);
            return ret.fail(ResultCode.HTTP_CLIENT_EXECUTE_EXCEPTION, e.getMessage());
        } finally {
            HttpClientUtils.closeQuietly(httpClient);
        }
	}
	
	public static void main(String[] args) throws Exception {
		
		//EastMoneyServiceImpl eastMoneyServiceImpl = new EastMoneyServiceImpl();
		
		//Object obj = eastMoneyServiceImpl.getKLinesOfMonth("000001", "20190101", "20191231");
		
		//System.out.println(obj);
		
		monthly();
		
	}

	public static void monthly() throws Exception {
    	
    	List<Pair<String, String>> tsCodePairs = Lists.newArrayList();
    	
    	String tableName = "stock_basic";
    	
    	DataServiceImpl dataServiceImpl = new DataServiceImpl();
    	dataServiceImpl.init();
    	
    	Map<String, Object> selectParams = new HashMap<String, Object>();
    	selectParams.put(VelocityContextKey.Limit, 4000);
    	ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
    	
    	Preconditions.checkArgument(selectRet.isSuccess());
    	
    	tsCodePairs = selectRet.getModel().stream()
    			.map(item->{
    				String tsCode = LangUtil.convert(item.get("ts_code"), String.class);
    				String symbol = LangUtil.convert(item.get("symbol"), String.class);
    				return Pair.of(tsCode, symbol);
    			})
    			.filter(pair -> pair != null && pair.fst != null && pair.snd != null)
    			.collect(Collectors.toList());
    	
    	//RateLimiter rateLimiter = RateLimiter.create(3);
    	for(Pair<String,String> tsCodePair : tsCodePairs) {
    		System.out.println("monthly=" + tsCodePair.fst);
    		
    		//rateLimiter.acquire();
    		try {
    			monthly(tsCodePair.fst, tsCodePair.snd, dataServiceImpl);
            }catch(Exception e) {
                System.out.println("monthly=" + tsCodePair.fst + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    

	public static void monthly(String tsCode, String symbol, DataService dataService) throws Exception {
		
        String tableName = "east_money_monthly";
       
        EastMoneyServiceImpl eastMoneyServiceImpl = new EastMoneyServiceImpl();
        ResultSupport<Map<String, Object>> kLinesOfMonthRet = eastMoneyServiceImpl.getKLinesOfMonth(symbol, "20190101", "20191231");
        
        Map<String, Object> kLinesOfMonth = kLinesOfMonthRet.getModel();
        //List<Map<String, Object>> kLines = (List<Map<String, Object>>) kLinesOfMonth.get("klines");

    	@SuppressWarnings("unchecked")
    	JSONArray kLines =  (JSONArray) kLinesOfMonth.get("klines");
    	
    	List<Map<String, Object>> insertKlines = kLines.stream().map(kline -> {
    		Map<String, Object> param = Maps.newHashMap();
    		param.put("ts_code", tsCode);
    		param.put("code", kLinesOfMonth.get("code"));
    		param.put("name", kLinesOfMonth.get("name"));
    		
    		String[] elems = String.valueOf(kline).split(",");
    		param.put("trade_date", elems[0]);
    		param.put("open", elems[1]);
    		param.put("close", elems[2]);
    		param.put("high", elems[3]);
    		param.put("low", elems[4]);
    		param.put("vol", elems[5]);
    		param.put("amount", elems[6]);
    		param.put("change", elems[7]);
    		return param;
    	})
    	.collect(Collectors.toList());

        for(Map<String, Object> insertKline : insertKlines) {
        	
        	Map<String, Object> selectParams = new HashMap<String, Object>();
        	selectParams.put("ts_code", insertKline.get("ts_code"));
        	selectParams.put("trade_date", insertKline.get("trade_date"));
        	
        	ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
        	if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
        		insertKline.put("id", selectRet.getModel().get(0).get("id"));
        		ResultSupport<Long> updateRet = dataService.update(tableName, insertKline);
        		Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
        	}else {
        		ResultSupport<Long> insertRet = dataService.insert(tableName, insertKline);
        		Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
        	}
        	
        }
    }
}
