package com.platform.task;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.EastMoneyService;
import com.platform.service.TuShareService;
import com.platform.service.impl.SourceService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.LangUtil;

@Component
public class SecuritiesSourceLogTask extends AbstractSecuritiesTask{
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesSourceLogTask.class);
	
	@Resource
	private EastMoneyService eastMoneyService;
	
	@Resource
	private TuShareService tuShareService;
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		JSONObject params = JSON.parseObject(taskParam.getPlainArgs());
		
		String sourceType = LangUtil.convert(params.get("sourceType"), String.class); 
		String sourceName = LangUtil.convert(params.get("sourceName"), String.class); 
		String securitiesCode = LangUtil.convert(params.get("securitiesCode"), String.class);
		String columnNames = LangUtil.convert(params.get("columnNames"), String.class);
		
		Map<String, Object> conditions = params.getJSONObject("conditions");
		
		ResultSupport<List<Map<String, Object>>> sourceRet = null;
		
		SourceService sourceService = null;
		if(Source.EAST_MONEY.equals(sourceType)) {
			sourceService = (SourceService) eastMoneyService;
		}else if(Source.TU_SHARE.equals(sourceType)) {
			sourceService = (SourceService) tuShareService;
		}else {
			return new ResultSupport<String>().fail("SOURCE_TYPE_NOT_SUPPORT", sourceType);
		}
		
		if(securitiesCode != null) {
			sourceRet = sourceService.source(sourceName, securitiesCode, columnNames, conditions);
		}else {
			sourceRet = sourceService.source(sourceName, columnNames, conditions);
		}
		
		String sourceRetJSONString = JSON.toJSONString(sourceRet);
		logger.error(taskParam.getPlainArgs());
		logger.error(sourceRetJSONString);
		
		return new ResultSupport<String>().success(sourceRetJSONString);
		
	}
	
	protected static Map<String, String> parseArgs(String args){
		return null;
	}
	
	public static void main(String[] args) {
		Map<String, Object> params = Maps.newHashMap();
		params.put("sourceName", "");
		params.put("securitiesCode", "");
		params.put("columnNames", "");
		
		Map<String, Object> conditiosns = Maps.newHashMap();
		conditiosns.put("", "");
		
		params.put("conditiosns", conditiosns);
	}

}
