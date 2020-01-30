package com.platform.jobx.domain;

import lombok.Getter;
import lombok.Setter;

public class TaskParam {
	
	@Getter @Setter Type type;
	
	@Getter @Setter String plainArgs;
	
	public enum Type{
		SimpleTask,
		;
	}
}
