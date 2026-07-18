# Agent 核心组件开发计划

> **文档版本**: v1.1
> **创建日期**: 2026-07-18
> **参考依据**: 架构设计原则 v1.0 + 仓库管理员评测集（36题）+ 评测标准与评分规范
> **覆盖角色**: 仓库管理员（一期），其余6个角色（二期）
> **重要调整**: YAML配置化延后到第三期，前期直接实现评测集场景

---

## 一、总体架构概览

```
┌──────────────────────────────────────────────────────┐
│  Agent API                                            │
│  POST /api/v1/agent/chat → 自然语言 → 结构化回答      │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│               AgentService (主流程编排)                │
│                                                       │
│  用户输入                                              │
│    │                                                   │
│    ├─ 意图识别 (IntentRecognizer)                      │
│    │  ├─ KeywordMatcher      (简单+中等 20题)          │
│    │  ├─ SemanticMatcher     (复杂+模糊, LLM)          │
│    │  ├─ MultiIntentMatcher  (多意图拆解)              │
│    │  └─ ConversationContext (指代消解)                │
│    │                                                   │
│    ├─ 意图路由 (IntentRouter)                          │
│    │  └─ 按置信度分流到: LLM Planner / InfoCollector    │
│    │     / OptionPresenter / ConfirmationGate / Fallback│
│    │                                                   │
│    ├─ LLM Planner (计划生成 + 边界检查) ←── LLM Sonnet │
│    │  ├─ 权限/安全/能力边界判断                         │
│    │  └─ 生成 ExecutionPlan (含条件分支)                │
│    │                                                   │
│    ├─ Determinate Executor (确定性执行) ←── 不调LLM    │
│    │  ├─ 按计划遍历步骤、传参、判条件                   │
│    │  └─ 调用工具层获取数据                             │
│    │                                                   │
│    ├─ LLM Analyst (数据分析) ←── LLM Sonnet (仅复杂)   │
│    │  └─ 读工具返回数据 → 分析报告                      │
│    │                                                   │
│    └─ OutputFormatter (输出格式化)                     │
│       └─ 模板/LLM润色 → 最终回答                       │
│                                                       │
│  依赖模块:                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │ 工具调用  │  │ 记忆模块  │  │  知识/RAG        │   │
│  │ Module   │  │ Module   │  │   Module         │   │
│  └──────────┘  └──────────┘  └──────────────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │ 异常处理  │  │ 可观测性  │  │                  │   │
│  │ Module   │  │ Module   │  │                  │   │
│  └──────────┘  └──────────┘  └──────────────────┘   │
└──────────────────────┬───────────────────────────────┘
                       │ 调用
┌──────────────────────▼───────────────────────────────┐
│              工具层 (原子能力)                          │
│                                                       │
│  InventoryListTool │ ExpiryWarningTool │ ...          │
│  InboundListTool   │ OutboundDetailTool │ ...        │
│  (包装现有 Controller，零修改)                         │
└──────────────────────────────────────────────────────┘
```

### 评测集覆盖矩阵

| 难度 | 用例数 | 核心能力 | 响应目标 | 实现方式 |
|------|--------|---------|---------|---------|
| 简单 | 10题 | 意图识别 + 单工具调用 + 格式化输出 | < 2秒 | Java硬编码 + 关键词匹配 |
| 中等 | 10题 | 多工具编排 + 数据聚合 + 简单推理 | < 4秒 | Java硬编码 + 串行链 |
| 复杂 | 8题(3题完成) | 跨模块分析 + LLM推理 + 建议生成 | < 8秒 | LLM + 硬编码编排 |
| 模糊 | 8题 | 意图澄清 + 多轮对话 + 选项引导 | < 3秒(首轮) | LLM + 关键词 + 上下文 |

---

## 二、核心组件模块开发计划

---

### 模块 1：意图识别模块 (Intent Recognition)

**职责**: 将用户自然语言输入匹配到评测集场景

#### 评测用例覆盖

| 难度 | 覆盖用例 | 关键能力 |
|------|---------|---------|
| 简单 | WM_SIMPLE_001~010 | 精确关键词匹配（"查询库存列表" → 场景1） |
| 中等 | WM_MEDIUM_001~010 | 关键词匹配 + 实体参数提取（SKU、仓库名、时间） |
| 复杂 | WM_COMPLEX_001~003 | 关键词兜底 + LLM语义匹配 |
| 模糊 | WM_AMBIGUOUS_001~008 | 低置信度 → 选项引导 + 多意图拆解 + 指代消解 |

#### 代码结构

```
com.wms.agent.core.intent/
├── IntentRecognizer.java          # 统一入口
├── IntentResult.java              # 识别结果 (sceneId + confidence + params)
├── matcher/
│   ├── KeywordMatcher.java        # 关键词匹配（覆盖简单10题+中等10题）
│   ├── SemanticMatcher.java       # LLM语义匹配（覆盖复杂3题+模糊5题）
│   └── MultiIntentMatcher.java    # 多意图拆解（覆盖模糊#6）
├── context/
│   └── ConversationContext.java   # 指代消解（覆盖模糊#7~#8）
└── rule/
    └── SceneKeywordRule.java       # 关键词匹配规则（硬编码配置）
```

#### 关键接口

```java
public class IntentRecognizer {
    /**
     * 识别用户意图
     *
     * @param userId  用户ID（角色过滤）
     * @param input   用户输入
     * @param history 对话历史（多轮上下文）
     * @return 意图识别结果列表（支持多意图）
     */
    public List<IntentResult> recognize(Long userId, String input, List<Message> history);
}
```

