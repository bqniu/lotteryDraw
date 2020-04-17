package com.bqniu.lotterydraw.zookeeper;

import java.util.List;

/**
 * @author nbq
 * @create 2020-03-06 下午6:37
 * @desc ..
 *
 * zookeeper对外提供服务, 实现系统ip自动注册,自动发现
 **/
public interface zookeeperService {

    void register(String url);  //ip,端口注册

    List<String> discover();   //获取ip列表

}
