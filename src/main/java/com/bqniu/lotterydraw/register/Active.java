package com.bqniu.lotterydraw.register;

import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 活动model
 1> online check
 2> handle active period && active period check
 3> drawTimeKind  check
 4> defaultPrizeId check
 5> handle white list
 */
@Data
public class Active {

    private Log log = LogFactory.getLog(Active.class);

    private Long id;   //活动id,作为key

    private List<Period> activeTime;   //活动时间config,支持多个

    private Long drawTimeKind;   // 1,2,3


    private Long maxTimeEachDay;


    private Long maxTimeTotal;


    private Long defaultPrizeId;   //默认奖品id

    private Map<Long,Long> whiteMap;  //白名单,指定某些用户中某些奖,不需要概率处理


    private Active(){

    }

    public static Active create(){    //init入口
        return new Active();
    }

    public Active Id(Long id){
        this.id = id;
        return this;
    }

    public Active activeTime(List<Period> activeTime){
        this.activeTime = activeTime;
        return this;
    }

    public Active drawTimeKind(Long drawTimeKind){
        this.drawTimeKind = drawTimeKind;
        return this;
    }

    public Active maxTimeEachDay(Long maxTimeEachDay){
        this.maxTimeEachDay = maxTimeEachDay;
        return this;
    }

    public Active maxTimeTotal(Long maxTimeTotal){
        this.maxTimeTotal = maxTimeTotal;
        return this;
    }

    public Active defaultPrizeId(Long defaultPrizeId){
        this.defaultPrizeId = defaultPrizeId;
        return this;
    }

    public Active whiteMap(Map whiteMap){
        this.whiteMap = whiteMap;
        return this;
    }

    public Active build(){
        log.info(">>> active ob build, active id: { "+ this.getId()+" }");
        return this;
    }

}

