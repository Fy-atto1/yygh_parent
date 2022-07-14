package com.atguigu.yygh.order.service;

import java.util.Map;

public interface WeixinService {

    // 生成微信支付二维码
    Map<String, Object> createNative(Long orderId);
}
