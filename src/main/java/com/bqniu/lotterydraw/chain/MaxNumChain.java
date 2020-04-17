package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.common.Function;
import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Prize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author nbq
 * @create 2020-03-09 上午11:13
 * @desc ..
 *
 * 奖品数量限制 do execute
 *
 **/
public class MaxNumChain implements ExecChain {

    private Log log = LogFactory.getLog(MaxNumChain.class);

    private ExecChain execChain;

    /**
     * 私有化
     * **/
    private MaxNumChain(){

    }

    public static MaxNumChain create(){
        return new MaxNumChain();
    }

    public MaxNumChain execChain(ExecChain execChain){
        this.execChain = execChain;
        return this;
    }

    public MaxNumChain build(){
        return this;
    }


    @Override
    public Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl) {
        Long maxKind = prize.getPrizeMaxNumKind();
        if (maxKind == -1L){   //没有数量限制
            return execChain.execute(prize, jedisPool, userId, dl);
        }
        if (maxKind == 1L){   //每天数量限制
            Long prizeMaxNumEachDay = prize.getPrizeMaxNumEachDay();   //  >=0
            Long number = Function.redisHandle(jedisPool, Function.getRedisNumEachDayKey(prize.getActiveId()), prize.getId());
            if (number < prizeMaxNumEachDay){  //限制通过
                return execChain.execute(prize, jedisPool, userId, dl);
            }else{
                dl.setChain("maxNumChain");
                return false;
            }
        }else{   //整个活动期间设置数量限制
            Long prizeMaxNumTotal = prize.getPrizeMaxNumTotal();  // >=0
            Long number = Function.redisHandle(jedisPool, Function.getRedisNumTotalKey(prize.getActiveId()), prize.getId());
            if (number < prizeMaxNumTotal){  //限制通过
                return execChain.execute(prize, jedisPool, userId, dl);
            }else{
                dl.setChain("maxNumChain");
                return false;
            }
        }
    }


    /**
     * 奖品数量callback
     * **/
    @Override
    public void callback(Prize prize, JedisPool jedisPool, Long userId) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            //每天奖品被抽取的hash 加1
            jedis.hincrBy(Function.getRedisNumEachDayKey(prize.getActiveId()), String.valueOf(prize.getId()), 1L);
            //整个活动奖品被抽取的hash 加1
            jedis.hincrBy(Function.getRedisNumTotalKey(prize.getActiveId()), String.valueOf(prize.getId()), 1L);
        }catch (Exception ex){
            log.info("callback exception for prizeId:"+prize.getId()+" message:"+ex.getMessage());
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
        execChain.callback(prize, jedisPool, userId);
    }

}
