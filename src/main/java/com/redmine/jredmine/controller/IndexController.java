package com.redmine.jredmine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author panfeng
 * @create 2025-01-25-16:41
 */
@RestController
public class IndexController {

    @GetMapping("/index")
    public String index() {
        return "Hello, Redmine!";
    }
}
