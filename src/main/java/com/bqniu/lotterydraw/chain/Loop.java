package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Active;
import com.colorv.lotterydraw.register.Prize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.JedisPool;

import java.util.Map;

/**
 * @author nbq
 * @create 2020-03-09 下午5:31
 * @desc ..
 *
 * 对奖品伪随机根据概率返回一个奖品prizeId,  对prizeId进行chain 链式调用,黑盒规则
 *
 *
 **/
public class Loop {

    private static Log log = LogFactory.getLog(Loop.class);


    /**
     * 前置处理 链式调用即可, 可以自由扩展,或者位置调整调用顺序调整.
     *
     * 加入明细
     * **/
    public static Map<String,Object> BeforeChainExec(Active active, JedisPool jedisPool, Long userId, DrawLog dl){

        log.info(">>>>>>>>before chain exec start...activeId: {"+active.getId() + "}, userId: {"+userId+" }");
        //组装各个节点chain
        BeforeExecChain DrawTimesExecChain = DrawTimesChain.create().build();   //抽奖次数node
        BeforeExecChain ActiveTimeExecChain = ActiveTimeChain.create().execChain(DrawTimesExecChain).build();   //活动时间node
        //调用开始
        log.info(">>>>before chain invokeing!!!");
        Map<String,Object> executeResult = ActiveTimeExecChain.execute(active, jedisPool, userId, dl);
        log.info(">>>>>>>>before chain exec end, the result :{"+executeResult.get("success")+" }, the reason is:"+executeResult.get("reason"));
        executeResult.put("ob", ActiveTimeExecChain);
        return executeResult;
    }





    /**
     * 后置处理 链式调用即可, 可以自由扩展,或者位置调整调用顺序调整.
     *
     * **/
    public static boolean AfterChainExec(Prize prize, JedisPool jedis, Long userId, DrawLog dl){

        log.info(">>>>>>>>after chain exec start...prizeId: {"+prize.getId() + "}, userId: {"+userId+" }");
        //组装各个节点chain
        ExecChain TimeExecChain = TimeSlotChain.create().build();   //时间槽node
        ExecChain HitDiffExecChain = HitDiffChain.create().execChain(TimeExecChain).build();   //中奖时间差node
        ExecChain InternalUserExecChain = InternalUserChain.create().execChain(HitDiffExecChain).build();   //内部用户node
        ExecChain MutexSelfExecChain = MutexSelfChain.create().execChain(InternalUserExecChain).build();    //自身互斥node
        ExecChain MutexExecChain = MutexChain.create().execChain(MutexSelfExecChain).build();       //奖品互斥mode
        ExecChain MaxNumExecChain = MaxNumChain.create().execChain(MutexExecChain).build();         //数量Node

        //调用开始
        Boolean executeResult = MaxNumExecChain.execute(prize, jedis, userId, dl);
        log.info(">>>>>>>>after chain exec end, the result :{"+executeResult+" }");

        if (executeResult){    //奖品通过黑盒规则 pass
            MaxNumExecChain.callback(prize,jedis,userId);   //数据回调链
            return true;
        }else{   //not pass
            return false;
        }
    }



}
