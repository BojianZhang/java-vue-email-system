<template>
  <div class="login-container">
    <div class="login-form">
      <div class="login-header">
        <h2>企业邮件系统</h2>
        <p>登录您的账户</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="email">
          <el-input
            v-model="loginForm.email"
            placeholder="请输入邮箱地址"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">
              记住登录状态
            </el-checkbox>
            <el-link type="primary" @click="showForgotPassword = true">
              忘记密码？
            </el-link>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>

        <el-form-item>
          <div class="register-link">
            还没有账户？
            <router-link to="/register" class="link">
              立即注册
            </router-link>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 忘记密码对话框 -->
    <el-dialog
      v-model="showForgotPassword"
      title="重置密码"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        size="large"
      >
        <el-form-item prop="email">
          <el-input
            v-model="forgotForm.email"
            placeholder="请输入注册邮箱"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showForgotPassword = false">
            取消
          </el-button>
          <el-button
            type="primary"
            :loading="forgotLoading"
            @click="handleForgotPassword"
          >
            发送重置邮件
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { forgotPassword } from '@/api/auth'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 表单引用
const loginFormRef = ref()
const forgotFormRef = ref()

// 登录表单
const loginForm = reactive({
  email: '',
  password: '',
  rememberMe: false
})

// 忘记密码表单
const forgotForm = reactive({
  email: ''
})

// 加载状态
const loading = ref(false)
const forgotLoading = ref(false)
const showForgotPassword = ref(false)

// 表单验证规则
const loginRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true

  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理忘记密码
const handleForgotPassword = async () => {
  if (!forgotFormRef.value) return

  const valid = await forgotFormRef.value.validate().catch(() => false)
  if (!valid) return

  forgotLoading.value = true

  try {
    await forgotPassword(forgotForm.email)
    ElMessage.success('重置密码邮件已发送，请查收邮箱')
    showForgotPassword.value = false
    forgotForm.email = ''
  } catch (error) {
    console.error('发送重置邮件失败:', error)
  } finally {
    forgotLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-form {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;

  h2 {
    color: #303133;
    font-size: 28px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  p {
    color: #909399;
    font-size: 14px;
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.register-link {
  text-align: center;
  color: #909399;
  font-size: 14px;

  .link {
    color: #409eff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}
</style>