package com.platform.jobx.client;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.TaskParam;

public interface JobXClient {
	
	default public void init() throws Exception{
		
		registerClient();
		
		registerTasks();
		
	}
	
	default public void registerClient() throws Exception{};
	
	default public void registerTasks() throws Exception{}
	
	ResultSupport<String> trigger(String taskName, TaskParam taskParam);
	
}
