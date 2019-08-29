package com.platform.utils;

public class ObjectUtil {
    
    public static boolean equals(Object obj1, Object obj2) {
        if(obj1 == null && obj2 == null) {
            return true;
        }
        if(obj1 == null || obj2 == null) {
            return false;
        }
        if(obj1 == obj2) {
            return true;
        }
        
        return obj1.equals(obj2);
    }
    
}
