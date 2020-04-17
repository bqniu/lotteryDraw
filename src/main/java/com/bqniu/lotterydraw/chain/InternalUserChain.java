package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Prize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.JedisPool;

import java.util.List;

/**
 * @author nbq
 * @create 2020-03-09 上午11:13
 * @desc ..
 *
 * 奖品内定用户  do execute
 *
 **/
public class InternalUserChain implements ExecChain {

    private Log log = LogFactory.getLog(InternalUserChain.class);

    private ExecChain execChain;

    /**
     * 私有化
     * **/
    private InternalUserChain(){

    }

    public static InternalUserChain create(){
        return new InternalUserChain();
    }

    public InternalUserChain execChain(ExecChain execChain){
        this.execChain = execChain;
        return this;
    }

    public InternalUserChain build(){
        return this;
    }


    @Override
    public Boolean execute(Prize prize, JedisPool jedisPool, Long userId, DrawLog dl) {
        List<Long> prizeInternalUser = prize.getPrizeInternalUser();
        if (prizeInternalUser == null || prizeInternalUser.size() == 0){
            return execChain.execute(prize,jedisPool,userId,dl);
        }
        if (prizeInternalUser.contains(userId)){   //内定用户
            return execChain.execute(prize,jedisPool,userId,dl);
        }else{
            dl.setChain("internalUserChain");
            return false;
        }
    }


    /**
     * 内定用户callback
     * **/
    @Override
    public void callback(Prize prize, JedisPool jedisPool, Long userId) {
        execChain.callback(prize, jedisPool, userId);
    }


}
