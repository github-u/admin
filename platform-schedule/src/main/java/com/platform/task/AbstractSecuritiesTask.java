package com.platform.task;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.jobx.service.SimpleTask;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

public abstract class AbstractSecuritiesTask implements SimpleTask{

	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam param) {
		
		Map<String, String> argMap = parseArgs(param.getPlainArgs());
		
		logger.error("title=" + "AbstractSecuritiesTask"
				+ "$mode=" + "process"
				+ "$action=" + "start"
				+ "$name=" + this.getClass().getName());
		
		long start = System.currentTimeMillis();
		ResultSupport<String> processRet;
		try {
			processRet = process(param, Collections.unmodifiableMap(argMap));
			long end = System.currentTimeMillis();
			if(!processRet.isSuccess()) {
				logger.error("title=" + "AbstractSecuritiesTask"
						+ "$mode=" + "process"
						+ "$action=" + "end"
						+ "$name=" + this.getClass().getName()
						+ "$ret=" + "fail"
						+ "$cost=" + (end - start));
				return new ResultSupport<String>().fail(processRet.getErrCode(), processRet.getErrMsg());
			}else {
				logger.error("title=" + "AbstractSecuritiesTask"
						+ "$mode=" + "process"
						+ "$action=" + "end"
						+ "$name=" + this.getClass().getName()
						+ "$ret=" + "success"
						+ "$cost=" + (end - start)
						+ "$msg=" + processRet.getModel()
						);
				return new ResultSupport<String>().success(LangUtil.convert(processRet.getModel(), String.class));
			}
		}catch(Exception e) {
			long end = System.currentTimeMillis();
			logger.error("title=" + "AbstractSecuritiesTask"
					+ "$mode=" + "process"
					+ "$action=" + "end"
					+ "$name=" + this.getClass().getName()
					+ "$ret=" + "exception"
					+ "$cost=" + (end - start),
					e);
			return new ResultSupport<String>().fail(ResultCode.PROCESS_EXCEPTION, e.getMessage());
		}
	}
	
	abstract protected ResultSupport<String> process(SimpleTaskParam param, Map<String, String> argMap);
	
	protected static Map<String, String> parseArgs(String args){
		Map<String, String> ret = Maps.newHashMap();
		if(args == null || args.trim().length() == 0) {
			return ret;
		}
		
		String[] argsToken = StringUtils.tokenizeToStringArray(args, ",;\t\n ");
		Map<String, String> argMap = Lists.newArrayList(argsToken).stream()
				.map(token -> {
					if(token == null || token.trim().length() == 0) {
						return null;
					}
					String[] kv = token.split("=");
					return Pair.of(kv[0].trim(), kv.length > 1 ? kv[1].trim() : null);
				})
				.filter(pair -> pair != null)
				.collect(Collector.of(
						()->{
							Map<String, String> s = new LinkedHashMap<String, String>();
							return s;
						},
						(s, e)->{
							s.put(e.fst, e.snd);
						}, 
						(s1, s2)->{
							s1.putAll(s2);
							return s1;
						},
						Collector.Characteristics.IDENTITY_FINISH
						));
		
		ret.putAll(argMap);
		return ret;
	}
	
	public static final class ResultCode{
		public static final String PROCESS_EXCEPTION = "PROCESS_EXCEPTION";
	}
	
	public static void main(String[] args) {}
}
