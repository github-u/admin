package com.platform.task;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

@Component
public class SecuritiesCodesTask extends AbstractSecuritiesTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		ResultSupport<Long> getBatchRet = securitiesService.getBatch(
				Source.TU_SHARE, 
				"stock_basic", 
				"ts_code,symbol,name,area,industry,fullname,enname,market,exchange,curr_type,list_status,list_date,delist_date,is_hs", 
				"ts_code", 
				Maps.newHashMap(),
				new Function<Map<String, Object>, List<Map<String, Object>>>() {

					@Override
					public List<Map<String, Object>> apply(Map<String, Object> paramT) {
						
						String tsCode = LangUtil.convert(paramT.get("ts_code"), String.class);
						String code = tsCode.split("\\.")[0];
						
						String name = LangUtil.convert(paramT.get("name"), String.class);
						String emCode = SecuritiesUtils.getSecuritiesType(code) + "." + code;
						
						paramT.put("code", code);
						paramT.put("name", name);
						paramT.put("em_code", emCode);
						
						return Lists.newArrayList(paramT);
					}
					
				},
				new Function<String, String>() {
					@Override
					public String apply(String t) {
						return "securities_codes";
					}
				}
				,parallel);
		
		if(!getBatchRet.isSuccess()) {
			return new ResultSupport<String>().fail(getBatchRet.getErrCode(), getBatchRet.getErrMsg());
		}else {
			return new ResultSupport<String>().success(LangUtil.convert(getBatchRet.getModel(), String.class));
		}
		
	}
	
	public static void main(String[] args) {
		
	}

}
