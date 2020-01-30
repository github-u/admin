package com.platform.jobx.bridge;

import com.platform.entity.ResultSupport;
import com.platform.jobx.client.JobXClient4Spring;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.jobx.service.SimpleTask;
import com.platform.utils.BeanUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.IJobHandler;

import lombok.Getter;
import lombok.Setter;

public class JobXClient4Spring4XXL extends JobXClient4Spring{
	
	private XxlJobSpringExecutor xxlJobSpringExecutor;
	
	@Override
    public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		
		BeanUtil.copy(this, xxlJobSpringExecutor, Boolean.TRUE);
		
		simpleTasks.entrySet().stream()
		.filter(simpleTask -> XxlJobSpringExecutor.loadJobHandler(simpleTask.getKey()) == null)
		.forEach(simpleTask -> {
			XxlJobSpringExecutor.registJobHandler(simpleTask.getKey(), new DefaultJobHandler(simpleTask.getValue()));
		});

		xxlJobSpringExecutor.afterPropertiesSet();
		
	}
	
	@Override
	public void destroy() throws Exception {
		xxlJobSpringExecutor.destroy();
		super.destroy();
	}
	
	public JobXClient4Spring4XXL() {
		xxlJobSpringExecutor = new XxlJobSpringExecutor();
	}
	
	@Getter @Setter private String adminAddresses;
	@Getter @Setter private String appName;
	@Getter @Setter private String ip;
	@Getter @Setter private int port;
	@Getter @Setter private String accessToken;
	@Getter @Setter private String logPath;
	@Getter @Setter private int logRetentionDays;
	
	public static class DefaultJobHandler extends IJobHandler{
		
		private SimpleTask simpleTask;
		
		public DefaultJobHandler(SimpleTask simpleTask) {
			this.simpleTask = simpleTask;
		}
		
		@Override
		public ReturnT<String> execute(String param) throws Exception {
			
			SimpleTaskParam simpleTaskParam = new SimpleTaskParam();
			simpleTaskParam.setPlainArgs(param);
			
			ResultSupport<String> processRet = this.simpleTask.process(simpleTaskParam);
			
			if(!processRet.isSuccess()) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, processRet.getErrCode() + "$" + processRet.getErrMsg());
			}else {
				return new ReturnT<String>(ReturnT.SUCCESS_CODE, processRet.getModel());
			}
		}
	}
}
