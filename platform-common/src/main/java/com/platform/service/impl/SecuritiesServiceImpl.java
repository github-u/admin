package com.platform.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
	private SourceService eastMoneySourceService;
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesServiceImpl.class);
	
	public ResultSupport<Long> get(String type, String name, String securitiesCode, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions){
		
		ResultSupport<Long> ret = new ResultSupport<Long>();
		
		ResultSupport<List<Map<String, Object>>> dataRet = source(type, name, securitiesCode, columnNames, conditions);
		if(!dataRet.isSuccess()) {
			return ret.fail(dataRet.getErrCode(), dataRet.getErrMsg());
		}
		
		AtomicLong counter = new AtomicLong(0L);
		dataRet.getModel().parallelStream()
		.forEach(oneSecuritiesTuple->{
			try {
				ResultSupport<Long> saveRet = save(name, oneSecuritiesTuple, uniqColumnNames);
				if(!saveRet.isSuccess()) {
					logger.error("title=" + "SecuritiesService"
							+ "$mode=" + "get"
							+ "$errCode=" + ResultCode.SAVE_FAIL
							+ "$table=" + name
							+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
							+ "$uniqColumnNames=" + uniqColumnNames
							+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple));
					return;
				}
				
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "get"
						+ "$errCode=" + "SUC"
						+ "$table=" + name
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple)); 
				counter.getAndIncrement();
			}catch(Exception e) {
				logger.error("title=" + "SecuritiesService"
						+ "$mode=" + "get"
						+ "$errCode=" + ResultCode.SAVE_EXCEPTION
						+ "$table=" + name
						+ "$code=" + getSecuritiesCode(oneSecuritiesTuple) 
						+ "$uniqColumnNames=" + uniqColumnNames
						+ "$oneSecuritiesTuple=" + JSON.toJSONString(oneSecuritiesTuple),
						e);
			}
		});
		
		return ret.success(counter.get());
		
	}
	
	public ResultSupport<List<Map<String, Object>>> source(String type, String sourceName, String securitiesCode, 
			String columnNames, Map<String, Object> conditions) {
		
		SourceService sourceService = null;
		if(Source.EAST_MONEY.equals(type)) {
			sourceService = eastMoneySourceService;
			
		}
		Preconditions.checkNotNull(type);
		
		return sourceService.source(sourceName, securitiesCode, columnNames, conditions);
		
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
	public static void main(String[] args) {
		SecuritiesService securitiesService = new SecuritiesServiceImpl();
		
		DataService dataService = new DataServiceImpl();
		((SecuritiesServiceImpl)securitiesService).setDataService(dataService);
		
		SourceService eastMoneySourceService = new EastMoneyServiceImpl();
		((SecuritiesServiceImpl)securitiesService).setEastMoneySourceService(eastMoneySourceService);
		
		String type = "";
		String name = "";
		String securitiesCode = "";
		String columnNames = "";
		String uniqColumnNames = "";
		Map<String, Object> conditions = null;

		ResultSupport<Long> ret = ((SecuritiesServiceImpl)securitiesService).get(type, name, securitiesCode, columnNames, uniqColumnNames, conditions);
		
		System.out.println(ret);
		
	}
}
