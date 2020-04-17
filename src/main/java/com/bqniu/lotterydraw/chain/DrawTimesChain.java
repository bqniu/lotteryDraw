package com.bqniu.lotterydraw.chain;


import com.colorv.lotterydraw.common.Function;
import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Active;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nbq
 * @create 2020-03-09 上午11:13
 * @desc ..
 *
 * 抽奖次数chain do execute
 *
 **/
public class DrawTimesChain implements BeforeExecChain {

    private Log log = LogFactory.getLog(DrawTimesChain.class);


    /**
     * 私有化
     * **/
    private DrawTimesChain(){

    }

    public static DrawTimesChain create(){
        return new DrawTimesChain();
    }


    public DrawTimesChain build(){
        return this;
    }


    /**
     * 抽奖次数chain
     *
     **/
    @Override
    public Map<String,Object> execute(Active active, JedisPool jedisPool, Long userId, DrawLog dl) {
        log.info(" *******DrawTimesChain******");
        Map<String,Object> mapResultSuccess = new HashMap<>();
        mapResultSuccess.put("success", "1");
        mapResultSuccess.put("reason", "success");
        Map<String,Object> mapResultFail = new HashMap<>();
        mapResultFail.put("success", "0");
        mapResultFail.put("reason", "没有抽奖次数");
        mapResultFail.put("chain", "drawTime");
        Long drawTimeKind = active.getDrawTimeKind();   //  1,2,3
        if (drawTimeKind == 1L){   //每人每天n次抽奖上限
            Long maxTimeEachDay = active.getMaxTimeEachDay();
            Long aLong = Function.redisHandle(jedisPool, Function.getRedisUserTimesEachDayKey(active.getId()), userId);
            if (aLong < maxTimeEachDay){   //还有抽奖机会
                return mapResultSuccess;
            }else{
                dl.setReason("没有抽奖次数");
                return mapResultFail;
            }
        }else if(drawTimeKind == 2L){  //前置事件完成，触发后继给用户加抽奖次数[整个活动]
            Long aLong = Function.redisHandle(jedisPool, Function.getRedisUserTimesKey(active.getId()), userId);  //用户总共的抽奖机会
            Long aLong1 = Function.redisHandle(jedisPool, Function.getRedisUserTimesTotalKey(active.getId()), userId);  //用户已经消耗的抽奖次数
            if (aLong1 < aLong){
                return mapResultSuccess;
            }else{
                dl.setReason("没有抽奖次数");
                return mapResultFail;
            }
        }else {   //每人整个活动期间总共有n次抽奖次数上限
            Long maxTimeTotal = active.getMaxTimeTotal();
            Long aLong = Function.redisHandle(jedisPool, Function.getRedisUserTimesTotalKey(active.getId()), userId);
            if (aLong < maxTimeTotal){
                return mapResultSuccess;
            }else{
                dl.setReason("没有抽奖次数");
                return mapResultFail;
            }
        }
    }


    @Override
    public void callback(Active active, JedisPool jedisPool, Long userId) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            jedis.hincrBy(Function.getRedisUserTimesEachDayKey(active.getId()), String.valueOf(userId), 1);
            jedis.hincrBy(Function.getRedisUserTimesTotalKey(active.getId()), String.valueOf(userId), 1);
        }catch (Exception ex){
            log.info(">>>>>draw times chain callback exception, activeId:{ "+active.getId()+" }, userId: {"+userId+" },  please check it");
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
    }

}
