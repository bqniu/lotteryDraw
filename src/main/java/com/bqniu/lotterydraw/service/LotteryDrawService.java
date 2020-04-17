package com.bqniu.lotterydraw.service;

import com.bqniu.commons.orm.ColorDao;
import com.bqniu.commons.orm.DaoQuery;
import com.bqniu.lotterydraw.chain.BeforeExecChain;
import com.bqniu.lotterydraw.chain.Loop;
import com.bqniu.lotterydraw.common.Function;
import com.bqniu.lotterydraw.model.DrawLog;
import com.bqniu.lotterydraw.register.Active;
import com.bqniu.lotterydraw.register.LotteryOb;
import com.bqniu.lotterydraw.register.Prize;
import com.bqniu.lotterydraw.register.RegisterCenter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author bqniu
 * @create 2020-02-24 18:38
 * @desc 抽奖服务
 *
 * 1>代码抽奖中，都是由先有的条件输出周期性很长的随机函数来决定下一个随机数，属于伪随机算法
 * 2>真随机算法 依赖于物理现象，真随机提供java api的可以参考：https://sourceforge.net/projects/randomjapi/ ，移至truerandom
 *
 * 抽奖服务
 **/

@Service
public class LotteryDrawService {

    private Log log = LogFactory.getLog(LotteryDrawService.class);


    private RegisterCenter register;   //注册中心,读取配置以及fresh各个节点


    private JedisPool jedisPool;


    private ColorDao colorDao;

    public LotteryDrawService(@Qualifier(value = "registerCenterLottery") RegisterCenter registerCenter, ColorDao colorDao, @Qualifier(value = "lotteryRedis") JedisPool jedisPool){
        this.register = registerCenter;
        this.colorDao = colorDao;
        this.jedisPool = jedisPool;
    }



    public boolean activeIdCheck(Long activeId){
        Active active = this.register.getActive(activeId);
        LotteryOb lotteryOb = this.register.getLotteryOb(activeId);
        if (active == null || lotteryOb == null){
            return false;
        }else{
            return true;
        }
    }



    /**
     *check
     * **/
    public boolean activeKindCheck(Long activeId){
        Active active = this.register.getActive(activeId);
        if (active.getDrawTimeKind() != 2L){
            return false;
        }else{
            return true;
        }
    }


