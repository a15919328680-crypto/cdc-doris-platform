# Contributing to CDC Doris Platform

我们非常欢迎并感谢所有的贡献！

## 如何贡献

### 提交 Issue
- 报告 Bug
- 提出新功能建议
- 提出问题

### 提交 Pull Request

1. Fork 本仓库
2. 创建你的分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 开发环境搭建

```bash
# 安装依赖
cd backend && mvn clean install
cd ../frontend && npm install

# 启动后端
cd backend && mvn spring-boot:run

# 启动前端
cd frontend && npm run dev
```

## 代码规范

### Java
- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 添加必要的注释

### Vue/JavaScript
- 使用 ESLint 检查代码
- 组件命名使用 PascalCase
- 使用 Composition API

### Git Commit 规范

```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具/配置相关
```

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 了解详情。
