package com.atguigu.yygh.order.service;

import java.util.Map;

public interface WeixinService {

    // 生成微信支付二维码
    Map<String, Object> createNative(Long orderId);

    // 调用微信接口查询支付状态
    Map<String, String> queryPayStatus(Long orderId);
}
