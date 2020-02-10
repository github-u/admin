package com.platform.service.impl;

import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;
import com.platform.service.SecuritiesService;
import com.platform.service.SQLService.VelocityContextKey;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

import lombok.Setter;

public class SecuritiesServiceImpl implements SecuritiesService {
	
	@Resource @Setter
	private DataService dataService; 
	
	@Setter
	private SourceService eastMoneyService;
	
	@Setter
	private SourceService tuShareService;
	
	public static int MAX_SECURITIES_CODES = 6000;
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesServiceImpl.class);
	
	private static int SECURITIES_LEAVE_DAYS_CACHE_MAX_SIZE = 2;
	
	private static String SECURITIES_LEAVE_DAYS_CACHE_KEY = "SECURITIES_LEAVE_DAYS_CACHE_KEY";
	
	private LoadingCache<String, Set<String>> securitiesLeaveDaysCache = CacheBuilder.newBuilder()
			.maximumSize(SECURITIES_LEAVE_DAYS_CACHE_MAX_SIZE)
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Set<String>>(){

				@Override
				public Set<String> load(String key) throws Exception {
					Preconditions.checkArgument(SECURITIES_LEAVE_DAYS_CACHE_KEY.equals(key));
					return leaveDays(new Date());
				}
						
			});
			
	 
	public ResultSupport<List<String>> getSecuritiesCodes() {
		
		ResultSupport<List<String>> ret = new ResultSupport<List<String>>();
		
		Map<String, Object> selectParams = Maps.newHashMap();
		selectParams.put(VelocityContextKey.Limit, MAX_SECURITIES_CODES);
		
		ResultSupport<List<Map<String, Object>>> selectRet = dataService.select("securities_codes", selectParams);
		if(!selectRet.isSuccess()) {
			return ret.fail(selectRet.getErrCode(), selectRet.getErrMsg());
		}
		
		List<String> securitiesCodes = 
				selectRet.getModel().stream()
				.map(tuple->{
					return LangUtil.convert(tuple.get("code"), String.class);
				})
				.filter(code -> code != null)
				.collect(Collectors.toList());
		
		return ret.success(securitiesCodes);	
		
	}
	
	@Override
	public ResultSupport<Boolean> isTradeDay(Date date) {
		Preconditions.checkNotNull(date);
		
		ResultSupport<Boolean> ret = new ResultSupport<Boolean>();
		int dayOfWeek = DateUtil.getDayOfWeek(date);
		if(dayOfWeek == DayOfWeek.SUNDAY.getValue() || dayOfWeek == DayOfWeek.SATURDAY.getValue()) {
			return ret.success(Boolean.FALSE);
		}
		
		String dataString = DateUtil.getDate(date, DateUtil.DAY_FORMATTER_1) + " 00:00:00";
		try {
			Set<String> leaveDays = securitiesLeaveDaysCache.get(SECURITIES_LEAVE_DAYS_CACHE_KEY);
			if(leaveDays.contains(dataString)) {
				return ret.success(Boolean.FALSE);
			}
			
			return ret.success(Boolean.TRUE);
		} catch (Exception e) {
			logger.error("title=" + "SecuritiesService"
					+ "$mode=" + "isTradeDay"
					+ "$errCode=" + ResultCode.IS_TRADE_DAY_EXCEPTION
					+ "$errMsg=" + date
					,e);
			return ret.fail(ResultCode.IS_TRADE_DAY_EXCEPTION, e.getMessage());
		}
		
	}
	
	public ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions){
		return getBatch(type, tableName, columnNames, uniqColumnNames, conditions, null, null, null, false);
	}
	
	public ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions, 
			Function<String, String> beforeSourceTableNameAliasProcessor,
			Function<Map<String, Object>, List<Map<String, Object>>> postSourceProcessor,
			Function<String, String> postSourceTableNameAliasProcessor,
			boolean parallel){
		
		ResultSupport<Long> ret = new ResultSupport<Long>();
		
		String sourceNameAlias = sourceNameAlias(beforeSourceTableNameAliasProcessor, tableName);
		ResultSupport<List<Map<String, Object>>> dataRet = source(type, sourceNameAlias, columnNames, conditions);
		if(!dataRet.isSuccess()) {
			return ret.fail(dataRet.getErrCode(), dataRet.getErrMsg());
		}
		
		ResultSupport<List<Map<String, Object>>> postSourceProcessRet = postSourceProcess(dataRet.getModel(), 
				postSourceProcessor, tableName, columnNames, uniqColumnNames, parallel);
		if(!postSourceProcessRet.isSuccess()) {
			return ret.fail(postSourceProcessRet.getErrCode(), postSourceProcessRet.getErrMsg());
		}
		
		AtomicLong counter = new AtomicLong(0L);
		String tableNameAlias = postSourceTableNameAliasProcessor != null ? 
				postSourceTableNameAliasProcessor.apply(tableName) : tableName;
		Stream<Map<String, Object>> stream = null;
		if(!parallel) {
			stream = postSourceProcessRet.getModel().stream();
		}else {
			stream = postSourceProcessRet.getModel().parallelStream();
		}
		stream.forEach(oneSecuritiesTuple->{
			try {
				ResultSupport<Long> saveRet = save(tableNameAlias, oneSecuritiesTuple, uniqColumnNames);
				if(!saveRet.isSuccess()) {
					logger.error("title=" + "SecuritiesService"
							+ "$mode=" + "getTotal"
							+ "$errCode=" + ResultCode.SAVE_FAIL
							+ "$table=" + tableNameAlias
							+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
							+ "$id=" +  oneSecuritiesTuple.get("id")
							+ "$uniqColumnNames=" + uniqColumnNames
							+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple));
					return;
				}
				
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "getTotal"
						+ "$errCode=" + "SUC"
						+ "$table=" + tableNameAlias
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple)
						+ "$id=" +  oneSecuritiesTuple.get("id")); 
				counter.getAndIncrement();
			}catch(Exception e) {
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "getTotal"
						+ "$errCode=" + ResultCode.SAVE_EXCEPTION
						+ "$table=" + tableNameAlias
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
		return get(type, tableName, securitiesCode, columnNames, uniqColumnNames, conditions, null, null, null, false);
	}
	
	public ResultSupport<Long> get(String type, String tableName, String securitiesCode, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions,
			Function<String, String> beforeSourceTableNameAliasProcessor,
			Function<Map<String, Object>, List<Map<String, Object>>> postSourceProcessor, 
			Function<String, String> postSourceTableNameAliasProcessor,
			boolean parallel){
		
		ResultSupport<Long> ret = new ResultSupport<Long>();
		
		String sourceNameAlias = sourceNameAlias(beforeSourceTableNameAliasProcessor, tableName);
		ResultSupport<List<Map<String, Object>>> dataRet = source(type, sourceNameAlias, securitiesCode, columnNames, conditions, null);
		if(!dataRet.isSuccess()) {
			return ret.fail(dataRet.getErrCode(), dataRet.getErrMsg());
		}
		
		ResultSupport<List<Map<String, Object>>> postSourceProcessRet = postSourceProcess(dataRet.getModel(), 
				postSourceProcessor, tableName, columnNames, uniqColumnNames, parallel);
		if(!postSourceProcessRet.isSuccess()) {
			return ret.fail(postSourceProcessRet.getErrCode(), postSourceProcessRet.getErrMsg());
		}
		
		String tableNameAlias = postSourceTableNameAliasProcessor != null ? 
				postSourceTableNameAliasProcessor.apply(tableName) : tableName;
		Stream<Map<String, Object>> stream = null;
		if(!parallel) {
			stream = postSourceProcessRet.getModel().stream();
		}else {
			stream = postSourceProcessRet.getModel().parallelStream();
		}
		
		AtomicLong counter = new AtomicLong(0L);
		stream
		.forEach(oneSecuritiesTuple->{
			try {
				ResultSupport<Long> saveRet = save(tableNameAlias, oneSecuritiesTuple, uniqColumnNames);
				if(!saveRet.isSuccess()) {
					logger.error("title=" + "SecuritiesService"
							+ "$mode=" + "get"
							+ "$errCode=" + ResultCode.SAVE_FAIL
							+ "$table=" + tableNameAlias
							+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
							+ "$id=" +  oneSecuritiesTuple.get("id")
							+ "$uniqColumnNames=" + uniqColumnNames
							+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple));
					return;
				}
				
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "get"
						+ "$errCode=" + "SUC"
						+ "$table=" + tableNameAlias
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple)
						+ "$id=" +  oneSecuritiesTuple.get("id")); 
				counter.getAndIncrement();
			}catch(Exception e) {
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "get"
						+ "$errCode=" + ResultCode.SAVE_EXCEPTION
						+ "$table=" + tableNameAlias
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
			String columnNames, Map<String, Object> conditions, Function<String, String> beforeSourceTableNameAliasProcessor) {
		
		SourceService sourceService = sourceService(type);
		
		return sourceService.source(sourceName, securitiesCode, columnNames, conditions);
		
	}
	
	private SourceService sourceService(String type) {
		SourceService sourceService = null;
		
		if(Source.EAST_MONEY.equals(type)) {
			sourceService = eastMoneyService;
			
		}else if(Source.TU_SHARE.equals(type)) {
			sourceService = tuShareService;
		}
		Preconditions.checkNotNull(sourceService);
		
		return sourceService;
	}
	
	private String sourceNameAlias(Function<String, String> beforeSourceTableNameAliasProcessor, String sourceName) {
		String sourceNameAlias = sourceName;
		if(beforeSourceTableNameAliasProcessor != null) {
			sourceNameAlias = beforeSourceTableNameAliasProcessor.apply(sourceName);
		}
		return sourceNameAlias;
	}
	
	private ResultSupport<List<Map<String, Object>>> postSourceProcess(
			List<Map<String, Object>> model, 
			Function<Map<String, Object>, List<Map<String, Object>>> postSourceProcessor,
			String tableName, 
			String columnNames, 
			String uniqColumnNames,
			boolean parallel){
		
		ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
		
		List<Map<String, Object>> transformedModel;
		if(postSourceProcessor != null) {
			try {
				//boolean keepSourceOrder;
				Stream<Map<String, Object>> stream = null;
				if(!parallel) {
					stream = model.stream();
				}else {
					stream = model.parallelStream();
				}
				transformedModel = stream
						.flatMap(oneSecuritiesTuple ->{
							try {
								return postSourceProcessor.apply(oneSecuritiesTuple).stream();
							}catch(Exception e) {
								logger.error("title=" + "SecuritiesService"
										+ "$mode=" + "getBatch"
										+ "$errCode=" + ResultCode.GET_BATCH_POST_SOURCE_TRRANSFER_EXCEPTION
										+ "$table=" + tableName
										+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
										+ "$id=" +  oneSecuritiesTuple.get("id")
										+ "$uniqColumnNames=" + uniqColumnNames
										+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple), e);
								throw e;
							}
						})		
						.collect(Collectors.toList());
			}catch(Exception e) {
				return ret.fail(ResultCode.GET_BATCH_POST_SOURCE_TRRANSFER_EXCEPTION, e.getMessage());
			}
		}else {
			transformedModel = model;
		}
		
		return ret.success(transformedModel);
		
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
	
	private Set<String> leaveDays(Date date) {
		
		Map<String, Object> selectParams = Maps.newHashMap();
		selectParams.put("date", String.valueOf(DateUtil.getYear(date)));
		ResultSupport<List<Map<String, Object>>> selectRet = dataService.select("securities_leave_days", selectParams);
		if(!selectRet.isSuccess()) {
			throw new RuntimeException();
		}
		
		Map<String, Map<String, Object>> leaveDays = 
				selectRet.getModel().stream()
				.map(tuple->{
					return Pair.of(LangUtil.convert(tuple.get("date"), String.class), tuple);
				})
				.filter(code -> code != null)
				.collect(Collectors.toMap(pair -> pair.fst, pair -> pair.snd));
		
		return leaveDays.keySet();
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
		((SecuritiesServiceImpl)securitiesService).setTuShareService(tuShareSourceService);
		
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
