package com.drone.platform.module.task.service;

import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.notification.service.NotificationService;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TransportTaskService 单元测试：创建校验、载重校验与起飞/到达/签收状态机。
 */
@ExtendWith(MockitoExtension.class)
class TransportTaskServiceTest {

    @Mock
    private TransportTaskMapper transportTaskMapper;

    @Mock
    private UavDeviceMapper deviceMapper;

    @Mock
    private NotificationService notificationService;

    private TransportTaskService service;

    @BeforeEach
    void setUp() {
        service = new TransportTaskService(deviceMapper, notificationService);
        ReflectionTestUtils.setField(service, "baseMapper", transportTaskMapper);
    }

    private TransportTask validTask() {
        TransportTask task = new TransportTask();
        task.setCargoName("急救药品");
        task.setCargoWeightKg(new BigDecimal("5"));
        task.setOriginLng(new BigDecimal("101.72"));
        task.setOriginLat(new BigDecimal("26.58"));
        task.setDestLng(new BigDecimal("101.80"));
        task.setDestLat(new BigDecimal("26.60"));
        return task;
    }

    private TransportTask persisted(int status) {
        TransportTask task = new TransportTask();
        task.setId(1L);
        task.setStatus(status);
        return task;
    }

    @Test
    @DisplayName("创建：缺少起运点坐标抛业务异常")
    void create_requires_origin_coords() {
        TransportTask task = validTask();
        task.setOriginLng(null);

        assertThatThrownBy(() -> service.create(task))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起运点和目的地坐标");
    }

    @Test
    @DisplayName("创建：缺少物资名称抛业务异常")
    void create_requires_cargo_name() {
        TransportTask task = validTask();
        task.setCargoName(" ");

        assertThatThrownBy(() -> service.create(task))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("物资名称");
    }

    @Test
    @DisplayName("创建：物资超过设备最大载重被拒绝")
    void create_rejects_overweight_cargo() {
        TransportTask task = validTask();
        task.setCargoWeightKg(new BigDecimal("12"));
        task.setDeviceId(9L);
        UavDevice device = new UavDevice();
        device.setId(9L);
        device.setMaxPayloadKg(new BigDecimal("10"));
        when(deviceMapper.selectById(9L)).thenReturn(device);

        assertThatThrownBy(() -> service.create(task))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最大载重");
    }

    @Test
    @DisplayName("创建成功：自动生成TT编号、默认状态待起飞")
    void create_success_generates_task_no_and_default_status() {
        TransportTask task = validTask();

        service.create(task);

        assertThat(task.getTaskNo()).startsWith("TT");
        assertThat(task.getStatus()).isZero();
        verify(transportTaskMapper).insert(task);
    }

    @Test
    @DisplayName("起飞：任务不存在抛业务异常")
    void depart_task_not_found() {
        when(transportTaskMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.depart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("起飞：仅待起飞状态可起飞")
    void depart_only_from_pending_status() {
        when(transportTaskMapper.selectById(1L)).thenReturn(persisted(1));

        assertThatThrownBy(() -> service.depart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("待起飞");
    }

    @Test
    @DisplayName("起飞成功：状态转运输中并记录起飞时间")
    void depart_success() {
        TransportTask task = persisted(0);
        when(transportTaskMapper.selectById(1L)).thenReturn(task);

        service.depart(1L);

        assertThat(task.getStatus()).isEqualTo(1);
        assertThat(task.getDepartTime()).isNotNull();
        verify(transportTaskMapper).updateById(task);
    }

    @Test
    @DisplayName("到达：仅运输中状态可到达")
    void arrive_only_from_flying_status() {
        when(transportTaskMapper.selectById(1L)).thenReturn(persisted(0));

        assertThatThrownBy(() -> service.arrive(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("运输中");
    }

    @Test
    @DisplayName("到达成功：状态转已到达并记录到达时间")
    void arrive_success() {
        TransportTask task = persisted(1);
        when(transportTaskMapper.selectById(1L)).thenReturn(task);

        service.arrive(1L);

        assertThat(task.getStatus()).isEqualTo(2);
        assertThat(task.getArriveTime()).isNotNull();
        verify(transportTaskMapper).updateById(task);
    }

    @Test
    @DisplayName("签收：未到达不能签收")
    void sign_requires_arrived_status() {
        when(transportTaskMapper.selectById(1L)).thenReturn(persisted(1));

        assertThatThrownBy(() -> service.sign(1L, 66L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("签收");
    }

    @Test
    @DisplayName("签收成功：状态转已签收并记录签收人与时间")
    void sign_success() {
        TransportTask task = persisted(2);
        when(transportTaskMapper.selectById(1L)).thenReturn(task);

        service.sign(1L, 66L);

        assertThat(task.getStatus()).isEqualTo(3);
        assertThat(task.getSignUserId()).isEqualTo(66L);
        assertThat(task.getSignTime()).isBeforeOrEqualTo(LocalDateTime.now());
        verify(transportTaskMapper).updateById(task);
    }
}
