package com.bqniu.lotterydraw.register;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author nbq
 * @create 2020-04-01 下午3:17
 * @desc ..获取本机Ip 端口
 **/

@Component
public class ServerConfig{

    private Log log = LogFactory.getLog(ServerConfig.class);

    private int port = 8000;


    //ip:port
    public String getUrl() {
        InetAddress ip4 = null;
        String hostAddress = null;
        try {
            ip4 = Inet4Address.getLocalHost();
            hostAddress = ip4.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String url = hostAddress + ":" + this.port;
        log.info(">>>server config url : "+ url);
        return url;
    }
}
