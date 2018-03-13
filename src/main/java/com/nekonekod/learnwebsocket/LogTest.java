package com.nekonekod.learnwebsocket;

import lombok.extern.log4j.Log4j2;

/**
 * Author: duwenjun
 * Date: 2018/03/05
 * Time: 下午2:31
 */
@Log4j2
public class LogTest {

    public static void main(String[] args) {
        log.debug("debug:{}", "debug");
        log.info("info:{}", "info");
        log.error("error:{}", "error");
    }

}
