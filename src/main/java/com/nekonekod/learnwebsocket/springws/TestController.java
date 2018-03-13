package com.nekonekod.learnwebsocket.springws;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author: duwenjun
 * Date: 2018/03/12
 * Time: 下午7:47
 * Project: learn-websocket
 */
@RestController
public class TestController {

    @RequestMapping("test")
    public Object fun() {
        return "OK";
    }

}
