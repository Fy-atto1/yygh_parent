package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.helper.JwtHelper;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.user.service.PatientService;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.atguigu.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    private RedisTemplate<String, String> redisTemplate;
    private PatientService patientService;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    // 用户手机号登录接口
    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        // 从loginVo中获取输入的手机号和验证码
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();
        // 判断手机号和验证码是否为空
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        // 判断手机验证码和输入的验证码是否一致
        String codeRedis = redisTemplate.opsForValue().get(phone);
        if (!code.equals(codeRedis)) {
            throw new YyghException(ResultCodeEnum.CODE_ERROR);
        }
        // 如果为微信登录，绑定手机号码
        UserInfo userInfo = null;
        if (!StringUtils.isEmpty(loginVo.getOpenid())) {
            userInfo = this.selectWxInfoOpenId(loginVo.getOpenid());
            if (null != userInfo) {
                userInfo.setPhone(loginVo.getPhone());
                this.updateById(userInfo);
            } else {
                throw new YyghException(ResultCodeEnum.DATA_ERROR);
            }
        }
        // 如果userInfo为空，进行正常的手机登录
        if (userInfo == null) {
            // 判断是否为第一次登录：根据手机号查询数据库，如果不存在相同手机号就是第一次登录
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", phone);
            userInfo = baseMapper.selectOne(wrapper);
            if (userInfo == null) {
                // 第一次使用这个手机号登录，添加信息到数据库
                userInfo = new UserInfo();
                userInfo.setName("");
                userInfo.setPhone(phone);
                userInfo.setStatus(1);
                baseMapper.insert(userInfo);
            }
        }
        //校验是否被禁用
        if (userInfo.getStatus() == 0) {
            throw new YyghException(ResultCodeEnum.LOGIN_DISABLED_ERROR);
        }
        // 不是第一次登录，那么直接登录
        // 返回登录信息
        // 返回登录用户名
        // 返回token
        Map<String, Object> map = new HashMap<>();
        String name = userInfo.getName();
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
        }
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getPhone();
        }
        map.put("name", name);
        // jwt生成token字符串
        String token = JwtHelper.createToken(userInfo.getId(), name);
        map.put("token", token);
        return map;
    }

    // 根据openid判断数据库中是否已经存在扫描人的信息
    @Override
    public UserInfo selectWxInfoOpenId(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        return baseMapper.selectOne(queryWrapper);
    }

    // 用户认证
    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        // 根据用户id查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        // 设置认证人的实际姓名
        userInfo.setName(userAuthVo.getName());
        // 设置其他认证信息
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());
        // 进行信息更新
        baseMapper.updateById(userInfo);
    }

    // 用户列表（条件查询带分页）
    @Override
    public IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo userInfoQueryVo) {
        // 从userInfo中获取条件值
        // 用户姓名
        String name = userInfoQueryVo.getKeyword();
        // 用户状态
        Integer status = userInfoQueryVo.getStatus();
        // 用户认证状态
        Integer authStatus = userInfoQueryVo.getAuthStatus();
        // 时间范围条件的开始时间
        String createTimeBegin = userInfoQueryVo.getCreateTimeBegin();
        // 时间范围条件的结束时间
        String createTimeEnd = userInfoQueryVo.getCreateTimeEnd();
        // 对条件值进行非空判断
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(name)) {
            queryWrapper.like("name", name);
        }
        if (!StringUtils.isEmpty(status)) {
            queryWrapper.eq("status", status);
        }
        if (!StringUtils.isEmpty(authStatus)) {
            queryWrapper.eq("auth_status", authStatus);
        }
        if (!StringUtils.isEmpty(createTimeBegin)) {
            queryWrapper.ge("create_time", createTimeBegin);
        }
        if (!StringUtils.isEmpty(createTimeEnd)) {
            queryWrapper.le("create_time", createTimeEnd);
        }
        IPage<UserInfo> pages = baseMapper.selectPage(pageParam, queryWrapper);
        // 将编码封装为对应的值
        pages.getRecords().forEach(this::packageUserInfo);
        return pages;
    }

    // 用户锁定
    @Override
    public void lock(Long userId, Integer status) {
        if (status == 0 || status == 1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setStatus(status);
            baseMapper.updateById(userInfo);
        }
    }

    // 获取用户详情
    @Override
    public Map<String, Object> show(Long userId) {
        Map<String, Object> map = new HashMap<>();
        // 根据userId查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        this.packageUserInfo(userInfo);
        map.put("userInfo", userInfo);
        // 根据userId查询就诊人信息
        List<Patient> patientList = patientService.findAllByUserId(userId);
        map.put("patientList", patientList);
        return map;
    }

    // 认证审批
    @Override
    public void approval(Long userId, Integer authStatus) {
        // 2：通过    -1：不通过
        if (authStatus == 2 || authStatus == -1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setAuthStatus(authStatus);
            baseMapper.updateById(userInfo);
        }
    }

    // 将编码封装为对应的值
    private void packageUserInfo(UserInfo userInfo) {
        // 处理用户认证状态编码
        userInfo.getParam().put("authStatusString",
                AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        // 处理用户状态编码
        String statusString = userInfo.getStatus() == 0 ? "锁定" : "正常";
        userInfo.getParam().put("statusString", statusString);
    }

}
