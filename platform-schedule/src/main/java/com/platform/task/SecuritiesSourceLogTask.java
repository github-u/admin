package com.platform.task;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.EastMoneyService;
import com.platform.service.TuShareService;
import com.platform.service.impl.SourceService;
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
		String sourceName = LangUtil.convert(params.get("sourceName"), String.class); 
		String securitiesCode = LangUtil.convert(params.get("sourceName"), String.class);
		String columnNames = LangUtil.convert(params.get("sourceName"), String.class);
		
		Map<String, Object> conditions = params.getJSONObject("conditions");
		
		ResultSupport<List<Map<String, Object>>> sourceRet = null;
		
		if(securitiesCode != null) {
			sourceRet = ((SourceService)eastMoneyService).source(sourceName, securitiesCode, columnNames, conditions);
		}else {
			sourceRet = ((SourceService)eastMoneyService).source(sourceName, columnNames, conditions);
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
		
	}

}
