package com.platform.entity.tushare;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import com.platform.annotation.FieldAnnotation;
import com.platform.utils.Pair;

import lombok.Getter;
import lombok.Setter;

public class TuShareData {
    
    @Getter @Setter List<String> fields;
    
    @Getter @Setter List<List<Object>> items;
    
    @Getter @Setter @FieldAnnotation(alias = "has_more") Boolean hasMore;
    
    public Map<String, Object> getItem(int index){
        if(items == null || index >= items.size() ) {
            return null;
        }
        
        List<Object> itemElem = items.get(index);
        
        return IntStream.range(0, itemElem.size())
                .mapToObj(i -> {
                    return Pair.of(fields.get(i), itemElem.get(i));
                })
                .collect(Collector.of(
                        ()->{
                            return new LinkedHashMap<String, Object>();
                        },
                        (s, e)->{
                            s.put(e.fst, e.snd);
                        }, 
                        (s1, s2)->{
                            s1.putAll(s2);
                            return s1;
                        },
                        Collector.Characteristics.IDENTITY_FINISH
                        ));
    }
    
}
