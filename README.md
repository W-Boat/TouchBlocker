# 触摸屏蔽器 (Touch Blocker)

一个功能强大的Android触摸屏蔽应用，支持通过Root权限或LSPosed框架来精确控制触摸屏蔽功能。

## 🚀 功能特性

### 核心功能
- **智能触摸屏蔽**: 精确控制屏幕触摸响应
- **多种授权方式**: 支持Root权限和LSPosed框架
- **无障碍服务集成**: 提供按键监听和控制功能
- **实时状态监控**: 动态显示各项功能状态
- **错误处理优化**: 详细的错误信息和用户引导

### 技术特性
- **现代化架构**: MVVM + Clean Architecture
- **响应式UI**: Jetpack Compose + Material Design 3
- **依赖注入**: Hilt框架管理依赖
- **数据持久化**: DataStore安全存储用户偏好
- **异步处理**: Kotlin Coroutines + Flow
- **完善测试**: 单元测试和集成测试
- **日志系统**: Timber结构化日志

## 🏗️ 架构设计

### 项目结构
```
app/src/main/java/com/example/myapplication/
├── data/                    # 数据层
│   ├── datastore/          # DataStore数据存储
│   ├── repository/         # 数据仓库
│   └── service/            # 数据服务
├── domain/                 # 领域层
│   └── model/             # 领域模型
├── presentation/           # 表现层
│   ├── ui/                # UI组件
│   └── viewmodel/         # ViewModel
├── service/               # 业务服务
│   ├── RootService.kt     # Root权限服务
│   ├── LSPosedService.kt  # LSPosed服务
│   └── PermissionService.kt # 权限管理服务
├── utils/                 # 工具类
│   ├── AccessibilityHelper.kt # 无障碍服务助手
│   ├── ErrorHandler.kt    # 错误处理
│   └── TimeoutHelper.kt   # 超时处理
└── di/                    # 依赖注入
    └── AppModule.kt       # Hilt模块
```

### 架构层次

#### 表现层 (Presentation Layer)
- **UI组件**: Jetpack Compose构建的现代化界面
- **ViewModel**: 管理UI状态和业务逻辑
- **状态管理**: 使用StateFlow和LiveData

#### 业务层 (Business Layer)
- **服务类**: 封装具体业务逻辑
- **权限管理**: 统一的权限检查和请求
- **错误处理**: 集中的异常处理和用户提示

#### 数据层 (Data Layer)
- **数据存储**: DataStore安全存储用户偏好
- **仓库模式**: 统一的数据访问接口
- **缓存策略**: 智能的数据缓存机制

## 🛠️ 技术栈

### 核心技术
- **Kotlin**: 100% Kotlin开发
- **Android Jetpack Compose**: 声明式UI框架
- **Hilt**: 依赖注入框架
- **DataStore**: 现代化数据存储
- **Coroutines + Flow**: 异步编程和响应式数据流

### 开发工具
- **Timber**: 结构化日志记录
- **MockK**: 单元测试Mock框架
- **Robolectric**: Android单元测试
- **Gradle**: 构建系统

## 📱 安装和使用

### 系统要求
- Android 7.0 (API 24) 或更高版本
- 可选: Root权限或LSPosed框架

### 安装步骤
1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd MyApplication
   ```

2. **构建项目**
   ```bash
   ./gradlew build
   ```

3. **安装应用**
   ```bash
   ./gradlew installDebug
   ```

### 配置说明

#### 无障碍服务配置
1. 打开应用后点击"启用无障碍服务"
2. 在系统设置中找到"触摸屏蔽器"服务
3. 启用该服务并授予权限

#### Root权限配置
1. 确保设备已获取Root权限
2. 应用会自动检测并请求Root权限
3. 在Root管理应用中授予权限

#### LSPosed配置
1. 安装LSPosed框架
2. 在LSPosed管理器中激活本模块
3. 重启设备使配置生效

## 🔧 开发指南

### 环境配置
- Android Studio Arctic Fox或更高版本
- JDK 11或更高版本
- Android SDK 33或更高版本

### 代码规范
- 遵循Kotlin官方编码规范
- 使用ktlint进行代码格式化
- 编写完整的KDoc文档

### 测试策略
- 单元测试覆盖率 > 80%
- 集成测试覆盖关键业务流程
- UI测试验证用户交互

### 构建命令
```bash
# 运行测试
./gradlew test

# 生成测试报告
./gradlew jacocoTestReport

# 代码质量检查
./gradlew ktlintCheck

# 构建Release版本
./gradlew assembleRelease
```

## 🔒 权限说明

### 必需权限
- **无障碍服务**: 监听按键事件和控制触摸屏蔽
- **前台服务**: 保持服务在后台运行

### 可选权限
- **Root权限**: 提供更强大的系统控制能力
- **LSPosed模块**: Root权限的替代方案

## 🐛 故障排除

### 常见问题

#### 无障碍服务无法启用
- 检查系统设置中的无障碍服务列表
- 确认应用已正确安装
- 重启设备后重试

#### Root权限请求失败
- 确认设备已正确Root
- 检查Root管理应用设置
- 查看应用日志获取详细错误信息

#### LSPosed模块不工作
- 确认LSPosed框架已正确安装
- 在LSPosed管理器中激活模块
- 重启设备使配置生效

### 日志调试
应用使用Timber进行日志记录，可通过以下方式查看：
```bash
adb logcat -s TouchBlocker
```

## 🤝 贡献指南

### 贡献方式
1. Fork项目到个人仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

### 代码贡献规范
- 遵循现有代码风格
- 添加适当的测试用例
- 更新相关文档
- 确保所有测试通过

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如果您遇到问题或有建议，请：
- 提交 [Issue](../../issues)
- 发送邮件至开发者
- 查看 [Wiki](../../wiki) 获取更多信息

## 🙏 致谢

感谢所有为项目做出贡献的开发者和用户。

---

**注意**: 本应用仅用于合法用途，请遵守当地法律法规。