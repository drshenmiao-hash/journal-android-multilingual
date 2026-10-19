# 曲线数据与内置消除参数

核对日期：2026-10-20。

## 两种曲线

效果时程沿用社区目录的 roas.duration 和 Journal 原生绘图器，不需要半衰期。支持参考时程与当前消费者最近一组日志。数据不完整时沿用原生降级显示，可能只有时长条；没有编造新的精神科治疗起效或疗效峰值数据。

理论消除曲线按历史剂量叠加，停药后衰减，再次给药保留残余。下述值为成人口服原形药参考，不是效果时长、实测血药浓度或个人半衰期。假定瞬时输入，忽略吸收、生物利用度、活性代谢物、相互作用与个体差异；缓释和持续给药记录不参与此模型。未来部分假定不再给药，不按连续用药天数判定稳态。

## 内置参考

仅摘录事实参数，不复制说明书正文。来源只有范围时，明确以算术中点作示意，不伪称人群平均值；范围不是曲线置信区间。已有自定义值优先，界面可恢复内置值。

- Alprazolam（阿普唑仑）：11.2 小时，健康成人范围 6.3–26.9 小时。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=60c90b4b-174f-4fd2-b65f-fc166c16bdc1)
- Lorazepam（劳拉西泮）：原形药约 12 小时。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=8602cef8-167e-4a8c-b10f-fc3295a3abcc)
- Sertraline（舍曲林）：约 26 小时，未模拟 N-去甲基代谢物。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=232a00a4-761e-422f-b62a-1a16de631eb6)
- Escitalopram（艾司西酞普兰）：范围 27–32 小时，示意中点 29.5 小时。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=84b2028d-eff4-434d-b71e-6a3c0812f625)
- Citalopram（西酞普兰）：约 35 小时。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=598cc779-06be-b27a-e053-2a91aa0a0909)
- Mirtazapine（米氮平）：范围 20–40 小时，示意中点 30 小时；年龄和性别有影响。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=edc62571-3808-b4b4-e053-2a95a90ab436)
- Olanzapine（奥氮平）：平均 30 小时；21–54 小时是说明书第 5–95 百分位范围。[口服片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=024e8e9b-a4fc-4882-b080-c070467166a6)
- Quetiapine（喹硫平）：原形药约 6 小时，未模拟去甲喹硫平。[普通片说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=5dd65e85-430b-4e25-bfe0-5b4c332c39c0)
- Pregabalin（普瑞巴林）：6.3 小时，仅适用于肾功能正常者。[LYRICA 胶囊说明书](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=0c5d6728-be43-4e1b-9f14-a7ce920e12a3)
- Methylphenidate（哌甲酯）：成人普通 Ritalin 片平均约 3.5 小时，范围 1.3–7.7 小时。资料来自 RITALIN LA 说明书中明确报告的普通片数据，不代表缓释释放时间，也不启用缓释曲线。[消除章节](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=effd952d-ac94-47bb-b107-589a4934dcca)

## 范围与待确认内容

此为先行参考集合，用户要加的具体药物清单仍待提供。目录有药名不代表有足够的效果时程资料。抗抑郁等数周治疗反应不能从血药达峰或消除半衰期推导。有重要活性代谢物、时变药代或特殊释放剂型的药物需要额外模型；未核对药物不自动赋值。

效果适配层检查药物、消费者、途径、剂型、时间和自定义单位身份，仅在绘图副本中换算兼容质量单位。剂量无法比较时用标准高度并说明，不把未知单位当成毫克。原日志、自定义单位定义、药物目录和数据库结构不变。

原本地 Journal 项目不可用，未完成旧代码对照。真机界面、个人临床效果、实测血药浓度以及真实数据导入与迁移尚未验证。
