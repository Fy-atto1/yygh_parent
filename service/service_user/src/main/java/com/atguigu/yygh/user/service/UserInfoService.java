package com.atguigu.yygh.user.service;

import com.atguigu.yygh.vo.user.LoginVo;

import java.util.Map;

public interface UserInfoService {
    // 用户手机号登录接口
    Map<String, Object> login(LoginVo loginVo);
}
