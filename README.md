![](https://img.shields.io/badge/License-MIT-blue.svg)

![](https://img.shields.io/badge/Version-1.0.0-brightgreen.svg)

![](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)

# 在线考试系统 (Online Exam System)

一个功能完善、多角色权限分离的现代化在线考试平台。系统支持从题库建设、智能组卷、在线答题到成绩分析的全流程管理，适用于学校考试、企业培训、知识竞赛等多种场景。


#### 安装环境
**️ 注意**：请确保您的开发环境已安装以下依赖。

JAVA 环境 

Node.js环境 [https://nodejs.org/en/] 选择14.17

Yarn 打开cmd， 输入npm install -g yarn !!!必须安装完毕nodejs

Mysql 数据库 一定要把账户和密码记住

redis

Idea 编译器

WebStorm OR VScode 编译器

#### 采用技术及功能

后端：SpringBoot、MybatisPlus、MySQL、Redis、
前端：Vue、Apex、Antd、Axios

平台前端：vue(框架) + vuex(全局缓存) + rue-router(路由) + axios(请求插件) + apex(图表)  + antd-ui(ui组件)

平台后台：springboot(框架) + redis(缓存中间件) + shiro(权限中间件) + mybatisplus(orm) + restful风格接口 + mysql(数据库)

开发环境：windows10 or windows7 ， vscode or webstorm ， idea + lambok

---

## 核心特性

系统采用 RBAC（基于角色的访问控制）模型，为不同用户提供专属工作台：

---

## ️ 功能模块概览

### 管理员 (Admin)

系统最高权限角色，负责全局统筹与数据维护。

- **管理仪表板**：全局数据概览，掌握系统整体运行状态。
- **考试/题库/题目管理**：统筹全系统考试安排、题库分类及底层题目数据的增删改查。
- **题库组卷**：制定组卷规则，批量生成标准化试卷。
- **学生/导师/班级管理**：维护系统人员架构，管理用户账号及班级归属。
- **成绩查询**：全局成绩监控与数据导出。
- **公告/轮播图**：发布系统通知，配置首页宣传展示内容。
- **可视化大屏**：实时展示考试数据、成绩分布等宏观图表。

### 教师 (Teacher)

教学执行角色，专注于班级教学与考试组织。

- **管理仪表板**：查看个人教学进度与待办事项。
- **考试/题库/题目管理**：创建并管理自己负责的课程考试、题库及具体题目。
- **题库组卷**：根据教学需求，从题库中抽题或手动编排试卷。
- **成绩查询**：查看并分析所教班级学生的考试成绩。
- **班级内管理**：管理所属班级的学生名单及日常教学事务。
- **可视化大屏**：查看所教班级的考试学情数据看板。

### 学生 (Student)

系统使用者，专注于在线学习与考核。

- **公告查看**：接收并阅读系统或老师发布的通知。
- **考试仪表板**：查看个人学习进度、待考安排及历史考试记录。
- **考试中心**：浏览可参加的考试列表及考试规则。
- **在线答题**：进入考场进行实时作答与交卷。
- **我的分数**：查询历次考试成绩及错题反馈。
- **交流沟通**：与教师或同学进行在线答疑与互动。
- **个人信息/修改密码**：维护个人基础资料及账号安全。

---

## 项目结构

```text
pharmacy-dispensing-system/
├── backend/            # 后端 API 服务
│   ├── src/
│   │   ├── controllers/  # 控制器
│   │   ├── models/       # 数据模型
│   │   ├── routes/       # 路由
│   │   └── services/     # 业务逻辑
│   └── package.json
├── frontend/           # 前端 Web 应用
│   ├── src/
│   │   ├── views/      # 页面视图
│   │   ├── components/ # 公共组件
│   │   └── store/      # 状态管理
│   └── package.json
├── docs/               # 项目文档与 API 接口说明
├── LICENSE
└── README.md
```
---


#### 前台启动方式
安装所需文件 yarn install 
运行 yarn run dev

#### 默认后台账户密码
[管理员]
admin
123456

[导师]
teacher_demo
123456

[学生]
fank
123456

#### 项目截图

|  |  |
|---------------------|---------------------|
| ![](./assets/image%20(1).png) | ![](./assets/image%20(2).png) |
| ![](./assets/image%20(3).png) | ![](./assets/image%20(4).png) |
| ![](./assets/image%20(5).png) | ![](./assets/image%20(6).png) |
| ![](./assets/image%20(7).png) | ![](./assets/image%20(8).png) |
| ![](./assets/image%20(9).png) | ![](./assets/image%20(10).png) |
| ![](./assets/image%20(11).png) | ![](./assets/image%20(12).png) |
| ![](./assets/image%20(13).png) | ![](./assets/image%20(14).png) |
| ![](./assets/image%20(15).png) | ![](./assets/image%20(16).png) |
| ![](./assets/image%20(17).png) | ![](./assets/image%20(18).png) |
| ![](./assets/image%20(19).png) | ![](./assets/image%20(20).png) | 
| ![](./assets/image%20(21).png) |  | 

#### 演示视频

暂无

#### 获取方式

Email: fan1ke2ke@gmail.com

WeChat: `Storm_Berserker`

`附带部署与讲解服务，因为要恰饭资源非免费，伸手党勿扰，谢谢理解😭`

> 1.项目纯原创，不做二手贩子 2.一次购买终身有效 3.项目讲解持续到答辩结束 4.非常负责的答辩指导 5.**黑奴价格**

> 项目部署调试不好包退！功能逻辑没讲明白包退！

<div align=center><img align="center" src="./assets/product_word.png" alt="fankekeke" /></div>

#### 其它资源
[2026年-答辩顺利通过-客户评价🀄](https://berserker287.github.io/2026/06/29/2026%E5%B9%B4%E7%AD%94%E8%BE%A9%E9%A1%BA%E5%88%A9%E9%80%9A%E8%BF%87/)

[2025年-答辩顺利通过-客户评价🍜](https://berserker287.github.io/2025/06/18/2025%E5%B9%B4%E7%AD%94%E8%BE%A9%E9%A1%BA%E5%88%A9%E9%80%9A%E8%BF%87/)

[2024年-答辩顺利通过-客户评价👻](https://berserker287.github.io/2024/06/06/2024%E5%B9%B4%E7%AD%94%E8%BE%A9%E9%A1%BA%E5%88%A9%E9%80%9A%E8%BF%87/)

[2023年-答辩顺利通过-客户评价🐢](https://berserker287.github.io/2023/06/14/2023%E5%B9%B4%E7%AD%94%E8%BE%A9%E9%A1%BA%E5%88%A9%E9%80%9A%E8%BF%87/)

[2022年-答辩通过率100%-客户评价🐣](https://berserker287.github.io/2022/05/25/%E9%A1%B9%E7%9B%AE%E4%BA%A4%E6%98%93%E8%AE%B0%E5%BD%95/)

[毕业答辩导师提问的高频问题](https://berserker287.github.io/2023/06/13/%E6%AF%95%E4%B8%9A%E7%AD%94%E8%BE%A9%E5%AF%BC%E5%B8%88%E6%8F%90%E9%97%AE%E7%9A%84%E9%AB%98%E9%A2%91%E9%97%AE%E9%A2%98/)

[50个高频答辩问题-技术篇](https://berserker287.github.io/2023/06/13/50%E4%B8%AA%E9%AB%98%E9%A2%91%E7%AD%94%E8%BE%A9%E9%97%AE%E9%A2%98-%E6%8A%80%E6%9C%AF%E7%AF%87/)

[计算机毕设答辩时都会问到哪些问题？](https://www.zhihu.com/question/31020988)

[计算机专业毕业答辩小tips](https://zhuanlan.zhihu.com/p/145911029)

#### 接JAVAWEB毕设，纯原创，价格公道，诚信第一

`网站建设、小程序、H5、APP、各种系统 选题+开题报告+任务书+程序定制+安装调试+项目讲解+论文+答辩PPT`

More info: [悲伤的橘子树](https://berserker287.github.io/)

<div align="center"><img align="center" src="./assets/chat_qr_code.png" alt="fankekeke" width=250 /></div>

<p><img align="center" src="./assets/wechat_group.png" alt="fankekeke" /></p>

**感谢查看**
