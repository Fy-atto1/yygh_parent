package com.atguigu.yygh.order.api;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.order.service.WeixinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order/weixin")
public class WeixinController {

    private final WeixinService weixinService;

    @Autowired
    public WeixinController(WeixinService weixinService) {
        this.weixinService = weixinService;
    }

    // 生成微信支付二维码
    @GetMapping("createNative/{orderId}")
    public Result<Map<String, Object>> createNative(@PathVariable Long orderId) {
        Map<String, Object> map = weixinService.createNative(orderId);
        return Result.ok(map);
    }
}
