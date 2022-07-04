package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.cmn.client.DictFeignClient;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;

    private DictFeignClient dictFeignClient;

    @Autowired
    public HospitalServiceImpl(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Autowired
    public void setDictFeignClient(DictFeignClient dictFeignClient) {
        this.dictFeignClient = dictFeignClient;
    }

    @Override
    public void save(Map<String, Object> parameterMap) {
        // 把map集合转换为hospital对象
        String mapString = JSONObject.toJSONString(parameterMap);
        Hospital hospital = JSONObject.parseObject(mapString, Hospital.class);
        // 判断是否已经存在这条数据
        String hoscode = hospital.getHoscode();
        Hospital hospitalExist = hospitalRepository.getHospitalByHoscode(hoscode);
        // 如果已经存在，进行修改操作
        if (hospitalExist != null) {
            hospital.setStatus(hospitalExist.getStatus());
            hospital.setCreateTime(hospitalExist.getCreateTime());
        } else {
            // 如果不存在，进行添加操作
            hospital.setStatus(0);
            hospital.setCreateTime(new Date());
        }
        hospital.setUpdateTime(new Date());
        hospital.setIsDeleted(0);
        hospitalRepository.save(hospital);
    }

    // 根据医院编号查询
    @Override
    public Hospital getByHoscode(String hoscode) {
        return hospitalRepository.getHospitalByHoscode(hoscode);
    }

    // 医院列表 （条件查询带分页）
    @Override
    public Page<Hospital> selectHospPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo) {
        // 将hospitalQueryVo对象转换为hospital对象
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo, hospital);
        // 创建条件匹配器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        // 创建Example对象
        Example<Hospital> example = Example.of(hospital, matcher);
        // 创建pageable对象
        Pageable pageable = PageRequest.of(page - 1, limit);
        // 调用方法实现查询
        Page<Hospital> pages = hospitalRepository.findAll(example, pageable);
        // 获取查询list集合，遍历进行医院等级封装
//        pages.getContent().stream().forEach(item -> {
//            this.setHospitalHosType(item);
//        });
        pages.getContent().forEach(this::setHospitalHosType);
        return pages;
    }

    // 更新医院上线状态
    @Override
    public void updateStatus(String id, Integer status) {
        // 根据id查询医院信息
        Hospital hospital = hospitalRepository.findById(id).orElse(null);
        // 设置修改的值
        if (hospital != null) {
            hospital.setStatus(status);
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);
        }
    }

    // 医院详情信息
    @Override
    public Map<String, Object> getHospById(String id) {
        Map<String, Object> map = new HashMap<>();
        Hospital hospital = hospitalRepository.findById(id).orElse(null);
        this.setHospitalHosType(Objects.requireNonNull(hospital));
        // 医院基本信息（包含医院等级）
        map.put("hospital", hospital);
        map.put("bookingRule", hospital.getBookingRule());
        // 不需要重复返回
        hospital.setBookingRule(null);
        return map;
    }

    // 获取医院名称
    @Override
    public String getHospName(String hoscode) {
        Hospital hospital = hospitalRepository.getHospitalByHoscode(hoscode);
        return hospital != null ? hospital.getHosname() : null;
    }

    // 根据医院名称查询
    @Override
    public List<Hospital> findByHosName(String hosname) {
        return hospitalRepository.getHospitalByHosnameLike(hosname);
    }

    // 根据医院编号获取医院预约挂号详情
    @Override
    public Map<String, Object> item(String hoscode) {
        Map<String, Object> result = new HashMap<>();
        // 设置医院详情
        Hospital hospital = this.getByHoscode(hoscode);
        this.setHospitalHosType(hospital);
        result.put("hospital", hospital);
        // 预约规则
        result.put("bookingRule", hospital.getBookingRule());
        // 不需要重复返回预约规则
        hospital.setBookingRule(null);
        return result;
    }

    // 获取查询list集合，遍历进行医院等级封装
    private void setHospitalHosType(Hospital hospital) {
        // 根据dictCode和value获取医院等级名称
        String hostypeString = dictFeignClient.getName("Hostype", hospital.getHostype());
        // 查询省、市和地区
        String provinceString = dictFeignClient.getName(hospital.getProvinceCode());
        String cityString = dictFeignClient.getName(hospital.getCityCode());
        String districtString = dictFeignClient.getName(hospital.getDistrictCode());
        hospital.getParam().put("fullAddress", provinceString + cityString + districtString);
        hospital.getParam().put("hostypeString", hostypeString);
    }
}
