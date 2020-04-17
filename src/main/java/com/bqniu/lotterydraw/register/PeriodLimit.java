package com.bqniu.lotterydraw.register;


import lombok.Data;

/**
 * @author nbq
 * @create 2020-03-05 下午2:41
 * @desc ..
 *
 * 表示某个期间,开始和结束时间,针对于奖品设置的最大值
 **/
@Data
public class PeriodLimit {

    private String start;   //开始时间  每天的时间格式:   HH:mm:ss, 整个活动时间格式: yyyy-MM-dd HH:mm:ss

    private String end;    //结束时间

    private Long max;      //最大值

    public PeriodLimit(){

    }
    public PeriodLimit(String start, String end, Long max){
        this.start = start;
        this.end = end;
        this.max = max;
    }
}
