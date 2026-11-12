# 储存柜领域字段与制造商端管理接口

## 领域和持久化

领域路径：`craftstudio-mes-domain/src/main/java/com/mes/domain/manufacturer/transBox/storageTank/`。
储存柜、储位、库存、操作记录是四种不同对象。柜内 `storageSlots` 为管理接口的聚合数据源；另有独立 `storageSlot` 集合，管理接口在同一 MongoDB 事务中同步两者。

### 四类实体共有的 BaseEntity 字段

| 字段 | 类型 | 含义和管理规则 |
| --- | --- | --- |
| id | String | MongoDB 文档主键；与下述业务 ID 不同。详情、编辑、删除储存柜使用此值。新增由数据库生成，不接受客户端指定。 |
| createTime | Date | 创建时间，服务器维护；编辑保留。BaseEntity JSON 格式为北京时间 yyyy-MM-dd HH:mm:ss。 |
| updateTime | Date | 最后更新时间，服务器维护。 |

### StorageTank：储存柜的全部现有字段

| 字段 | 类型 | 含义和管理规则 |
| --- | --- | --- |
| storageTankId | String | 储存柜业务 ID，新增自动生成 TANK 前缀 ID；储位、库存和操作记录通过此字段关联柜。编辑保持不变。 |
| storageTankName | String | 人可读的柜名称；新增、编辑必填。 |
| storageTankCode | String | 柜编码，用于设备标识、标签等；新增、编辑必填。当前未定义全局或制造商内唯一索引。 |
| storageTankType | String | 柜类型，如 DEFAULT；原模型没有类型枚举，按业务约定填写。 |
| manufacturerId | String | 所属制造商业务 ID；接口从登录令牌取得，不信任请求体传入的制造商。此次补齐 PO 字段及双向转换。 |
| location | String | 柜所在位置，例如厂区/车间/区域。 |
| status | String | 柜运行状态：ACTIVE 激活、INACTIVE 未激活、MAINTENANCE 维护中；不同于储位占用状态。新增默认 ACTIVE，编辑省略则保留。 |
| movable | Boolean | 是否可移动：true 移动柜，false 固定柜；新增默认 false，编辑省略保留。 |
| totalSlots | Integer | 目标储位总数量，唯一的储位配置入参，可空；服务端生成/调整储位后按实际列表数量保存。 |
| usedSlots | Integer | 已用储位数，统计状态为 OCCUPIED 的储位，不是工件数量之和。 |
| remainingSlots | Integer | 可用储位数，统计状态为 AVAILABLE 的储位。 |
| maxCapacity | Double | 以 capacityUnit 为单位的柜最大容量；本轮暂不开放配置，新柜为 null，编辑保留历史值。 |
| currentCapacity | Double | 当前已占用的容量，与 maxCapacity 使用同一单位；暂不开放配置，新柜为 null，编辑保留历史值。不能把工件件数自动当作 kg 或 m³。 |
| capacityUnit | String | 容量单位，如 kg、m³、件；暂不开放配置，新柜为 null，编辑保留历史值。 |
| temperatureRange | String | 柜级允许/要求的温度范围，例如 18~25℃；这是存储要求，不是实时读数。现有模型为自由文本；暂不开放配置，新柜为 null，编辑保留历史值。 |
| humidityRange | String | 柜级允许/要求的湿度范围，例如 40~60%RH；是存储要求，不是实时读数；暂不开放配置，新柜为 null，编辑保留历史值。 |
| description | String | 柜用途、适用品类等说明。 |
| storageSlots | List<StorageSlot> | 柜内储位明细，仅用于返回，不接受请求体传入；根据 totalSlots 自动生成和调整。 |
| managerId | String | 柜管理员标识；当前无额外关联校验。 |
| managerName | String | 柜管理员显示名称；不会自动由 managerId 查询。 |

StorageTank 还提供 `getUsedSlotCount()`、`getRemainingSlotCount()`、`hasAvailableSlot()` 和 `findAvailableSlot()`，分别动态统计已用/可用数、判断是否有空位、返回第一个 AVAILABLE 储位；它们不是另行持久化的业务字段。

### StorageSlot：储位的全部现有字段

