package com.platform.task;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

@Component
public class SecuritiesWeeklyEastMoneyPETask extends AbstractSecuritiesCodesIteratorWeeklySupportTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap, Date date) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("secId", SecuritiesUtils.getEastMoneySecuritiesCode(securitiesCode));
		conditions.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
		conditions.put("fields1", "f1,f2,f3,f4,f5");
		conditions.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58");
		conditions.put("klt", "103");
		conditions.put("fqt", "1");
		//conditions.put("beg", beg);
		//conditions.put("end", end);
		conditions.put("smplmt", "460");
		conditions.put("_", String.valueOf(new Date().getTime()));
		
		ResultSupport<Long> getRet = securitiesService.get(
				Source.EAST_MONEY, 
				"securities_weekly", 
				securitiesCode,
				"", 
				"code,year,week", 
				conditions,
				new Function<Map<String, Object>, Map<String, Object>>() {

					@Override
					public Map<String, Object> apply(Map<String, Object> paramT) {
						
						String tsCode = LangUtil.convert(paramT.get("ts_code"), String.class);
						String code = tsCode.split("\\.")[0];
						
						long year = DateUtil.getYear(date);
						long week = DateUtil.getWeekOfYear(date);
						
						String tradeDate = DateUtil.getDate(date, DateUtil.DAY_FORMATTER_1);
						
						paramT.put("code", code);
						paramT.put("year", year);
						paramT.put("week", week);
						paramT.put("trade_date", tradeDate);
						paramT.put("ts_pe_ttm", paramT.get("pe_ttm"));
						
						return paramT;
					}
					
				},
				new Function<String, String>() {
					@Override
					public String apply(String t) {
						return "securities_weekly";
					}
				}
				,parallel
				);
		
		if(!getRet.isSuccess()) {
			return new ResultSupport<String>().fail(getRet.getErrCode(), getRet.getErrMsg());
		}else {
			return new ResultSupport<String>().success(LangUtil.convert(getRet.getModel(), String.class));
		}
		
	}
	
	public static void main(String[] args) {

	}

}