#### 匹配流程

```
用户输入
  ├─ KeywordMatcher → ≥0.85 → 直接执行 (简单10题 + 中等10题全部命中)
  ├─ KeywordMatcher → 0.50~0.85 → 执行+确认
  ├─ KeywordMatcher → 0.20~0.50 → SemanticMatcher(LLM) → 0.50~0.85 → 执行+确认
  │                                                       0.20~0.50 → 返回选项
  ├─ MultiIntentMatcher → 拆解为多个场景 (模糊#6)
  ├─ ConversationContext → 指代消解 (模糊#7~#8)
  └─ 全不匹配 → 兜底回答
```

> **完整设计见**: [意图识别模块设计_v1.md](意图识别模块设计_v1.md)

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 1.1 | KeywordMatcher + 全量场景规则配置 | 无 | 3h | 简单10题全部命中 ≥0.85 |
| 1.2 | 参数提取（SKU/仓库/时间/单号） | 无 | 2h | 中等题参数正确 |
| 1.3 | ConversationContext（指代消解） | 记忆模块 | 2h | 模糊#7~#8 |
| 1.4 | SemanticMatcher（LLM语义） | LLM模块 | 3h | 复杂3题 + 模糊#1~#5 |
| 1.5 | MultiIntentMatcher | 无 | 1h | 模糊#6 |

---

### 模块 2：工具调用模块 (Tool Calling)

**职责**: 封装现有系统API为 AgentTool，统一执行

#### 评测用例覆盖

| 难度 | 覆盖用例 | 关键能力 |
|------|---------|---------|
| 简单 | WM_SIMPLE_001~010 | 单工具调用 + 参数传递 |
| 中等 | WM_MEDIUM_001~010 | 多工具串行（2-4个API） |
| 复杂 | WM_COMPLEX_001~003 | 多工具并行（6-10+ API） |

#### 代码结构

```
com.wms.agent.tool/
├── AgentTool.java                 # 工具接口
├── ToolRegistry.java              # 工具注册中心
├── ToolExecutor.java              # 工具执行引擎
├── ToolResult.java                # 统一执行结果
├── execution/
│   ├── SequentialExecutor.java    # 串行执行（前依赖后）
│   └── ParallelExecutor.java      # 并行执行（独立工具）
└── tools/                         # 具体工具（包装Controller）
    ├── InventoryListTool.java
    ├── InventoryDetailTool.java
    ├── InventorySummaryTool.java
    ├── ExpiryWarningTool.java
    ├── InboundOrderListTool.java
    ├── InboundProgressTool.java
    ├── OutboundOrderListTool.java
    ├── OutboundDetailTool.java
    ├── PickTaskTool.java
    ├── PickRecordTool.java
    ├── StocktakeListTool.java
    ├── StocktakeDetailTool.java
    ├── StocktakeItemsTool.java
    ├── ExceptionListTool.java
    ├── ExceptionStatTool.java
    ├── ReturnOrderListTool.java
    ├── WarehouseInfoTool.java
    ├── ZoneListTool.java
    ├── LocationListTool.java
    ├── ProductQueryTool.java
    ├── SupplierQueryTool.java
    └── SupplierProductTool.java
```

#### 关键接口

```java
public interface AgentTool {
    String getName();                    // 工具唯一标识
    String getDescription();             // 描述
    ToolResult execute(Map<String, Object> params);  // 执行
}

public class ToolExecutor {
    /** 单工具执行 */
    ToolResult execute(AgentTool tool, Map<String, Object> params);

    /** 并行执行（独立工具） */
    Map<String, ToolResult> executeParallel(List<AgentTool> tools, Map<String, Map<String, Object>> params);

    /** 串行链执行（前一个结果传给后一个） */
    List<ToolResult> executeChain(List<AgentTool> tools, Map<String, Object> initialParams);
}
```

#### AgentTool 示例

```java
@Component
public class InventoryListTool implements AgentTool {

    private final InventoryController inventoryController;

    @Override
    public String getName() { return "inventory_list"; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        InventoryQueryDTO query = new InventoryQueryDTO();
        if (params.containsKey("skuCode")) query.setSkuCode((String) params.get("skuCode"));
        if (params.containsKey("warehouseId")) query.setWarehouseId((Long) params.get("warehouseId"));
        // 默认分页
        query.setPage(1); query.setLimit(20);

        Result<Map<String, Object>> result = inventoryController.queryInventory(query);
        return ToolResult.success(result.getData());
    }
}
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 2.1 | AgentTool接口 + ToolRegistry | 无 | 1h | 工具注册 |
| 2.2 | 核心工具15个（覆盖简单10题+中等部分） | 现有Controller | 6h | 简单10题全部通过 |
| 2.3 | SequentialExecutor（串行链） | ToolExecutor | 2h | 中等题多工具组合 |
| 2.4 | ParallelExecutor（并行组） | ToolExecutor | 2h | 复杂题多工具并行 |
| 2.5 | 辅助工具7个（覆盖中等/复杂全部） | 现有Controller | 3h | 中等+复杂全部通过 |

---

### 模块 3：场景规划与执行模块 (Scenario Planning & Execution)

**职责**: 将意图识别结果 → 动态生成执行计划 → 确定性执行
**设计思想**: LLM Planner 出计划 + Determinate Executor 跑计划，分离推理和执行

#### 评测用例覆盖

| 难度 | 覆盖场景数 | 说明 |
|------|-----------|------|
| 简单 | 10个场景 | 线性计划（1个工具 + 固定参数） |
| 中等 | 10个场景 | 串行/并行计划（2-4个工具 + 参数依赖） |
| 复杂 | 3个场景 | 条件分支计划（5-10个工具 + 条件 + LLM分析） |
| 模糊 | 8个场景 | 特殊处理：意图澄清 + 多意图 + 指代消解 |

#### 架构组成

场景规划与执行由三个组件协作完成：

```
LLM Planner (出计划)
  │ 收到 sceneId + params + userRole
  │ 步骤1: 边界安全检查（权限/数据敏感/能力范围）
  │ 步骤2: 生成 ExecutionPlan（工具步骤 + 条件分支 + 参数映射）
  │ 输出: ExecutionPlan（结构化JSON）
  │
  ▼
