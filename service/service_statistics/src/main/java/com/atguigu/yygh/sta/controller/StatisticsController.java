package com.atguigu.yygh.sta.controller;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.order.client.OrderFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    private final OrderFeignClient orderFeignClient;

    @Autowired
    public StatisticsController(OrderFeignClient orderFeignClient) {
        this.orderFeignClient = orderFeignClient;
    }

    // 获取预约统计数据
    @GetMapping("getCountMap")
    public Result<Map<String, Object>> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        Map<String, Object> countMap = orderFeignClient.getCountMap(orderCountQueryVo);
        return Result.ok(countMap);
    }

}
