package com.platform.task;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.EastMoneyUtils;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

@Component
public class SecuritiesWeeklyEastMoneyPriceTask extends AbstractSecuritiesCodesIteratorWeeklyTask{
	
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
		conditions.put("klt", "101");
		conditions.put("fqt", "1");
		conditions.put("beg", DateUtil.getDate(date, DateUtil.DAY_FORMATTER_2));
		conditions.put("end", DateUtil.getDate(date, DateUtil.DAY_FORMATTER_2));
		conditions.put("smplmt", "460");
		conditions.put("_", String.valueOf(new Date().getTime()));
		
		ResultSupport<Long> getRet = securitiesService.get(
				Source.EAST_MONEY, 
				"", 
				securitiesCode,
				"", 
				"code,year,week", 
				conditions,
				new Function<String, String>(){

					@Override
					public String apply(String paramT) {
						return EastMoneyUtils.getKlinesURLPath();
					}
					
				},
				new Function<Map<String, Object>, List<Map<String, Object>>>() {

					@Override
					public List<Map<String, Object>> apply(Map<String, Object> paramT) {
						
						List<Map<String, Object>> ret = Lists.newArrayList();
						
						@SuppressWarnings("unchecked")
						List<String> klines = (List<String>) paramT.get("klines");
						
						long year = DateUtil.getYear(date);
						long week = DateUtil.getWeekOfYear(date);
						String tradeDate = DateUtil.getDate(date, DateUtil.DAY_FORMATTER_1);

						for(String kline : klines) {
							Map<String, Object> klineMap = Maps.newLinkedHashMap();
							
							String[] klineArray = kline.split(",");
							klineMap.put("code", securitiesCode);
							klineMap.put("year", year);
							klineMap.put("week", week);
							klineMap.put("trade_date", tradeDate);
							klineMap.put("em_open", klineArray[1]);
							klineMap.put("em_high", klineArray[3]);
							klineMap.put("em_low", klineArray[4]);
							klineMap.put("em_close", klineArray[2]);
							ret.add(klineMap);
						}

						return ret;
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

}