Determinate Executor (跑计划)
  │ 遍历 ExecutionPlan.steps，按步骤执行:
  │ ├─ 串行: 前序结果 → 参数映射 → 调工具 → 存结果
  │ ├─ 并行: 同时调多个独立工具 → 等待全部完成
  │ ├─ 条件: 计算 condition (true/false) → 跳过或继续
  │ └─ 可选: optional=true → 失败时跳过不影响主流程
  │ 输出: ScenarioResult（原始工具数据）
  │
  ▼
LLM Analyst (分析数据, 仅 needsLLM=true)
  │ 读 ScenarioResult → 深度分析 → 分析报告
  │ 适用: 运营诊断、缺货风险评估、拣货路径优化
```

#### 各难度场景的典型计划形态

| 场景难度 | 计划复杂度 | 工具数 | 典型结构 |
|---------|-----------|--------|---------|
| 简单 | 线性 | 1个 | 单步：调一个工具，直接返回 |
| 中等 | 链式+并行 | 2-4个 | 串行依赖 + 并行组 |
| 复杂 | 条件分支 | 5-10个 | 条件判断 + 并行组 + 后处理 + LLM分析 |

**简单场景计划示例**（LLM Planner 输出）：

```
WM_SIMPLE_001 - 查询库存列表

executionPlan:
  steps:
    - tool: InventoryListTool
      params: { page: 1, limit: 20 }
  outputFormat: TABLE
```

**复杂场景计划示例**：

```
WM_COMPLEX_002 - 缺货风险评估

executionPlan:
  steps:
    - tool: InventoryListTool
      params: { pageSize: 200 }
      outputKey: allInventory

    - tool: OutboundOrderTool
      params: { days: 7, status: "COMPLETED" }
      dependsOn: [allInventory]        # 用上一步的SKU列表
      outputKey: consumption

    - tool: SupplierProductTool
      params: { productIds: "${consumption.highRiskSkus}" }
      condition: "${consumption.hasHighRisk == true}"  # 条件分支
      outputKey: supplierInfo
      optional: false

  parallel:
    - tool: WarehouseSummaryTool
      outputKey: crossWarehouse
      optional: true                   # 非必需

  postProcess: computeRiskLevel        # 后处理计算

  llmAnalysis: true                    # 需要LLM Analyst读数据出报告
```

#### 代码结构

```
com.wms.agent.core.planner/
├── LLMPlanner.java                # LLM计划器（边界检查 + 出计划）
├── ExecutionPlan.java             # 执行计划（步骤列表 + 条件 + 依赖）
├── ExecutionStep.java             # 单步定义
├── ConditionEvaluator.java        # 条件求值器（true/false，不调LLM）

com.wms.agent.core.execution/
├── DeterminateExecutor.java       # 确定性执行器（遍历步骤、调工具、传参）
├── SequentialHandler.java         # 串行处理
├── ParallelHandler.java           # 并行处理
├── PlanContext.java               # 执行上下文（步骤间数据传递）
├── StepResult.java                # 单步执行结果
└── ScenarioResult.java            # 整体执行结果
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 3.1 | ExecutionPlan + ExecutionStep 模型 | 无 | 1h | 计划结构正确 |
| 3.2 | DeterminateExecutor（串行+并行） | 工具模块 | 3h | 简单10题全部通过 |
| 3.3 | ConditionEvaluator（条件判断） | 无 | 1h | 复杂题条件分支 |
| 3.4 | LLMPlanner（边界检查 + 出计划） | LLM模块 | 4h | 复杂3题 |
| 3.5 | PlanContext（步骤间数据传递） | 无 | 1h | 中等题参数传递 |
| 3.6 | AmbiguityHandler + 模糊场景 | 意图模块 | 3h | 模糊8题全部通过 |

---

### 模块 4：知识/RAG 模块 (Knowledge Retrieval)

**职责**: 检索业务规则，为LLM推理提供知识上下文

#### 评测用例覆盖

| 难度 | 覆盖用例 | 关键能力 |
|------|---------|---------|
| 中等 | WM_MEDIUM_006 | 盘点差异原因分析（知识规则） |
| 复杂 | WM_COMPLEX_001 | 仓库运营瓶颈分析（诊断标准） |
| 复杂 | WM_COMPLEX_002 | 缺货风险评估（风险分级规则） |
| 复杂 | WM_COMPLEX_003 | 拣货路径优化（库位布局规则） |

#### 代码结构

