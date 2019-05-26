package com.platform.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FieldAnnotation {
    
    /**
     * field alias
     */
    String alias() default "";
    
    /**
     * field descriptor
     */
    String desc() default "";
    
    /**
     * field , specify if covert to map 
     */
    boolean mapIgnore() default false;
    
}
