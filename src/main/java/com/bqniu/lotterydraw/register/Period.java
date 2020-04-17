package com.bqniu.lotterydraw.register;


import lombok.Data;

/**
 * @author nbq
 * @create 2020-03-05 下午2:41
 * @desc ..
 *
 * 表示某个期间,开始和结束时间,针对于活动
 **/
@Data
public class Period{

    private String start;   //开始时间

    private String end;    //结束时间

    public Period(){

    }

    public Period(String start, String end){
        this.start = start;
        this.end = end;
    }

}
