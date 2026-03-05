package com.redis.demos.smarttriage.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoWebController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
