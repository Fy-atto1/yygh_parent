package com.atguigu.yygh.order.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PaymentService extends IService<PaymentInfo> {

    // 向支付记录表中添加信息
    void savePaymentInfo(OrderInfo order, Integer status);
}
