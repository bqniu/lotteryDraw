package com.bqniu.lotterydraw.controller;

import com.colorv.lotterydraw.model.DrawLog;
import com.colorv.lotterydraw.service.LotteryDrawService;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author nbq
 * @create 2020-03-10 下午2:59
 * @desc ..
 * 抽奖
 **/

@RestController
@RequestMapping("/lottery")
@DefaultProperties(
        commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "8000"),
                @HystrixProperty(name = "circuitBreaker.forceClosed", value = "true"),
        },
        threadPoolProperties = {
                @HystrixProperty(name = "coreSize", value = "64"),
                @HystrixProperty(name = "maxQueueSize", value = "100"),
                @HystrixProperty(name = "queueSizeRejectionThreshold", value = "100"),
        }
)
public class LotteryDrawController {
    private Log log = LogFactory.getLog(LotteryDrawController.class);

    @Autowired
    private LotteryDrawService lotteryDrawService;  //抽奖服务



    /**
     * 抽奖do
     **/
    @HystrixCommand
    @RequestMapping(value= "do", method = RequestMethod.GET)
    public Object dolottery (@RequestParam(value = "activeId") Long activeId,
                             @RequestParam (value = "userId") Long userId){
        log.info(">>>lottery do, activeId: {"+activeId+" }, userId: {"+userId+" }");
        try {
            //activeId check
            if (!lotteryDrawService.activeIdCheck(activeId)){
                return ImmutableMap.of("status", 500, "msg","active not found");
            }
            Map<String, String> draw = lotteryDrawService.draw(activeId, userId);
            Long prizeId = Long.valueOf(draw.get("prizeId"));
            if (prizeId == -1L){  //抽奖失败
                return ImmutableMap.of("status", 500, "msg",draw.get("reason"));
            }
            return ImmutableMap.of("status", 200, "prizeId", prizeId);
        }catch (Exception ex){
            ex.printStackTrace();
            log.info(">>>>lottery do, fail, the reason :{ "+ex.getMessage()+" }");
            return ImmutableMap.of("status", 500);
        }
    }



    /**
     * refresh 刷新配置
     **/
    @HystrixCommand
    @RequestMapping(value= "refresh", method = RequestMethod.GET)
    public Object refresh(){
        log.info(">>>lottery do, refresh");
        boolean refresh = lotteryDrawService.refresh();
        if (refresh){
            return ImmutableMap.of("status", 200);
        }else{
            return ImmutableMap.of("status", 500);
        }
    }


    /**
     * refresh 立即刷新配置,供其他节点调用
     **/
    @HystrixCommand
    @RequestMapping(value= "refresh/now", method = RequestMethod.GET)
    public Object refreshNow(){
        log.info(">>>lottery do now, refresh");
        boolean refresh = lotteryDrawService.refreshNow();
        if (refresh){
            return ImmutableMap.of("status", 200);
        }else{
            return ImmutableMap.of("status", 500);
        }
    }





    /**
     * 查询用户在某个活动中已经抽奖次数和未抽奖次数
     **/
    @HystrixCommand
    @RequestMapping(value= "times/query", method = RequestMethod.GET)
    public Object timesQuery(@RequestParam (value = "userId") Long userId,
                             @RequestParam(value = "activeId") Long activeId){
        log.info(">>>lottery times query, activeId :{" + activeId + " }, userId: {"+ userId + "  }");
        try{
            //activeId check
            if (!lotteryDrawService.activeIdCheck(activeId)){
                return ImmutableMap.of("status", 500, "msg","active not found");
            }
            Map<String, Long> stringLongMap = lotteryDrawService.TimesQuery(activeId, userId);
            return ImmutableMap.of("status", 200, "result", stringLongMap);
        }catch (Exception ex){
            ex.printStackTrace();
            log.info(">>>>lottery times query exception, activeId : {" +activeId +" }, userId: {" + userId + " }");
            return ImmutableMap.of("status", 500);
        }
    }



    /**
     * 前置事件触发,给用户加抽奖次数,针对于活动kind=2
     **/
    @HystrixCommand
    @RequestMapping(value= "times/add", method = RequestMethod.GET)
    public Object timesAdd(@RequestParam (value = "userId") Long userId,
                           @RequestParam(value = "activeId") Long activeId,
                           @RequestParam(value = "time") Long time){
        log.info(">>>lottery times query, activeId :{" + activeId + " }, userId: {"+ userId + "  }");
        try{
            //activeId check
            if (!lotteryDrawService.activeIdCheck(activeId)){
                return ImmutableMap.of("status", 500, "msg","active not found");
            }
            //kind check
            if (!lotteryDrawService.activeKindCheck(activeId)){
                return ImmutableMap.of("status", 500, "msg","active kind not support");
            }
            Long aLong = lotteryDrawService.drawTimeAdd(activeId, userId, time);
            if (aLong == -1L){
                return ImmutableMap.of("status", 500, "msg","active time add error");
            }else{
                return ImmutableMap.of("status", 200, "times",aLong);
            }
        }catch (Exception ex){
            ex.printStackTrace();
            log.info(">>>>lottery times query exception, activeId : {" +activeId +" }, userId: {" + userId + " }");
            return ImmutableMap.of("status", 500);
        }
    }





    /**
     * 查询抽奖明细  activeId, Long userId, Long success ,Integer start, Integer length
     **/
    @HystrixCommand
    @RequestMapping(value= "log/query", method = RequestMethod.GET)
    public Object logQuery(@RequestParam(value = "activeId") Long activeId,
                            @RequestParam (value = "userId", defaultValue = "-1", required = false) Long userId,
                           @RequestParam (value = "success", defaultValue = "1L", required = false) Long success,
                           @RequestParam (value = "start") Integer start,
                           @RequestParam (value = "length", defaultValue = "20", required = false) Integer length){
        log.info(">>>logQuery");
        try{
            //activeId check
            if (!lotteryDrawService.activeIdCheck(activeId)){
                return ImmutableMap.of("status", 500, "msg","active not found");
            }
            if (success != 0L && success != 1L){
                return ImmutableMap.of("status", 500, "msg","success param error");
            }
            List<DrawLog> lg = lotteryDrawService.LogQuery(activeId, userId, success, start, length);
            return ImmutableMap.of("status", 200, "result", lg);
        }catch (Exception ex){
            ex.printStackTrace();
            log.info(">>>>logQuery error");
            return ImmutableMap.of("status", 500);
        }
    }


    /**
     * print of memory data
     **/
    @HystrixCommand
    @RequestMapping(value= "memory", method = RequestMethod.GET)
    public Object memory(){
        log.info(">>>print memory data");
        String data = lotteryDrawService.memoryData();
        return ImmutableMap.of("status", 200, "data", data);
    }

}