    /**
     *给用户加抽奖次数
     * **/
    public Long drawTimeAdd(Long activeId, Long userId, Long num){
        Jedis jedis = null;
        try{
            jedis = this.jedisPool.getResource();
            Long aLong = jedis.hincrBy(Function.getRedisUserTimesKey(activeId), String.valueOf(userId), num);
            return aLong;
        }catch (Exception ex){
            log.info(">>>>drawTimeAdd exception: activeId: {" +activeId+" }, userId: { "+ userId + "}, num:{"+num+" }");
            return -1L;
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
    }




    /**
     * 抽奖主逻辑 前置处理+draw+后置处理
     * map:  prizeId  -1:表示抽奖失败 reason:原因
     * **/
    public Map<String,String> draw(Long activeId, Long userId){
        log.info(">>>> start loop: activeId :{ "+activeId+"  }, userId: { "+ userId + " }");
        DrawLog dl = new DrawLog();   //抽奖明细
        dl.setActiveId(activeId);
        dl.setUserId(userId);
        //前置处理
        Map<String,String> mapS = new HashMap<>();
        Active active = register.getActive(activeId);
        Map<String, Object> mapResult = Loop.BeforeChainExec(active, this.jedisPool, userId, dl);
        Long resultFlag = Long.valueOf(String.valueOf(mapResult.get("success")));
        if (resultFlag == 0L){   //中断,没有通过前置处理
            mapS.put("prizeId", "-1");
            mapS.put("reason", String.valueOf(mapResult.get("reason")));
            dl.setPrizeId(-1L);
            dl.setSuccess(0L);
            Map<String, Long> stringLongMap = TimesQuery(activeId, userId);
            dl.setDrawTime(Long.valueOf(stringLongMap.get("drawTime")));   //已经抽奖次数
            dl.setFreeTime(Long.valueOf(stringLongMap.get("drawFree")));   //剩余抽奖次数
            colorDao.create(dl);  //save
            //如果是活动时间过期导致抽奖失败,需要refresh
            if (String.valueOf(mapResult.get("chain")).equals("activeTime")){
                this.register.refresh();
            }
            return mapS;
        }
        //开始抽奖
        Long prizeId = null;
        //白名单判断
        Map<Long, Long> whiteMap = active.getWhiteMap();//user_id-->prizeId
        if (whiteMap == null || whiteMap.getOrDefault(userId, null) == null){  //没有配置白名单
            prizeId = doDraw(activeId);
            if (prizeId == null || prizeId == -1L){
                mapS.put("prizeId", "-1");
                mapS.put("reason", "奖品配置有误");
                dl.setPrizeId(-1L);
                dl.setSuccess(0L);
                dl.setReason("奖品配置有误");
                Map<String, Long> stringLongMap = TimesQuery(activeId, userId);
                dl.setDrawTime(Long.valueOf(stringLongMap.get("drawTime")));   //已经抽奖次数
                dl.setFreeTime(Long.valueOf(stringLongMap.get("drawFree")));   //剩余抽奖次数
                colorDao.create(dl);  //save
                return mapS;
            }
        }else{
            prizeId = whiteMap.get(userId);
        }
        //对抽到的奖品后置处理
        boolean b = Loop.AfterChainExec(register.getPrize(prizeId), this.jedisPool, userId, dl);
        //前置事件回调
        BeforeExecChain ob = (BeforeExecChain)mapResult.get("ob");
        ob.callback(active, this.jedisPool, userId);
        Long pass = 0L;
        if (b){   //pass黑盒规则
            pass = 1L;
            mapS.put("prizeId", String.valueOf(prizeId));
            mapS.put("reason", "success");
        }else {   //未通过黑盒规则,返回默认奖品
            mapS.put("prizeId", String.valueOf(active.getDefaultPrizeId()));
            mapS.put("reason", "success");
        }
        //log
        dl.setPrizeId(Long.valueOf(mapS.get("prizeId")));
        dl.setSuccess(1L);
        dl.setReason("success");
        dl.setPass(pass);
        Map<String, Long> stringLongMap = TimesQuery(activeId, userId);
        dl.setDrawTime(Long.valueOf(stringLongMap.get("drawTime")));   //已经抽奖次数
        dl.setFreeTime(Long.valueOf(stringLongMap.get("drawFree")));   //剩余抽奖次数
        colorDao.create(dl);  //save
        return mapS;
    }




    //根据权重实现随机抽奖
    private Long doDraw(Long activeId) {
        LotteryOb lotteryOb = this.register.getLotteryOb(activeId);
        List<Prize> prizeList = lotteryOb.getPrizeList();
        if (prizeList == null || prizeList.size() == 0){   //config error
            return -1L;
        }
        //计算权重总和
        Long probabilitySum = 0L;
        for (Prize p:prizeList) {
            probabilitySum += p.getProbability();
        }
        if (probabilitySum <= 0L){    //config error
            return -1L;
        }
        //开始随机
        Random random = new Random();      //伪随机
        int probabilitySumT = probabilitySum.intValue();
        Integer n = random.nextInt(probabilitySumT);
        Integer m = 0;
        Long prizeId = null;
        for (Prize p:prizeList) {
            if (m<=n && n< m+p.getProbability().intValue()){
                prizeId = p.getId();
                break;
            }
            m+=p.getProbability().intValue();
        }
        return prizeId;
    }



    /**
     * 刷新配置中心
     * **/
    public boolean refresh(){
        try{
            this.register.refresh();
            return true;
        }catch (Exception ex){
            log.info(">>>>>lottery draw service refresh registerCenter fail, the reason :"+ex.getMessage()+"   ,please check it!");
            return false;
        }
    }

    /**
     * 刷新配置中心 now
     * **/
    public boolean refreshNow(){
        try{
            this.register.refreshNow();
            return true;
        }catch (Exception ex){
            log.info(">>>>>lottery draw service refresh now registerCenter fail, the reason :"+ex.getMessage()+"   ,please check it!");
            return false;
        }
    }



    /**
     * print data of Memory
     * **/
    public String memoryData(){
        return this.register.toString();
    }







    /**
     * 查询用户抽奖和未抽奖次数
     * **/
    public Map<String,Long> TimesQuery(Long activeId, Long userId){
        Map<String,Long> mapResult = new HashMap<>();
        Active active = register.getActive(activeId);
        Long drawTimeKind = active.getDrawTimeKind();
        if (drawTimeKind == 1L){   //每人每天n次抽奖上限
            Long maxTimeEachDay = active.getMaxTimeEachDay();
            Long drawTime = redisHandle(jedisPool, Function.getRedisUserTimesEachDayKey(active.getId()), userId);
            Long drawFree = maxTimeEachDay - drawTime;
            mapResult.put("drawTime",drawTime);
            mapResult.put("drawFree", drawFree);
            return mapResult;
        }else if(drawTimeKind == 2L){  //前置事件完成，触发后继给用户加抽奖次数[整个活动]
            Long TotalTime = redisHandle(jedisPool, Function.getRedisUserTimesKey(active.getId()), userId);  //用户总共的抽奖机会
            Long drawTime = redisHandle(jedisPool, Function.getRedisUserTimesTotalKey(active.getId()), userId);  //用户已经消耗的抽奖次数
            Long drawFree = TotalTime - drawTime;
            mapResult.put("drawTime",drawTime);
            mapResult.put("drawFree", drawFree);
            return mapResult;
        }else {   //每人整个活动期间总共有n次抽奖次数上限
            Long maxTimeTotal = active.getMaxTimeTotal();
            Long drawTime = redisHandle(jedisPool, Function.getRedisUserTimesTotalKey(active.getId()), userId);
            Long drawFree = maxTimeTotal - drawTime;
            mapResult.put("drawTime",drawTime);
            mapResult.put("drawFree", drawFree);
            return mapResult;
        }
    }



    /**
     * 查询抽奖明细
     * **/
    public List<DrawLog> LogQuery(Long activeId, Long userId, Long success , Integer start, Integer length){
        DaoQuery<DrawLog> select = colorDao.select(DrawLog.class);
        if (userId != -1L){   //查询某个人的
            select = select.eq("user_id", userId);
        }
        List<DrawLog> listLog = select.eq("success", success).orderBy("id", true).offset(start).limit(length).list();
        return listLog;
    }






    //redis查询某个hash item的值
    private Long redisHandle(JedisPool jedisPool, String key, Long item){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String hget = jedis.hget(key, String.valueOf(item));
            Long value = 0L;
            if (hget != null && (!hget.equals(""))){
                value = Long.valueOf(hget);
            }
            return value;
        }catch (Exception ex){
            log.info(">>>>>>redis get key :" + key + "exception, please check it!");
            return 0L;
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
    }








}
