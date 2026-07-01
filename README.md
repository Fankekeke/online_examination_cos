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
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/872d9d0a-f5c8-477e-a31a-47040f0cb4b6.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/1a75c239-e4c7-4cbf-ba5b-04608e0da8da.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/514d1b65-ba00-473a-b20a-9dc76ed65e2f.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/e64d82cc-80e3-4daa-9d0f-fe2ea9db0bcc.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/317eff6a-3c28-4323-9555-fb44f40394d2.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/d4243278-6a8f-4d65-b761-f820fe7d16d8.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/91ed5580-50d3-467d-b075-867356625a5b.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/d9ac77a9-7793-4c68-b8ea-2b12fcdc1baf.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/71a1f687-1a0b-4138-8d75-5219b2e652fa.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/cbcef96a-b3f1-4bee-9f62-aefcbb622222.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/059ad9c8-3373-4dbf-94b1-385dd5a1c1eb.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/ca617219-6f9b-4572-ba5a-ab3efccd59ca.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/33ed8731-3579-4c69-abd0-bc09d3241ac8.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/c1b2858c-6072-4c23-8e8e-4d61b839394d.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/20a12e09-3fc3-47bc-9f35-303f70a2db06.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/b609b858-91bd-4a27-bfde-e80867ce1357.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/5b0963a2-f3c9-4125-8a0b-2d7908176a22.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/a96cd369-2baa-47ea-8f10-1daea7325b18.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/1df22f75-4355-4864-85da-8f115df609f6.png) | ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/46886e5f-0a6b-429b-8357-b3335b8033d0.png) |
| ![](https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/1add60ed-96d2-47ed-b36a-be96358bd3c3.png) |  |

#### 演示视频

暂无

#### 获取方式

Email: fan1ke2ke@gmail.com

WeChat: `Storm_Berserker`

`附带部署与讲解服务，因为要恰饭资源非免费，伸手党勿扰，谢谢理解😭`

> 1.项目纯原创，不做二手贩子 2.一次购买终身有效 3.项目讲解持续到答辩结束 4.非常负责的答辩指导 5.**黑奴价格**

> 项目部署调试不好包退！功能逻辑没讲明白包退！

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

<p><img align="center" src="https://fank-bucket-oss.oss-cn-beijing.aliyuncs.com/img/%E5%90%88%E4%BD%9C%E7%89%A9%E6%96%99%E6%A0%B7%E5%BC%8F%20(3).png" alt="fankekeke" /></p>
