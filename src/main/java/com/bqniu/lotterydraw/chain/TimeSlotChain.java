package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.common.Function;
import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.PeriodLimit;
import com.colorv.lotterydraw.register.Prize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nbq
 * @create 2020-03-09 上午11:13
 * @desc ..
 *
 * 奖品时间槽限制   do execute
 *
 **/
public class TimeSlotChain implements ExecChain {

    private Log log = LogFactory.getLog(TimeSlotChain.class);

    /**
     * 私有化
     * **/
    private TimeSlotChain(){

    }

    public static TimeSlotChain create(){
        return new TimeSlotChain();
    }


    public TimeSlotChain build(){
        return this;
    }


    @Override
    public Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl) {
        Long prizeTimeSlotKind = prize.getPrizeTimeSlotKind();   // -1, 1, 2
        if (prizeTimeSlotKind == -1L){   //不受时间槽限制
            return true;
        }
        if (prizeTimeSlotKind == 1L){   //每天时间槽限制
            List<PeriodLimit> prizeTimeSlotEachDay = prize.getPrizeTimeSlotEachDay();
            if (prizeTimeSlotEachDay == null || prizeTimeSlotEachDay.size() == 0){
                return true;
            }
            //判断当前时间是否在时间槽限制中
            boolean limit = isLimit(prizeTimeSlotEachDay);
            if (!limit){
                return true;
            }
            //获取key值
            Map<String, Long> map = getKey(prizeTimeSlotEachDay);
            Long aLong = redisHandle(jedisPool, Function.getRedisTimeSlotEachDayKey(prize.getActiveId(), prize.getId()), String.valueOf(map.get("index")));
            if (aLong < map.get("max")){  //通过
                return true;
            }else{
                dl.setChain("timeSlotChain");
                return false;
            }
        }else {   //整个活动时间槽限制
            List<PeriodLimit> prizeTimeSlotTotal = prize.getPrizeTimeSlotTotal();
            if (prizeTimeSlotTotal == null || prizeTimeSlotTotal.size() == 0){
                return true;
            }
            //判断当前时间是否在时间槽限制中
            boolean limit = isLimitTotal(prizeTimeSlotTotal);
            if (!limit){
                return true;
            }
            //获取key值
            Map<String, Long> map = getKeyTotal(prizeTimeSlotTotal);
            Long aLong = redisHandle(jedisPool, Function.getRedisTimeSlotTotalKey(prize.getActiveId(), prize.getId()), String.valueOf(map.get("index")));
            if (aLong < map.get("max")){  //通过
                return true;
            }else{
                dl.setChain("timeSlotChain");
                return false;
            }
        }
    }



    /**
     * 时间槽callback
     * **/
    @Override
    public void callback(Prize prize, JedisPool jedisPool, Long userId) {
        Long prizeTimeSlotKind = prize.getPrizeTimeSlotKind();   // -1, 1, 2
        if (prizeTimeSlotKind == -1L){   //不受时间槽限制
            return;
        }
        if (prizeTimeSlotKind == 1L){   //每天时间槽限制
            List<PeriodLimit> prizeTimeSlotEachDay = prize.getPrizeTimeSlotEachDay();
            if (prizeTimeSlotEachDay == null || prizeTimeSlotEachDay.size() == 0){
                return;
            }
            //判断当前时间是否在时间槽限制中
            boolean limit = isLimit(prizeTimeSlotEachDay);
            if (!limit){
                return;
            }
            //获取key值
            Map<String, Long> map = getKey(prizeTimeSlotEachDay);
            Jedis jedis = null;
            try{
                jedis = jedisPool.getResource();
                jedis.hincrBy(Function.getRedisTimeSlotEachDayKey(prize.getActiveId(), prize.getId()),String.valueOf(map.get("index")), 1);
            }catch (Exception ex){
                log.info("timeslot callback error,  message is :" +ex.getMessage());
            }finally {
                jedis.close();
            }
        }else {   //整个活动时间槽限制
            List<PeriodLimit> prizeTimeSlotTotal = prize.getPrizeTimeSlotTotal();
            if (prizeTimeSlotTotal == null || prizeTimeSlotTotal.size() == 0){
                return;
            }
            //判断当前时间是否在时间槽限制中
            boolean limit = isLimitTotal(prizeTimeSlotTotal);
            if (!limit){
                return;
            }
            //获取key值
            Map<String, Long> map = getKeyTotal(prizeTimeSlotTotal);
            Jedis jedis = null;
            try{
                jedis = jedisPool.getResource();
                jedis.hincrBy(Function.getRedisTimeSlotTotalKey(prize.getActiveId(), prize.getId()),String.valueOf(map.get("index")), 1);
            }catch (Exception ex){
                log.info("timeslot callback error,  message is :" +ex.getMessage());
            }finally {
                jedis.close();
            }
        }
    }



    //判断当前时间属于哪一个Index  针对于每天
    private Map<String, Long> getKey(List<PeriodLimit> prizeTimeSlotEachDay){
        String now = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Map<String,Long> map = new HashMap<>();
        Long index = 1L;
        for (PeriodLimit p:prizeTimeSlotEachDay) {
            if (Function.between(p.getStart(), p.getEnd(), now)){
                map.put("max", p.getMax());
                break;
            }
            index += 1;
        }
        map.put("index", index);
        return map;
    }


    //判断当前时间属于哪一个Index  针对于整个活动
    private Map<String, Long> getKeyTotal(List<PeriodLimit> prizeTimeSlotTotal){
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String,Long> map = new HashMap<>();
        Long index = 1L;
        for (PeriodLimit p:prizeTimeSlotTotal) {
            if (Function.between(p.getStart(), p.getEnd(), now)){
                map.put("max", p.getMax());
                break;
            }
            index += 1;
        }
        map.put("index", index);
        return map;
    }





    //判断当前时间是否在Limit中
    private boolean isLimit(List<PeriodLimit> prizeTimeSlotEachDay){
        String now = new SimpleDateFormat("HH:mm:ss").format(new Date());
        boolean islimit = false;
        for (PeriodLimit p:prizeTimeSlotEachDay){
            if (Function.between(p.getStart(), p.getEnd(), now)){
                islimit = true;
                break;
            }
        }
        return islimit;
    }



    //判断当前时间是否在Limit中,整个活动
    private boolean isLimitTotal(List<PeriodLimit> prizeTimeSlotTotal){
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        boolean islimit = false;
        for (PeriodLimit p:prizeTimeSlotTotal){
            if (Function.between(p.getStart(), p.getEnd(), now)){
                islimit = true;
                break;
            }
        }
        return islimit;
    }





    //redis查询某个hash item的值
    private Long redisHandle(JedisPool jedisPool, String key, String item){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String hget = jedis.hget(key, item);
            Long value = 0L;
            if (hget != null && (!hget.equals(""))){
                value = Long.valueOf(hget);
            }
            return value;
        }catch (Exception ex){
            log.info(">>>>>>redis time slot key :" + key + "exception, please check it!");
            return 0L;
        }finally {
            if (jedis != null){
                jedis.close();

            }
        }
    }

}
