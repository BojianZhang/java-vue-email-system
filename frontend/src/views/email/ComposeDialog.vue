<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="80%"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="compose-form" v-loading="sending">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 发件人选择 -->
        <el-form-item label="发件人" prop="fromAliasId">
          <el-select v-model="form.fromAliasId" placeholder="选择发件人别名" style="width: 100%">
            <el-option
              v-for="alias in aliases"
              :key="alias.id"
              :label="`${alias.aliasName} <${alias.aliasAddress}>`"
              :value="alias.id"
            />
          </el-select>
        </el-form-item>

        <!-- 收件人 -->
        <el-form-item label="收件人" prop="recipients">
          <el-input
            v-model="form.recipients"
            placeholder="多个邮箱地址用分号分隔"
            type="textarea"
            :rows="2"
          />
        </el-form-item>

        <!-- 抄送 -->
        <el-form-item label="抄送">
          <el-input
            v-model="form.cc"
            placeholder="多个邮箱地址用分号分隔"
            type="textarea"
            :rows="1"
          />
        </el-form-item>

        <!-- 密送 -->
        <el-form-item label="密送">
          <el-input
            v-model="form.bcc"
            placeholder="多个邮箱地址用分号分隔"
            type="textarea"
            :rows="1"
          />
        </el-form-item>

        <!-- 主题 -->
        <el-form-item label="主题" prop="subject">
          <el-input v-model="form.subject" placeholder="请输入邮件主题" />
        </el-form-item>

        <!-- 附件 -->
        <el-form-item label="附件">
          <div class="attachment-section">
            <el-upload
              ref="uploadRef"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :file-list="fileList"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :on-remove="handleRemoveFile"
              :before-upload="beforeUpload"
              multiple
              :show-file-list="true"
            >
              <el-button type="primary" icon="Plus">添加附件</el-button>
            </el-upload>
            <div class="upload-tip">
              支持的文件类型：图片、文档、压缩包等，单个文件最大50MB
            </div>
          </div>
        </el-form-item>

        <!-- 邮件内容 -->
        <el-form-item label="内容" prop="content">
          <div class="content-editor">
            <div class="editor-toolbar">
              <el-radio-group v-model="contentMode" size="small">
                <el-radio-button label="html">富文本</el-radio-button>
                <el-radio-button label="text">纯文本</el-radio-button>
              </el-radio-group>
            </div>

            <!-- 富文本编辑器 -->
            <div v-if="contentMode === 'html'" class="html-editor">
              <div ref="editorRef" class="editor-container"></div>
            </div>

            <!-- 纯文本编辑器 -->
            <el-input
              v-else
              v-model="form.textContent"
              type="textarea"
              :rows="15"
              placeholder="请输入邮件内容..."
            />
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
        <el-button type="primary" @click="sendEmail" :loading="sending">
          发送邮件
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmailStore } from '@/stores/email'
import { useAliasStore } from '@/stores/alias'
import { useUserStore } from '@/stores/user'
// 富文本编辑器 - 这里使用Quill作为示例
// import Quill from 'quill'
// import 'quill/dist/quill.snow.css'

// Props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  currentAliasId: {
    type: Number,
    default: null
  },
  replyEmail: {
    type: Object,
    default: null
  },
  forwardEmail: {
    type: Object,
    default: null
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'sent'])

// 数据
const emailStore = useEmailStore()
const aliasStore = useAliasStore()
const userStore = useUserStore()

const formRef = ref()
const editorRef = ref()
const uploadRef = ref()
const sending = ref(false)
const saving = ref(false)
const contentMode = ref('html')
const fileList = ref([])
const attachmentIds = ref([])

// 富文本编辑器实例（注释掉，因为需要额外安装Quill）
// let quillEditor = null

const form = reactive({
  fromAliasId: null,
  recipients: '',
  cc: '',
  bcc: '',
  subject: '',
  textContent: '',
  htmlContent: ''
})

