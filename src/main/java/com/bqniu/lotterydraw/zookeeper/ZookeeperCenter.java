package com.bqniu.lotterydraw.zookeeper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

/**
 * @author nbq
 * @create 2020-03-06 下午6:52
 * @desc ..
 **/
public class ZookeeperCenter implements zookeeperService {


    private Log log = LogFactory.getLog(ZookeeperCenter.class);

    private String zookeeper_server;

    private CuratorFramework client = null;

    private  String NS = "/nodeAddress";


    public ZookeeperCenter(String zs){
        log.info(">>>zookeeper center Initialization");
        this.zookeeper_server = zs;
    }



    /**
     * 初始化启动
     * **/
    public void init(){
        if (client!=null){
            return;
        }
        //创建zookeeper客户端
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder().connectString(this.zookeeper_server)
                .sessionTimeoutMs(10000)   //设置session超时时间10s
                .retryPolicy(retryPolicy)
                .namespace("lotteryDraw")
                .build();
        client.start();
        try {
            if (client.checkExists().forPath(NS)==null){
                client.create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(NS);
                log.info("zookeeper初始化成功");
            }
        } catch (Exception e){
            log.info(">>>>zookeeper init fail, the reason :" + e.getMessage() + " ");
            e.printStackTrace();
        }
    }


    /**
     * ip注册
     * **/
    @Override
    public void register(String url) {
        // 创建 node ip
        String nodePath = NS + "/" + url;
        try {
            if (client.checkExists().forPath(nodePath)==null){
                client.create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(nodePath);
                log.info("zookeeper nodePath create success, nodePath :" + nodePath + " ");
            }
        } catch (Exception e){
            log.info(">>>>zookeeper nodePath create fail");
            e.printStackTrace();
        }
    }



    /**
     * 查询节点ip端口列表
     * **/
    @Override
    public List<String> discover() {
        try {
            if (client.checkExists().forPath(NS)==null){
                return null;
            }
            List<String> ipList = client.getChildren().forPath(NS);
            //log
            for (String ip:ipList){
                log.info(">>>>> zookeeper center discover ip :{" + ip + " }");
            }
            return ipList;
        } catch (Exception e){
            log.info(">>>>zookeeper nodePath discover fail");
            return null;
        }
    }
}