```
com.wms.agent.knowledge/
├── KnowledgeRetriever.java        # 知识检索入口
├── KnowledgeResult.java           # 检索结果
├── ContextAssembler.java          # 上下文组装（知识+工具数据→LLM Prompt）
├── rules/                         # 硬编码业务规则（当前阶段）
│   ├── StockoutRiskRule.java      # 缺货风险分级规则
│   ├── EfficiencyRule.java        # 效率评估标准
│   └── PickPathRule.java          # 拣货路径规则
└── prompt/
    └── PromptTemplates.java       # 各场景Prompt模板（硬编码字符串）
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 4.1 | 业务规则硬编码（3个场景） | 无 | 2h | 复杂#2 缺货风险分级 |
| 4.2 | ContextAssembler | LLM模块 | 2h | Prompt组装 |
| 4.3 | PromptTemplates | 无 | 2h | 各场景模板 |

---

### 模块 5：记忆模块 (Memory)

**职责**: 管理多轮对话上下文、工具结果缓存、会话状态

#### 评测用例覆盖

| 难度 | 覆盖用例 | 关键能力 |
|------|---------|---------|
| 模糊 | WM_AMBIGUOUS_007 | 指代消解（"那个产品" → 上文产品） |
| 模糊 | WM_AMBIGUOUS_008 | 上下文关联（"处理一下" → 上文异常单） |
| 全部 | 所有 | 工具结果缓存（避免重复查询） |

#### 代码结构

```
com.wms.agent.memory/
├── MemoryManager.java             # 记忆管理器
├── SessionContext.java            # 会话上下文（当前对话）
├── WorkingMemory.java             # 工作记忆（工具结果缓存）
├── ConversationHistory.java       # 对话历史
├── Message.java                   # 消息结构
└── store/
    └── InMemoryStore.java         # 内存存储
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 5.1 | Message + ConversationHistory | 无 | 1h | 对话记录 |
| 5.2 | SessionContext + MemoryManager | 无 | 1h | 会话管理 |
| 5.3 | WorkingMemory（结果缓存） | 工具模块 | 1h | 重复查询走缓存 |

---

### 模块 6：LLM 推理模块 (LLM Reasoning)

**职责**: 提供三个独立的 LLM 能力：语义匹配、计划生成、数据分析

#### 评测用例覆盖

| 难度 | 覆盖用例 | 关键能力 |
|------|---------|---------|
| 复杂 | WM_COMPLEX_001~003 | 深度推理（瓶颈分析 + 方案生成 + 预测） |
| 复杂 | WM_COMPLEX_001~003 | LLM Planner（条件分支计划生成） |
| 复杂 | WM_COMPLEX_001~003 | LLM Analyst（数据读取 + 分析报告） |
| 模糊 | WM_AMBIGUOUS_001~005 | SemanticMatcher（意图澄清 + 选项生成） |

#### LLM 的三个角色

```
┌─────────────────────────────────────────────────────────┐
│                     LLM 服务                              │
│                                                          │
│  角色1: SemanticMatcher (意图识别阶段)                    │
│  ├─ 模型: Haiku 4.5                                     │
│  ├─ 输入: 用户原始输入                                   │
│  └─ 输出: sceneId + confidence + params                  │
│                                                          │
│  角色2: LLM Planner (路由后、执行前)                      │
│  ├─ 模型: Sonnet 4.6                                    │
│  ├─ 输入: sceneId + params + userRole + 可用工具清单      │
│  ├─ 步骤1: 边界安全检查（权限/敏感/能力范围）              │
│  ├─ 步骤2: 生成 ExecutionPlan（步骤+条件+参数映射）       │
│  └─ 输出: ExecutionPlan 或 拒绝消息                       │
│                                                          │
│  角色3: LLM Analyst (执行后、输出前)                      │
│  ├─ 模型: Sonnet 4.6                                    │
│  ├─ 输入: ScenarioResult（原始工具数据）                   │
│  ├─ 仅复杂场景走此路径                                    │
│  └─ 输出: 分析报告（瓶颈原因、改进方案、风险分级）          │
└─────────────────────────────────────────────────────────┘
```

#### 代码结构

```
com.wms.agent.core.llm/
├── LLMService.java                # LLM调用入口（统一管理API调用）
├── SemanticMatcher.java           # 角色1: 语义匹配
├── LLMPlanner.java                # 角色2: 计划生成 + 边界检查
├── LLMAnalyst.java                # 角色3: 数据分析
├── planner/
│   └── ExecutionPlan.java         # 执行计划模型
├── PromptBuilder.java             # Prompt构建器（按角色组装不同Prompt）
├── ResponseParser.java            # 响应解析器
├── FallbackEngine.java            # 降级引擎（LLM不可用→规则引擎）
└── ModelConfig.java               # 模型配置
```

#### 模型选择

| LLM角色 | 场景难度 | 模型 | temperature | maxTokens | 原因 |
|---------|---------|------|------------|-----------|------|
| SemanticMatcher | 复杂/模糊 | Haiku 4.5 | 0.2 | 500 | 轻量分类任务 |
| LLM Planner | 简单/中等/复杂 | Sonnet 4.6 | 0.3 | 1500 | 需要推理边界+出计划 |
| LLM Analyst | 仅复杂 | Sonnet 4.6 | 0.3 | 2000 | 深度数据分析 |

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 6.1 | LLMService（基础调用） | 无 | 2h | API调用正常 |
| 6.2 | SemanticMatcher（意图识别） | 意图模块 | 2h | 复杂+模糊意图识别 |
| 6.3 | LLMPlanner（边界检查+出计划） | LLMService | 4h | 复杂3题计划正确 |
| 6.4 | LLMAnalyst（数据分析） | LLMService | 2h | 运营诊断+缺货报告 |
| 6.5 | PromptBuilder | 无 | 1h | 各角色Prompt |
| 6.6 | ResponseParser + FallbackEngine | 无 | 2h | LLM降级 |

