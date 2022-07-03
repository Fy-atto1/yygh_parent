package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.helper.HttpRequestHelper;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.common.utils.MD5;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/hosp")
public class ApiController {

    private HospitalService hospitalService;
    private HospitalSetService hospitalSetService;
    private DepartmentService departmentService;
    private ScheduleService scheduleService;

    @Autowired
    public void setHospitalService(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @Autowired
    public void setHospitalSetService(HospitalSetService hospitalSetService) {
        this.hospitalSetService = hospitalSetService;
    }

    @Autowired
    public void setDepartmentService(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Autowired
    public void setScheduleService(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // 删除排班接口
    @PostMapping("schedule/remove")
    public Result<Void> removeSchedule(HttpServletRequest request) {
        // 获取传递过来的科室信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 获取排班编号
        String hosScheduleId = (String) parameterMap.get("hosScheduleId");
        scheduleService.remove(hoscode, hosScheduleId);
        return Result.ok();
    }

    // 查询排班接口
    @PostMapping("schedule/list")
    public Result<Page<Schedule>> findSchedule(HttpServletRequest request) {
        // 获取传递过来的排班信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 当前页和每页记录数
        String pageParam = (String) parameterMap.get("page");
        int page = StringUtils.isEmpty(pageParam) ? 1 : Integer.parseInt(pageParam);
        String limitParam = (String) parameterMap.get("limit");
        int limit = StringUtils.isEmpty(limitParam) ? 1 : Integer.parseInt(limitParam);
        // 获取科室编号
        String depcode = (String) parameterMap.get("depcode");
        // 构建查询条件
        ScheduleQueryVo scheduleQueryVo = new ScheduleQueryVo();
        scheduleQueryVo.setHoscode(hoscode);
        scheduleQueryVo.setDepcode(depcode);
        // 调用service方法
        Page<Schedule> pageModel = scheduleService.finaPageSchedule(page, limit, scheduleQueryVo);
        return Result.ok(pageModel);
    }

    // 上传排班接口
    @PostMapping("saveSchedule")
    public Result<Void> saveSchedule(HttpServletRequest request) {
        // 获取传递过来的排班信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 调用service的方法
        scheduleService.save(parameterMap);
        return Result.ok();
    }

    // 删除科室接口
    @PostMapping("department/remove")
    public Result<Void> removeDepartment(HttpServletRequest request) {
        // 获取传递过来的科室信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 获取科室编号
        String depcode = (String) parameterMap.get("depcode");
        departmentService.remove(hoscode, depcode);
        return Result.ok();
    }

    // 查询科室接口
    @PostMapping("department/list")
    public Result<Page<Department>> findDepartment(HttpServletRequest request) {
        // 获取传递过来的科室信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 当前页和每页记录数
        String pageParam = (String) parameterMap.get("page");
        int page = StringUtils.isEmpty(pageParam) ? 1 : Integer.parseInt(pageParam);
        String limitParam = (String) parameterMap.get("limit");
        int limit = StringUtils.isEmpty(limitParam) ? 1 : Integer.parseInt(limitParam);
        // 构建查询条件
        DepartmentQueryVo departmentQueryVo = new DepartmentQueryVo();
        departmentQueryVo.setHoscode(hoscode);
        // 调用service方法
        Page<Department> pageModel = departmentService.finaPageDepartment(page, limit, departmentQueryVo);
        return Result.ok(pageModel);
    }

    // 上传科室接口
    @PostMapping("saveDepartment")
    public Result<Void> saveDepartment(HttpServletRequest request) {
        // 获取传递过来的科室信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 调用service的方法
        departmentService.save(parameterMap);
        return Result.ok();
    }

    // 查询医院接口
    @PostMapping("hospital/show")
    public Result<Hospital> getHospital(HttpServletRequest request) {
        // 获取传递过来的医院信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // 调用service方法实现根据医院编号查询
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        return Result.ok(hospital);
    }

    // 上传医院接口
    @PostMapping("saveHospital")
    public Result<Void> saveHosp(HttpServletRequest request) {
        // 获取传递过来的医院信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestMap);
        // 1 获取医院系统传递过来的签名，签名进行了MD5加密
        String hospSign = (String) parameterMap.get("sign");
        // 2 根据传递过来的医院编码，查询数据库，查询签名
        String hoscode = (String) parameterMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        // 3 把数据库查询的签名进行MD5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        // 4 判断签名是否一致
        if (!hospSign.equals(signKeyMD5)) {
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        // base64图片编码包含大量的加号，在传输过程中"+"会变成" "，因此需要转换回来
        String logoData = (String) parameterMap.get("logoData");
        logoData = logoData.replaceAll(" ", "+");
        parameterMap.put("logoData", logoData);
        // 调用service的方法
        hospitalService.save(parameterMap);
        return Result.ok();
    }
}
