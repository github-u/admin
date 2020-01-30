package com.platform.jobx.client;

import java.util.Map;

import com.google.common.collect.Maps;
import com.platform.jobx.service.SimpleTask;

public abstract class AbstractJobXClient implements JobXClient {
	
	protected Map<String, SimpleTask> simpleTasks = Maps.newConcurrentMap();
	
}
