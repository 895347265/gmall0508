package com.atguigu.gmall.passport;

import com.atguigu.gmall.util.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

    @Test
    public void contextLoads() {

        Map<String, Object> atguigugamll = JwtUtil.decode("atguigugamll", "eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6ImplcnJ5IiwidXNlcklkIjoiMiJ9.wKcOalTPKXjNhKbYRAbx9S0_MAJmoLuw2IaA8y-XtGA", "127.0.0.1");

        System.out.println(atguigugamll.get("userId"));


    }

}
