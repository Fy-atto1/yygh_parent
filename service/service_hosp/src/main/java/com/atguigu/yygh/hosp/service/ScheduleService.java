package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ScheduleService {
    // 上传排班接口
    void save(Map<String, Object> parameterMap);

    // 查询排班接口
    Page<Schedule> finaPageSchedule(int page, int limit, ScheduleQueryVo scheduleQueryVo);

    // 删除排班接口
    void remove(String hoscode, String hosScheduleId);

    // 根据医院编号和科室编号，查询排班规则数据
    Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode);

    // 根据医院编号、科室编号和工作日期，查询排班详细信息
    List<Schedule> getDetailSchedule(String hoscode, String depcode, String workDate);

    // 获取可预约的排班数据
    Map<String, Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode);

    // 根据排班id获取排班数据
    Schedule getScheduleById(String scheduleId);

    // 根据排班id获取预约下单数据
    ScheduleOrderVo getScheduleOrderVo(String scheduleId);

    // 更新排班数据，用于mq
    void update(Schedule schedule);
}
