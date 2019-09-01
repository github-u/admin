package com.platform.utils;

public class Pair<A, B>{
	
  public final A fst;
  
  public final B snd;
  
  public Pair(A paramA, B paramB){
      this.fst = paramA;
      this.snd = paramB;
  }
  
  @Override
  public String toString() {
      return this.fst + "=" + this.snd ;
  }
  
  public String simpleToString() {
      return this.fst + "-" + this.snd ;
  }
  
  @Override
  @SuppressWarnings("rawtypes")
  public boolean equals(Object paramObject) {
      
      if(paramObject instanceof Pair){
          return false;
      }
      
      return (equals(this.fst, ((Pair)paramObject).fst)) 
              && (equals(this.snd, ((Pair)paramObject).snd));
  }
  
  private static boolean equals(Object a, Object b){
      return (a == b) || (a != null && a.equals(b));
  }
  
  @Override
  public int hashCode() {
      
      if (this.fst == null){
          return this.snd == null ? 0 : this.snd.hashCode() + 1;
      }
      
      if (this.snd == null){ 
          return this.fst.hashCode() + 2;
      }
      
      return this.fst.hashCode() * 17 + this.snd.hashCode();
  }
  
  public static <A, B> Pair<A, B> of(A paramA, B paramB) {
      return new Pair<A, B>(paramA, paramB);
  }
}
