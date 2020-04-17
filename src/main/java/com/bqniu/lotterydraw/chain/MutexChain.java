package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.common.Function;
import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Prize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

/**
 * @author nbq
 * @create 2020-03-09 上午11:13
 * @desc ..
 *
 * 奖品互斥限制 do execute
 *
 **/
public class MutexChain implements ExecChain {

    private Log log = LogFactory.getLog(MutexChain.class);

    private ExecChain execChain;

    /**
     * 私有化
     * **/
    private MutexChain(){

    }

    public static MutexChain create(){
        return new MutexChain();
    }

    public MutexChain execChain(ExecChain execChain){
        this.execChain = execChain;
        return this;
    }

    public MutexChain build(){
        return this;
    }


    @Override
    public Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl) {
        Long prizeMutexKind = prize.getPrizeMutexKind();
        if (prizeMutexKind == -1L){  //没有互斥限制
            return execChain.execute(prize,jedisPool,userId,dl);
        }
        if (prizeMutexKind == 1L){   //每天互斥限制
            List<Long> prizeMutexEachDay = prize.getPrizeMutexEachDay();
            if (prizeMutexEachDay == null || prizeMutexEachDay.size() == 0){
                return execChain.execute(prize,jedisPool,userId,dl);
            }
            if (redisHandle(jedisPool, Function.getRedisMutexEachDayKey(prize.getActiveId(), userId), prizeMutexEachDay)){ //已经抽到过互斥奖品
                dl.setChain("mutexChain");
                return false;
            }else{
                return execChain.execute(prize,jedisPool,userId,dl);
            }
        }else{   //整个活动期间互斥限制
            List<Long> prizeMutexTotal = prize.getPrizeMutexTotal();
            if (prizeMutexTotal == null || prizeMutexTotal.size() == 0){
                return execChain.execute(prize,jedisPool,userId,dl);
            }
            if (redisHandle(jedisPool, Function.getRedisMutexTotalKey(prize.getActiveId(), userId), prizeMutexTotal)){ //已经抽到过互斥奖品
                dl.setChain("mutexChain");
                return false;
            }else{
                return execChain.execute(prize,jedisPool,userId,dl);
            }
        }
    }


    /**
     * 奖品互斥callback
     * **/
    @Override
    public void callback(Prize prize, JedisPool jedisPool, Long userId) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            //用户每天抽取奖品加成
            jedis.sadd(Function.getRedisMutexEachDayKey(prize.getActiveId(), userId), String.valueOf(prize.getId()));
            //用户整个活动抽奖奖品加成
            jedis.sadd(Function.getRedisMutexTotalKey(prize.getActiveId(), userId), String.valueOf(prize.getId()));
        }catch (Exception ex){
            log.info("callback exception for prizeId:"+prize.getId()+" message:"+ex.getMessage());
        }finally {
            if (jedis != null){
                jedis.close();
            }

        }
        execChain.callback(prize, jedisPool, userId);
    }


    //redis 判断某个用户是否已经抽到 prize
    private Boolean redisHandle(JedisPool jedisPool, String key, List<Long> prizeIdList){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            for (Long prizeId: prizeIdList){
                if (jedis.sismember(key, String.valueOf(prizeId))){   //如果用户已经中过 互斥奖品中其中一个
                    return true;
                }
            }
            return false;
        }catch (Exception ex){
            log.info(">>>>>>redis mutex key :" + key + "exception, please check it!");
            return false;
        }finally {
            if (jedis != null){
                jedis.close();

            }
        }
    }

}
