package com.platform.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.platform.annotation.FieldAnnotation;

public class ExtendKV {
    
    @FieldAnnotation(mapIgnore = true)
    private Map<String, Object> kvs = new ConcurrentHashMap<String, Object>();
    
    public Object getValue(String key) {
        return kvs.get(key);
    }
    
    public <V> V getValue(String key, Class<V> clz) {
        return LangUtil.convert(kvs.get(key), clz);
    }
    
    public <V> void putValue(String key, V val){
        checkNotNull(key);
        checkNotNull(val);
        
        kvs.put(key, val);
    }
    
    public static void checkNotNull(Object obj) {
        if(obj == null) {
            throw new RuntimeException();
        }
    }
    
    public static void checkArgument(Boolean b) {
        if(b == null || b.booleanValue() == false) {
            throw new RuntimeException();
        }
    }
    
    public static void main(String[] args) {
        ExtendKV kv = new ExtendKV();
        kv.putValue("1", 1L);
        kv.putValue("2", new Object());
        
        System.out.println(kv.getValue("1", String.class));
        System.out.println(kv.getValue("2", Object.class));
        System.out.println(kv.getValue("2", Long.class));
    }
}
