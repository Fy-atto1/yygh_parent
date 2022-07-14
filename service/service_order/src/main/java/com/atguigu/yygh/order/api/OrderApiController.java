package com.atguigu.yygh.order.api;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.order.service.OrderService;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order/orderInfo")
public class OrderApiController {

    private final OrderService orderService;

    @Autowired
    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 生成挂号订单
    @PostMapping("auth/submitOrder/{scheduleId}/{patientId}")
    public Result<Long> saveOrders(@PathVariable String scheduleId,
                                   @PathVariable Long patientId) {
        Long orderId = orderService.saveOrder(scheduleId, patientId);
        return Result.ok(orderId);
    }

    // 根据订单id查询订单详情
    @GetMapping("auth/getOrders/{orderId}")
    public Result<OrderInfo> getOrders(@PathVariable String orderId) {
        OrderInfo orderInfo = orderService.getOrder(orderId);
        return Result.ok(orderInfo);
    }

    // 订单列表（条件查询带分页）
    @GetMapping("auth/{page}/{limit}")
    public Result<IPage<OrderInfo>> list(@PathVariable Long page,
                                         @PathVariable Long limit,
                                         OrderQueryVo orderQueryVo,
                                         HttpServletRequest request) {
        // 仅可以查询当前用户的订单
        orderQueryVo.setUserId(AuthContextHolder.getUserId(request));
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        IPage<OrderInfo> pageModel = orderService.selectPage(pageParam, orderQueryVo);
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "获取订单状态")
    @GetMapping("auth/getStatusList")
    public Result<List<Map<String, Object>>> getStatusList() {
        return Result.ok(OrderStatusEnum.getStatusList());
    }

}
