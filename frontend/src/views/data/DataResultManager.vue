<template>
  <div class="data-result-manage">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.dataType" placeholder="成果类型" clearable style="width: 160px" @change="loadData">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.deptId" placeholder="所属部门" clearable filterable style="width: 170px" @change="loadData">
          <el-option v-for="item in depts" :key="item.id" :label="item.deptName" :value="item.id" />
        </el-select>
        <el-input-number
          v-model="query.flightRecordId"
          :min="1"
          :precision="0"
          :controls="false"
          placeholder="飞行记录ID"
          style="width: 160px"
        />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="loadData"
        />
        <el-input
          v-model="query.keyword"
          placeholder="搜索名称/格式/备注"
          clearable
          style="width: 220px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button type="success" @click="openCreate">上传成果</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="预览" width="110">
          <template #default="{ row }">
            <el-image
              v-if="row.dataType === 2 || row.thumbnailUrl"
              :src="row.thumbnailUrl || row.fileUrl"
              :preview-src-list="[row.thumbnailUrl || row.fileUrl]"
              preview-teleported
              fit="cover"
              class="result-preview"
            />
            <el-button v-else size="small" text type="primary" @click="openPreview(row)">预览</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="成果名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="dataTypeName" label="类型" width="110" />
        <el-table-column prop="modelFormat" label="格式" width="110">
          <template #default="{ row }">{{ row.modelFormat || "-" }}</template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ fileSizeText(row.fileSizeMb) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属部门" width="130">
          <template #default="{ row }">{{ row.deptName || "-" }}</template>
        </el-table-column>
        <el-table-column prop="flightRecordId" label="飞行记录" width="110">
          <template #default="{ row }">{{ row.flightRecordId || "-" }}</template>
        </el-table-column>
        <el-table-column prop="captureTime" label="采集时间" width="170">
          <template #default="{ row }">{{ row.captureTime || "-" }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="openPreview(row)">预览</el-button>
              <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确定删除该成果？" @confirm="handleDelete(row.id)">
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

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑成果' : '上传成果'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="成果类型" required>
              <el-select v-model="form.dataType" style="width: 100%">
                <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成果名称" required>
              <el-input v-model="form.fileName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="isEdit ? '替换文件' : '成果文件'" :required="!isEdit">
          <el-upload
            :show-file-list="false"
            :http-request="uploadMainFile"
            accept=".jpg,.jpeg,.png,.webp,.mp4,.mov,.avi,.obj,.glb,.gltf,.fbx,.tif,.tiff,.las,.laz,.zip,.json,.b3dm"
          >
            <el-button>
              <el-icon><UploadFilled /></el-icon>
              {{ form.fileUrl ? "重新上传" : "选择文件" }}
            </el-button>
          </el-upload>
          <div v-if="form.fileUrl" class="uploaded-file">已上传：{{ form.fileName || form.fileUrl }}</div>
        </el-form-item>

        <el-form-item v-if="category === 'media'" label="缩略图">
          <el-upload :show-file-list="false" :http-request="uploadThumbnail" accept=".jpg,.jpeg,.png,.webp">
            <el-button>
              <el-icon><Picture /></el-icon>
              {{ form.thumbnailUrl ? "重新上传" : "选择缩略图" }}
            </el-button>
          </el-upload>
        </el-form-item>

        <el-row v-if="category === 'model'" :gutter="12">
          <el-col :span="12">
            <el-form-item label="模型格式">
              <el-input v-model="form.modelFormat" placeholder="3D Tiles / OBJ / LAS" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="飞行记录">
              <el-input-number v-model="form.flightRecordId" :min="1" :precision="0" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="category === 'media'" label="飞行记录">
          <el-input-number v-model="form.flightRecordId" :min="1" :precision="0" :controls="false" style="width: 100%" />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="采集时间">
              <el-date-picker
                v-model="form.captureTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" clearable filterable style="width: 100%">
                <el-option v-for="item in depts" :key="item.id" :label="item.deptName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="form.lng" :min="73" :max="135.35" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="form.lat" :min="3.86" :max="53.55" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="授权范围">
          <el-input v-model="form.authScope" placeholder="部门ID英文逗号分隔，留空表示公开" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="preview?.fileName || '成果预览'" :width="preview && is3dModel(preview) ? 1100 : 880" destroy-on-close>
      <div v-if="preview" class="preview-container">
        <video v-if="preview.dataType === 1" :src="preview.fileUrl" controls class="media-viewer" />
        <el-image
          v-else-if="preview.dataType === 2"
          :src="preview.fileUrl"
          :preview-src-list="[preview.fileUrl]"
          preview-teleported
          fit="contain"
          class="media-viewer"
        />
        <Model3DViewer
          v-else-if="is3dModel(preview)"
          :url="preview.fileUrl"
          :format="preview.modelFormat"
        />
        <div v-else class="model-placeholder">
          <el-icon size="46"><Box /></el-icon>
          <div class="model-title">{{ preview.fileName }}</div>
          <div class="model-description">该格式暂不支持在线预览，可下载后查看。</div>
          <el-button type="primary" @click="openExternal(preview.fileUrl)">下载文件</el-button>
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
  getDataResultPage,
  createDataResult,
  updateDataResult,
  deleteDataResult,
  uploadDataFile,
  type DataResult,
} from "../../api/data";
import { getDeptList } from "../../api/system";
import Model3DViewer from "../../components/Model3DViewer.vue";

