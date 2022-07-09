package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Autowired
    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // 上传科室接口
    @Override
    public void save(Map<String, Object> parameterMap) {
        // 把map集合转换为department对象
        String mapString = JSONObject.toJSONString(parameterMap);
        Department department = JSONObject.parseObject(mapString, Department.class);
        // 根据医院编号和科室编号判断是否已经存在这条数据
        Department departmentExist = departmentRepository
                .getDepartmentByHoscodeAndDepcode(department.getHoscode(), department.getDepcode());
        // 如果已经存在，进行修改操作
        if (departmentExist != null) {
            departmentExist.setUpdateTime(new Date());
            departmentExist.setIsDeleted(0);
            departmentRepository.save(departmentExist);
        } else {
            // 如果不存在，进行添加操作
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            department.setIsDeleted(0);
            departmentRepository.save(department);
        }
    }

    // 查询科室接口
    @Override
    public Page<Department> finaPageDepartment(int page, int limit, DepartmentQueryVo departmentQueryVo) {
        // 创建pageable对象，设置当前页和每页记录数
        // page为0代表第一页
        Pageable pageable = PageRequest.of(page - 1, limit);
        // 创建Example对象
        Department department = new Department();
        BeanUtils.copyProperties(departmentQueryVo, department);
        department.setIsDeleted(0);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Department> example = Example.of(department, matcher);
        return departmentRepository.findAll(example, pageable);
    }

    // 删除科室接口
    @Override
    public void remove(String hoscode, String depcode) {
        // 根据医院编号和科室编号进行查询
        Department department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        if (department != null) {
            departmentRepository.deleteById(department.getId());
        }
    }

    // 根据医院编号，查询医院的所有科室列表
    @Override
    public List<DepartmentVo> findDeptTree(String hoscode) {
        // 创建list集合，用于最终的数据封装
        List<DepartmentVo> result = new ArrayList<>();
        // 根据医院编号，查询医院的所有科室信息
        Department departmentQuery = new Department();
        departmentQuery.setHoscode(hoscode);
        Example<Department> example = Example.of(departmentQuery);
        // 得到所有科室列表
        List<Department> departmentList = departmentRepository.findAll(example);
        // 根据大科室编号bigcode进行分组，然后获取每个大科室的下级子科室
        Map<String, List<Department>> departmentMap =
                departmentList.stream().collect(Collectors.groupingBy(Department::getBigcode));
        // 遍历map集合
        for (Map.Entry<String, List<Department>> entry : departmentMap.entrySet()) {
            // 大科室编号
            String bigcode = entry.getKey();
            // 大科室编号对应的全部数据
            List<Department> bigDepartmentList = entry.getValue();
            // 封装大科室
            DepartmentVo departmentVo = new DepartmentVo();
            departmentVo.setDepcode(bigcode);
            departmentVo.setDepname(bigDepartmentList.get(0).getBigname());
            // 封装小科室
            List<DepartmentVo> childrenList = new ArrayList<>();
            for (Department department : bigDepartmentList) {
                DepartmentVo departmentVo1 = new DepartmentVo();
                departmentVo1.setDepcode(department.getDepcode());
                departmentVo1.setDepname(department.getDepname());
                // 封装到list集合
                childrenList.add(departmentVo1);
            }
            // 把小科室的list集合放到大科室的children里面
            departmentVo.setChildren(childrenList);
            // 放到最终的result里面
            result.add(departmentVo);
        }
        // 返回最终的封装结果
        return result;
    }

    // 根据医院编号和科室编号，查询科室名称
    @Override
    public String getDepName(String hoscode, String depcode) {
        Department department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        return department != null ? department.getDepname() : null;
    }

    // 根据医院编号和科室编号，查询科室
    @Override
    public Department getDepartment(String hoscode, String depcode) {
        return departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
    }
}
