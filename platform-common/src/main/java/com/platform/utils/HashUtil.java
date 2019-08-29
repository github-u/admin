package com.platform.utils;

public class HashUtil {
    
    public static int hashCode(Object... args){
        checkNotNull(args);
        
        int hashCode = 1, prime = 31;
        for(Object arg : args) {
            hashCode = hashCode * prime + (arg != null ? arg.hashCode() : 0);
        }
        
        return hashCode;
    }
    
    public static void checkNotNull(Object obj) {
        if (obj == null) {
            throw new RuntimeException();
        }
    }
    
    public static void checkArgument(Boolean b) {
        if (b == null || b.booleanValue() == false) {
            throw new RuntimeException();
        }
    }
    
    public static void main(String[] args) {
        int hashCode1 = HashUtil.hashCode("1", 2L, 3, null);
        int hashCode2 = HashUtil.hashCode(1L, 2L, 3, null);
        int hashCode3 = HashUtil.hashCode("1", 2L, 3, "x");
        int hashCode4 = HashUtil.hashCode("1", 2L, 3, "x");
        
        System.out.println(hashCode1);
        System.out.println(hashCode2);
        System.out.println(hashCode3);
        System.out.println(hashCode4);
    }
    
}
