package com.atguigu.yygh.order.api;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.order.service.PaymentService;
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

    private PaymentService paymentService;

    @Autowired
    public WeixinController(WeixinService weixinService) {
        this.weixinService = weixinService;
    }

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 生成微信支付二维码
    @GetMapping("createNative/{orderId}")
    public Result<Map<String, Object>> createNative(@PathVariable Long orderId) {
        Map<String, Object> map = weixinService.createNative(orderId);
        return Result.ok(map);
    }

    // 查询支付状态
    @GetMapping("queryPayStatus/{orderId}")
    public Result<Object> queryPayStatus(@PathVariable Long orderId) {
        // 调用微信接口查询支付状态
        Map<String, String> resultMap = weixinService.queryPayStatus(orderId);
        // 判断支付状态
        if (resultMap == null) {
            return Result.fail().message("支付出错");
        }
        if ("SUCCESS".equals(resultMap.get("trade_state"))) {
            // 支付成功
            // 获取订单编码
            String out_trade_no = resultMap.get("out_trade_no");
            // 更新订单状态
            paymentService.paySuccess(out_trade_no, resultMap);
            return Result.ok().message("支付成功");
        }
        return Result.ok().message("支付中");
    }

}
