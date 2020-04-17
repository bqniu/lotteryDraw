package com.bqniu.lotterydraw.register;

/**
 * @author nbq
 * @create 2020-03-04 下午6:13
 * @desc ..
 * 注册接口[管理配置信息]
 *
 * 1> 通过活动id查询所有信息[活动和奖品配置信息]
 * 2> refresh接口[增删改oper] 需要refresh多节点处理
 * 3> 根据id 查询活动或者奖品config
 *
 **/
public interface Register{

    LotteryOb getLotteryOb(Long activeId);

    void refresh();

    Active getActive(Long activeId);

    Prize getPrize(Long prizeId);

}
