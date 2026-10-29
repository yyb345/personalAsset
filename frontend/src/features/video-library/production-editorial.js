// Reviewed against the public production library on 2026-10-29.
// Classification uses titles and channels, not a claim that every video was watched.
// Unknown difficulty remains unlabelled. Stable YouTube IDs survive imports/reordering.
const groups = {
  basics: ['EsaUQNx59vA', 'sJ4VJWycX9M', 'CxXgV54KzpQ', 'Mklj3Y2-fNg', 'LCEmiRjPEtQ', 'cWSTqbt5dyg', '4E27qlfYw0A'],
  models: ['u8MPjmuCkEQ', 'AVQzG2MY858', 'yWTVEG9UIHo', 'yAvJ7b_FxUA', 'GBd7iuJkW08', 'cKSwj_qZ8Jg'],
  agents: ['UqMtkgQe-kI', 'BDgGDCQX3G4', 'a00xIn5kwhM', 'Lg-meK5IU8Q'],
  applications: ['WriCcrOP8s4', 'n3XmK7rKPvs', 'VbqaL_eHhKY', 'eh8bcBIAAFo', 'PplmzlgE0kg', 's5r4wdOWLjk', 'U7PcyE0p54s', '-MD9NsVj2xo'],
  business: ['Za6c0NcAmOk', 'abtHadERzXU', 'URiIsrdplbo', 'I79cK9iV-vQ', 'DGU0wXMKqf4'],
  technology: ['c-MnSFGTSN8', 'jYUZAF3ePFE', 'rkZzg7Vowao', 'sW9npZVpiMI', '0KmUoTfGa34'],
  japanese: ['pXh1Mc5LonQ', 'MmvaYPjogwQ'],
  english: [
    '_p64_S4bbjU', 'GkrwjFvnW3E', 'ZVP8cmlHwgU', 'L_WK9XUwhXk', '0-O_t_SstEs',
    'cH9auq8TVXs', 'VOqb01Bln_s', '3C3wOhIuTgY', 'Y3vulWfEpEE', 'cnG5plbTssA',
    'kQ6WIAb8Wsc', 'ZyEMuN8NYw8', '52d1ec7B_gU', 'ATz5oDYMDMY', 'Ja2qxjUnw9Q',
    'WH-PXI6rbUw', 'awSZylgjIlc', '_AR345TtYGE', '-r8FU-gV8Ss', 'KhwO6gQtyAE',
    'tv2nyeQdCl8', 'c58UpZLN4Ug', '8OzUGQLR7ys', 'xpyrefzvTpI', 'VVqGRv5fnSQ',
    'WLy9Tab_8XQ', 'RdZ_shAlGsI', 'kBz51Ldxw9M', '5tJa81SIXOQ', 'jF2TJVCRdFc',
    'XxyNyyoj_Kg', '05TLyn5vgEA', '6icayqBzeBg', '85yYJByDPtk', 'Wses7f_czfE',
    'Wxad6IONBvE', 'Li4RdAQx11g', 'w5JfEBFIvtA', '-ZVOwqJpuBY', 'CL5WS5pao7Y',
    'nLRm8348sPY', 'FD12D9kySCg', 'AGovqGO2t0M', 'rtuKId76DZI', 'Fp_o3jJrk9I',
    'uxRf7KS3abo', 'O0WFna-1gkM'
  ]
}

export const productionEditorial = Object.fromEntries(
  Object.entries(groups).flatMap(([category, ids]) => ids.map(id => [id, { category }]))
)

// Explicit topic sequences. Failed/unready videos stay in the library but are not
// recommended as the next lesson. No difficulty is inferred from video length.
export const curatedSequences = {
  foundations: ['AVQzG2MY858', 'LCEmiRjPEtQ', 'yAvJ7b_FxUA', 'GBd7iuJkW08', 'cKSwj_qZ8Jg'],
  building: ['Lg-meK5IU8Q', 'a00xIn5kwhM', 'BDgGDCQX3G4', 'VbqaL_eHhKY', 'PplmzlgE0kg'],
  english: ['Wxad6IONBvE', 'WH-PXI6rbUw', 'ATz5oDYMDMY', 'rtuKId76DZI', '_p64_S4bbjU']
}

const objectives = {
  AVQzG2MY858: ['区分 LLM、SLM 与基础模型，整理模型选型的比较维度。', 'Compare LLMs, SLMs and foundation models, and record model-selection criteria.'],
  LCEmiRjPEtQ: ['理解 AI 对软件开发方式的影响，记录新的开发思路与英文术语。', 'Explore how AI changes software development and collect key terms.'],
  yAvJ7b_FxUA: ['梳理后训练的作用，以及它与模型行为和偏好的关系。', 'Explore post-training and its relationship to model behaviour and preferences.'],
  GBd7iuJkW08: ['整理表示学习与世界模型之间的概念联系。', 'Map the connections between representation learning and world modelling.'],
  cKSwj_qZ8Jg: ['比较注意力机制的替代方案，记录需要补充的技术基础。', 'Compare alternatives to attention and note prerequisite concepts.'],
  'Lg-meK5IU8Q': ['理解 Agent Skills 的用途与工作方式。', 'Understand the purpose and operation of agent skills.'],
  a00xIn5kwhM: ['了解强化学习环境如何支持智能体学习真实工作任务。', 'Explore how RL environments support agents learning real-world work.'],
  BDgGDCQX3G4: ['了解软件开发智能体的产品思路与应用场景。', 'Explore product ideas and use cases for software-development agents.'],
  VbqaL_eHhKY: ['整理 AI 辅助设计的方法，思考适合自己项目的使用场景。', 'Collect AI-assisted design methods and possible uses in your own project.'],
  PplmzlgE0kg: ['观察 Claude Code 产品团队的工作方式，记录产品迭代思路。', 'Explore how the Claude Code product team works and iterates.'],
  Wxad6IONBvE: ['积累精力管理相关表达，并用英语复述一个可执行的方法。', 'Collect energy-management expressions and retell one practical method.'],
  'WH-PXI6rbUw': ['积累习惯与自律相关表达，并练习用英语描述自己的习惯。', 'Learn habit and self-discipline expressions and describe your own habits.'],
  ATz5oDYMDMY: ['积累工作压力和日常安排的英语表达。', 'Build English expressions for work stress and daily routines.'],
  rtuKId76DZI: ['积累技术求职和录用相关词汇，练习描述自己的职业选择。', 'Learn tech job-offer vocabulary and describe career choices.'],
  _p64_S4bbjU: ['积累软件工程师访谈中的职业表达，尝试复述问答重点。', 'Collect career expressions from a software-engineer interview and retell key points.']
}
for (const [id, [zh, en]] of Object.entries(objectives)) {
  productionEditorial[id].objective = { 'zh-CN': zh, 'en-US': en }
}
