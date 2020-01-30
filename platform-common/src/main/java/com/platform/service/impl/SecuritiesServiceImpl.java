package com.platform.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.LangUtil;

import lombok.Setter;

public class SecuritiesServiceImpl implements SecuritiesService {
	
	@Resource @Setter
	private DataService dataService; 
	
	@Resource @Setter
	private SourceService eastMoneyService;
	
	@Resource @Setter
	private SourceService tuShareServcie;
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesServiceImpl.class);
	
	public void securitiesCodes() {
		
	}
	
	public ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions){
		return getBatch(type, tableName, columnNames, uniqColumnNames, conditions, null);
	}
	
	public ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions, 
			Function<Map<String, Object>, Map<String, Object>> postSourceProcessor){
		
		ResultSupport<Long> ret = new ResultSupport<Long>();
		
		ResultSupport<List<Map<String, Object>>> dataRet = source(type, tableName, columnNames, conditions);
		if(!dataRet.isSuccess()) {
			return ret.fail(dataRet.getErrCode(), dataRet.getErrMsg());
		}
		
		AtomicLong counter = new AtomicLong(0L);
		dataRet.getModel().parallelStream()
		.map(oneSecuritiesTuple ->{
			if(postSourceProcessor == null) {
				return oneSecuritiesTuple;
			}else {
				return postSourceProcessor.apply(oneSecuritiesTuple);
			}
		})
		.forEach(oneSecuritiesTuple->{
			try {
				ResultSupport<Long> saveRet = save(tableName, oneSecuritiesTuple, uniqColumnNames);
				if(!saveRet.isSuccess()) {
					logger.error("title=" + "SecuritiesService"
							+ "$mode=" + "getTotal"
							+ "$errCode=" + ResultCode.SAVE_FAIL
							+ "$table=" + tableName
							+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
							+ "$id=" +  oneSecuritiesTuple.get("id")
							+ "$uniqColumnNames=" + uniqColumnNames
							+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple));
					return;
				}
				
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "getTotal"
						+ "$errCode=" + "SUC"
						+ "$table=" + tableName
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple)
						+ "$id=" +  oneSecuritiesTuple.get("id")); 
				counter.getAndIncrement();
			}catch(Exception e) {
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "getTotal"
						+ "$errCode=" + ResultCode.SAVE_EXCEPTION
						+ "$table=" + tableName
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
						+ "$id=" +  oneSecuritiesTuple.get("id")
						+ "$uniqColumnNames=" + uniqColumnNames
						+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple),
						e);
			}
		});
		
		return ret.success(counter.get());
	}
	
	public ResultSupport<Long> get(String type, String tableName, String securitiesCode, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions){
		
		ResultSupport<Long> ret = new ResultSupport<Long>();
		
		ResultSupport<List<Map<String, Object>>> dataRet = source(type, tableName, securitiesCode, columnNames, conditions);
		if(!dataRet.isSuccess()) {
			return ret.fail(dataRet.getErrCode(), dataRet.getErrMsg());
		}
		
		AtomicLong counter = new AtomicLong(0L);
		dataRet.getModel().parallelStream()
		.forEach(oneSecuritiesTuple->{
			try {
				ResultSupport<Long> saveRet = save(tableName, oneSecuritiesTuple, uniqColumnNames);
				if(!saveRet.isSuccess()) {
					logger.error("title=" + "SecuritiesService"
							+ "$mode=" + "get"
							+ "$errCode=" + ResultCode.SAVE_FAIL
							+ "$table=" + tableName
							+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
							+ "$id=" +  oneSecuritiesTuple.get("id")
							+ "$uniqColumnNames=" + uniqColumnNames
							+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple));
					return;
				}
				
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "get"
						+ "$errCode=" + "SUC"
						+ "$table=" + tableName
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple)
						+ "$id=" +  oneSecuritiesTuple.get("id")); 
				counter.getAndIncrement();
			}catch(Exception e) {
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "get"
						+ "$errCode=" + ResultCode.SAVE_EXCEPTION
						+ "$table=" + tableName
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple)
						+ "$id=" +  oneSecuritiesTuple.get("id")
						+ "$uniqColumnNames=" + uniqColumnNames
						+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple),
						e);
			}
		});
		
		return ret.success(counter.get());
		
	}
	
	public ResultSupport<List<Map<String, Object>>> source(String type, String sourceName,  
			String columnNames, Map<String, Object> conditions) {
		
		SourceService sourceService = sourceService(type);
		
		return sourceService.source(sourceName, columnNames, conditions);
		
	}
	
	public ResultSupport<List<Map<String, Object>>> source(String type, String sourceName, String securitiesCode, 
			String columnNames, Map<String, Object> conditions) {
		
		SourceService sourceService = sourceService(type);
		
		return sourceService.source(sourceName, securitiesCode, columnNames, conditions);
		
	}
	
	private SourceService sourceService(String type) {
		SourceService sourceService = null;
		
		if(Source.EAST_MONEY.equals(type)) {
			sourceService = eastMoneyService;
			
		}else if(Source.TU_SHARE.equals(type)) {
			sourceService = tuShareServcie;
		}
		Preconditions.checkNotNull(sourceService);
		
		return sourceService;
	}
	
	public ResultSupport<Long> save(String tableName, Map<String, Object> params, String uniqColumnNames) {
		Preconditions.checkNotNull(tableName);
		Preconditions.checkNotNull(params);
		Preconditions.checkNotNull(uniqColumnNames);
		
		Preconditions.checkArgument(!"".equals(tableName));
		Preconditions.checkArgument(!params.isEmpty());
		Preconditions.checkArgument(!"".equals(uniqColumnNames));
		
		ResultSupport<Long> ret = new ResultSupport<Long>();
		
		String[] uniqColumnNameArray = uniqColumnNames.split(",");
		Map<String, Object> selectParams = Lists.newArrayList(uniqColumnNameArray).stream()
				.collect(
						Collectors.toMap(
								uniqColumnName -> {
									Preconditions.checkNotNull(uniqColumnName);
									return uniqColumnName;
								},
								uniqColumnName ->{
									Object val = params.get(uniqColumnName);
									Preconditions.checkNotNull(val);
									
									return val;
								})
						);
				
        ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
        
        Long id = null;
        if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
        	id = LangUtil.safeLong(selectRet.getModel().get(0).get("id"));
        	Preconditions.checkNotNull(id);
        	params.put("id", id);
            ResultSupport<Long> updateRet = dataService.update(tableName, params);
            Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
        }else {
            ResultSupport<Long> insertRet = dataService.insert(tableName, params);
            Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            id = insertRet.getModel();
        }
        
		return ret.success(id);
	}
	
	private String getSecuritiesCode(Map<String, Object> oneSecuritiesTuple) {
		return oneSecuritiesTuple.get("code") != null 
				? LangUtil.safeString(oneSecuritiesTuple.get("code"))
				: LangUtil.safeString(oneSecuritiesTuple.get("ts_code"));
	}
	
	public static void main(String[] args) throws Exception {
		
		SecuritiesService securitiesService = new SecuritiesServiceImpl();
		
		DataService dataService = new DataServiceImpl();
		((DataServiceImpl)dataService).init();
		((SecuritiesServiceImpl)securitiesService).setDataService(dataService);
		
		SourceService eastMoneySourceService = new EastMoneyServiceImpl();
		((SecuritiesServiceImpl)securitiesService).setEastMoneyService(eastMoneySourceService);
		
		SourceService tuShareSourceService = new TuShareServiceImpl();
		((TuShareServiceImpl)tuShareSourceService).init();
		((SecuritiesServiceImpl)securitiesService).setTuShareServcie(tuShareSourceService);
		
		String type = Source.EAST_MONEY;
		String name = "east_money_monthly";
		String securitiesCode = "000001";
		String columnNames = "";
		String uniqColumnNames = "ts_code,trade_date";
		Map<String, Object> conditions = Maps.newLinkedHashMap();
		conditions.put("beg", "20190101");
		conditions.put("end", "20191231");
		
		BasicConfigurator.configure(appender());
		ResultSupport<Long> ret = ((SecuritiesServiceImpl)securitiesService).get(type, name, securitiesCode, columnNames, uniqColumnNames, conditions);
		
		System.out.println(ret);
		
	}
	
	public static Appender appender() {
		DailyRollingFileAppender appender = new DailyRollingFileAppender();
		appender.setFile("D:\\working_log\\securities.log");
		appender.setAppend(Boolean.TRUE);
		appender.setThreshold(Level.INFO);
		appender.setEncoding("UTF-8");
		appender.setImmediateFlush(Boolean.TRUE);
		
		org.apache.log4j.PatternLayout layout = new org.apache.log4j.PatternLayout();
		layout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss SSS}|%5p|%F.%M:%L|%m%n");
		appender.setLayout(layout);
		
		appender.activateOptions();
		return appender;
		
	}
	
}
