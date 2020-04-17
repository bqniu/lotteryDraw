package com.bqniu.lotterydraw.chain;

import com.colorv.lotterydraw.common.Function;
import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.register.Active;
import com.colorv.lotterydraw.register.Period;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * 活动时间限制 do execute
 *
 **/
public class ActiveTimeChain implements BeforeExecChain {

    private Log log = LogFactory.getLog(ActiveTimeChain.class);

    private BeforeExecChain execChain;

    /**
     * 私有化
     * **/
    private ActiveTimeChain(){

    }

    public static ActiveTimeChain create(){
        return new ActiveTimeChain();
    }

    public ActiveTimeChain execChain(BeforeExecChain execChain){
        this.execChain = execChain;
        return this;
    }

    public ActiveTimeChain build(){
        return this;
    }


    /**
     * 活动时间前置处理  格式:  yyyy-MM-dd HH:mm:ss
     * **/
    @Override
    public Map<String,Object> execute(Active active, JedisPool jedisPool, Long userId, DrawLog dl) {
        log.info(" *******ActiveTimeChain******");
        List<Period> activeTime = active.getActiveTime();
        if (activeTime == null || activeTime.size() == 0){
            Map<String,Object> mapReturn = new HashMap();
            mapReturn.put("success", "0");
            mapReturn.put("reason", "不在活动时间内");
            mapReturn.put("chain", "activeTime");
            dl.setReason("不在活动时间内");
            return mapReturn;
        }
        Boolean between = false;
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (Period p:activeTime) {
            if (Function.between(p.getStart(), p.getEnd(), now)){
                between = true;
                break;
            }
        }
        if (!between){
            Map<String,Object> mapReturn = new HashMap();
            mapReturn.put("success", "0");
            mapReturn.put("reason", "不在活动时间内");
            mapReturn.put("chain", "activeTime");
            dl.setReason("不在活动时间内");
            return mapReturn;
        }else {
            return execChain.execute(active, jedisPool, userId, dl);
        }
    }



    @Override
    public void callback(Active active, JedisPool jedisPool, Long userId) {
        execChain.callback(active, jedisPool, userId);
    }


}
