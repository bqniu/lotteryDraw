package com.bqniu.lotterydraw.register;



import com.bqniu.lotterydraw.*;
import com.bqniu.lotterydraw.zookeeper.ZookeeperCenter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author nbq
 * @create 2020-03-04 下午6:10
 * @desc ..注册中心
 *   应用启动的时候,直接收集所有上线的活动,进行online check,&& 其他check
 *   init所有活动和奖品信息register到配置中心,后续可通过fresh接口进行修改生效
 **/
public class RegisterCenter implements Register {

    private Log log = LogFactory.getLog(RegisterCenter.class);


    private ReentrantReadWriteLock lock;

    private Lock rlock;   //读锁 可重入

    private Lock wlock;   //写锁  可重入


    private Map<String, LotteryOb> lobMap = new HashMap<>();   //系统 活动和奖品实体map, 需要注册

    private Map<String, Active> activeMap = new HashMap<>();   //系统活动配置map

    private Map<String, Prize> prizeMap = new HashMap<>();     //系统奖品配置map


    private ColorDao colorDao;


    private ZookeeperCenter zookeeperCenter;

    private ServerConfig serverConfig;

    private Set<Long> drawTimeKindSet = new HashSet<>();     //抽奖次数类型

    private Set<Long> prizeMaxNumOrMutexOrSelfKindSet = new HashSet<>();  //奖品数量限制,奖品互斥,奖品自身互斥类型

    private static final OkHttpClient okhttp = new OkHttpClient();

    public RegisterCenter(ZookeeperCenter zookeeperCenter, ColorDao colorDao, ServerConfig serverConfig){
        this.zookeeperCenter = zookeeperCenter;
        this.colorDao = colorDao;
        this.serverConfig = serverConfig;
    }



    /**
     * 系统启动进行Init
     * **/
    public void init(){
        log.info(">>>RegisterCenter init...");
        this.lock = new ReentrantReadWriteLock();
        this.rlock = this.lock.readLock();
        this.wlock = this.lock.writeLock();
        //draw time kind init
        this.drawTimeKindSet.add(1L);
        this.drawTimeKindSet.add(2L);
        this.drawTimeKindSet.add(3L);
        //other limit
        this.prizeMaxNumOrMutexOrSelfKindSet.add(-1L);
        this.prizeMaxNumOrMutexOrSelfKindSet.add(1L);
        this.prizeMaxNumOrMutexOrSelfKindSet.add(2L);
        log.info(">>>upload ip and port for zookeeper");
        updateIpForZookeeper();     //上报ip到zookeeper
        log.info(">>>>start read data from db");
        doLogic();
    }


    private void updateIpForZookeeper(){
        String url = this.serverConfig.getUrl();
        this.zookeeperCenter.register(url);
    }



    /**
     * 装载配置
     * **/
    private void doLogic() {
        this.wlock.lock();
        try{
            List<ActiveConfig> activeConfigList = colorDao.select(ActiveConfig.class).eq("online", 1).list();
            log.info(">>>>size of activeConfigList:" + activeConfigList.size());
            for(ActiveConfig act:activeConfigList){
                ImmutableMap activeMapImmute = this.checkPipleForActive(act);   //活动config
                if (activeMapImmute.get(R.object) == R.N){
                    log.info(">>>>act not ok, the reason : "+ activeMapImmute.get(R.msg).toString());
                    continue;
                }
                Active active = (Active) activeMapImmute.get(R.object);
                List<Prize> prizeList = new ArrayList<>();
                //init该活动下所有的奖品config
                List<PrizeConfig> prizeConfigList = colorDao.select(PrizeConfig.class).eq("active_id", active.getId()).eq("online", 1).list();
                log.info(">>>> size of prizeConfigList for active_id, size: "+ prizeConfigList.size() + ", active_id:"+active.getId());
                for (PrizeConfig pc:prizeConfigList){
                    ImmutableMap prizeMapImmute = this.checkPipleForPrize(pc);
                    if (prizeMapImmute.get(R.object) == R.N){
                        log.info(">>>>prize not ok, the reason : "+ prizeMapImmute.get(R.msg).toString());
                        continue;
                    }
                    Prize prize = (Prize)prizeMapImmute.get(R.object);
                    prizeList.add(prize);
                }
                log.info(">>>doLogic start active && prize put to map");
                LotteryOb lotteryOb = LotteryOb.create().Active(active).prizeList(prizeList).build();

                lobMap.putIfAbsent(Function.getLoKey(active.getId()), lotteryOb);
                activeMap.putIfAbsent(Function.getActiveKey(active.getId()), active);
                for (Prize p:prizeList){
                    prizeMap.putIfAbsent(Function.getPrizeKey(p.getId()), p);
                }
            }
        }catch (Exception ex){
            log.info(">>>doLogic Init exception, exception reason :"+ex.getMessage() + " ,please check");
        }finally {
            this.wlock.unlock();
        }
    }




