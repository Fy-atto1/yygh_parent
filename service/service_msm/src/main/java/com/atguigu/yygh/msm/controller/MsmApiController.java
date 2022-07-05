package com.atguigu.yygh.msm.controller;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/msm")
public class MsmApiController {

    private final MsmService msmService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public MsmApiController(MsmService msmService, RedisTemplate<String, String> redisTemplate) {
        this.msmService = msmService;
        this.redisTemplate = redisTemplate;
    }

    // 发送手机验证码
    @GetMapping("send/{phone}")
    public Result<Object> sendCode(@PathVariable String phone) {
        // 从redis中根据手机号获取验证码，如果能获取到，返回ok
        String code = redisTemplate.opsForValue().get(phone);
        if (!StringUtils.isEmpty(code)) {
            return Result.ok();
        }
        // 如果从redis中获取不到
        // 生成验证码
        code = RandomUtil.getSixBitRandom();
        // 调用service方法，通过整合短信服务进行发送
        boolean isSend = msmService.send(phone, code);
        // 将生成的验证码放到redis里面，并设置有效时间
        if (isSend) {
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
            return Result.ok();
        } else {
            return Result.fail().message("发送短信失败");
        }
    }
}
