package com.atguigu.demo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @RequestMapping("/test1")
    public String demo(){
        System.out.print("ddddd");
        return "test";
    }

}
