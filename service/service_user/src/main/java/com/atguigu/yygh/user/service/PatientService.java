package com.atguigu.yygh.user.service;

import com.atguigu.yygh.model.user.Patient;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PatientService extends IService<Patient> {
    // 获取就诊人列表
    List<Patient> findAllByUserId(Long userId);

    // 根据就诊人的id获取就诊人信息
    Patient getPatientById(Long id);
}