---

### 模块 7：可观测性/日志模块 (Observability)

**职责**: 记录每次对话全过程，支持问题排查

#### 评测用例覆盖

| 难度 | 覆盖用例 | 关键能力 |
|------|---------|---------|
| 全部 | 所有36题 | 每次对话生成JSON日志 |
| 自评测 | — | 日志可回放、可评分 |

#### 代码结构

```
com.wms.agent.observability/
├── TraceContext.java              # 追踪上下文
├── ConversationLog.java           # 对话日志
├── ConversationStep.java          # 单步记录
├── ConversationLogger.java        # 日志写入器
├── TraceIndexService.java         # DB索引服务
└── model/
    ├── StepType.java
    └── StepStatus.java
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 7.1 | TraceContext + ConversationStep | 无 | 1h | 基础追踪 |
| 7.2 | ConversationLogger（JSON写入） | 文件系统 | 1h | 日志落盘 |
| 7.3 | TraceIndexService（DB索引） | DB表 | 2h | 日志搜索 |
| 7.4 | 与场景编排集成 | 场景模块 | 2h | 全链路追踪 |

---

### 模块 8：异常处理模块 (Exception Handling)

**职责**: 分级处理异常，提供优雅降级

#### 代码结构

```
com.wms.agent.core.exception/
├── AgentExceptionHandler.java     # 异常处理器
├── AgentException.java            # 异常基类
├── RetryStrategy.java             # 重试策略
├── SkipStrategy.java              # 跳过策略
└── DegradeStrategy.java           # 降级策略
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 8.1 | 异常类型 + RetryStrategy | 工具模块 | 1h | 工具重试 |
| 8.2 | SkipStrategy + DegradeStrategy | 场景模块 | 1h | 优雅降级 |

---

### 模块 9：输出格式化模块 (Output Formatting)

**职责**: 根据评测集期望输出格式，生成结构化、美观的回答

#### 代码结构

```
com.wms.agent.core.output/
├── OutputFormatter.java           # 格式化入口
├── FormattedResult.java           # 格式化结果
├── section/
│   ├── TextSection.java
│   ├── TableSection.java
│   ├── ListSection.java
│   └── SuggestionSection.java
└── enhancement/
    ├── EmojiHelper.java
    └── HighlightHelper.java
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 9.1 | TextSection + ListSection | 无 | 1h | 简单题输出 |
| 9.2 | TableSection | 无 | 1h | 表格数据 |
| 9.3 | EmojiHelper + HighlightHelper | 无 | 1h | 格式美化 |
| 9.4 | SuggestionSection | 无 | 1h | 建议段落 |

---

### 模块 10：Agent 入口模块 (Agent API)

**职责**: 对外提供 REST API，编排完整流程

#### API 设计

```
POST /api/v1/agent/chat              # 聊天（同步）
GET  /api/v1/agent/sessions/{id}     # 会话详情
DELETE /api/v1/agent/sessions/{id}   # 删除会话
```

#### AgentService 主流程

```java
@Service
public class AgentService {

    public ChatResponse chat(Long userId, String input, String sessionId) {
        // 1. 可观测性 → 开始追踪
        TraceContext trace = TraceContext.begin(userId, input);

        // 2. 记忆 → 加载会话上下文
        SessionContext session = memoryManager.loadSession(sessionId);

        // 3. 意图识别 → 匹配场景
        List<IntentResult> intents = intentRecognizer.recognize(userId, input, session.getHistory());

        // 4. 意图路由 → 按置信度分流
        RouteAction action = intentRouter.route(intents);
        switch (action.getType()) {
            case CLARIFY:          // 低置信度 → 返回选项
                return buildClarificationResponse(action.getOptions());
            case CONFIRM:          // 中等置信度 → 确认后继续
                return buildConfirmResponse(action.getIntent());
            case INFO_COLLECT:     // 缺参数 → 反问
                return buildInfoRequest(action.getMissingParams());
            case FALLBACK:         // 完全不匹配 → 兜底
                return buildFallbackResponse();
            case EXECUTE:          // 高置信度 → 进入执行流程
                break;  // 继续往下
        }

        IntentResult intent = intents.get(0);

        // 5. LLM Planner → 边界检查 + 出执行计划
        ExecutionPlan plan = llmPlanner.plan(intent.getSceneId(), intent.getParams(), userId);
        if (plan.isRejected()) {
            return ChatResponse.rejected(plan.getRejectReason());  // 越界
        }

        // 6. Determinate Executor → 按计划执行工具链
        ScenarioResult data = determinateExecutor.execute(plan);

        // 7. LLM Analyst → 数据分析（仅复杂场景需要）
        String analysis = null;
        if (plan.needsAnalysis()) {
            analysis = llmAnalyst.analyze(data, intent.getSceneId());
        }

        // 8. 输出格式化
        String output = outputFormatter.format(data, analysis, intent.getSceneId());

        // 9. 记录日志
        trace.end(output);
        conversationLogger.save(trace);

        return ChatResponse.success(output);
    }
}
```

#### 实现顺序

| 步骤 | 内容 | 依赖 | 预计工时 | 评测验证 |
|------|------|------|---------|---------|
| 10.1 | AgentController + ChatRequest/Response | 无 | 1h | 入口API |
| 10.2 | AgentService 主流程 | 全部模块 | 4h | 端到端流程 |

---

## 三、分期规划

### 第一期：评测集场景直接实现（MVP — 可评测）

**核心思想**: 不做什么 YAML 引擎，直接 Java 硬编码 21 个评测集场景
**目标**: 跑通仓库管理员全部 36 题

| Sprint | 模块 | 交付物 | 验证标准 |
|--------|------|--------|---------|
| Sprint 1 (1周) | 工具层 | ToolRegistry + 15个核心工具 | 简单10题正确调用API |
| Sprint 2 (1周) | 简单10场景实现 | 10个Scenario类 + 关键词匹配 | 简单10题全部通过 |
| Sprint 3 (1周) | 中等10场景 + 格式化 | 10个Scenario类 + 输出格式化 | 中等10题全部通过 |
| Sprint 4 (1周) | 复杂3题 + 模糊8题 + 日志 | 3个复杂场景 + LLM + 意图 + 可观测性 | 复杂3题 + 模糊8题通过 |

**交付标准**:
- [ ] 简单10题：通过率 ≥ 95%，平均响应 < 2秒
- [ ] 中等10题：通过率 ≥ 85%，平均响应 < 4秒
- [ ] 复杂3题：通过率 ≥ 75%，平均响应 < 8秒
- [ ] 模糊8题：通过率 ≥ 65%，首轮响应 < 3秒
- [ ] 每次对话生成完整JSON日志

---

### 第二期：多角色扩展

| Sprint | 模块 | 交付物 |
|--------|------|--------|
| Sprint 5~8 (4周) | 其余6角色场景实现 | 入库/出库/质检/库存/物流/管理 各20题 |

**交付标准**:
- [ ] 每角色20题全部通过
- [ ] 整体评测通过率 ≥ 80%

---

### 第三期：YAML 配置化 + 场景管理后台

| Sprint | 模块 | 交付物 |
|--------|------|--------|
| Sprint 9 (1周) | YAML 配置模型定义 | ScenarioConfig/ToolConfig/OutputConfig |
| Sprint 10 (1周) | YAML 加载+验证 | ConfigLoader/ConfigValidator/HotReload |
| Sprint 11 (1周) | 场景配置外移 | 将现有21个硬编码场景迁移到YAML |
| Sprint 12 (1周) | 场景管理后台 | Web界面 → 表单生成YAML |

**为什么要第三期做YAML**:
```
第一期: 21个场景硬编码 → 验证架构正确、评测通过
第二期: 验证可扩展性（7角色×20题→560题）
第三期: YAML配置化 → 消除重复代码、实现零代码