    /**
     * prize model check and build
     *  */
    private ImmutableMap checkPipleForPrize(PrizeConfig pc){
        // probability check
        if (pc.getProbability() == null || pc.getProbability() < 0L){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize probability do not match!");
        }
        //prizeMaxNumKind check
        if (!this.prizeMaxNumOrMutexOrSelfKindSet.contains(pc.getPrizeMaxNumKind())){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize maxNumKind do not match!");
        }
        //prize max num set eachday or total
        if (pc.getPrizeMaxNumEachDay() < 0 || pc.getPrizeMaxNumTotal() < 0){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize maxNum each day or total do not match!");
        }
        //prizeMutexKind check
        if (!this.prizeMaxNumOrMutexOrSelfKindSet.contains(pc.getPrizeMutexKind())){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize mutexKind do not match!");
        }
        List<Long> prizeMutexEachDay = null;
        List<Long> prizeMutexTotal = null;
        if (pc.getPrizeMutexEachDay() != null && !pc.getPrizeMutexEachDay().equals("")){
            try{
                Gson gson = new Gson();
                Type type = new TypeToken<List<Long>>(){}.getType();
                prizeMutexEachDay = gson.fromJson(pc.getPrizeMutexEachDay(), type);
            }catch (Exception ex){
                log.info(">>>checkPipleForActive prizeMutexEachDay for prizeConfig fromJson error,please check..prizeConfig id:"+pc.getId());
                prizeMutexEachDay = null;
            }
        }
        if (pc.getPrizeMutexTotal()!= null && !pc.getPrizeMutexTotal().equals("")){
            try{
                Gson gson = new Gson();
                Type type = new TypeToken<List<Long>>(){}.getType();
                prizeMutexTotal = gson.fromJson(pc.getPrizeMutexTotal(), type);
            }catch (Exception ex){
                log.info(">>>checkPipleForActive prizeMutexTotal for prizeConfig fromJson error,please check..prizeConfig id:"+pc.getId());
                prizeMutexTotal = null;
            }
        }
        //prizeMutexSelfKind check
        if (!this.prizeMaxNumOrMutexOrSelfKindSet.contains(pc.getPrizeMutexSelfKind())){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize mutex Self kind do not match!");
        }
        //prizeHitDiff check
        if (pc.getPrizeHitDiff() < -1L){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize hit diff do not match!");
        }
        //prizeInternalUser check
        List<Long> prizeInternalUser = null;
        if (pc.getPrizeInternalUser()!= null && !pc.getPrizeInternalUser().equals("")){
            try{
                Gson gson = new Gson();
                Type type = new TypeToken<List<Long>>(){}.getType();
                prizeInternalUser = gson.fromJson(pc.getPrizeInternalUser(), type);
            }catch (Exception ex){
                log.info(">>>checkPipleForActive prizeInternalUser for prizeConfig fromJson error,please check..prizeConfig id:"+pc.getId());
                prizeInternalUser = null;
            }
        }
        //prizeTimeSlotKind check
        if (!this.prizeMaxNumOrMutexOrSelfKindSet.contains(pc.getPrizeTimeSlotKind())){
            return ImmutableMap.of(R.object, R.N, R.msg, "prize time slot kind do not match!");
        }
        //prizeTimeSlotEachDay && prizeTimeSlotTotal check
        List<PeriodLimit> prizeTimeSlotEachDay = null;
        List<PeriodLimit> prizeTimeSlotTotal = null;
        if (pc.getPrizeTimeSlotEachDay() != null && !pc.getPrizeTimeSlotEachDay().equals("")){
            try{
                Gson gson = new Gson();
                Type type = new TypeToken<List<PeriodLimit>>(){}.getType();
                prizeTimeSlotEachDay = gson.fromJson(pc.getPrizeTimeSlotEachDay(), type);
            }catch (Exception ex){
                log.info(">>>checkPipleForActive prizeTimeSlotEachDay for prizeConfig fromJson error,please check..prizeConfig id:"+pc.getId());
                prizeTimeSlotEachDay = null;
            }
        }
        if (pc.getPrizeTimeSlotTotal() != null && !pc.getPrizeTimeSlotTotal().equals("")){
            try{
                Gson gson = new Gson();
                Type type = new TypeToken<List<PeriodLimit>>(){}.getType();
                prizeTimeSlotTotal = gson.fromJson(pc.getPrizeTimeSlotTotal(), type);
            }catch (Exception ex){
                log.info(">>>checkPipleForActive prizeTimeSlotTotal for prizeConfig fromJson error,please check..prizeConfig id:"+pc.getId());
                prizeTimeSlotTotal = null;
            }
        }
        //build
        Prize prize = Prize.create().Id(pc.getId()).probability(pc.getProbability()).activeId(pc.getActiveId()).prizeMaxNumKind(pc.getPrizeMaxNumKind())
                .prizeMaxNumEachDay(pc.getPrizeMaxNumEachDay()).prizeMaxNumTotal(pc.getPrizeMaxNumTotal()).prizeMutexKind(pc.getPrizeMutexKind())
                .prizeMutexEachDay(prizeMutexEachDay).prizeMutexTotal(prizeMutexTotal).prizeMutexSelfKind(pc.getPrizeMutexSelfKind())
                .prizeHitDiff(pc.getPrizeHitDiff()).prizeInternalUser(prizeInternalUser).prizeTimeSlotKind(pc.getPrizeTimeSlotKind())
                .prizeTimeSlotEachDay(prizeTimeSlotEachDay).prizeTimeSlotTotal(prizeTimeSlotTotal).build();
        return ImmutableMap.of(R.object, prize, R.msg, "ok");
    }



