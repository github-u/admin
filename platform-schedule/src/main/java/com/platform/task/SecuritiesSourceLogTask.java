package com.platform.task;

import java.util.Date;
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
import com.platform.utils.SecuritiesUtils;

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
		params.put("sourceType", Source.EAST_MONEY);
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("secId", SecuritiesUtils.getEastMoneySecuritiesCode("000001"));
		conditions.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
		conditions.put("fields1", "f1,f2,f3,f4,f5");
		conditions.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58");
		conditions.put("klt", "103");
		conditions.put("fqt", "1");
		//conditions.put("beg", beg);
		//conditions.put("end", end);
		conditions.put("smplmt", "460");
		conditions.put("_", String.valueOf(new Date().getTime()));
		
		
		params.put("conditiosns", conditions);
		
		System.out.println(JSON.toJSONString(params));
	}

}