如果一开始就做YAML:
  风险1: 架构没验证，YAML格式可能设计错误
  风险2: YAML引擎的Bug影响评测结果
  风险3: 额外工作量推迟了真正重要的评测集实现
```

---

## 四、第一期详细任务拆解（Sprint 1~4）

### Sprint 1: 工具注册 + 核心工具（7天）

```
day 1-2: AgentTool接口 + ToolRegistry
  ├── 定义 AgentTool 接口 (getName, getDescription, execute)
  ├── 实现 ToolRegistry (Spring启动时自动扫描注册)
  └── 实现 ToolResult (统一返回格式)

day 3-4: 核心工具 10个（覆盖简单10题）
  ├── InventoryListTool     → /api/v1/inventory/list
  ├── InventoryDetailTool   → /api/v1/inventory/detail/{productId}
  ├── InventorySummaryTool  → /api/v1/inventory/summary
  ├── ExpiryWarningTool     → /api/v1/inventory/expiry-warning
  ├── InboundOrderListTool  → /api/v1/inbound/orders
  ├── OutboundOrderListTool → /api/v1/outbound/orders
  ├── OutboundDetailTool    → /api/v1/outbound/orders/{id}
  ├── StocktakeListTool     → /api/v1/stocktake/list
  ├── ExceptionListTool     → /api/v1/exception
  └── ReturnOrderListTool   → /api/v1/return/orders

day 5-7: 辅助工具 5个（覆盖中等题依赖）
  ├── InboundProgressTool   → /api/v1/inbound/progress/by-batch
  ├── ProductQueryTool      → /api/v1/system/products
  ├── WarehouseInfoTool     → /api/v1/system/warehouses
  ├── ZoneListTool          → /api/v1/system/zones/warehouse/{id}
  └── LocationListTool      → /api/v1/system/locations

  验证: 每个工具独立测试，Mock Controller 返回 → 结果正确
```

### Sprint 2: 意图识别 + 执行引擎基础（7天）

```
day 1-3: 意图识别（关键词匹配）
  ├── 实现 KeywordMatcher
  │   ├── 每个场景维护 keywords + excludeKeywords + priority
  │   └── 置信度计算: 命中数 × 权重 + 独占场景加分
  ├── 实现 IntentResult (sceneId + confidence + params)
  ├── 配置 简单10题 + 中等10题 的关键词规则
  └── 实现 IntentRouter（按置信度分流）
      验证: 每个场景输入 → 正确的sceneId + ≥0.85置信度

day 3-5: 执行引擎基础
  ├── 实现 ExecutionPlan / ExecutionStep 模型
  ├── 实现 DeterminateExecutor（串行执行）
  │   ├── 遍历步骤 → 调工具 → 传参数 → 存结果
  │   └── 支持简单线性计划（1个工具）
  ├── 实现 PlanContext（步骤间数据传递）
  └── 实现 ConditionEvaluator（条件求值）
      验证: 简单10题 → 执行计划正确、工具调用正确

day 5-7: 简单10题端到端
  ├── LLM Planner 集成（简单场景计划生成）
  │   ├── 输入 sceneId + params → 输出线性 ExecutionPlan
  │   └── 先不做边界检查（简单场景无需）
  ├── 实现 OutputFormatter 基础（列表+表格渲染）
  └── 集成测试: 简单10题全部通过
      验证: 意图识别 → 路由 → LLM Planner → 执行 → 格式化