    /**
     * active model check and build
     */
    private ImmutableMap checkPipleForActive(ActiveConfig activeConfig){
        //必须设置activeTime
        if (activeConfig.getActiveTime() == null || activeConfig.getActiveTime().equals("")){
            return ImmutableMap.of(R.object, R.N, R.msg, "active time is null");
        }
        //activeTime check && handle
        List<Period> activeTime = null;
        try{
            Gson gson = new Gson();
            Type type = new TypeToken<List<Period>>(){}.getType();
            activeTime = gson.fromJson(activeConfig.getActiveTime(), type);
        }catch (Exception ex){
            log.info(">>>checkPipleForActive activeTime for activeConfig fromJson error,please check..activeConfig id:"+activeConfig.getId());
            ex.printStackTrace();
            return ImmutableMap.of(R.object, R.N, R.msg, "active time gson error!");
        }
        Boolean between = false;
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (Period p:activeTime) {
            if (Function.between(p.getStart(), p.getEnd(), now)){
                   between = true;
                   break;
            }
        }
        if (!between){    //活动已经过期
            ActiveConfig byId = colorDao.getById(ActiveConfig.class, activeConfig.getId());
            byId.setOnline(0L);
            colorDao.update(byId);   //update db
            return ImmutableMap.of(R.object, R.N, R.msg, "active is expired!");
        }
        //drawTimeKind  check
        if (!this.drawTimeKindSet.contains(activeConfig.getDrawTimeKind())){
            return ImmutableMap.of(R.object, R.N, R.msg, "drawTimeKindSet error!");
        }
        //defaultPrizeId check
        if (activeConfig.getDefaultPrizeId() == null || activeConfig.getDefaultPrizeId() <= 0L){
            return ImmutableMap.of(R.object, R.N, R.msg, "DefaultPrizeId is null!");
        }
        PrizeConfig pc = colorDao.getById(PrizeConfig.class, activeConfig.getDefaultPrizeId());
        if (pc == null || (!pc.getActiveId().equals(activeConfig.getId()))){
            return ImmutableMap.of(R.object, R.N, R.msg, "default pc is null or pc id not match!");
        }
        if (pc.getPrizeMaxNumKind() != -1L || pc.getPrizeMutexKind() != -1L || pc.getPrizeMutexSelfKind() != -1L || pc.getPrizeHitDiff() != -1L
        || pc.getPrizeTimeSlotKind() != -1L ){
            return ImmutableMap.of(R.object, R.N, R.msg, "pc numKind|mutexKind|....|not match");
        }
        if (pc.getPrizeInternalUser() != null && !pc.getPrizeInternalUser().equals("")){
            return ImmutableMap.of(R.object, R.N, R.msg, "pc internalUser not match");
        }
        //prize online check
        if (pc.getOnline() != 1L){
            return ImmutableMap.of(R.object, R.N, R.msg, "pc online not match");
        }
        Map<Long,Long> whiteMap = null;
        //white list check
        if (activeConfig.getWhiteList() != null && !activeConfig.getWhiteList().equals("")){  //如果设置了白名单需要进行转化
            try{
                Gson gson = new Gson();
                Type type = new TypeToken<Map<Long,Long>>(){}.getType();
                whiteMap = gson.fromJson(activeConfig.getWhiteList(), type);
            }catch (Exception ex){
                log.info(">>>checkPipleForActive whiteMap for activeConfig fromJson error,please check..activeConfig id:"+activeConfig.getId());
                whiteMap = null;
            }
        }
        //build
        Active active  = Active.create().Id(activeConfig.getId()).activeTime(activeTime).drawTimeKind(activeConfig.getDrawTimeKind())
                .maxTimeEachDay(activeConfig.getMaxTimeEachDay()).maxTimeTotal(activeConfig.getMaxTimeTotal()).defaultPrizeId(activeConfig.getDefaultPrizeId())
                .whiteMap(whiteMap).build();
        return ImmutableMap.of(R.object, active, R.msg, "ok");
    }


