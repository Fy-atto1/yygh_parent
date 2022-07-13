package com.atguigu.yygh.user.api;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// 就诊人管理
@RestController
@RequestMapping("/api/user/patient")
public class PatientApiController {

    private final PatientService patientService;

    @Autowired
    public PatientApiController(PatientService patientService) {
        this.patientService = patientService;
    }

    // 获取就诊人列表
    @GetMapping("auth/findAll")
    public Result<List<Patient>> findAll(HttpServletRequest request) {
        // 获取当前用户的id
        Long userId = AuthContextHolder.getUserId(request);
        List<Patient> list = patientService.findAllByUserId(userId);
        return Result.ok(list);
    }

    // 添加就诊人
    @PostMapping("auth/save")
    public Result<Void> savePatient(@RequestBody Patient patient, HttpServletRequest request) {
        // 获取当前用户的id
        Long userId = AuthContextHolder.getUserId(request);
        patient.setUserId(userId);
        patientService.save(patient);
        return Result.ok();
    }

    // 根据就诊人的id获取就诊人信息
    @GetMapping("auth/get/{id}")
    public Result<Patient> getPatient(@PathVariable Long id) {
        Patient patient = patientService.getPatientById(id);
        return Result.ok(patient);
    }

    // 修改就诊人
    @PostMapping("auth/update")
    public Result<Void> updatePatient(@RequestBody Patient patient) {
        patientService.updateById(patient);
        return Result.ok();
    }

    // 删除就诊人
    @DeleteMapping("auth/remove/{id}")
    public Result<Patient> removePatient(@PathVariable Long id) {
        patientService.removeById(id);
        return Result.ok();
    }

    // 根据就诊人id获取就诊人信息
    @GetMapping("inner/get/{id}")
    public Patient getPatientOrder(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

}