const props = defineProps<{
  category: "media" | "model";
}>();

const list = ref<DataResult[]>([]);
const depts = ref<any[]>([]);
const loading = ref(false);
const saving = ref(false);
const total = ref(0);
const formVisible = ref(false);
const isEdit = ref(false);
const previewVisible = ref(false);
const preview = ref<DataResult | null>(null);
const dateRange = ref<[string, string] | null>(null);

const mediaTypes = [
  { value: 1, label: "视频" },
  { value: 2, label: "照片" },
];
const modelTypes = [
  { value: 3, label: "DOM" },
  { value: 4, label: "DSM" },
  { value: 5, label: "点云" },
  { value: 6, label: "三维模型" },
];
const typeOptions = computed(() => props.category === "media" ? mediaTypes : modelTypes);

const query = reactive({
  current: 1,
  size: 10,
  dataType: null as number | null,
  deptId: null as number | null,
  flightRecordId: null as number | null,
  keyword: "",
});

const emptyForm = {
  id: null as number | null,
  dataType: props.category === "media" ? 2 : 6,
  fileName: "",
  fileUrl: "",
  thumbnailUrl: "",
  fileSizeMb: null as number | null,
  modelFormat: "",
  captureTime: "",
  lng: null as number | null,
  lat: null as number | null,
  deptId: null as number | null,
  flightRecordId: null as number | null,
  authScope: "",
  remark: "",
};
const form = reactive({ ...emptyForm });

const fileSizeText = (value: number | string | null | undefined) => {
  const num = Number(value ?? 0);
  return num ? `${num.toFixed(2)} MB` : "-";
};

const is3dModel = (item: DataResult) => {
  if (!item?.fileUrl) return false;
  const url = item.fileUrl.toLowerCase();
  return url.endsWith(".glb") || url.endsWith(".gltf") || url.endsWith(".obj");
};

const openExternal = (url?: string) => {
  if (url) window.open(url, "_blank");
};

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getDataResultPage({
      ...query,
      category: props.category,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    });
    list.value = res.data.records || [];
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  Object.assign(form, emptyForm);
}

function openCreate() {
  isEdit.value = false;
  resetForm();
  formVisible.value = true;
}

function openEdit(row: DataResult) {
  isEdit.value = true;
  resetForm();
  Object.assign(form, row);
  formVisible.value = true;
}

function openPreview(row: DataResult) {
  preview.value = row;
  previewVisible.value = true;
}

async function uploadMainFile(options: UploadRequestOptions) {
  const res: any = await uploadDataFile(options.file as File);
  form.fileUrl = res.data.url;
  form.fileName = form.fileName || res.data.fileName;
  form.fileSizeMb = Number((Number(res.data.size) / 1048576).toFixed(2));
  ElMessage.success("成果文件上传成功");
}

async function uploadThumbnail(options: UploadRequestOptions) {
  const res: any = await uploadDataFile(options.file as File);
  form.thumbnailUrl = res.data.url;
  ElMessage.success("缩略图上传成功");
}

async function handleSave() {
  if (!form.dataType) {
    ElMessage.warning("请选择成果类型");
    return;
  }
  if (!form.fileName) {
    ElMessage.warning("请填写成果名称");
    return;
  }
  if (!form.fileUrl) {
    ElMessage.warning("请上传成果文件");
    return;
  }
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateDataResult(form);
    } else {
      await createDataResult(form);
    }
    ElMessage.success("保存成功");
    formVisible.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id: number) {
  await deleteDataResult(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(async () => {
  const res: any = await getDeptList();
  depts.value = res.data || [];
  loadData();
});
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.result-preview {
  width: 76px;
  height: 48px;
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

.uploaded-file {
  width: 100%;
  margin-top: 6px;
  color: #67c23a;
  font-size: 12px;
  word-break: break-all;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 360px;
}

.media-viewer {
  width: 100%;
  max-height: 560px;
  border-radius: 6px;
  background: #000;
}

.model-placeholder {
  text-align: center;
  color: #606266;
  padding: 40px;
}

.model-title {
  margin: 12px 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.model-description {
  margin-bottom: 16px;
  font-size: 13px;
  color: #909399;
}
</style>
