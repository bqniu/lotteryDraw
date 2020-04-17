package com.bqniu.lotterydraw.register;

import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;


/**
 奖品model
 1>probability check  > 0
 2>prizeMaxNumKind check
 3>prizeMutexKind check
 4>互斥奖品 handle  && prizeMutexSelfKind  check  && prizeHitDiff check > 0
 5> prizeInternalUser handle && prizeTimeSlotKind check&& online check && prizeTimeSlotEachDay handle
 */
@Data
public class Prize {

    private Log log = LogFactory.getLog(Prize.class);

    private Long id;   //奖品id

    private Long probability;   //奖品被抽中概率【权重】不为空,默认0

    private Long activeId;      //对应的活动id


    private Long prizeMaxNumKind;


    private Long prizeMaxNumEachDay;    //每天Max上限  0[默认]


    private Long prizeMaxNumTotal;      //全局max上限   0[默认]


    private Long prizeMutexKind;

    private List<Long> prizeMutexEachDay;    //每天互斥奖品id列表

    private List<Long> prizeMutexTotal;      //全局互斥奖品id列表

    private Long prizeMutexSelfKind;

    private Long prizeHitDiff;

    private List<Long> prizeInternalUser;

    private Long prizeTimeSlotKind;

    private List<PeriodLimit> prizeTimeSlotEachDay;

    private List<PeriodLimit> prizeTimeSlotTotal;

    private Prize(){

    }

    public static Prize create(){
        return new Prize();
    }

    public Prize Id(Long id){
        this.id = id;
        return this;
    }

    public Prize probability(Long probability){
        this.probability = probability;
        return this;
    }

    public Prize activeId(Long activeId){
        this.activeId = activeId;
        return this;
    }

    public Prize prizeMaxNumKind(Long prizeMaxNumKind){
        this.prizeMaxNumKind = prizeMaxNumKind;
        return this;
    }

    public Prize prizeMaxNumEachDay(Long prizeMaxNumEachDay){
        this.prizeMaxNumEachDay = prizeMaxNumEachDay;
        return this;
    }

    public Prize prizeMaxNumTotal(Long prizeMaxNumTotal){
        this.prizeMaxNumTotal = prizeMaxNumTotal;
        return this;
    }

    public Prize prizeMutexKind(Long prizeMutexKind){
        this.prizeMutexKind = prizeMutexKind;
        return this;
    }

    public Prize prizeMutexEachDay(List<Long> prizeMutexEachDay){
        this.prizeMutexEachDay = prizeMutexEachDay;
        return this;
    }

    public Prize prizeMutexTotal(List<Long> prizeMutexTotal){
        this.prizeMutexTotal = prizeMutexTotal;
        return this;
    }

    public Prize prizeMutexSelfKind(Long prizeMutexSelfKind){
        this.prizeMutexSelfKind = prizeMutexSelfKind;
        return this;
    }

    public Prize prizeHitDiff(Long prizeHitDiff){
        this.prizeHitDiff = prizeHitDiff;
        return this;
    }

    public Prize prizeInternalUser(List<Long> prizeInternalUser){
        this.prizeInternalUser = prizeInternalUser;
        return this;
    }

    public Prize prizeTimeSlotKind(Long prizeTimeSlotKind){
        this.prizeTimeSlotKind = prizeTimeSlotKind;
        return this;
    }

    public Prize prizeTimeSlotEachDay(List<PeriodLimit> prizeTimeSlotEachDay){
        this.prizeTimeSlotEachDay = prizeTimeSlotEachDay;
        return this;
    }

    public Prize prizeTimeSlotTotal(List<PeriodLimit> prizeTimeSlotTotal){
        this.prizeTimeSlotTotal = prizeTimeSlotTotal;
        return this;
    }


    public Prize build(){
        log.info(">>> prize ob build, prize id: { "+ this.getId() + " }");
        return this;
    }
}

