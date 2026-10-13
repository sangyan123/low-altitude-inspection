package com.drone.platform.module.event.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.mapper.AiAlgorithmMapper;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.event.entity.EventHandleLog;
import com.drone.platform.module.event.entity.EventInfo;
import com.drone.platform.module.event.mapper.EventHandleLogMapper;
import com.drone.platform.module.event.mapper.EventInfoMapper;
import com.drone.platform.module.notification.service.NotificationService;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.mapper.SysDeptMapper;
import com.drone.platform.module.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EventService 单元测试：事件全生命周期工作流（创建/推送/派单/处置/核验/销号/删除）。
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventInfoMapper eventMapper;
    @Mock private EventHandleLogMapper handleLogMapper;
    @Mock private AiRecognitionResultMapper recognitionMapper;
    @Mock private AiAlgorithmMapper algorithmMapper;
    @Mock private SysDeptMapper deptMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private NotificationService notificationService;

    private EventService service;

    @BeforeEach
    void initTableMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), EventInfo.class);
    }

    @BeforeEach
    void setUp() {
        service = new EventService(handleLogMapper, recognitionMapper, algorithmMapper,
                deptMapper, userMapper, notificationService);
        ReflectionTestUtils.setField(service, "baseMapper", eventMapper);
    }

    private EventInfo event(int status) {
        EventInfo event = new EventInfo();
        event.setId(1L);
        event.setStatus(status);
        event.setEventType("垃圾堆积");
        event.setSubmitUserId(20L);
        return event;
    }

    @Test
    @DisplayName("人工上报：类型与描述必填")
    void createManual_requires_type_and_description() {
        assertThatThrownBy(() -> service.createManual(1L, " ", "描述", null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("事件类型");
        assertThatThrownBy(() -> service.createManual(1L, "垃圾堆积", " ", null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("事件描述");
    }

    @Test
    @DisplayName("人工上报成功：人工来源、待处置状态并写处置日志")
    void createManual_success() {
        doAnswer(inv -> {
            EventInfo e = inv.getArgument(0);
            e.setId(88L);
            return 1;
        }).when(eventMapper).insert(any(EventInfo.class));

        Long id = service.createManual(1L, "垃圾堆积", "河边有垃圾", new BigDecimal("101.7"),
                new BigDecimal("26.5"), 3L, "备注");

        assertThat(id).isEqualTo(88L);
        ArgumentCaptor<EventInfo> captor = ArgumentCaptor.forClass(EventInfo.class);
        verify(eventMapper).insert(captor.capture());
        EventInfo saved = captor.getValue();
        assertThat(saved.getEventNo()).startsWith("EV");
        assertThat(saved.getEventSource()).isEqualTo(2);
        assertThat(saved.getStatus()).isZero();
        assertThat(saved.getDeptId()).isEqualTo(3L);

        ArgumentCaptor<EventHandleLog> logCaptor = ArgumentCaptor.forClass(EventHandleLog.class);
        verify(handleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("创建事件");
        assertThat(logCaptor.getValue().getEventId()).isEqualTo(88L);
    }

    @Test
    @DisplayName("AI推送事件：仅已确认结果可推送，且不可重复推送")
    void pushRecognitionEvent_guards() {
        when(recognitionMapper.selectById(5L)).thenReturn(null);
        assertThatThrownBy(() -> service.pushRecognitionEvent(5L, 1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI识别结果不存在");

        AiRecognitionResult pending = new AiRecognitionResult();
        pending.setId(5L);
        pending.setStatus(0);
        when(recognitionMapper.selectById(5L)).thenReturn(pending);
        assertThatThrownBy(() -> service.pushRecognitionEvent(5L, 1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有已确认");

        AiRecognitionResult confirmed = new AiRecognitionResult();
        confirmed.setId(5L);
        confirmed.setStatus(1);
        confirmed.setResultType("垃圾堆积");
        confirmed.setConfirmUserId(20L);
        when(recognitionMapper.selectById(5L)).thenReturn(confirmed);
        when(eventMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.pushRecognitionEvent(5L, 1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已推送事件");
    }

    @Test
    @DisplayName("AI推送事件成功：AI来源事件落库、识别结果转已推送并通知")
    void pushRecognitionEvent_success() {
        AiRecognitionResult confirmed = new AiRecognitionResult();
        confirmed.setId(5L);
        confirmed.setStatus(1);
        confirmed.setResultType("垃圾堆积");
        confirmed.setErrorMessage("画面发现生活垃圾");
        confirmed.setConfirmUserId(20L);
        when(recognitionMapper.selectById(5L)).thenReturn(confirmed);
        when(eventMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            EventInfo e = inv.getArgument(0);
            e.setId(99L);
            return 1;
        }).when(eventMapper).insert(any(EventInfo.class));

        Long eventId = service.pushRecognitionEvent(5L, 1L, 3L, "备注");

        assertThat(eventId).isEqualTo(99L);
        ArgumentCaptor<EventInfo> captor = ArgumentCaptor.forClass(EventInfo.class);
        verify(eventMapper).insert(captor.capture());
        EventInfo saved = captor.getValue();
        assertThat(saved.getEventSource()).isEqualTo(1);
        assertThat(saved.getRecognitionId()).isEqualTo(5L);
        assertThat(saved.getPushSystem()).isEqualTo("AI识别中心");

        assertThat(confirmed.getStatus()).isEqualTo(2);
        verify(recognitionMapper).updateById(confirmed);
        verify(notificationService).push(eq(20L), anyString(), anyString(), eq(2), eq(99L));
    }

    @Test
    @DisplayName("派单：仅待处置事件、处置部门必填且必须存在")
    void dispatch_guards() {
        when(eventMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.dispatch(1L, 1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("事件不存在");

        when(eventMapper.selectById(1L)).thenReturn(event(1));
        assertThatThrownBy(() -> service.dispatch(1L, 1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有待处置");

        when(eventMapper.selectById(1L)).thenReturn(event(0));
        assertThatThrownBy(() -> service.dispatch(1L, 1L, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("处置部门不能为空");

        when(eventMapper.selectById(1L)).thenReturn(event(0));
        when(deptMapper.selectById(3L)).thenReturn(null);
        assertThatThrownBy(() -> service.dispatch(1L, 1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("处置部门不存在");
    }

    @Test
    @DisplayName("派单成功：状态转已派单并通知上报人")
    void dispatch_success() {
        EventInfo event = event(0);
        when(eventMapper.selectById(1L)).thenReturn(event);
        SysDept dept = new SysDept();
        dept.setId(3L);
        when(deptMapper.selectById(3L)).thenReturn(dept);

        service.dispatch(1L, 1L, 3L, "请尽快处理");

        assertThat(event.getStatus()).isEqualTo(1);
        assertThat(event.getDeptId()).isEqualTo(3L);
        assertThat(event.getDispatchTime()).isNotNull();
        verify(handleLogMapper).insert(any(EventHandleLog.class));
        verify(notificationService).push(eq(20L), anyString(), anyString(), eq(1), eq(1L));
    }

    @Test
    @DisplayName("处置：仅已派单事件、处置说明必填")
    void handle_guards() {
        when(eventMapper.selectById(1L)).thenReturn(event(0));
        assertThatThrownBy(() -> service.handle(1L, 1L, "说明"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有已派单");

        when(eventMapper.selectById(1L)).thenReturn(event(1));
        assertThatThrownBy(() -> service.handle(1L, 1L, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("处置说明");
    }

    @Test
    @DisplayName("处置成功：状态转已处置并记录处置人")
    void handle_success() {
        EventInfo event = event(1);
        when(eventMapper.selectById(1L)).thenReturn(event);

        service.handle(1L, 8L, "已清理完毕");

        assertThat(event.getStatus()).isEqualTo(2);
        assertThat(event.getHandleUserId()).isEqualTo(8L);
        assertThat(event.getHandleRemark()).isEqualTo("已清理完毕");
        verify(eventMapper).updateById(event);
    }

    @Test
    @DisplayName("核验：结果通过转已核验，不通过退回已派单")
    void verify_result_branches() {
        EventInfo pass = event(2);
        when(eventMapper.selectById(1L)).thenReturn(pass);
        service.verify(1L, 9L, 1, "现场已复核");
        assertThat(pass.getStatus()).isEqualTo(3);
        assertThat(pass.getVerifyResult()).isEqualTo(1);

        EventInfo reject = event(2);
        when(eventMapper.selectById(1L)).thenReturn(reject);
        service.verify(1L, 9L, 2, "未清理干净");
        assertThat(reject.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("核验：非法结果值被拒绝")
    void verify_rejects_invalid_result() {
        when(eventMapper.selectById(1L)).thenReturn(event(2));

        assertThatThrownBy(() -> service.verify(1L, 9L, 5, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("核验结果不合法");
    }

    @Test
    @DisplayName("销号：仅核验通过事件可销号，成功后状态转已销号")
    void close_workflow() {
        when(eventMapper.selectById(1L)).thenReturn(event(2));
        assertThatThrownBy(() -> service.close(1L, 9L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("核验通过");

        EventInfo event = event(3);
        when(eventMapper.selectById(1L)).thenReturn(event);
        service.close(1L, 9L, "归档");
        assertThat(event.getStatus()).isEqualTo(4);
        assertThat(event.getCloseTime()).isNotNull();
    }

    @Test
    @DisplayName("删除：仅待处置可删，AI来源事件删除后回滚识别结果状态")
    void deleteEvent_resets_recognition() {
        when(eventMapper.selectById(1L)).thenReturn(event(3));
        assertThatThrownBy(() -> service.deleteEvent(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有待处置");

        EventInfo aiEvent = event(0);
        aiEvent.setEventSource(1);
        aiEvent.setRecognitionId(5L);
        when(eventMapper.selectById(1L)).thenReturn(aiEvent);
        AiRecognitionResult pushed = new AiRecognitionResult();
        pushed.setId(5L);
        pushed.setStatus(2);
        when(recognitionMapper.selectById(5L)).thenReturn(pushed);

        service.deleteEvent(1L);

        verify(eventMapper).deleteById(1L);
        assertThat(pushed.getStatus()).isEqualTo(1);
        verify(recognitionMapper).updateById(pushed);
    }
}

