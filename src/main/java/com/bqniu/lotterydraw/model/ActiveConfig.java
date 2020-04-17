package com.bqniu.lotterydraw.model;

import com.colorv.commons.orm.DBColumn;
import com.colorv.commons.orm.DBTable;
import lombok.Data;

import java.util.Date;

/**
 * Created by nbq on 2020-02-26
 * 抽奖活动config 以及用户抽奖次数config
 *
 */

@Data
@DBTable(value = "cv_active_config", ds = "LotteryDB")
public class ActiveConfig {

    @DBColumn
    private Long id;

    @DBColumn
    private String name;   //抽奖活动名称

    @DBColumn
    private Long online;   //活动状态 0表示下线[默认] 1表示上线

    @DBColumn
    private String activeTime;    //活动时间配置，可以配置多个时间段,只允许在多个时间段内抽奖


    /**
     * 用户抽奖次数 类型：目前支持3种，并且3种互斥
     * 1: 每人每天n次抽奖上限[默认]
     * 2：前置事件完成，触发后继给用户加抽奖次数  [redis,是否考虑活动结束后数据持久化]
     * 3：每人整个活动期间总共有n次抽奖次数上限
     *
     * **/
    @DBColumn
    private Long drawTimeKind;


    /**
     * 如果上述kind=1,则必须设置下面的值，默认0
     * **/
    @DBColumn
    private Long maxTimeEachDay;




    /**
     * 如果上述kind=3,则必须设置下面的值，默认0
     * **/
    @DBColumn
    private Long maxTimeTotal;


    @DBColumn
    private String extend;    //活动的扩展信息 json


    @DBColumn
    private Long defaultPrizeId;   //没有通过黑盒限制之后,设置的返回奖品id,必须设置

    @DBColumn
    private String whiteList;     //白名单,设置某些用户直接中某些奖,不需要概率处理json


    @DBColumn
    private Date createdAt;

    @DBColumn
    private Date updatedAt;
}

