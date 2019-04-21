package com.platform.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MemCacheUtils {
    
     private static Map<String, Object> memCache = new HashMap<String, Object>();
     
    public static Object get(String key) {
        return memCache.get(key);
    }
    
    public static void put(String key, Object value) {
        memCache.put(key, value);
    }

    public static void remove(String key) {
        memCache.remove(key);
    }


    public static Collection<String> keys() {
        return memCache.keySet();
    }
   
}
