<template>
  <div class="ai-recognition">
    <el-card shadow="never" class="upload-card">
      <template #header>新建识别</template>
      <div class="upload-layout">
        <el-upload
          class="image-uploader"
          drag
          accept=".jpg,.jpeg,.png,.webp"
          :show-file-list="false"
          :http-request="handleUpload"
        >
          <img v-if="form.imageUrl" :src="form.imageUrl" class="preview-image" />
          <div v-else class="upload-empty">
            <el-icon size="32"><Picture /></el-icon>
            <div class="upload-text">点击或拖拽上传图片</div>
            <div class="upload-tip">支持 JPG / PNG / WEBP，最大 20MB</div>
          </div>
        </el-upload>

        <div class="recognize-form">
          <el-form :model="form" label-width="90px">
            <el-form-item label="识别算法" required>
              <el-select v-model="form.algoId" filterable placeholder="选择识别算法" style="width: 100%">
                <el-option
                  v-for="item in algorithms"
                  :key="item.id"
                  :label="`${item.algoName}（${item.algoCategory}）`"
                  :value="item.id!"
                  :disabled="item.status !== 1"
                />
              </el-select>
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="经度">
                  <el-input-number
                    v-model="form.lng"
                    :min="73"
                    :max="135.35"
                    :precision="6"
                    :controls="false"
                    placeholder="可选"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="纬度">
                  <el-input-number
                    v-model="form.lat"
                    :min="3.86"
                    :max="53.55"
                    :precision="6"
                    :controls="false"
                    placeholder="可选"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="飞行记录">
              <el-input-number
                v-model="form.flightRecordId"
                :min="1"
                :precision="0"
                :controls="false"
                placeholder="可选"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="识别引擎">
              <el-select v-model="form.provider" style="width: 100%">
                <el-option label="智谱 GLM 视觉识别" value="glm" />
                <el-option label="Mock 模拟识别" value="mock" />
              </el-select>
            </el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="recognizing"
              :disabled="!form.imageUrl || !form.algoId"
              @click="handleRecognize"
            >
              开始识别
            </el-button>
          </el-form>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div class="toolbar">
        <el-select v-model="query.algoId" placeholder="算法" clearable filterable style="width: 180px" @change="loadData">
          <el-option v-for="item in algorithms" :key="item.id" :label="item.algoName" :value="item.id!" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="loadData">
          <el-option label="待确认" :value="0" />
          <el-option label="已确认" :value="1" />
          <el-option label="已推送事件" :value="2" />
          <el-option label="误报" :value="3" />
        </el-select>
        <el-select v-model="query.provider" placeholder="识别引擎" clearable style="width: 150px" @change="loadData">
          <el-option label="GLM" value="glm" />
          <el-option label="Mock" value="mock" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索结果类型/模型/图片"
          clearable
          style="width: 220px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
              class="table-image"
            />
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="algoName" label="算法" width="130" />
        <el-table-column prop="resultType" label="识别结果" min-width="140" />
        <el-table-column label="置信度" width="110">
          <template #default="{ row }">
            <el-tag :type="confidenceTag(row.confidence)" size="small">
              {{ confidenceText(row.confidence) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="引擎" width="100" />
        <el-table-column prop="modelName" label="模型" width="150" show-overflow-tooltip />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">{{ row.sourceType === 1 ? "飞行识别" : "人工上传" }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="识别时间" width="170" />
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status !== 1 && row.status !== 2"
              size="small"
              type="primary"
              @click="handleConfirm(row.id)"
            >
              确认
            </el-button>
            <el-button
              v-if="row.status === 1"
              size="small"
              type="success"
              @click="handlePushEvent(row.id)"
            >
              推送事件
            </el-button>
            <el-button
              v-if="row.status !== 3 && row.status !== 2"
              size="small"
              type="warning"
              @click="handleFalseReport(row.id)"
            >
              误报
            </el-button>
            <el-popconfirm
              v-if="row.status !== 2"
              title="确定删除该识别结果？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @change="loadData"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="识别结果详情" width="760px">
      <div v-if="detail" class="detail-layout">
        <div class="detail-image-wrap">
          <el-image :src="detail.imageUrl" :preview-src-list="[detail.imageUrl]" preview-teleported fit="contain" class="detail-image" />
        </div>
        <div class="detail-info">
          <div class="info-row"><span>识别算法</span><b>{{ detail.algoName || detail.algoId }}</b></div>
          <div class="info-row"><span>结果类型</span><b>{{ detail.resultType || "-" }}</b></div>
          <div class="info-row"><span>置信度</span><b>{{ confidenceText(detail.confidence) }}</b></div>
          <div class="info-row"><span>状态</span><b>{{ statusName(detail.status) }}</b></div>
          <div class="info-row"><span>引擎 / 模型</span><b>{{ detail.provider || "-" }} / {{ detail.modelName || "-" }}</b></div>
          <div class="info-row"><span>坐标</span><b>{{ detail.lng ?? "-" }}, {{ detail.lat ?? "-" }}</b></div>
          <div class="info-row"><span>目标框</span><b>{{ detail.bbox || "-" }}</b></div>
          <div class="info-row"><span>说明</span><b>{{ detail.errorMessage || "-" }}</b></div>
          <div class="raw-response">
            <div class="raw-title">AI 原始返回</div>
            <pre>{{ prettyJson(detail.rawResponse) }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import {
  uploadImage,
  getAlgorithmList,
  createRecognition,
  getRecognitionPage,
  confirmRecognition,
  falseReportRecognition,
  deleteRecognition,
  pushRecognitionEvent,
  type AiAlgorithm,
  type AiRecognitionResult,
} from "../../api/ai";

const algorithms = ref<AiAlgorithm[]>([]);
const list = ref<AiRecognitionResult[]>([]);
const loading = ref(false);
const recognizing = ref(false);
const total = ref(0);
const detailVisible = ref(false);
const detail = ref<AiRecognitionResult | null>(null);

const form = reactive({
  algoId: null as number | null,
  imageUrl: "",
  flightRecordId: null as number | null,
  lng: null as number | null,
  lat: null as number | null,
  provider: "mock",
});

const query = reactive({
  current: 1,
  size: 10,
  algoId: null as number | null,
  status: null as number | null,
  provider: "",
  keyword: "",
});

const confidenceText = (value: number | string | null | undefined) => {
  const num = Number(value ?? 0);
  return num ? `${(num * 100).toFixed(1)}%` : "-";
};

const confidenceTag = (value: number | string | null | undefined) => {
  const num = Number(value ?? 0);
  if (num >= 0.85) return "success";
  if (num >= 0.7) return "warning";
  return "info";
};

const statusName = (status: number) =>
  ({ 0: "待确认", 1: "已确认", 2: "已推送事件", 3: "误报" }[status] || "-");
const statusTag = (status: number) =>
  ({ 0: "warning", 1: "success", 2: "primary", 3: "info" }[status] || "info");

const prettyJson = computed(() => {
  return (value?: string) => {
    if (!value) return "-";
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  };
});

async function loadAlgorithms() {
  const res: any = await getAlgorithmList();
  algorithms.value = res.data || [];
}

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getRecognitionPage(query);
    list.value = res.data.records || [];
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

async function handleUpload(options: UploadRequestOptions) {
  const file = options.file as File;
  const res: any = await uploadImage(file);
  form.imageUrl = res.data.url;
  ElMessage.success("图片上传成功");
}

async function handleRecognize() {
  if (!form.algoId) {
    ElMessage.warning("请选择识别算法");
    return;
  }
  if (!form.imageUrl) {
    ElMessage.warning("请上传识别图片");
    return;
  }
  recognizing.value = true;
  try {
    const res: any = await createRecognition({
      algoId: form.algoId,
      imageUrl: form.imageUrl,
      flightRecordId: form.flightRecordId,
      lng: form.lng,
      lat: form.lat,
      provider: form.provider,
    });
    const count = res.data?.length ?? 0;
    ElMessage.success(count ? `识别完成，共 ${count} 条结果` : "识别完成，未发现目标");
    query.current = 1;
    loadData();
  } finally {
    recognizing.value = false;
  }
}

function openDetail(row: AiRecognitionResult) {
  detail.value = row;
  detailVisible.value = true;
}

async function handleConfirm(id: number) {
  await confirmRecognition(id);
  ElMessage.success("已确认");
  loadData();
}

async function handleFalseReport(id: number) {
  await falseReportRecognition(id);
  ElMessage.success("已标记误报");
  loadData();
}

async function handlePushEvent(id: number) {
  await pushRecognitionEvent(id);
  ElMessage.success("已推送事件");
  loadData();
}

async function handleDelete(id: number) {
  await deleteRecognition(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(() => {
  loadAlgorithms();
  loadData();
});
</script>

<style scoped>
.ai-recognition {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-layout {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(300px, 1fr);
  gap: 20px;
}

.image-uploader {
  width: 100%;
}

.image-uploader :deep(.el-upload-dragger) {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.preview-image {
  max-width: 100%;
  max-height: 260px;
  border-radius: 6px;
}

.upload-empty {
  text-align: center;
  color: #909399;
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.upload-tip {
  margin-top: 4px;
  font-size: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.table-image {
  width: 64px;
  height: 42px;
  border-radius: 4px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.row-actions :deep(.el-button) {
  flex: 0 0 auto;
  padding: 5px 8px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(280px, 1fr);
  gap: 18px;
}

.detail-image-wrap {
  display: flex;
  align-items: center;
  background: #f6f8fb;
  border-radius: 6px;
  padding: 10px;
}

.detail-image {
  width: 100%;
  max-height: 420px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.info-row span {
  color: #909399;
}

.info-row b {
  color: #303133;
  font-weight: normal;
  text-align: right;
}

.raw-response {
  margin-top: 14px;
}

.raw-title {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.raw-response pre {
  max-height: 180px;
  overflow: auto;
  padding: 10px;
  border-radius: 6px;
  background: #111827;
  color: #e5e7eb;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .upload-layout,
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