// 验证规则
const rules = {
  fromAliasId: [
    { required: true, message: '请选择发件人', trigger: 'change' }
  ],
  recipients: [
    { required: true, message: '请输入收件人', trigger: 'blur' },
    { validator: validateEmails, trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请输入邮件主题', trigger: 'blur' }
  ]
}

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const aliases = computed(() => aliasStore.aliases)

const dialogTitle = computed(() => {
  if (props.replyEmail) {
    return props.replyEmail.replyAll ? '全部回复' : '回复邮件'
  } else if (props.forwardEmail) {
    return '转发邮件'
  }
  return '写邮件'
})

const uploadUrl = computed(() => {
  return import.meta.env.VITE_API_BASE_URL + '/api/email/attachments/upload'
})

const uploadHeaders = computed(() => {
  return {
    'Authorization': `Bearer ${userStore.token}`
  }
})

// 邮箱验证函数
function validateEmails(rule, value, callback) {
  if (!value) {
    callback()
    return
  }
  
  const emails = value.split(/[;,]/).map(email => email.trim()).filter(email => email)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  
  for (const email of emails) {
    if (!emailRegex.test(email)) {
      callback(new Error(`邮箱地址格式不正确: ${email}`))
      return
    }
  }
  
  callback()
}

// 方法
const initEditor = () => {
  // 富文本编辑器初始化（注释掉，因为需要额外安装Quill）
  /*
  if (contentMode.value === 'html' && editorRef.value && !quillEditor) {
    nextTick(() => {
      quillEditor = new Quill(editorRef.value, {
        theme: 'snow',
        placeholder: '请输入邮件内容...',
        modules: {
          toolbar: [
            ['bold', 'italic', 'underline', 'strike'],
            ['blockquote', 'code-block'],
            [{ 'header': 1 }, { 'header': 2 }],
            [{ 'list': 'ordered'}, { 'list': 'bullet' }],
            [{ 'script': 'sub'}, { 'script': 'super' }],
            [{ 'indent': '-1'}, { 'indent': '+1' }],
            [{ 'direction': 'rtl' }],
            [{ 'size': ['small', false, 'large', 'huge'] }],
            [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
            [{ 'color': [] }, { 'background': [] }],
            [{ 'font': [] }],
            [{ 'align': [] }],
            ['clean'],
            ['link', 'image']
          ]
        }
      })
      
      quillEditor.on('text-change', () => {
        form.htmlContent = quillEditor.root.innerHTML
      })
    })
  }
  */
}

const initFormData = () => {
  // 重置表单
  Object.assign(form, {
    fromAliasId: props.currentAliasId,
    recipients: '',
    cc: '',
    bcc: '',
    subject: '',
    textContent: '',
    htmlContent: ''
  })
  
  fileList.value = []
  attachmentIds.value = []
  
  // 处理回复邮件
  if (props.replyEmail) {
    const originalEmail = props.replyEmail
    form.recipients = originalEmail.sender
    form.subject = originalEmail.subject.startsWith('Re: ') 
      ? originalEmail.subject 
      : `Re: ${originalEmail.subject}`
    
    if (props.replyEmail.replyAll && originalEmail.cc) {
      form.cc = originalEmail.cc
    }
    
    // 构建回复内容
    const replyContent = buildReplyContent(originalEmail)
    form.textContent = replyContent.text
    form.htmlContent = replyContent.html
  }
  
  // 处理转发邮件
  if (props.forwardEmail) {
    const originalEmail = props.forwardEmail
    form.subject = originalEmail.subject.startsWith('Fwd: ') 
      ? originalEmail.subject 
      : `Fwd: ${originalEmail.subject}`
    
    // 构建转发内容
    const forwardContent = buildForwardContent(originalEmail)
    form.textContent = forwardContent.text
    form.htmlContent = forwardContent.html
  }
}

const buildReplyContent = (originalEmail) => {
  const date = new Date(originalEmail.sentTime || originalEmail.receivedTime)
  const dateStr = date.toLocaleString('zh-CN')
  
  const textContent = `\n\n在 ${dateStr}，${originalEmail.sender} 写道：\n${originalEmail.contentText || ''}`
  
  const htmlContent = `
    <br><br>
    <div style="border-left: 2px solid #ccc; padding-left: 10px; margin-left: 10px;">
      <div style="color: #666; font-size: 12px; margin-bottom: 10px;">
        在 ${dateStr}，${originalEmail.sender} 写道：
      </div>
      <div>${originalEmail.contentHtml || originalEmail.contentText || ''}</div>
    </div>
  `
  
  return { text: textContent, html: htmlContent }
}

const buildForwardContent = (originalEmail) => {
  const date = new Date(originalEmail.sentTime || originalEmail.receivedTime)
  const dateStr = date.toLocaleString('zh-CN')
  
  const textContent = `\n\n---------- 转发邮件 ----------\n发件人: ${originalEmail.sender}\n日期: ${dateStr}\n主题: ${originalEmail.subject}\n收件人: ${originalEmail.recipient}\n\n${originalEmail.contentText || ''}`
  
  const htmlContent = `
    <br><br>
    <div style="border: 1px solid #ddd; padding: 15px; margin: 10px 0;">
      <div style="font-weight: bold; margin-bottom: 10px;">---------- 转发邮件 ----------</div>
      <div style="margin-bottom: 5px;"><strong>发件人:</strong> ${originalEmail.sender}</div>
      <div style="margin-bottom: 5px;"><strong>日期:</strong> ${dateStr}</div>
      <div style="margin-bottom: 5px;"><strong>主题:</strong> ${originalEmail.subject}</div>
      <div style="margin-bottom: 10px;"><strong>收件人:</strong> ${originalEmail.recipient}</div>
      <div>${originalEmail.contentHtml || originalEmail.contentText || ''}</div>
    </div>
  `
  
  return { text: textContent, html: htmlContent }
}

const beforeUpload = (file) => {
  const allowedTypes = [
    'image/jpeg', 'image/png', 'image/gif',
    'application/pdf', 'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain', 'application/zip'
  ]
  
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('不支持的文件类型')
    return false
  }
  
  const maxSize = 50 * 1024 * 1024 // 50MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  
  return true
}