| 字段 | 类型 | 含义和管理规则 |
| --- | --- | --- |
| slotId | String | 储位业务 ID，由服务端生成 SLOT 前缀 ID；调整数量保留已有储位 ID，不需要请求回传。 |
| slotCode | String | 储位可读编码，自动生成 SLOT_1、SLOT_2 等，在柜内避免与现有编码重复。 |
| storageTankId | String | 所属柜的业务 ID；服务端强制设置，不接受请求指定。 |
| slotOrder | Integer | 储位显示/物理顺序，新柜从 1 开始递增；扩容从现有最大顺序号后继续，保留已有顺序。 |
| status | String | AVAILABLE 可用、OCCUPIED 已占用；新增储位默认 AVAILABLE；管理配置接口不直接修改，由出入库流程维护。 |
| productionPieceId | String | 当前存放的生产工件业务标识；编辑储位时保留，由入库写入、出库清空。 |
| productionPieceType | String | 工件类别，如 PRODUCTION_CELL；由出入库流程维护。 |
| quantity | Integer | 当前储位存放工件数量，表示件数而非柜容量；新储位默认 0，后续由出入库维护。 |
| storageTime | Date | 最近入库时间；storeItem() 设置为当前时间，retrieveItem() 不清除此历史值。 |
| outTime | Date | 最近出库时间；retrieveItem() 设置为当前时间，storeItem() 不清除此历史值。 |
| remarks | String | 储位备注；新储位默认 null，暂不开放单独编辑。 |
| weight | Double | 当前存放物品重量；现有模型未定义单位字段，应由业务统一约定。新储位默认 null，暂不开放配置。不是储位最大承重。 |
| volume | Double | 当前存放物品体积；现有模型未定义单位字段。新储位默认 null，暂不开放配置。不是储位最大容积。 |
| temperature | String | 该储位记录的存储时温度，例如 22℃；新储位默认 null，暂不开放配置，不等于柜级温度范围。 |
| humidity | String | 该储位记录的存储时湿度，例如 50%RH；新储位默认 null，暂不开放配置，不等于柜级湿度范围。 |

储位配置只接收 `totalSlots`，不接收储位明细。新储位自动设置 slotId、storageTankId、slotCode、slotOrder、status=AVAILABLE、quantity=0；工件信息、出入库时间、备注、重量、体积、温度、湿度均为 null，数据库 ID 和创建/更新时间由持久化维护。

新增时 totalSlots 省略、null 或 0 都创建空柜。编辑时省略或 null 保留原储位；传数值为目标总数，增加时追加默认空位，减少时从列表末尾移除空位并保留占用储位。若目标数小于有库存的储位数量则拒绝。已有储位的业务 ID、状态、工件、数量、出入库时间及其他记录值不变。totalSlots 必须是非负整数。

### StorageInventory：库存汇总的全部现有字段

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| inventoryId | String | 库存记录业务 ID。 |
| storageTankId | String | 所属柜业务 ID。 |
| storageTankName | String | 柜名称快照。 |
| productionPieceId | String | 汇总对应的生产工件 ID。 |
| productionPieceType | String | 对应工件类别。 |
| totalQuantity | Integer | 总库存数量。 |
| availableQuantity | Integer | 当前可用数量。 |
| lockedQuantity | Integer | 已锁定/预占、不可直接使用的数量。 |
| unit | String | 库存数量的计量单位。 |
| lastCheckTime | Date | 最近盘点时间。 |
| lastCheckOperator | String | 最近盘点操作人，现有字段未区分 ID 和姓名。 |
| status | String | 库存状态，现有模型未规定枚举。 |

这些是库存业务记录，不由柜配置接口创建或更改；原模型未自动强制 totalQuantity = availableQuantity + lockedQuantity。

### StorageOperationRecord：操作记录的全部现有字段

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| recordId | String | 操作记录业务 ID。 |
| storageTankId | String | 被操作柜的业务 ID。 |
| storageTankName | String | 操作时柜名称快照，柜改名不重写历史。 |
| slotId | String | 被操作储位业务 ID。 |
| operationType | String | IN 入库、OUT 出库、TRANSFER 转移、CHECK 盘点。 |
| productionPieceId | String | 操作涉及的工件 ID。 |
| productionPieceType | String | 操作涉及的工件类型。 |
| quantity | Integer | 此次操作的数量，不是操作后的剩余库存。 |
| operatorId | String | 操作人 ID。 |
| operatorName | String | 操作人姓名快照。 |
| operationTime | Date | 操作发生时间。 |
| remarks | String | 操作原因、说明等。 |
| status | String | 操作执行状态；现有入库流程使用 SUCCESS，模型未规定完整枚举。 |

四个 OPERATION_TYPE_* 常量表示操作类型；isInOperation()/isOutOperation() 是判断方法。管理接口不删除历史操作记录，也不随柜改名更新历史名称。

## manufacturerSide 接口

Controller：`craftstudio-mes-interfaces/src/main/java/com/mes/interfaces/api/platform/manufacturerSide/storageTank/StorageTankController.java`。
配套应用服务：`AppStorageTankService`，请求对象：`StorageTankRequest` / `StorageTankListRequest`。

所有接口使用 `Authorization: Bearer <实际登录 token>`；制造商范围由 `AppLoginService.getManufacturerMetaIdByToken()` 解析。默认通用 Authorization 值无法建立制造商身份，本接口不接受其代替实际登录。请求体不需要 manufacturerId。跨制造商或不存在的主键均返回业务 404。

