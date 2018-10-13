package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void contextLoads() throws IOException, MyException {
        //加载fdfs配置文件
        String path = GmallManageWebApplicationTests.class.getClassLoader().getResource("tracker.conf").getPath();
        ClientGlobal.init(path);

        //创建一个tracker的连接
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();


        //从tracker中返回一个可用的storage
        StorageClient storageClient = new StorageClient(connection, null);

        String[] jpgs = storageClient.upload_file("d:/heihei.jpg", "jpg", null);

        String url = "http://192.168.159.188";

        for (String gif:jpgs) {
            url = url+"/"+gif;
        }
        System.err.println(url);

    }

}
