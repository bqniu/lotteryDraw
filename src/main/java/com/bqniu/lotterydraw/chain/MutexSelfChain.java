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
 * 奖品自身互斥限制 do execute
 *
 **/
public class MutexSelfChain implements ExecChain {

    private Log log = LogFactory.getLog(MutexSelfChain.class);

    private ExecChain execChain;

    /**
     * 私有化
     * **/
    private MutexSelfChain(){

    }

    public static MutexSelfChain create(){
        return new MutexSelfChain();
    }

    public MutexSelfChain execChain(ExecChain execChain){
        this.execChain = execChain;
        return this;
    }

    public MutexSelfChain build(){
        return this;
    }


    @Override
    public Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl) {
        Long prizeMutexSelfKind = prize.getPrizeMutexSelfKind();  // -1,1,2
        if (prizeMutexSelfKind == -1L){  //表示自身没有互斥限制
            return execChain.execute(prize,jedisPool,userId,dl);
        }
        if (prizeMutexSelfKind == 1L){  //自身每天互斥限制
            if (redisHandle(jedisPool, Function.getRedisMutexEachDayKey(prize.getActiveId(), userId), prize.getId())){ //已经抽到过互斥奖品
                dl.setChain("mutexSelfChain");
                return false;
            }else{
                return execChain.execute(prize,jedisPool,userId,dl);
            }
        }else{  // 自身整个活动期间互斥限制
            if (redisHandle(jedisPool, Function.getRedisMutexTotalKey(prize.getActiveId(), userId), prize.getId())){ //已经抽到过互斥奖品
                dl.setChain("mutexSelfChain");
                return false;
            }else{
                return execChain.execute(prize,jedisPool,userId,dl);
            }
        }
    }


    /**
     * 奖品自身互斥callback
     * **/
    @Override
    public void callback(Prize prize, JedisPool jedisPool, Long userId) {
        execChain.callback(prize, jedisPool, userId);
    }




    //redis 判断某个用户是否已经抽到 prize
    private Boolean redisHandle(JedisPool jedisPool, String key, Long prizeId){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            if (jedis.sismember(key, String.valueOf(prizeId))){  //表示用户已经中过该奖品了
                return true;
            }else{
                return false;
            }
        }catch (Exception ex){
            log.info(">>>>>>redis self mutex key :" + key + "exception, please check it!");
            return false;
        }finally {
            if (jedis != null){
                jedis.close();

            }
        }
    }

}
