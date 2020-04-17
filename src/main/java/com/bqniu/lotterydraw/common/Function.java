package com.bqniu.lotterydraw.common;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author nbq
 * @create 2020-03-05 下午3:01
 * @desc ..通用的静态方法
 **/
public class Function {

    /**
     * 对日期字符串a和日期字符串b进行比对
     * -1:  a < b
     * 0:   a = b
     * 1:   a > b
     */
    public static int compare(String a, String b){
        int diff = a.compareTo(b);
        if (diff < 0){
            return -1;
        }else if(diff == 0){
            return 0;
        }else {
            return 1;
        }
    }


    /**
     * 判断日期date是否在start和end之间
     * **/
    public static Boolean between(String start,String end,String date){
        int ds = compare(start, date);
        int de = compare(date, end);
        if (ds <= 0 && de <= 0){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 组装activeMap 中Key For register
     * **/
    public static String getActiveKey(Long activeId){
        return "lotteryActiveId_"+activeId;
    }

    /**
     * 组装prizeMap 中Key For register
     * **/
    public static String getPrizeKey(Long prizeId){
        return "lotteryPrizeId_"+prizeId;
    }

    /**
     * 组装lobMap 中Key For register
     * **/
    public static String getLoKey(Long activeId){
        return "lotteryLoId_"+activeId;
    }







    /**
     * For redis, 某个奖品每天已经被抽取的数量 hash  prize_id-->num
     * **/
    public static String getRedisNumEachDayKey(Long activeId){
        String now = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "lottery_redis_NumEachDay_"+activeId+"_"+now;
    }


    /**
     * For redis, 某个奖品活动期间已经被抽取的数量 hash prize_id-->num
     * **/
    public static String getRedisNumTotalKey(Long activeId){
        return "lottery_redis_NumTotal_"+activeId;
    }




    /**
     *  For redis, 某个用户某一天抽取到的奖品  set   prize_id
     * **/
    public static String getRedisMutexEachDayKey(Long activeId, Long userId){
        String now = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "lottery_redis_MutexEachDay_"+activeId+"_"+now+"_"+userId;
    }

    /**
     *  For redis, 某个用户整个活动期间抽取到的奖品  set  prize_id
     * **/
    public static String getRedisMutexTotalKey(Long activeId, Long userId){
        return "lottery_redis_MutexTotal_"+activeId+"_"+userId;
    }


    /**
     *  For redis, 某个活动中 某个奖品 最后一次的中奖时间  hash  prize_id-->时间戳
     * **/
    public static String getRedisHitdiffKey(Long activeId){
        return "lottery_redis_Hitdiff_"+activeId;
    }




    /**
     * For redis, 某个奖品每天 时间槽设置  hash  step-->num
     * **/
    public static String getRedisTimeSlotEachDayKey(Long activeId, Long prizeId){
        String now = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "lottery_redis_TimeSlotEachDay_"+activeId+"_"+now+"_"+prizeId;
    }


    /**
     * For redis, 某个奖品 整个活动期间时间槽设置  hash   step-->num
     * **/
    public static String getRedisTimeSlotTotalKey(Long activeId, Long prizeId){
        return "lottery_redis_TimeSlotTotal_"+activeId+"_"+prizeId;
    }




    /**
     * For redis,  某个人某天已经抽奖的次数  hash  user_id--->draw times
     * **/
    public static String getRedisUserTimesEachDayKey(Long activeId){
        String now = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "lottery_redis_userTimesEachDay_"+activeId+"_"+now;
    }


    /**
     * For redis, 整个活动期间 某个人已经抽奖次数 hash  user_id--->draw times
     * **/
    public static String getRedisUserTimesTotalKey(Long activeId){
        return "lottery_redis_userTimesTotal_"+activeId;
    }


    /**
     * For redis, 整个活动期间 保存某个人的抽奖次数,针对于前置事件触发给用户加抽奖次数的场景, hash   user_id--->draw times
     * **/
    public static String getRedisUserTimesKey(Long activeId){
        return "lottery_redis_userTimes_"+activeId;
    }


    //redis查询某个hash item的值
    public static Long redisHandle(JedisPool jedisPool, String key, Long item){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String hget = jedis.hget(key, String.valueOf(item));
            Long value = 0L;
            if (hget != null && (!hget.equals(""))){
                value = Long.valueOf(hget);
            }
            return value;
        }catch (Exception ex){
            return 0L;
        }finally {
            if (jedis != null){
                jedis.close();

            }
        }
    }


}
