# HackerOne 别名同步配置指南

## 功能说明

本功能可以从 HackerOne 平台同步您的用户名和显示名称，自动更新本地邮件系统中的别名显示名称。

**支持的邮箱格式:**
- 基础格式: `username@wearehackerone.com`
- 扩展格式: `username+extension@wearehackerone.com`
- 示例: `alice+bug123@wearehackerone.com`

## 配置步骤

### 1. 获取 HackerOne API Token

1. 登录 [HackerOne](https://hackerone.com)
2. 进入 **Settings** → **API Token**
3. 点击 **Create API Token**
4. 设置合适的权限（需要读取用户信息的权限）
5. 复制生成的 API Token

### 2. 在邮件系统中配置同步

1. 登录邮件系统
2. 进入 **别名管理** → 选择对应的别名 → **外部同步**
3. 填写配置信息：
   - **平台类型**: 选择 `HackerOne`
   - **平台地址**: `https://api.hackerone.com`
   - **API密钥**: 粘贴您的 API Token
   - **HackerOne用户名**: 您的 HackerOne 用户名
   - **外部别名地址**: 完整的别名地址（支持扩展格式）
   - **同步频率**: 建议设置为 60 分钟

### 3. 使用便捷功能

#### 复制示例格式
- 在 **外部别名地址** 字段中，点击 **"复制示例"** 按钮
- 系统会自动复制 `alice+bug123@wearehackerone.com` 到剪贴板
- 如果输入框为空，会自动填入示例格式

#### 复制已配置的别名地址
- 在同步配置列表中，点击别名名称旁边的复制图标
- 可快速复制完整的别名地址到剪贴板

### 4. 测试连接

点击 **测试连接** 按钮验证配置是否正确。

### 5. 保存并启动同步

保存配置后，系统将自动开始同步过程。

## 配置示例

### 基础格式示例
假设您的 HackerOne 用户名是 `johndoe`：

```json
{
  "platformType": "HACKERONE",
  "platformUrl": "https://api.hackerone.com",
  "apiKey": "your_api_token_here",
  "externalUsername": "johndoe",
  "externalAliasAddress": "johndoe@wearehackerone.com",
  "syncFrequencyMinutes": 60,
  "autoSyncEnabled": true
}
```

### 扩展格式示例
假设您要使用扩展格式：

```json
{
  "platformType": "HACKERONE",
  "platformUrl": "https://api.hackerone.com",
  "apiKey": "your_api_token_here",
  "externalUsername": "alice",
  "externalAliasAddress": "alice+bug123@wearehackerone.com",
  "syncFrequencyMinutes": 60,
  "autoSyncEnabled": true
}
```

## 同步效果

**同步前:**
- 本地别名显示名称: "用户邮箱" 或 "alice+bug123@wearehackerone.com"

**同步后:**
- 本地别名显示名称: "Alice Smith" (从 HackerOne 获取的真实姓名)
- 别名地址保持不变: "alice+bug123@wearehackerone.com"

## 用户界面功能

### 1. 智能输入提示
- 输入框会显示格式提示
- 自动识别 HackerOne 平台并显示相关说明

### 2. 一键复制功能
- **复制示例**: 点击按钮复制 `alice+bug123@wearehackerone.com`
- **复制别名**: 在列表中直接复制已配置的别名地址

### 3. 实时验证
- 自动验证邮箱格式
- 测试外部平台连接状态

## API 详情

系统会调用以下 HackerOne API 端点：

- **用户信息**: `GET /api/v1/users/{username}`
- **当前用户**: `GET /api/v1/me`

从响应中提取以下字段：
- `data.attributes.username` - 用户名
- `data.attributes.name` - 显示名称
- `data.attributes.display_name` - 备用显示名称

## 故障排除

### 常见问题

1. **连接测试失败**
   - 检查 API Token 是否正确
   - 确认 API Token 有足够的权限
   - 验证用户名是否正确

2. **同步失败**
   - 检查 HackerOne 用户名是否存在
   - 确认别名地址格式正确
   - 查看同步错误日志

3. **权限错误**
   - 确保 API Token 有读取用户信息的权限
   - 检查用户名是否为公开信息

4. **复制功能不工作**
   - 现代浏览器默认支持剪贴板API
   - 如果失败会自动降级到传统复制方法
   - 确保网站在HTTPS环境下运行

### 支持的数据源

- 用户的公开个人资料信息
- 用户名到邮箱地址的映射
- 账户状态（活跃/禁用）
- 完整的邮箱地址格式（包括扩展部分）

## 扩展格式说明

HackerOne 的邮箱系统支持 `+` 号扩展格式，这在漏洞报告中非常有用：

- **基础邮箱**: `username@wearehackerone.com`
- **项目专用**: `username+project123@wearehackerone.com`
- **漏洞追踪**: `username+bug456@wearehackerone.com`
- **测试用途**: `username+test@wearehackerone.com`

系统会完整保留这些扩展格式，只同步显示名称。

## 安全说明

- API Token 将加密存储在数据库中
- 所有 API 调用都使用 HTTPS 加密传输
- 只同步公开的用户信息
- 定期检查 API Token 的有效性
- 复制操作在本地浏览器中完成，不涉及服务器

## 注意事项

1. HackerOne 的 API 可能有访问限制，建议同步频率不要过高
2. 确保您的 HackerOne 账户处于活跃状态
3. 如果用户名变更，需要重新配置同步设置
4. 系统只同步用户的显示名称，不会修改邮箱地址本身
5. 扩展格式的邮箱地址会完整保留，包括 `+extension` 部分