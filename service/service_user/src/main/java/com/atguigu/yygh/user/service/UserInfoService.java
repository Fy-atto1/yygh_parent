package com.atguigu.yygh.user.service;

import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.vo.user.LoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    // 用户手机号登录接口
    Map<String, Object> login(LoginVo loginVo);

    // 根据openid判断数据库中是否已经存在扫描人的信息
    UserInfo selectWxInfoOpenId(String openid);
}
