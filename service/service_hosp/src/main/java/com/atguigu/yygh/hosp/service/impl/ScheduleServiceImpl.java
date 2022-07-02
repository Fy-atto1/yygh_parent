package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MongoTemplate mongoTemplate;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;

    @Autowired
    public ScheduleServiceImpl(ScheduleRepository scheduleRepository, MongoTemplate mongoTemplate, HospitalService hospitalService, DepartmentService departmentService) {
        this.scheduleRepository = scheduleRepository;
        this.mongoTemplate = mongoTemplate;
        this.hospitalService = hospitalService;
        this.departmentService = departmentService;
    }

    // 上传排班接口
    @Override
    public void save(Map<String, Object> parameterMap) {
        // 把map集合转换为department对象
        String mapString = JSONObject.toJSONString(parameterMap);
        Schedule schedule = JSONObject.parseObject(mapString, Schedule.class);
        // 根据医院编号和排班编号判断是否已经存在这条数据
        Schedule scheduleExist = scheduleRepository
                .getScheduleByHoscodeAndHosScheduleId(schedule.getHoscode(), schedule.getHosScheduleId());
        if (scheduleExist != null) {
            // 如果已经存在，进行修改操作
            scheduleExist.setUpdateTime(new Date());
            scheduleExist.setIsDeleted(0);
            scheduleExist.setStatus(1);
            scheduleRepository.save(scheduleExist);
        } else {
            // 如果不存在，进行添加操作
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);
        }
    }

    // 查询排班接口
    @Override
    public Page<Schedule> finaPageSchedule(int page, int limit, ScheduleQueryVo scheduleQueryVo) {
        // 创建pageable对象，设置当前页和每页记录数
        // page为0代表第一页
        Pageable pageable = PageRequest.of(page - 1, limit);
        // 创建Example对象
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(scheduleQueryVo, schedule);
        schedule.setIsDeleted(0);
        schedule.setStatus(1);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Schedule> example = Example.of(schedule, matcher);
        return scheduleRepository.findAll(example, pageable);
    }

    // 删除排班接口
    @Override
    public void remove(String hoscode, String hosScheduleId) {
        // 根据医院编号和排班编号进行查询
        Schedule schedule = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if (schedule != null) {
            scheduleRepository.deleteById(schedule.getId());
        }
    }

    // 根据医院编号和科室编号，查询排班规则数据
    @Override
    public Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode) {
        // 1 根据医院编号和科室编号查询排班
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);
        // 2 根据工作日期workDate进行分组
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria), // 匹配条件
                Aggregation.group("workDate") // 分组字段
                        .first("workDate").as("workDate")
                        // 3 统计号源数量
                        .count().as("docCount")
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber"),
                // 根据工作日期进行排序
                Aggregation.sort(Sort.Direction.DESC, "workDate"),
                // 4 实现分页
                Aggregation.skip((page - 1) * limit),
                Aggregation.limit(limit)
        );
        // 调用方法，最终执行
        AggregationResults<BookingScheduleRuleVo> aggregateResults =
                mongoTemplate.aggregate(aggregation, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = aggregateResults.getMappedResults();
        // 获取分组查询的总记录数
        Aggregation totalAggregation = Aggregation.newAggregation(
                Aggregation.match(criteria), // 匹配条件
                Aggregation.group("workDate") // 分组字段
        );
        AggregationResults<BookingScheduleRuleVo> totalAggregateResults =
                mongoTemplate.aggregate(totalAggregation, Schedule.class, BookingScheduleRuleVo.class);
        int total = totalAggregateResults.getMappedResults().size();
        // 根据日期获取星期几
        for (BookingScheduleRuleVo bookingScheduleRuleVo : bookingScheduleRuleVoList) {
            Date workDate = bookingScheduleRuleVo.getWorkDate();
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
        }
        // 设置最终数据，进行返回
        Map<String, Object> result = new HashMap<>();
        result.put("bookingScheduleRuleList", bookingScheduleRuleVoList);
        result.put("total", total);
        // 获取医院名称
        String hosName = hospitalService.getHospName(hoscode);
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("hosname", hosName);
        result.put("baseMap", baseMap);

        return result;
    }

    // 根据医院编号、科室编号和工作日期，查询排班详细信息
    @Override
    public List<Schedule> getDetailSchedule(String hoscode, String depcode, String workDate) {
        List<Schedule> scheduleList = scheduleRepository
                .findScheduleByHoscodeAndDepcodeAndWorkDate(hoscode, depcode, new DateTime(workDate).toDate());
        // 遍历list集合，向其中设置其他值：医院名称、科室名称和日期对应的星期
//        scheduleList.stream().forEach(item -> {
//            this.packageSchedule(item);
//        });
        scheduleList.forEach(this::packageSchedule);

        return scheduleList;
    }

    // 封装排班详情中的其他值：医院名称、科室名称和日期对应的星期
    private void packageSchedule(Schedule schedule) {
        // 设置医院名称
        schedule.getParam().put("hosname", hospitalService.getHospName(schedule.getHoscode()));
        // 设置医院编号
        schedule.getParam().put("depcode", departmentService.getDepName(schedule.getHoscode(), schedule.getDepcode()));
        // 设置日期对应的星期
        schedule.getParam().put("dayOfWeek", this.getDayOfWeek(new DateTime(schedule.getWorkDate())));
    }

    /**
     * 根据日期获取周几数据
     *
     * @param dateTime 日期
     * @return 星期几
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }
}
