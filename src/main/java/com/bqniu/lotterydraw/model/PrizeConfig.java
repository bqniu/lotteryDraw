package com.bqniu.lotterydraw.model;

import com.colorv.commons.orm.DBColumn;
import com.colorv.commons.orm.DBTable;
import lombok.Data;

import java.util.Date;

/**
 * Created by nbq on 2020-02-26
 * 奖品config
 *
 */

@Data
@DBTable(value = "cv_prize_config", ds = "LotteryDB")
public class PrizeConfig {

    @DBColumn
    private Long id;

    @DBColumn
    private String prizeName;   //奖品名称

    @DBColumn
    private Long probability;   //奖品被抽中概率【权重】不为空,默认0

    @DBColumn
    private Long activeId;      //对应的活动id


    /**
     * 奖品数量限制
     * -1：表示不受数量限制[默认]
     * 1: 每天设置数量上限
     * 2：整个活动期间设置数量上限
     * **/
    @DBColumn
    private Long prizeMaxNumKind;

    @DBColumn
    private Long prizeMaxNumEachDay;    //每天Max上限  0[默认]

    @DBColumn
    private Long prizeMaxNumTotal;      //全局max上限   0[默认]


    /**
     * 奖品互斥限制
     * -1：表示不受奖品互斥限制[默认]
     * 1: 每天互斥的奖品id列表
     * 2：整个活动期间互斥的奖品id列表
     * **/
    @DBColumn
    private Long prizeMutexKind;

    @DBColumn
    private String prizeMutexEachDay;    //每天互斥奖品id列表  ''[默认]

    @DBColumn
    private String prizeMutexTotal;      //全局互斥奖品id列表  ''[默认]



    /**
     * 奖品自身互斥限制
     * -1：表示奖品自身不互斥[默认]
     * 1: 每天奖品自身互斥
     * 2：整个活动期间奖品自身互斥
     * **/
    @DBColumn
    private Long prizeMutexSelfKind;

    /**
     * 奖品被抽中时间限制[奖品两次连续抽中的时间差,针对于全量用户,秒单位]
     * -1：表示不受时间差限制[默认]
     * **/
    @DBColumn
    private Long prizeHitDiff;

    /**
     * 奖品内定用户列表[奖品只能被列表中的用户抽中]
     * '':表示没有内定列表[默认]
     * **/
    @DBColumn
    private String prizeInternalUser;


    /**
     * 奖品时间段限制
     * -1：表示不受奖品时间段限制[默认]
     * 1: 每天时间段限制
     * 2：全局时间段限制
     * **/
    @DBColumn
    private Long prizeTimeSlotKind;

    @DBColumn
    private String prizeTimeSlotEachDay;    // ''[默认]

    @DBColumn
    private String prizeTimeSlotTotal;      // ''[默认]


    @DBColumn
    private Long online;   //奖品是否有效 0表示下线[默认]  1表示上线


    @DBColumn
    private String extend;    //扩展信息json string


    @DBColumn
    private Date createdAt;

    @DBColumn
    private Date updatedAt;
}