    /**
     * 刷新配置 多节点刷新
     * **/
    @Override
    public void refresh(){
        this.wlock.lock();
        try{
            freeMemory();
            doLogic();
        }catch (Exception ex){
            log.info("refresh config fail, the reason: "+ex.getMessage() +" ,please check");
        }finally {
            this.wlock.unlock();
        }
        List<String> IpList = this.zookeeperCenter.discover();
        try {
            String localKey = this.serverConfig.getUrl();
            for (String ip:IpList) {
                log.info(">>>>> refresh logic, iplist: { " + ip + " }");
                if (!ip.equals(localKey)){
                    //send request
                    String postUrl = "http://"+ip+"/lottery/refresh/now";
                    Request request = new Request.Builder().url(postUrl).build();
                    Response response = okhttp.newCall(request).execute();
                    String result = response.body().string();
                    log.info(">>>>>>>>>>.result："+result);
                }
            }
        } catch (Exception e) {
            log.info("refresh config error,  please check,  the reason is:" + e.getMessage() + " .");
        }
    }


    /**
     * 刷新配置now
     * **/
    public void refreshNow(){
        this.wlock.lock();
        try{
            freeMemory();
            doLogic();
        }catch (Exception ex){
            log.info("refresh now config fail, the reason: "+ex.getMessage() +" ,please check");
            throw ex;
        }finally {
            this.wlock.unlock();
        }
    }



    /**
     * 释放map 内存
     * **/
    private void freeMemory(){
        this.prizeMap = new HashMap<>();
        this.activeMap = new HashMap<>();
        this.lobMap = new HashMap<>();
    }



    /**
     * 根据活动id返回活动信息
     * **/
    @Override
    public Active getActive(Long activeId) {
        this.rlock.lock();
        try{
            Active active = this.activeMap.getOrDefault(Function.getActiveKey(activeId), null);
            return active;
        }catch (Exception ex){
            log.info("register center get active fail, the reason is: "+ ex.getMessage() + " ,please check");
            return null;
        }finally {
            this.rlock.unlock();
        }
    }


    /**
     * 根据奖品id返回奖品信息
     * **/
    @Override
    public Prize getPrize(Long prizeId) {
        this.rlock.lock();
        try{
            Prize prize = this.prizeMap.getOrDefault(Function.getPrizeKey(prizeId), null);
            return prize;
        }catch (Exception ex){
            log.info("register center get prize fail, the reason is: "+ ex.getMessage() + " ,please check");
            return null;
        }finally {
            this.rlock.unlock();
        }
    }


    /**
     * 根据活动id返回 LotteryOb实体
     * **/

    @Override
    public LotteryOb getLotteryOb(Long activeId) {
        this.rlock.lock();
        try{
            LotteryOb lotteryOb = this.lobMap.getOrDefault(Function.getLoKey(activeId), null);
            return lotteryOb;
        }catch (Exception ex){
            log.info("register center get lotteryOb fail, the reason is: "+ ex.getMessage() + " ,please check");
            return null;
        }finally {
            this.rlock.unlock();
        }
    }



    /**
     * print out data of Memory
     * **/
    @Override
    public String toString() {
        Gson gson = new Gson();
        StringBuffer sb = new StringBuffer();
        sb.append("lobMap data:-----------------------");   //组合数据
        sb.append(gson.toJson(this.lobMap));
        sb.append("activeMap data:---------------------");  //活动数据
        sb.append(gson.toJson(this.activeMap));
        sb.append("prizeMap data:----------------------");   //奖品数据
        sb.append(gson.toJson(this.prizeMap));
        sb.append("zookeeper ip list data:---------------");   //zookeeper ip list
        sb.append(gson.toJson(this.zookeeperCenter.discover()));
        sb.append("local ip url:-------------------------");   //local ip url
        sb.append(gson.toJson(this.serverConfig.getUrl()));
        return sb.toString();
    }
}
