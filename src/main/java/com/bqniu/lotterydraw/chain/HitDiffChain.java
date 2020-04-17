package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.common.Function;
import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Prize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Date;

/**
 * @author nbq
 * @create 2020-03-09 上午11:13
 * @desc ..
 *
 * 奖品中奖时间diff限制  do execute
 *
 **/
public class HitDiffChain implements ExecChain {

    private Log log = LogFactory.getLog(HitDiffChain.class);

    private ExecChain execChain;

    /**
     * 私有化
     * **/
    private HitDiffChain(){

    }

    public static HitDiffChain create(){
        return new HitDiffChain();
    }

    public HitDiffChain execChain(ExecChain execChain){
        this.execChain = execChain;
        return this;
    }

    public HitDiffChain build(){
        return this;
    }


    @Override
    public Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl) {
        Long prizeHitDiff = prize.getPrizeHitDiff();  // >= -1
        if (prizeHitDiff < 1L){   //不受限制
            return execChain.execute(prize,jedisPool,userId, dl);
        }
        Long aLong = redisHandle(jedisPool, Function.getRedisHitdiffKey(prize.getActiveId()), prize.getId());  //获取时间diff差
        if (aLong == 0L || aLong >= prizeHitDiff){
            return execChain.execute(prize,jedisPool,userId, dl);
        }else{
            dl.setChain("hitdiffChain");
            return false;
        }
    }


    /**
     * 时间差callback
     * **/
    @Override
    public void callback(Prize prize, JedisPool jedisPool, Long userId) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            //设置奖品最后中奖时间
            Long now = new Date().getTime()/1000;  //当前时间戳秒
            jedis.hset(Function.getRedisHitdiffKey(prize.getActiveId()), String.valueOf(prize.getId()), String.valueOf(now));
        }catch (Exception ex){
            log.info("callback exception for prizeId:"+prize.getId()+" message:"+ex.getMessage());
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
        execChain.callback(prize, jedisPool, userId);
    }



    //redis 判断两个时间戳的差值
    private Long redisHandle(JedisPool jedisPool, String key, Long prizeId){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String time = jedis.hget(key, String.valueOf(prizeId));
            if (time == null || time.equals("")){  //如果该奖品还没有中过
                return 0L;
            }else{
                Long now = new Date().getTime()/1000;  //当前时间戳秒
                Long last = Long.valueOf(time);
                return now - last;
            }
        }catch (Exception ex){
            log.info(">>>>>>redis hitdiff key :" + key + "exception, please check it!");
            return 0L;
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
    }

}