```

### Sprint 3: 中等10题 + 输出格式化 + 并行执行（7天）

```
day 1-3: 执行引擎增强 + 中等10题
  ├── DeterminateExecutor 增强: 串行链（前序结果→参数映射）
  ├── 实现 ParallelHandler（并行组，同时调多个工具）
  ├── LLM Planner 增强: 生成串行+并行计划
  │   └── 中等场景: 输出含 dependsOn + parallel 的计划
  └── 集成测试: 中等10题全部通过

day 4-5: 输出格式化通用组件
  ├── OutputFormatter: 格式化入口
  ├── TableSection: 表格渲染 ═══ ═══
  ├── ListSection: 列表渲染 1. xxx
  ├── EmojiHelper: Emoji自动匹配
  └── HighlightHelper: ⚠️ 💡 等标注

day 6-7: 中等题调优
  ├── 数据聚合逻辑验证
  ├── 输出格式与评测期望逐项对比
  └── 全部20题回归测试
```

### Sprint 4: 复杂3题 + 模糊8题 + 可观测性（7天）

```
day 1-2: LLM 三角色集成
  ├── 实现 LLMService (调用外部LLM API)
  ├── 实现 LLMPlanner（边界检查 + 条件分支计划生成）
  │   ├── 边界安全检查: 权限/数据敏感/能力范围
  │   └── 生成条件分支 ExecutionPlan
  ├── 实现 LLMAnalyst（工具数据 → 分析报告）
  ├── 实现 PromptBuilder（按角色组装不同 Prompt）
  └── 实现 FallbackEngine（LLM不可用→规则引擎）

day 2-4: 复杂3题端到端
  ├── ConditionEvaluator 增强: 支持条件表达式求值
  ├── LLM Planner 生成复杂计划（条件分支 + 并行组）
  ├── DeterminateExecutor 执行条件分支计划
  │   └── condition=true → 继续 / false → 跳过
  └── LLMAnalyst 分析工具返回数据 → 运营诊断/缺货风险报告

day 4-5: 模糊8题实现
  ├── SemanticMatcher (LLM语义匹配)
  ├── MultiIntentMatcher (多意图拆解)
  ├── ConversationContext (指代消解)
  ├── AmbiguityHandler (模糊意图→选项引导)
  └── ClarificationBuilder (生成5~6个引导选项)

day 5-6: 可观测性
  ├── TraceContext (一次对话一个traceId)
  ├── ConversationStep (每一步记录输入+输出+耗时)
  ├── ConversationLogger (写入JSON文件)
  └── TraceIndexService (DB索引，便于搜索)

day 7: 集成测试 + 评测
  ├── 全部36题端到端评测
  ├── 日志完整性检查
  └── 修复问题
