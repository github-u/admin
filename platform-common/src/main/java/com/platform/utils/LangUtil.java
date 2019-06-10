package com.platform.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LangUtil {
    
    private static Map<Class<?>, Convertor<?>> convertors = new ConcurrentHashMap<Class<?>, Convertor<?>>();
    static {
        registerConvertor(String.class, new StringConvertor());
        registerConvertor(Long.class, new LongConvertor());
        registerConvertor(Integer.class, new IntegerConvertor());
        registerConvertor(Boolean.class, new BooleanConvertor());
        
        registerConvertor(long.class, new LongConvertor());
        registerConvertor(int.class, new IntegerConvertor());
        registerConvertor(boolean.class, new BooleanConvertor());
    }
    
    @SuppressWarnings("unchecked")
    //safe convert , if can't convert anyway, just return null,
    //if not this situation, just dont user this 
    public static <T> T convert(Object obj, Class<T> clz){
        checkNotNull(clz);
        if(obj == null) {
            return null;
        }
        
        Convertor<T> convertor = (Convertor<T>) convertors.get(clz);
        if(convertor == null) {
            return safeObject(obj, clz);
        }
        
        return convertor.as(obj);
    }
    
    public static interface Convertor<T>{
        T as(Object obj);
    }
    
    public static void registerConvertor(Class<?> clz, Convertor<?> convertor) {
        checkNotNull(clz);
        checkNotNull(convertor);
        convertors.put(clz, convertor);
    }
    
    public static class StringConvertor implements Convertor<String>{
        @Override
        public String as(Object obj) {
            return safeString(obj);
        }
    }
    
    public static class LongConvertor implements Convertor<Long>{
        @Override
        public Long as(Object obj) {
            return safeLong(obj);
        }
    }
    
    public static class IntegerConvertor implements Convertor<Integer>{
        @Override
        public Integer as(Object obj) {
            return safeInteger(obj);
        }
    }
    
    public static class BooleanConvertor implements Convertor<Boolean>{
        @Override
        public Boolean as(Object obj) {
            return safeBoolean(obj);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T safeObject(Object val, Class<T> destClz) {
        if(val == null || destClz == null) {
            return null;
        }
        
        Class<?> clz = val.getClass();
        if(!destClz.isAssignableFrom(clz)) {
            return null;
        }
        
        return (T) val;
    }
    
    public static String safeString(Object val){
        if(val == null){
            return null;
        }
        if(val instanceof String){
            return (String) val;
        }else{
            return String.valueOf(val);
        }
    }
    
    public static Long safeLong(Object val){
        
        if(val == null){
            return null;
        }
        
        if(val instanceof Number){
            return ((Number) val).longValue();
        }
        
        if(val instanceof String){
            try{
                return Long.valueOf((String)val);
            }catch(Exception e){
                return null;
            }
        }
        
        return null;
    }
    
    public static Integer safeInteger(Object val){
        
        if(val == null){
            return null;
        }
        
        if(val instanceof Number){
            return ((Number) val).intValue();
        }
        
        if(val instanceof String){
            try{
                return Integer.valueOf((String)val);
            }catch(Exception e){
                return null;
            }
        }
        
        return null;
    }
    
    public static Boolean safeBoolean(Object obj){
        
        if(obj == null){
            return Boolean.FALSE;
        }
        
        if(obj instanceof String){
            return Boolean.valueOf((String) obj);
        }
        
        if(obj instanceof Boolean){
            return (Boolean) obj;
        }
        
        if(obj instanceof Number){
            return ((Number) obj).intValue() > 0;
        }
        
        return Boolean.FALSE;
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
    
    public static class A{
        
    }
    
    public static class B extends A{
        
    }
    
    public static void main(String[] args) {
        
        System.out.println(LangUtil.convert("string", String.class));
        System.out.println(LangUtil.convert(1L, String.class));
        System.out.println(LangUtil.convert(new Object(), String.class));
        
        System.out.println(LangUtil.convert("10", Long.class));
        System.out.println(LangUtil.convert(new Double(20), Long.class));
        System.out.println(LangUtil.convert("12.3", Long.class));
        System.out.println(LangUtil.convert(new Object(), Long.class));
        
        System.out.println(LangUtil.convert("10", Integer.class));
        System.out.println(LangUtil.convert(new Double(20), Integer.class));
        System.out.println(LangUtil.convert("12.3", Integer.class));
        System.out.println(LangUtil.convert(new Object(), Integer.class));
        
        System.out.println(LangUtil.convert("true", Boolean.class));
        System.out.println(LangUtil.convert(new Double(20), Boolean.class));
        System.out.println(LangUtil.convert(1L, Boolean.class));
        System.out.println(LangUtil.convert(new Object(), Boolean.class));
        
        System.out.println(LangUtil.convert(new B(), A.class));
        System.out.println(LangUtil.convert(new A(), B.class));

    }
}

