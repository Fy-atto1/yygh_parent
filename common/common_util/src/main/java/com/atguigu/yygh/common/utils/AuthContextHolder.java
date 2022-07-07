package com.atguigu.yygh.common.utils;

import com.atguigu.yygh.common.helper.JwtHelper;

import javax.servlet.http.HttpServletRequest;

// 获取当前用户信息的工具类
public class AuthContextHolder {

    // 获取当前用户的id
    public static Long getUserId(HttpServletRequest request) {
        // 从header中获取token
        String token = request.getHeader("token");
        // 使用jwt根据token获取userId
        return JwtHelper.getUserId(token);
    }

    // 获取当前用户的名称
    public static String getUserName(HttpServletRequest request) {
        // 从header中获取token
        String token = request.getHeader("token");
        // 使用jwt根据token获取userName
        return JwtHelper.getUserName(token);
    }

}