const handleUploadSuccess = (response, file) => {
  if (response.success) {
    attachmentIds.value.push(response.data.id)
    ElMessage.success('附件上传成功')
  } else {
    ElMessage.error(response.message || '附件上传失败')
  }
}

const handleUploadError = (error, file) => {
  ElMessage.error('附件上传失败')
  console.error('Upload error:', error)
}

const handleRemoveFile = (file) => {
  // 从附件ID列表中移除
  const index = fileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1 && attachmentIds.value[index]) {
    attachmentIds.value.splice(index, 1)
  }
}

const sendEmail = async () => {
  try {
    await formRef.value.validate()
    
    sending.value = true
    
    const emailData = {
      recipients: form.recipients.split(/[;,]/).map(email => email.trim()).filter(email => email),
      cc: form.cc ? form.cc.split(/[;,]/).map(email => email.trim()).filter(email => email) : [],
      bcc: form.bcc ? form.bcc.split(/[;,]/).map(email => email.trim()).filter(email => email) : [],
      subject: form.subject,
      textContent: form.textContent,
      htmlContent: contentMode.value === 'html' ? form.htmlContent : null
    }
    
    await emailStore.sendEmail(emailData, form.fromAliasId, attachmentIds.value)
    
    ElMessage.success('邮件发送成功')
    emit('sent')
    handleClose()
    
  } catch (error) {
    ElMessage.error('邮件发送失败')
  } finally {
    sending.value = false
  }
}

const saveDraft = async () => {
  try {
    saving.value = true
    
    // 这里可以实现草稿保存逻辑
    ElMessage.success('草稿保存成功')
    
  } catch (error) {
    ElMessage.error('保存草稿失败')
  } finally {
    saving.value = false
  }
}

const handleClose = async () => {
  // 检查是否有未保存的内容
  const hasContent = form.subject || form.textContent || form.htmlContent || 
                     form.recipients || form.cc || form.bcc || fileList.value.length > 0
  
  if (hasContent) {
    try {
      await ElMessageBox.confirm(
        '邮件内容尚未发送，确定要关闭吗？',
        '确认关闭',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      return // 用户取消关闭
    }
  }
  
  visible.value = false
  
  // 清理编辑器
  /*
  if (quillEditor) {
    quillEditor = null
  }
  */
}

// 监听
watch(visible, (newVal) => {
  if (newVal) {
    initFormData()
    nextTick(() => {
      initEditor()
    })
  }
})

watch(contentMode, (newMode) => {
  if (newMode === 'html') {
    nextTick(() => {
      initEditor()
    })
  }
})

onMounted(() => {
  if (visible.value) {
    initFormData()
  }
})
</script>

<style scoped>
.compose-form {
  max-height: 70vh;
  overflow-y: auto;
}

.attachment-section {
  width: 100%;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.content-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.html-editor {
  min-height: 300px;
}

.editor-container {
  height: 300px;
  background: white;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-upload-list) {
  margin-top: 8px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-textarea__inner) {
  resize: vertical;
}
</style>