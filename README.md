# MySQL -> TiDB 兼容性报告工具

基于 Spring Boot + Java，自动扫描 MySQL 中实际使用的对象与配置，并生成 TiDB 兼容性 HTML 报告。

## 功能覆盖

- 字符集、排序规则
- 存储引擎
- SQL_MODE
- 临时表使用迹象（统计变量 + 对象定义）
- 自定义变量使用迹象
- MySQL UDF (`mysql.func`)
- 存储函数、存储过程
- 触发器、事件
- Sequence（序列对象）
- 空间类型 / GIS（空间列、SPATIAL 索引、GIS 函数使用证据）
- FULLTEXT 索引
- XML Functions（ExtractValue / UpdateXML 使用证据）
- 事务隔离级别

输出内容包括：
- MySQL 已使用对象/配置
- TiDB 兼容性判断（COMPATIBLE / PARTIAL / INCOMPATIBLE / UNKNOWN）
- 不兼容对象清单（对象名、原因；字符集/排序规则/引擎场景附表结构）
- 判断依据与迁移建议

## 配置

编辑 `src/main/resources/application.yml`：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

建议使用具备 `information_schema`、`performance_schema` 及 `mysql.func` 查询权限的账号，以获得完整报告。

## 运行

```bash
mvn spring-boot:run
```

## 访问

- 在线报告：`http://localhost:8080/report`
- 指定 schema：`http://localhost:8080/report?schema=your_db`
- 导出 HTML：`http://localhost:8080/report/export`

导出文件目录由 `report.output-dir` 控制，默认 `./report-output`。

## 注意

- 部分兼容项需结合目标 TiDB 版本二次校验。
- 如果权限不足，页面会在“扫描告警”中展示失败查询。
