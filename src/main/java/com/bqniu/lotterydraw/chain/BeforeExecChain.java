package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Active;
import redis.clients.jedis.JedisPool;

import java.util.Map;

/**
 * @author nbq
 * @create 2020-03-09 下午7:07
 * @desc ..
 *
 * 抽奖前置处理调用链
 *
 * 活动时间check && 抽奖次数check
 *
 *
 **/
public interface BeforeExecChain {

    Map<String,Object> execute(Active active, JedisPool jedisPool, Long userId, DrawLog dl);

    void callback(Active active, JedisPool jedisPool, Long userId);

}
