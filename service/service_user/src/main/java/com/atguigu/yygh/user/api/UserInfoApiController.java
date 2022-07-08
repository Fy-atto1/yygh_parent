package com.atguigu.yygh.user.api;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserInfoApiController {

    private final UserInfoService userInfoService;

    @Autowired
    public UserInfoApiController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    // 用户手机号登录接口
    @PostMapping("login")
    public Result<Map<String, Object>> login(@RequestBody LoginVo loginVo) {
        Map<String, Object> info = userInfoService.login(loginVo);
        return Result.ok(info);
    }

    // 用户认证接口
    @PostMapping("auth/userAuth")
    public Result<Void> userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request) {
        // 传递两个参数：用户id和认证数据vo对象
        userInfoService.userAuth(AuthContextHolder.getUserId(request), userAuthVo);
        return Result.ok();
    }

    // 根据userId获取用户信息
    @GetMapping("auth/getUserInfo")
    public Result<UserInfo> getUserInfo(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        UserInfo userInfo = userInfoService.getById(userId);
        return Result.ok(userInfo);
    }

}