| 方法 | 路径 | 参数和行为 |
| --- | --- | --- |
| POST | /api/manufacturerSide/storageTank/list | current 默认 1，size 默认 20（1~100）；可按 storageTankName 字面子串、storageTankCode 精确值、status 精确值筛选。items 和 total 使用相同制造商范围及条件。 |
| GET | /api/manufacturerSide/storageTank/{id} | id 为数据库主键，返回完整柜与储位。 |
| POST | /api/manufacturerSide/storageTank/add | 柜基础信息及可选 totalSlots，返回含服务器 ID 及自动生成储位的柜。 |
| POST | /api/manufacturerSide/storageTank/edit | 请求体必须含数据库 id；更新基础信息并按可选 totalSlots 调整储位，返回编辑后的柜。 |
| DELETE | /api/manufacturerSide/storageTank/{id} | 软删除空柜及其独立储位；柜内/独立集合存在已占用储位、工件或正数量，或 currentCapacity > 0 时拒绝。 |

列表响应沿用 PagedApiResponse：`data.items/current/size/total`；其他沿用 ApiResponse：`code/message/data/timestamp`。删除成功 data 为 success；新增、编辑、详情成功 data 为 StorageTank。字段验证错误业务 code=400；不存在/无归属 code=404。

### 新增示例

```json
{
  "storageTankName": "一车间储存柜",
  "storageTankCode": "CAB-001",
  "totalSlots": 9
}
```

除柜名称和编码必填外，其余请求字段可省略。totalSlots 也可省略或传 null：新柜没有储位，三个储位计数均为 0。新柜默认状态 ACTIVE、movable=false；未提供的柜基础文本信息为 null，容量和环境全部为 null。

可选柜基础字段还有 storageTankType、location、status、movable、description、managerId、managerName。请求不再提供 storageSlots、maxCapacity、currentCapacity、capacityUnit、temperatureRange、humidityRange，也不接受储位重量、体积、温湿度明细。

编辑时添加返回的数据库 id，totalSlots 表示调整后的总储位数，例如从 9 改为 12 时追加 3 个默认空储位。编辑仍需提交名称和编码；其他普通文本字段省略会清空，status、movable 省略保留。totalSlots 省略/null 时保留原储位，0 表示移除所有空储位（存在有库存储位时拒绝）。容量与环境历史值保持不变。

## 同步、兼容性与运行要求

- 新增、编辑、删除均使用现有 MongoTransactionManager 和 @Transactional，柜文档和独立储位集合在同一事务提交/回滚。MongoDB 必须支持事务（副本集或分片集群）；不降级为部分成功的非事务写入。
- 编辑保留业务 ID 和创建时间，按数量增减储位后重新统计 totalSlots、usedSlots、remainingSlots。容量和环境暂不开放配置，不从工件件数自动换算重量/体积。
- 独立集合已有库存与柜内同 ID 储位的状态、工件或数量冲突时拒绝覆盖，要求先核对数据；不会静默丢失库存。
- 原 StorageTankPo 缺失 manufacturerId；此次仅修复后续持久化，不猜测历史归属。历史文档需要根据可信业务数据补齐 manufacturerId 后才会在制造商接口中可见。
- 旧 AppTransBoxService/StorageSlotService 的出入库写入路径尚未统一：部分只写柜内或只写独立储位，且旧 AppTransBoxService 某些流程把 quantity 累加到 currentCapacity。此次管理接口不重写这些既有流程；需要后续统一容量计量口径和储位写入路径。两个集合已冲突的数据应先核对。
- 不自动修改 StorageInventory 或历史 StorageOperationRecord；库存初始化/调整仍通过出入库/盘点流程处理。

## 可重试写入兼容处理

新增接口会执行储位插入和储存柜插入，MongoDB 驱动默认启用 retryable writes。若部署不支持该协议，会返回 `This MongoDB deployment does not support retryable writes`。该错误与 totalSlots 参数无关，也不能仅凭此错误判断事务是否受支持。

应用通过 `MongoWriteCompatibilityCustomizer` 在 Spring Boot 标准连接设置之后执行 `retryWrites(false)`，无需修改数据库部署、连接串或配置文件。此设置作用于 Spring Boot 自动配置的 MongoDB 客户端，因此同客户端的其他写入也不再使用驱动自动重试；地址、认证、读重试、写确认和事务逻辑不变。修改后需重新构建并重启应用生效。

此处理针对可重试写入协议，不会给数据库增加事务能力。现有多集合事务要求仍然适用，未对目标数据库执行真实新增验证。

## 验证

新增 AppStorageTankServiceTest：覆盖按数量自动生成默认储位、null/0/负数处理、扩容/缩容同步、库存与历史容量保留、制造商范围、分页条件、占用保护和集合冲突保护。
新增 StorageTankPoTest：覆盖 manufacturerId、容量、环境、嵌套储位库存的 PO 往返映射。

完整 reactor 测试命令：

```bash
mvn -pl craftstudio-mes-application,craftstudio-mes-interfaces -am \
  -Dtest=AppStorageTankServiceTest,StorageTankPoTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

当前环境缺失私有 com.piliofpala.craftstudio 依赖，Maven 在依赖解析阶段失败，尚未执行测试或完成全项目编译；需在可访问私有制品库的环境重跑。已使用真实源码和公共依赖单独编译并执行 StorageTankPoTest 的往返映射断言，通过。尚未对真实 MongoDB 执行事务集成验证。
