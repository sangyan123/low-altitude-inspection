package com.drone.platform.module.task.service;

import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.notification.service.NotificationService;
import com.drone.platform.module.system.entity.SysUser;
import com.drone.platform.module.system.mapper.SysUserMapper;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.entity.TaskDispatch;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.mapper.TaskDispatchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PatrolTaskService 单元测试：创建默认值与派单工作流。
 */
@ExtendWith(MockitoExtension.class)
class PatrolTaskServiceTest {

    @Mock
    private TaskDispatchMapper dispatchMapper;

    @Mock
    private UavDeviceMapper deviceMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PatrolTaskMapper patrolTaskMapper;

    private PatrolTaskService service;

    @BeforeEach
    void setUp() {
        service = new PatrolTaskService(dispatchMapper, deviceMapper, userMapper, notificationService);
        ReflectionTestUtils.setField(service, "baseMapper", patrolTaskMapper);
    }

    @Test
    @DisplayName("创建：名称与场景类型必填")
    void create_requires_name_and_scene() {
        PatrolTask task = new PatrolTask();
        task.setTaskName("河道巡查");
        task.setSceneType(" ");

        assertThatThrownBy(() -> service.create(task))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("任务名称和场景类型");
    }

    @Test
    @DisplayName("创建成功：生成PT编号并填充默认状态与优先级")
    void create_success_fills_defaults() {
        PatrolTask task = new PatrolTask();
        task.setTaskName("河道巡查");
        task.setSceneType("水源保护");

        service.create(task);

        assertThat(task.getTaskNo()).startsWith("PT");
        assertThat(task.getStatus()).isZero();
        assertThat(task.getPriority()).isEqualTo(2);
        verify(patrolTaskMapper).insert(task);
    }

    @Test
    @DisplayName("派单：任务不存在抛业务异常")
    void dispatch_task_not_found() {
        when(patrolTaskMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.dispatch(1L, 2L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    @DisplayName("派单：已完成任务不可派单")
    void dispatch_rejects_finished_task() {
        PatrolTask task = new PatrolTask();
        task.setId(1L);
        task.setStatus(2);
        when(patrolTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.dispatch(1L, 2L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法派单");
    }

    @Test
    @DisplayName("派单：设备或飞手不存在分别抛异常")
    void dispatch_rejects_missing_device_or_user() {
        PatrolTask task = new PatrolTask();
        task.setId(1L);
        task.setStatus(0);
        when(patrolTaskMapper.selectById(1L)).thenReturn(task);
        when(deviceMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.dispatch(1L, 9L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备不存在");

        UavDevice device = new UavDevice();
        device.setId(9L);
        when(deviceMapper.selectById(9L)).thenReturn(device);
        when(userMapper.selectById(3L)).thenReturn(null);

        assertThatThrownBy(() -> service.dispatch(1L, 9L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("飞手不存在");
    }

    @Test
    @DisplayName("派单成功：写入调度记录、任务转执行中并通知上报人")
    void dispatch_success_writes_dispatch_and_notifies() {
        PatrolTask task = new PatrolTask();
        task.setId(1L);
        task.setStatus(0);
        task.setTaskName("河道巡查");
        task.setSubmitUserId(20L);
        when(patrolTaskMapper.selectById(1L)).thenReturn(task);

        UavDevice device = new UavDevice();
        device.setId(9L);
        when(deviceMapper.selectById(9L)).thenReturn(device);

        SysUser pilot = new SysUser();
        pilot.setId(3L);
        when(userMapper.selectById(3L)).thenReturn(pilot);

        service.dispatch(1L, 9L, 3L);

        ArgumentCaptor<TaskDispatch> captor = ArgumentCaptor.forClass(TaskDispatch.class);
        verify(dispatchMapper).insert(captor.capture());
        TaskDispatch dispatch = captor.getValue();
        assertThat(dispatch.getTaskType()).isEqualTo(1);
        assertThat(dispatch.getPatrolTaskId()).isEqualTo(1L);
        assertThat(dispatch.getDeviceId()).isEqualTo(9L);
        assertThat(dispatch.getUserId()).isEqualTo(3L);
        assertThat(dispatch.getStatus()).isEqualTo(1);

        assertThat(task.getStatus()).isEqualTo(1);
        verify(patrolTaskMapper).updateById(task);
        verify(notificationService).push(eq(20L), any(), any(), any(), any());
    }
}

