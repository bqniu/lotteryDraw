package com.bqniu.lotterydraw.model;

import com.colorv.commons.orm.DBColumn;
import com.colorv.commons.orm.DBTable;
import lombok.Data;

import java.util.Date;

/**
 * Created by nbq on 2020-02-26
 * 抽奖明细
 *
 */

@Data
@DBTable(value = "cv_draw_log", ds = "LotteryDB")
public class DrawLog {

    @DBColumn
    private Long id;

    @DBColumn
    private Long userId;

    @DBColumn
    private Long activeId;

    @DBColumn
    private Long prizeId;

    @DBColumn
    private Long success;   //抽奖是否成功 0:失败  1:成功

    @DBColumn
    private String reason;   //抽奖失败的原因

    @DBColumn
    private Long pass;   //是否通过后置调用链处理  0:未通过  1:通过

    @DBColumn
    private String chain;   //没有通过后置调用链处理的原因


    @DBColumn
    private Long drawTime;   //已经抽奖的次数

    @DBColumn
    private Long freeTime;   //剩余抽奖次数


    @DBColumn
    private Date createdAt;

    @DBColumn
    private Date updatedAt;
}

