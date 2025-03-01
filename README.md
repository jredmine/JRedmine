## Java 版本的 Redmine

### JRedmine 简介

使用 `SpringBoot` 基于 `Maven` 构建的实现类似 `Redmine` 的功能 `Java` 版本的 `Redmine` 项目。

目前还在框架搭建阶段，欢迎大家一起参与进来，共同完善这个项目。

目前的项目开发成员有：

* Lei Dong
* Dacheng Gao
* Feng Pan

### 本地部署

1. 克隆项目到本地

    ```shell
    git clone https://github.com/dgp-dream/JRedmine.git
    ```

2. 安装MySQL8.0，并创建数据库 `jredmine`

3. 导入项目中的 `resources/data/jredmine.sql` 到 `jredmine` 数据库

4. 复制 `application-dev.yml` 到 `application-local.yml` 并修改成本地数据库配置

5. 配置 `Maven` 镜像为阿里云镜像

   修改 `~/.m2/settings.xml` 配置文件为如下配置：

    ```xml
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
        https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <mirrors>
        <mirror>
            <id>nexus-aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Nexus aliyun</name>
            <url>http://maven.aliyun.com/nexus/content/groups/public</url>
        </mirror>
    </mirrors>
    </settings>
    ```

6. 编译构建项目

    ```shell
    mvn clean compile 
    ```

7. 启动项目

    ```shell
    mvn spring-boot:run
    ```

8. 访问 `http://localhost:8088/index` 可测试是否部署成功

9. 注册接口测试，使用IDEA内置的 `REST Client` 插件或其他工具，发送如下请求

    ```http request
    POST http://localhost:8088/api/users/register
    Content-Type: application/json
    
    {
      "login": "test3",
      "password": "12345678",
      "confirmPassword": "12345678",
      "firstname": "abc",
      "lastname": "abc",
      "email": "test3@qq.com",
      "hideEmailFlag": false
    }
    ```


### 技术栈

* JDK 21
* SpringBoot 3.5.0-M1
* SpringJPA
* SpringQueryDSL
* SpringSecurity
* JWT
* MapStruct
* MySQL 8.0
* Redis
* **...待续**
 
### 功能模块

- [ ] 用户管理
- [ ] 权限管理
- [ ] 项目管理
- [ ] 任务管理
- [ ] 甘特图 
- [ ] 议程Wiki
- [ ] 文档管理
- [ ] 时间跟踪
- [ ] 系统设置
- [ ] 统计分析
- [ ] 邮件通知
- [ ] 短信通知
- [ ] 微信通知
- [ ] 推送通知
- [ ] 第三方集成
- [ ] 多语言支持
- [ ] 多数据源支持
- [ ] 多种主题风格支持
- [ ] 插件系统
- [ ] **...待续**