package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Prize;
import redis.clients.jedis.JedisPool;

/**
 * @author nbq
 * @create 2020-03-09 上午11:06
 * @desc ..
 *
 * 对抽奖得到的奖品进行责任链调用,限制处理, pass之后需要callback数据处理
 *
 **/
public interface ExecChain {

    Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl);

    void callback(Prize prize, JedisPool jedisPool, Long userId);

}
