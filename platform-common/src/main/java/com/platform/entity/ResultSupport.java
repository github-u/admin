package com.platform.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ResultSupport<T> implements Serializable{

    private static final long serialVersionUID = 1L;

    @Getter @Setter private boolean success = false;

    @Getter @Setter private T model;

    @Getter @Setter private String errCode;

    @Getter @Setter private String errMsg;

    @Getter @Setter private Map<String, String> partErrCodeMap = new HashMap<String, String>();

    @Getter @Setter private Map<String, String> partErrMsgMap = new HashMap<String, String>();

    public void addPartFail(String moduleName, String errCode, String errMsg){
        if(moduleName == null){
            return;
        }
        this.getPartErrCodeMap().put(moduleName, errCode);
        this.getPartErrMsgMap().put(moduleName, errMsg);
    }

    public ResultSupport<T> fail(String errCode, String errMsg){

        this.success = false;
        this.errCode = errCode;
        this.errMsg = errMsg;

        return this;
    }

    public ResultSupport<T> success(T model){

        this.success = true;
        this.model = model;

        return this;

    }


}
