package com.platform.jobx.client;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.base.Preconditions;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.jobx.domain.TaskParam;
import com.platform.jobx.domain.TaskParam.Type;
import com.platform.jobx.service.SimpleTask;

public class JobXClient4Spring extends AbstractJobXClient implements ApplicationContextAware, InitializingBean, DisposableBean{
	
	private static Logger logger = LoggerFactory.getLogger(JobXClient4Spring.class);
	
	private ApplicationContext applicationContext;
	
	public void registerTasks() throws Exception{
		Map<String, SimpleTask> simpleTasksFromContext = applicationContext.getBeansOfType(SimpleTask.class);
		simpleTasksFromContext = simpleTasksFromContext.entrySet().stream()
				.filter(kv -> kv.getValue() != null)
				.collect(Collectors.toMap(a -> a.getValue().getClass().toString(), a -> a.getValue()));
		
		simpleTasks.putAll(simpleTasksFromContext);
	}
	
	@Override
    public void afterPropertiesSet() throws Exception {
		init();
	}
	
	@Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

	@Override
	public void destroy() throws Exception {
		
	}

	@Override
	public ResultSupport<String> trigger(String taskName, TaskParam taskParam) {
		try {
			return trigger0(taskName, taskParam);
		}catch(Exception e) {
			logger.error("title=" + "JobXClient4Spring"
                    + "$mode=" + "trigger"
                    + "$errCode=" + ResultCode.TRIGGER_EXCEPTION
                    + "$errMsg=", e);
			return new ResultSupport<String>().fail(ResultCode.TRIGGER_EXCEPTION, e.getMessage());
		}
	}
	
	public ResultSupport<String> trigger0(String taskName, TaskParam taskParam) {
		
		Preconditions.checkNotNull(taskName);
		Preconditions.checkNotNull(taskParam);
		Preconditions.checkNotNull(taskParam.getType());
		
		ResultSupport<String> ret = new ResultSupport<String>();
		
		Type taskType = taskParam.getType();
		if(Type.SimpleTask.equals(taskType)) {
			SimpleTask simpleTask = simpleTasks.get(taskName);
			Preconditions.checkNotNull(simpleTask);
			
			ResultSupport<String> processRet = simpleTask.process((SimpleTaskParam) taskParam);
			Preconditions.checkNotNull(processRet);
			
			if(!processRet.isSuccess()) {
				return ret.fail(processRet.getErrCode(), processRet.getErrMsg());
			}else {
				return ret.success(processRet.getModel());
			}
		}else {
			throw new RuntimeException("TaskType not support, type is " + taskType.toString());
		}
		
	}
	
	public static final class ResultCode{
		public static final String TRIGGER_EXCEPTION = "TRIGGER_EXCEPTION"; 
	}
	
}
