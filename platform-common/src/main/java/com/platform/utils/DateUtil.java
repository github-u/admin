package com.platform.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class DateUtil {
	
	private static Logger logger = LoggerFactory.getLogger(DateUtil.class);
	
	public static final String SENCOND_PARTTERN = "yyyy-MM-dd HH:mm:ss";
	
	public static final String DAY_PARTTERN_1 = "yyyy-MM-dd";
	
	public static final String DAY_PARTTERN_2 = "yyyyMMdd";
	
	public static final DateTimeFormatter SENCONDS_FORMATTER = DateTimeFormatter.ofPattern(SENCOND_PARTTERN);
	
	public static final DateTimeFormatter DAY_FORMATTER_1 = DateTimeFormatter.ofPattern(DAY_PARTTERN_1);
	
	public static final DateTimeFormatter DAY_FORMATTER_2 = DateTimeFormatter.ofPattern(DAY_PARTTERN_2);
	
	public static boolean isSameDay(Date day1, Date day2) {
		
		Preconditions.checkNotNull(day1);
		Preconditions.checkNotNull(day2);
		
		LocalDateTime d1 = LocalDateTime.ofInstant(day1.toInstant(), ZoneId.systemDefault());
		LocalDateTime d2 = LocalDateTime.ofInstant(day2.toInstant(), ZoneId.systemDefault());
		
		return d1.getYear() == d2.getYear() &&
				d1.getDayOfYear() == d2.getDayOfYear();
	}
	
	public static List<Date> getWeekDays(Date date){
		LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		return getWeekDays(localDateTime.getYear(), localDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
	}
	
	public static List<Date> getWeekDays(){
		return getWeekDays(new Date());
	}
	
	public static Integer getYear(Date date) {
		Preconditions.checkNotNull(date);
		return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getYear();
	}
	
	public static Integer getDayOfWeek(Date date) {
		Preconditions.checkNotNull(date);
		return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getDayOfWeek().getValue();
	}
	
	public static Integer getWeekOfYear(Date date) {
		Preconditions.checkNotNull(date);
		return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
	}
	
	public static String getDate(Date date) {
		return getDate(date, SENCONDS_FORMATTER);
	}
	
	public static Date getDate(String date) {
		return getDate(date, SENCONDS_FORMATTER);
	}
	
	public static List<Date> getWeekDays(int year, int week) {
		List<Date> weekDays = Lists.newArrayList();
		
		Date fisrtDayOfYear = getDate(String.valueOf(year + "-01-01 00:00:00")); 
		
		LocalDateTime weekDateTime = LocalDateTime.ofInstant(fisrtDayOfYear.toInstant(), ZoneId.systemDefault()).plusWeeks(week - 1);
		
		LocalDateTime firstDayOfWeekDateTime = weekDateTime.minusDays(weekDateTime.getDayOfWeek().getValue() - 1);
		
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(0).atZone(ZoneId.systemDefault()).toInstant()));
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(2).atZone(ZoneId.systemDefault()).toInstant()));
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(3).atZone(ZoneId.systemDefault()).toInstant()));
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(4).atZone(ZoneId.systemDefault()).toInstant()));
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(5).atZone(ZoneId.systemDefault()).toInstant()));
		weekDays.add(Date.from(firstDayOfWeekDateTime.plusDays(6).atZone(ZoneId.systemDefault()).toInstant()));
		
		return weekDays;
	}
	
	public static String getDate(Date date, DateTimeFormatter dateTimeFormatter) {
		Preconditions.checkNotNull(date);
		
		try {
			LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
			return localDateTime.format(dateTimeFormatter);
		}catch(Exception e) {
			logger.error("title=" + "DateUtil"
                    + "$mode=" + "getDateD"
                    + "$errCode=" + "" 
                    + "$errMsg=", e);
			return null;
		}
		
	}
	
	public static Date getDate(String date, DateTimeFormatter dateTimeFormatter) {
		
		Preconditions.checkNotNull(date);
		try {
			LocalDateTime localDateTime = LocalDateTime.parse(date, SENCONDS_FORMATTER);
			return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
		}catch(Exception e) {
			logger.error("title=" + "DateUtil"
                    + "$mode=" + "getDateD"
                    + "$errCode=" + "" 
                    + "$errMsg=", e);
			return null;
		}
		
	}
	
	public static void main(String[] args) {
		System.out.println(getWeekDays(2020, 2));
		System.out.println(getWeekDays());
		System.out.println(getDate(new Date(), DAY_FORMATTER_1));
		
		LocalDateTime localDateTime = LocalDateTime.now();
		
		localDateTime.getYear();
	}
}
