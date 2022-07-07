package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.cmn.client.DictFeignClient;
import com.atguigu.yygh.enums.DictEnum;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.mapper.PatientMapper;
import com.atguigu.yygh.user.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PatientServiceImpl
        extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    private DictFeignClient dictFeignClient;

    @Autowired
    public void setDictFeignClient(DictFeignClient dictFeignClient) {
        this.dictFeignClient = dictFeignClient;
    }

    // 获取就诊人列表
    @Override
    public List<Patient> findAllByUserId(Long userId) {
        // 根据userId查询所有就诊人信息的列表
        QueryWrapper<Patient> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<Patient> patientList = baseMapper.selectList(queryWrapper);
        // 通过远程调用，查询数据字典
        // 封装patient对象中的其他参数
        patientList.forEach(this::packPatient);
        return patientList;
    }

    // 根据就诊人的id获取就诊人信息
    @Override
    public Patient getPatientById(Long id) {
        Patient patient = baseMapper.selectById(id);
        this.packPatient(patient);
        return patient;
    }

    // 封装patient对象中的其他参数
    private void packPatient(Patient patient) {
        // 根据证件类型编码，获取证件类型的具体名称
        String certificatesTypeString = dictFeignClient
                .getName(DictEnum.CERTIFICATES_TYPE.getDictCode(), patient.getCertificatesType());
        // 联系人证件类型
        String contactsCertificatesTypeString;
        if (StringUtils.isEmpty(patient.getContactsCertificatesType())) {
            contactsCertificatesTypeString = null;
        } else {
            contactsCertificatesTypeString = dictFeignClient
                    .getName(DictEnum.CERTIFICATES_TYPE.getDictCode(), patient.getContactsCertificatesType());
        }
        // 省
        String provinceString = dictFeignClient.getName(patient.getProvinceCode());
        // 市
        String cityString = dictFeignClient.getName(patient.getCityCode());
        // 区
        String districtString = dictFeignClient.getName(patient.getDistrictCode());
        // 封装参数
        patient.getParam().put("certificatesTypeString", certificatesTypeString);
        patient.getParam().put("contactsCertificatesTypeString", contactsCertificatesTypeString);
        patient.getParam().put("provinceString", provinceString);
        patient.getParam().put("cityString", cityString);
        patient.getParam().put("districtString", districtString);
        patient.getParam().put("fullAddress",
                provinceString + cityString + districtString + patient.getAddress());
    }
}
