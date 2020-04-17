package com.bqniu.lotterydraw.common;

import com.colorv.commons.orm.ColorDao;
import com.colorv.lotterydraw.register.RegisterCenter;
import com.colorv.lotterydraw.register.ServerConfig;
import com.colorv.lotterydraw.zookeeper.ZookeeperCenter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author nbq
 * @create 2020-03-04 下午5:22
 * @desc ..
 *
 * 配置中心
 *
 **/
@Configuration(value = "lotteryConfig")
public class LotteryConfig {

    /**
     * 抽奖db配置
     * **/
    @Bean
    @ConfigurationProperties(prefix = "app.datasource.color-lottery")
    public DataSource LotteryDB() {
        return DataSourceBuilder.create().build();
    }


    /**
     * 抽奖redis配置,单节点
     * **/
    @Bean
    public JedisPool lotteryRedis(@Value("${app.redis-lottery}") String uri) throws URISyntaxException {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(64);
        poolConfig.setMaxIdle(32);
        return new JedisPool(poolConfig, new URI(uri));
    }



    /**
     * zookeeper center
     * **/
    @Bean(initMethod = "init")
    public ZookeeperCenter zookeeperCenterLottery(@Value("${app.zookeeper-single}") String zurl){
        ZookeeperCenter zookeeperCenter = new ZookeeperCenter(zurl);
        return zookeeperCenter;
    }


    /**
     * register config center
     * **/
    @Bean(initMethod = "init")
    @DependsOn(value = "LotteryDB")
    public RegisterCenter registerCenterLottery(@Qualifier(value = "zookeeperCenterLottery") ZookeeperCenter zookeeperCenter, ColorDao colorDao, ServerConfig serverConfig){
        RegisterCenter registerCenter = new RegisterCenter(zookeeperCenter, colorDao, serverConfig);
        return registerCenter;
    }

}
