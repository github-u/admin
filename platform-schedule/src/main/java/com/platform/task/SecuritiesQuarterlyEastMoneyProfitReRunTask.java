package com.platform.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.EastMoneyService;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.EastMoneyUtils;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

@Component
public class SecuritiesQuarterlyEastMoneyProfitReRunTask extends AbstractSecuritiesCodesIteratorTask{
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("code", code(securitiesCode));
		conditions.put("endDate", argMap.get("endDate") != null ? argMap.get("endDate") : "");
		conditions.put("reportType", "1");
		conditions.put("reportDateType", "0");
		conditions.put("companyType", companyType(securitiesCode));
		//endDate=2018-09-30
		//companyType=4&reportDateType=0&reportType=1&endDate=&code=SH600018
		
		StringBuffer sb = new StringBuffer(securitiesCode).append("=");
		ResultSupport<Long> getRet = securitiesService.get(
				Source.EAST_MONEY, 
				"", 
				securitiesCode,
				"", 
				"code,year,quarter", 
				conditions,
				new Function<String, String>(){

					@Override
					public String apply(String paramT) {
						return EastMoneyUtils.getProfitURLPath();
					}
					
				},
				new Function<Map<String, Object>, List<Map<String, Object>>>() {

					@Override
					public List<Map<String, Object>> apply(Map<String, Object> paramT) {
						
						JSONArray jsonArray = (JSONArray) paramT.get(SourceService.Result.JSON_ARRAY_KEY);
						List<Map<String, Object>> ret = Lists.newArrayList();
						//net profit attributable to equity holders of the company 归属于本公司股东所有者的净利润
						//扣除非经常性损益后净利润 Net profit after deducting non-recurring gains and losses
							
						for(Object obj : jsonArray) {
							Map<String, Object> jsonObject = (Map) obj;
							
							String reportDateTime = LangUtil.safeString(jsonObject.get("REPORTDATE"));
							String reportDate = reportDateTime.split(" ")[0];
							String[] reportDateArray = reportDate.split("/");
							String yearS = reportDateArray[0];
							String monthS = reportDateArray[1];
							String dayS = reportDateArray[2];
							
							LocalDate localReportDate = LocalDate.now();

							localReportDate = localReportDate
									.withYear(LangUtil.safeInteger(yearS))
									.withMonth(LangUtil.safeInteger(monthS))
									.withDayOfMonth(LangUtil.safeInteger(dayS));
							
							LocalDateTime localDateTime = localReportDate.atStartOfDay();
							Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
							
							if(!quarterTrigger(date)) {
								/**
								logger.error("title=" + "AbstractSecuritiesCodesIteratorWeeklySupportTask"
										+ "$mode=" + "process"
										+ "$code=" + securitiesCode + "^" + klineArray[0]
										+ "$errCode=" + ResultCode.NOT_QUARTER_TRIGGER_DAY
										+ "$date=" + date);
								 */
								continue;
							}else {
								long year = DateUtil.getYear(date);
								long quarter = DateUtil.getQuarterOfYear(date);
								String tradeDate = DateUtil.getDate(date, DateUtil.DAY_FORMATTER_1);
								String netProfitAttrubutableToEquityHolders = LangUtil.safeString(jsonObject.get("PARENTNETPROFIT"));
								String netProfitAfterDeductingNonRecurringGainAndLosses = LangUtil.safeString(jsonObject.get("KCFJCXSYJLR"));
							
								Map<String, Object> quarterMap = Maps.newLinkedHashMap();
								
								quarterMap.put("code", securitiesCode);
								quarterMap.put("year", year);
								quarterMap.put("quarter", quarter);
								quarterMap.put("trade_date", tradeDate);
								quarterMap.put("em_net_profit_after_deducting_nrgl", netProfitAfterDeductingNonRecurringGainAndLosses);
								quarterMap.put("em_net_profit_attributable_to_eh", netProfitAttrubutableToEquityHolders);
								
								sb.append(tradeDate).append("^");
								
								ret.add(quarterMap);
								
							}
						}

						return ret;
					}
					
				},
				new Function<String, String>() {
					@Override
					public String apply(String t) {
						return "securities_quarterly";
					}
				}
				,parallel
				);
		
		logger.error("title=" + "SecuritiesWeeklyEastMoneyPriceReRunTask"
				+ "$mode=" + "process"
				+ "$code=" + securitiesCode
				+ "$errCode=" + "SUC"
				+ "$errMsg=" + sb.toString());
		if(!getRet.isSuccess()) {
			return new ResultSupport<String>().fail(getRet.getErrCode(), getRet.getErrMsg());
		}else {
			return new ResultSupport<String>().success(LangUtil.convert(getRet.getModel(), String.class));
		}
		
	}
	
	private String code(String securitiesCode) {
		
		int type = SecuritiesUtils.getSecuritiesType(securitiesCode);
		String code = 	type == 0 ? "SZ" + securitiesCode :
						type == 1 ? "SH" + securitiesCode :
						null;
		
		return code;
	}
	
	private String companyType(String securitiesCode) {
		Set<String> whiteList = Sets.newHashSet(
				"000001");
 		String companyType = whiteList.contains(securitiesCode)	? 
 				"3" : "4";
		return companyType;
	}
	
	public static void main(String[] args) {
		Date d = DateUtil.getDate("2019-01-02", DateUtil.DAY_FORMATTER_1);
		System.out.println(d);
	}
	
}
