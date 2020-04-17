package com.bqniu.lotterydraw.register;

import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * @author nbq
 * @create 2020-03-04 下午6:30
 * @desc ..
 *
 * 通过活动和奖品配置,组装lotteryOb实体
 * 1 active + n prize
 **/
@Data
public class LotteryOb {

    private Log log = LogFactory.getLog(LotteryOb.class);

    private Active active;

    private List<Prize> prizeList;


    /**
     * 禁止从外部直接new对象,需要持有create引用
     * **/
    private LotteryOb(){
    }


    /**
     * init 实体,可增加前置处理
     * */
    public static LotteryOb create(){
        return new LotteryOb();
    }

    public LotteryOb Active(Active active){  //设置活动
        this.active = active;
        return this;
    }

    public LotteryOb prizeList(List<Prize> prizeList){   //设置奖品列表
        this.prizeList = prizeList;
        return this;
    }


    public LotteryOb build(){    //可扩展做一些处理
        log.info(">>>LotteryOb ob build. active id: { "+this.active.getId()+" }");
        return this;
    }

}
