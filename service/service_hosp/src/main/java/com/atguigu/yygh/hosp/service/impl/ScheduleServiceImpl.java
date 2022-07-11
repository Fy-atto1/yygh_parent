package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MongoTemplate mongoTemplate;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;

    @Autowired
    public ScheduleServiceImpl(ScheduleRepository scheduleRepository, MongoTemplate mongoTemplate,
                               HospitalService hospitalService, DepartmentService departmentService) {
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

    // 获取可预约的排班数据
    @Override
    public Map<String, Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode) {
        Map<String, Object> result = new HashMap<>();
        // 获取预约规则
        // 根据医院编号获取预约规则
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        if (hospital == null) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        BookingRule bookingRule = hospital.getBookingRule();
        // 获取可预约日期的数据（分页）
        IPage<Date> iPage = this.getListDate(page, limit, bookingRule);
        // 获取当前可预约日期
        List<Date> dateList = iPage.getRecords();
        // 获取可预约日期的科室剩余预约数
        Criteria criteria = Criteria.where("hoscode").is(hoscode)
                .and("depcode").is(depcode)
                .and("workDate").in(dateList);
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate").first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("availableNumber").as("availableNumber")
                        .sum("reservedNumber").as("reservedNumber")
        );
        AggregationResults<BookingScheduleRuleVo> aggregate =
                mongoTemplate.aggregate(aggregation, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> scheduleVoList = aggregate.getMappedResults();
        // 合并数据 将统计数据scheduleVoList根据“安排日期”合并到BookingScheduleRuleVo
        Map<Date, BookingScheduleRuleVo> scheduleVoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(scheduleVoList)) {
            scheduleVoMap = scheduleVoList.stream()
                    .collect(Collectors.toMap(BookingScheduleRuleVo::getWorkDate,
                            BookingScheduleRuleVo -> BookingScheduleRuleVo));
        }
        // 获取可预约的排班规则
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        for (int i = 0; i < dateList.size(); i++) {
            Date date = dateList.get(i);
            // 从map集合中根据key（日期）获取value值
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleVoMap.get(date);
            // 如果当天没有排班医生
            if (bookingScheduleRuleVo == null) {
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                // 设置就诊医生人数
                bookingScheduleRuleVo.setDocCount(0);
                // 设置科室剩余预约数 -1表示无号
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            // 设置当前预约日期对应星期几
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
            // 最后一页最后一条记录为即将预约   状态0：正常    1：即将放号    -1：当天已停止挂号
            if (i == dateList.size() - 1 && page == iPage.getPages()) {
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }
            // 当天预约如果过了停号时间， 不能预约
            if (i == 0 && page == 1) {
                DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if (stopTime.isBeforeNow()) {
                    // 停止预约
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }
        // 可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        // 总记录数
        result.put("total", iPage.getTotal());
        // 其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        // 医院名称
        baseMap.put("hosname", hospitalService.getHospName(hoscode));
        // 科室
        Department department = departmentService.getDepartment(hoscode, depcode);
        // 大科室名称
        baseMap.put("bigname", department.getBigname());
        // 科室名称
        baseMap.put("depname", department.getDepname());
        // 月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        // 放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        // 停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;
    }

    // 根据排班id获取排班数据
    @Override
    public Schedule getScheduleById(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule != null) {
            this.packageSchedule(schedule);
        }
        return schedule;
    }

    // 获取可预约日期的数据（分页）
    private IPage<Date> getListDate(Integer page, Integer limit, BookingRule bookingRule) {
        // 获取放号时间：年 月 日 小时 分钟
        DateTime releaseTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        // 获取预约周期
        Integer cycle = bookingRule.getCycle();
        // 如果当天放号时间已经过去，那么预约周期的之后一天为即将放号时间，周期加一
        if (releaseTime.isBeforeNow()) {
            cycle += 1;
        }
        // 获取可预约的所有日期，最后一天显示即将放号
        List<Date> dateList = new ArrayList<>();
        for (int i = 0; i < cycle; i++) {
            DateTime curDateTime = new DateTime().plusDays(i);
            String dateString = curDateTime.toString("yyyy-MM-dd");
            dateList.add(new DateTime(dateString).toDate());
        }
        // 日期分页，由于预约周期不一样，页面一排最多显示7天数据，多于7天就要分页显示
        List<Date> pageDateList = new ArrayList<>();
        int start = (page - 1) * limit;
        int end = (page - 1) * limit + limit;
        // 设置当前页显示的可预约的最后日期为哪一天
        if (dateList.size() < end) {
            end = dateList.size();
        }
        // 设置当前页需要显示的可预约日期
        for (int i = start; i < end; i++) {
            pageDateList.add(dateList.get(i));
        }
        // 分页
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination
                .Page<>(page, 7, dateList.size());
        iPage.setRecords(pageDateList);
        return iPage;
    }

    /**
     * 将Date日期（yyyy-MM-dd HH:mm）转换为DateTime
     */
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " " + timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }

    // 封装排班详情中的其他值：医院名称、科室名称和日期对应的星期
    private void packageSchedule(Schedule schedule) {
        // 设置医院名称
        schedule.getParam().put("hosname", hospitalService.getHospName(schedule.getHoscode()));
        // 设置医院编号
        schedule.getParam().put("depname", departmentService.getDepName(schedule.getHoscode(), schedule.getDepcode()));
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
