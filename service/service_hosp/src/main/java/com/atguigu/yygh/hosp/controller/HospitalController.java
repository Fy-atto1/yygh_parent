package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/hosp/hospital")
@CrossOrigin
public class HospitalController {
    private HospitalService hospitalService;

    @Autowired
    public void setHospitalService(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    // 医院列表 （条件查询带分页）
    @GetMapping("list/{page}/{limit}")
    public Result<Page<Hospital>> listHosp(@PathVariable Integer page,
                                           @PathVariable Integer limit,
                                           HospitalQueryVo hospitalQueryVo) {
        Page<Hospital> pageModel = hospitalService.selectHospPage(page, limit, hospitalQueryVo);
        return Result.ok(pageModel);
    }

    // 更新医院上线状态
    @ApiOperation(value = "更新医院上线状态")
    @GetMapping("updateHospStatus/{id}/{status}")
    public Result<Void> updateHospStatus(@PathVariable String id,
                                         @PathVariable Integer status) {
        hospitalService.updateStatus(id, status);
        return Result.ok();
    }

    // 医院详情信息
    @ApiOperation(value = "医院详情信息")
    @GetMapping("showHospDetail/{id}")
    public Result<Map<String, Object>> showHospDetail(@PathVariable String id) {
        Map<String, Object> map = hospitalService.getHospById(id);
        return Result.ok(map);
    }
}