```

---

## 五、代码包结构总览

```
server/src/main/java/com/wms/
├── agent/                          # Agent 模块（新增，与现有代码完全隔离）
│   ├── AgentApplication.java       # Agent自动配置入口
│   │
│   ├── api/
│   │   ├── AgentController.java    # POST /api/v1/agent/chat
│   │   ├── AgentService.java       # 主流程编排
│   │   └── dto/
│   │       ├── ChatRequest.java
│   │       └── ChatResponse.java
│   │
│   ├── core/
│   │   ├── intent/                 # 意图识别
│   │   │   ├── IntentRecognizer.java
│   │   │   ├── IntentResult.java
│   │   │   ├── matcher/
│   │   │   │   ├── KeywordMatcher.java
│   │   │   │   ├── SemanticMatcher.java
│   │   │   │   └── MultiIntentMatcher.java
│   │   │   ├── context/
│   │   │   │   └── ConversationContext.java
│   │   │   └── rule/
│   │   │       └── SceneKeywordRule.java
│   │   │
│   │   ├── scenario/               # 场景实现
│   │   │   ├── Scenario.java       # 场景接口
│   │   │   ├── ScenarioDirector.java
│   │   │   ├── ExecutionPlan.java
│   │   │   ├── ExecutionStep.java
│   │   │   ├── StepResult.java
│   │   │   ├── ScenarioResult.java
│   │   │   ├── impl/
│   │   │   │   ├── simple/         # 10个简单场景
│   │   │   │   ├── medium/         # 10个中等场景
│   │   │   │   └── complex/        # 3个复杂场景
│   │   │   └── ambiguous/          # 模糊场景处理器
│   │   │
│   │   ├── execution/
│   │   │   ├── SequentialExecutor.java
│   │   │   └── ParallelExecutor.java
│   │   │
│   │   ├── llm/
│   │   │   ├── LLMService.java
│   │   │   ├── PromptBuilder.java
│   │   │   ├── ResponseParser.java
│   │   │   └── FallbackEngine.java
│   │   │
│   │   ├── output/
│   │   │   ├── OutputFormatter.java
│   │   │   ├── FormattedResult.java
│   │   │   ├── section/
│   │   │   └── enhancement/
│   │   │
│   │   └── exception/
│   │       ├── AgentExceptionHandler.java
│   │       ├── AgentException.java
│   │       ├── RetryStrategy.java
│   │       ├── SkipStrategy.java
│   │       └── DegradeStrategy.java
│   │
│   ├── tool/                       # 工具层
│   │   ├── AgentTool.java
│   │   ├── ToolRegistry.java
│   │   ├── ToolExecutor.java
│   │   ├── ToolResult.java
│   │   └── tools/                  # 23个工具实现
│   │       ├── InventoryListTool.java
│   │       ├── ...
│   │       └── SupplierProductTool.java
│   │
│   ├── knowledge/                  # 知识/RAG
│   │   ├── KnowledgeRetriever.java
│   │   ├── ContextAssembler.java
│   │   └── rules/
│   │       ├── StockoutRiskRule.java
│   │       ├── EfficiencyRule.java
│   │       └── PickPathRule.java
│   │
│   ├── memory/                     # 记忆
│   │   ├── MemoryManager.java
│   │   ├── SessionContext.java
│   │   ├── WorkingMemory.java
│   │   ├── ConversationHistory.java
│   │   └── Message.java
│   │
│   └── observability/              # 可观测性
│       ├── TraceContext.java
│       ├── ConversationLog.java
│       ├── ConversationStep.java
│       ├── ConversationLogger.java
│       └── TraceIndexService.java
│
├── system/         # 现有系统代码（完全不变）
├── inbound/        # 现有业务模块（完全不变）
├── outbound/       # 现有业务模块（完全不变）
├── inventory/      # 现有业务模块（完全不变）
├── stocktake/      # 现有业务模块（完全不变）
├── returnorder/    # 现有业务模块（完全不变）
└── exception/      # 现有业务模块（完全不变）
```

---

## 六、与现有系统的关系

### 集成点

| 现有模块 | Agent如何使用 | 需要修改 |
|---------|-------------|---------|
| 所有 Controller | 通过 AgentTool 包装调用 | **无需修改** |
| `Result<T>` | Agent 统一使用 | **无需修改** |
| `SecurityConfig` | Agent API 加入白名单 | 加一行配置 |
| `application.yml` | 添加 Agent 配置项 | 新增配置段 |

### 核心约束

```
✅ 现有 Java 代码一行不改
✅ Agent 通过包装 Controller 调用，保持安全链
✅ Agent 代码写在 com.wms.agent 包下，物理隔离
✅ 只新增 agent_trace_index 一张表（用于日志搜索）
```

---

## 七、关键决策记录

### ADR-AGENT-001: 优先硬编码场景，延后YAML

**决策**: 第一期直接 Java 实现 21 个评测集场景，YAML 配置化延后到第三期

**理由**:
- 评测集是确定的（36题），不存在灵活扩展的需求
- 硬编码开发快、易于调试、类型安全
- YAML 配置化需要额外的加载/验证/热更新工作量
- 先跑通评测验证架构，再抽象成配置

**代价**: 后期需要将 21 个场景从 Java 迁移到 YAML（预期 2 天内完成）

### ADR-AGENT-005: LLM Planner + Determinate Executor 分离

**决策**: LLM 只负责出计划和边界判断，不参与工具执行。执行由无 LLM 的 Determinate Executor 完成。

**理由**:
- 工具链含条件分支时（如"库存不足才查供应商"），需要 LLM 规划
- 但执行过程需要确定性（参数对了没、条件真不真），LLM 参与执行会产生幻觉和延迟
- 分离后：LLM Planner 可以灵活调整计划，Determinate Executor 保证执行可靠

**代价**: 每次请求多一次 LLM 调用（出计划），但简单场景的计划生成很快（<500ms），并且避免了 LLM 在执行环节的不可控风险。

### ADR-AGENT-002: AgentTool 包装 Controller

**决策**: AgentTool 调用 Controller 方法，而非直接调用 Service

**理由**: Controller 已封装参数校验、权限、日志，保持安全链完整

### ADR-AGENT-003: 简单场景无需LLM

**决策**: 简单10题 + 中等10题 不走LLM，纯关键词匹配 + 格式化

**理由**:
- 评测标准不考核LLM推理
- 响应更快（< 2秒），零成本

### ADR-AGENT-004: 可观测性用 JSON 文件

**决策**: 对话日志写 JSON 文件，DB 仅存搜索索引

**理由**: 自包含、可回放、不影响数据库性能、便于归档

---

## 八、风险与应对

| 风险 | 概率 | 影响 | 应对 |
|------|------|------|------|
| LLM不稳定 | 中 | 复杂/模糊题不可用 | FallbackEngine + 重试 |
| 存储过多日志 | 低 | 磁盘不足 | 定时清理（保留30天） |
| 角色扩展难度大 | 低 | 二期延期 | 角色间差异不大，主要为场景内容不同 |
| 评测用例覆盖不全 | 低 | 遗漏场景 | 一期只做仓库管理员36题 |

---

## 九、评测验证计划

```bash
# 每次 Sprint 结束执行
./scripts/evaluate.sh --role warehouse_manager --difficulty simple --cases 10
./scripts/evaluate.sh --role warehouse_manager --difficulty medium --cases 10
./scripts/evaluate.sh --role warehouse_manager --difficulty complex --cases 3
./scripts/evaluate.sh --role warehouse_manager --difficulty ambiguous --cases 8
```

| 里程碑 | 评测范围 | 通过标准 |
|--------|---------|---------|
| Sprint 2 | 简单10题 | 准确率≥90%，平均响应<2s |
| Sprint 3 | 简单10 + 中等10 | 简单≥95%，中等≥85% |
| Sprint 4 | 全部36题 | 简单≥95%，中等≥85%，复杂≥75%，模糊≥65% |

---

> **文档状态**: ✅ 已完成
> **下一阶段**: Sprint 1 启动 — 工具注册 + AgentTool 实现
> **重要提示**: YAML 配置化延后到第三期，当前所有场景直接 Java 实现
