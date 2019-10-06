package com.platform.entity.tushare;

import lombok.Getter;
import lombok.Setter;

public class TuShareResult {
    
    @Getter @Setter String requestId;
    
    @Getter @Setter long code;
    
    @Getter @Setter String msg;
    
    @Getter @Setter TuShareData data;
    
}
