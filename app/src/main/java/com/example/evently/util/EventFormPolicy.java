package com.example.evently.util;
import androidx.annotation.Nullable;
public final class EventFormPolicy {
 private EventFormPolicy() { }
 @Nullable public static Long positiveLong(@Nullable CharSequence value) {
  if(value==null) return null; try { long n=Long.parseLong(value.toString().trim()); return n>0?n:null; } catch(NumberFormatException e){ return null; }
 }
 public static boolean capacityValid(long maximum,long occupied){ return maximum>=occupied; }
 @Nullable public static Long volunteerCapacity(boolean enabled, @Nullable CharSequence value) {
  if (!enabled) return 0L;
  return positiveLong(value);
 }
 public static boolean registrationDayNotExpired(long dateLong,long now){
  long threshold=now-86_400_000L;
  return dateLong>threshold;
 }
 public static boolean sameUtcDay(long first,long second){
  return Math.floorDiv(first,86_400_000L)==Math.floorDiv(second,86_400_000L);
 }
 public static long preserveTimeWhenSameDay(long original,long selectedUtcDay){
  return original>0 && sameUtcDay(original,selectedUtcDay) ? original : selectedUtcDay;
 }
}
