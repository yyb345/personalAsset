export default {
  "en-US": {
    "path": "/articles/jeff-dean-discovery-loop-ai-scientific-discovery",
    "lang": "en",
    "hreflang": "en",
    "ogLocale": "en_US",
    "file": "article-discovery-loop-en.html",
    "title": "Jeff Dean and Fei-Fei Li: How Discovery Loop Aims to Accelerate AI Scientific Discovery",
    "description": "Explore Jeff Dean and Fei-Fei Li’s discussion of Discovery Loop, AI agents for scientific discovery, recursive self-improvement, and the skills researchers need.",
    "intro": [
      "Can AI move beyond answering scientific questions to proposing hypotheses, running experiments, and helping discover new knowledge?",
      "In a talk and conversation published by Stanford HAI, Jeff Dean outlined Discovery Loop’s approach: organize scientific and engineering work into automated cycles that run, evaluate results, and improve over time. He then joined Fei-Fei Li to discuss the limits of AI self-improvement, changes in mathematics, and how young people can navigate rapid technological change.",
      "The central question is practical: if AI reduces the waiting between experiments, learns from previous results, and helps researchers explore more alternatives, how much faster could discovery become?"
    ],
    "sections": [
      [
        "discovery-loop",
        "What is Discovery Loop? Bringing AI into the experimental cycle",
        [
          "Discovery Loop aims to build AI systems that automatically solve important problems in machine learning, science, and engineering. Dean described a research cycle shared across these fields.",
          "Traditionally, researchers move between writing code, scheduling computations, waiting for results, analyzing data, and planning the next experiment. Discovery Loop aims to automate more of these transitions and execution steps while using feedback to improve subsequent exploration.",
          "Three variables matter: the time required for each cycle, the number of alternatives explored, and how much useful information each experiment contributes to future decisions. Faster execution is only part of the opportunity. Earlier experiments should also improve the quality of later ones."
        ],
        [
          "Propose an experiment.",
          "Implement and run it.",
          "Measure and analyze the results.",
          "Use the findings to decide what to try next."
        ]
      ],
      [
        "agents",
        "Why do researchers need AI agents as well as foundation models?",
        [
          "Dean argued that a capable foundation model alone is insufficient for complex research. Scientific and engineering tasks involve multiple steps, external tools, and experimental environments.",
          "After a model suggests an idea, that idea still needs to become an executable experiment. Someone or some system must then determine whether the results support it. AI agents connect models, tools, and feedback across these stages.",
          "Creativity matters too. Discovery often requires combining existing knowledge into an untested approach and checking whether it works. Tool use, exploration, and learning from previous experiments are therefore central to the proposed workflow."
        ],
        [
          "Break a research goal into actionable subproblems.",
          "Write or modify code and run experiments using computing resources.",
          "Collect metrics and compare alternatives.",
          "Adjust the next round of exploration based on the results."
        ]
      ],
      [
        "focus",
        "Why start with machine learning and hardware design?",
        [
          "In the conversation, Dean identified three initial areas of focus: machine learning research, machine learning engineering, and hardware design. A useful property of these areas is that many research steps can take place digitally.",
          "Researchers can modify a model in code, train it, and evaluate the results. Compared with experiments that require material preparation, biological growth, or other physical processes, these tasks can be easier to organize into rapid feedback cycles.",
          "This also helps explain why AI for Science may progress at different speeds across disciplines. Physical experiments still face constraints involving time, equipment, and resources. In hardware, digital design and simulation must also be distinguished from manufacturing and physical validation.",
          "Dean expressed interest in healthcare as a possible future direction, rather than describing it as the immediate focus."
        ]
      ],
      [
        "self-improvement",
        "Self-improvement vs. recursive self-improvement",
        [
          "Li pressed Dean to clarify two frequently conflated concepts. In his explanation, the key distinction is whether a system improves a target task or improves the underlying system that proposes improvements.",
          "Self-improvement: consider an AI system developing a model to help interpret MRI images. It can test architectures, run experiments, compare accuracy, and optimize the resulting model. The underlying system deciding which experiments to try need not improve in the process. This is iterative improvement on a specified task.",
          "Recursive self-improvement: the experimental approach is also applied to improving the underlying system’s ability to propose ideas, design experiments, and choose actions. Improving both a target model and the system responsible for producing those improvements creates another level of feedback.",
          "This distinction helps avoid labeling every automated tuning process, experiment search, or model optimization as recursive self-improvement."
        ]
      ],
      [
        "limits",
        "Why recursive self-improvement does not imply infinite acceleration",
        [
          "Even when AI schedules experiments, analyzes results, and continues optimization automatically, research still takes time. Dean offered a simple example: an experiment finishes overnight, but its human researcher only checks it in the morning. Automation can reduce that delay and start the next experiment sooner.",
          "Training models, executing computations, saving data, and evaluating results still require resources and remain subject to physical constraints. Faster iteration therefore does not establish that research can accelerate without limit.",
          "To assess a self-improving system, researchers still need to measure the delays it removes, the improvements in experimental quality, and the computing resources it consumes."
        ]
      ],
      [
        "productivity",
        "What does the goal of 20–50× higher productivity mean?",
        [
          "Dean described a goal of making domain experts 20–50 times more productive. This was an ambition stated in the talk, not a publicly demonstrated result that should be assumed to apply broadly.",
          "The proposed approach would allow a small number of specialists to explore problems that would otherwise require more human effort. Researchers would still decide which questions matter, whether evaluations are appropriate, and what results mean in practice.",
          "AI systems could take on more experimental execution, comparison, and feedback processing. If that collaboration works, researchers could devote more attention to research direction and consequential judgments."
        ]
      ],
      [
        "mathematics",
        "When AI generates a mathematical proof, does understanding still matter?",
        [
          "Turning to mathematics, Li asked how researchers should understand the value of AI participation in difficult problems. Dean observed that a machine-generated proof can be formally checked yet remain lengthy and difficult to interpret as a clear, concise argument.",
          "This separates two research outcomes: establishing that a conclusion holds, and understanding why it holds in a way that yields techniques useful elsewhere. An elegant proof can also introduce a method that transfers to other problems.",
          "Human researchers can therefore continue asking whether a shorter proof exists, whether it reveals a more general structure, and whether its ideas solve related questions. Verification and understanding remain valuable goals in their own right."
        ]
      ],
      [
        "skills",
        "What skills should young people develop in the AI era?",
        [
          "At the end of the conversation, Li raised students’ anxiety about jobs and the future. Dean advised learning about emerging technologies and understanding how to use them in the areas that matter most to you.",
          "That advice extends beyond software to medicine, literature, and other professions. Tools and workflows will change, while domain judgment still affects whether a person can ask valuable questions, identify errors, and apply technology to real needs."
        ],
        [
          "Build domain foundations: understand professional problems, evaluation criteria, and common mistakes.",
          "Practice using AI tools in research, programming, analysis, or experimental workflows.",
          "Develop judgment: check reliability and determine whether an output actually addresses the problem."
        ]
      ],
      [
        "takeaways",
        "What Discovery Loop suggests about the future of research",
        [
          "Discovery Loop presents a research workflow in which AI participates continuously in proposing approaches, running experiments, evaluating results, and planning subsequent actions.",
          "Its progress will depend on reliable feedback, clearly assessable tasks, and the ability to learn useful lessons from earlier attempts.",
          "AI scientific discovery depends on model capabilities, experimental infrastructure, and human judgment working together. Connecting them effectively could bring more ideas into testing and ultimately produce meaningful new knowledge."
        ]
      ]
    ],
    "faq": [
      [
        "What does Discovery Loop focus on?",
        "In this talk, Dean described an ambition to automate experimental loops in science and engineering, initially focusing on machine learning research, machine learning engineering, and hardware design."
      ],
      [
        "How can AI agents help scientific research?",
        "Agents can connect models with tools and participate in experiment design, code execution, evaluation, and subsequent planning to form an iterative workflow."
      ],
      [
        "Does recursive self-improvement mean AI will accelerate infinitely?",
        "That conclusion does not follow. Dean emphasized that computing, training, and experimentation still face time and physical constraints."
      ],
      [
        "Has Discovery Loop demonstrated 20–50× higher productivity?",
        "The talk presented this figure as a goal for domain expert productivity, not as a verified general result."
      ]
    ]
  },
  "zh-CN": {
    "path": "/zh/articles/jeff-dean-discovery-loop-ai-scientific-discovery",
    "lang": "zh-CN",
    "hreflang": "zh-Hans",
    "ogLocale": "zh_CN",
    "file": "article-discovery-loop-zh.html",
    "title": "Jeff Dean 对话李飞飞：Discovery Loop 如何用 AI 加速科学发现",
    "description": "解读 Jeff Dean 与李飞飞关于 Discovery Loop 的演讲与对话，了解 AI Agent 如何自动化科研实验、自我改进与递归自我改进的区别，以及 AI 时代研究者需要具备的能力。",
    "intro": [
      "AI 能否从回答科学问题，进一步参与提出假设、运行实验和发现新知识？",
      "在 Stanford HAI 发布的演讲与对话中，Jeff Dean 介绍了 Discovery Loop 的思路：将科学与工程中的实验过程组织成可以持续运行、评估和改进的自动化循环。随后，他与李飞飞讨论了 AI 自我改进的边界、数学研究的变化，以及年轻人如何面对技术快速发展带来的不确定性。",
      "这场讨论的核心，是一个具体的工程问题：如果 AI 能减少实验之间的等待、从历史结果中学习，并帮助研究者探索更多方案，科学发现的速度会发生什么变化？"
    ],
    "sections": [
      [
        "discovery-loop",
        "Discovery Loop 是什么？让 AI 参与完整的实验循环",
        [
          "Discovery Loop 希望构建能够自动解决机器学习、科学与工程领域重要问题的 AI 系统。Jeff Dean 将这些领域共有的研究过程概括为一个循环。",
          "传统研究中，这些环节往往需要研究者不断切换工作：编写代码、安排计算任务、等待结果、分析数据，再设计下一轮实验。",
          "Discovery Loop 的思路，是让 AI 系统承担更多衔接与执行工作。系统既要能够运行实验，也要利用实验反馈，改进后续探索的方向。",
          "这里有三个关键变量：每轮实验需要多长时间、能够同时探索多少方案，以及每次实验能为后续决策提供多少有效信息。缩短循环时间只是其中一部分，更有价值的能力，是让已经完成的实验持续改善未来实验的质量。"
        ],
        [
          "提出实验方案。",
          "实现并运行实验。",
          "测量、分析实验结果。",
          "根据结果决定下一步行动。"
        ]
      ],
      [
        "agents",
        "为什么基础模型之外，还需要 AI Agent？",
        [
          "Jeff Dean 在演讲中强调，仅有能力强大的基础模型，还不足以解决复杂科研问题。科学与工程任务通常涉及多个步骤，也需要与外部工具和实验环境交互。",
          "模型提出一个想法之后，还需要将其转化为可执行的实验，再判断结果是否支持原先的设想。AI Agent 在这里承担的是连接模型、工具和反馈的作用。",
          "这样的工作流还要求系统具备创造性。科学发现往往需要组合已有知识，提出尚未验证的新方法，并通过实验检验它们。因此，Discovery Loop 关注工具使用、方案探索，以及从以往实验经验中学习。"
        ],
        [
          "将研究目标拆解为可执行的子问题。",
          "编写或修改实验代码，调用计算资源运行实验。",
          "收集指标并比较不同方案。",
          "根据结果调整下一轮探索。"
        ]
      ],
      [
        "focus",
        "为什么先从机器学习和硬件设计入手？",
        [
          "在对话中，Jeff Dean 表示，Discovery Loop 当时优先关注机器学习研究、机器学习工程，以及硬件设计。这些领域的重要优势，是许多研究步骤能够在数字环境中完成。",
          "例如，研究者可以通过代码修改模型结构，运行训练任务，再利用评估指标判断变化是否有效。相比需要等待材料制备、生物培养或其他物理过程的实验，这类任务更容易形成快速迭代的闭环。",
          "这也解释了为什么 AI for Science 的落地速度会因领域而异。当研究依赖现实世界的实验时，实验本身的时间、设备和资源约束仍然存在。对于硬件设计，也需要区分数字设计与仿真环节，以及后续制造和实物验证。",
          "Jeff Dean 同时表达了对医疗健康领域的兴趣，但将其描述为未来可能拓展的方向。"
        ]
      ],
      [
        "self-improvement",
        "自我改进与递归自我改进，有什么区别？",
        [
          "李飞飞专门追问了一个容易被混用的概念。按照 Jeff Dean 在本次对话中的解释，关键区别在于：系统改善的是目标任务，还是负责产生改进方案的系统本身。",
          "自我改进：假设一个 AI 系统要开发辅助解读 MRI 图像的模型。它可以尝试不同模型结构、运行实验、比较准确率，再根据结果继续优化。但负责设计实验、决定下一步尝试什么的基础系统，并不一定因此获得改进。这属于对指定任务的持续优化。",
          "递归自我改进：系统进一步利用类似的实验方法，改善自身提出方案、设计实验和选择行动的能力。如果系统既能改进目标模型，也能提升负责推动这些改进的基础系统，就形成了更深一层的反馈。",
          "理解这个区别，有助于避免将所有自动调参、实验搜索或模型优化，都笼统称为递归自我改进。"
        ]
      ],
      [
        "limits",
        "为什么递归自我改进不意味着无限加速？",
        [
          "即使 AI 能自动安排实验、分析结果并继续优化，研究过程仍然需要时间。Jeff Dean 举例说，实验在凌晨结束，而研究者早上才开始查看结果，自动化系统可以减少这段等待，更早启动下一轮工作。",
          "但训练模型、执行计算、保存数据和评估结果都需要实际资源，也受到物理条件约束。因此，自动化能够提高研究迭代速度，却不能据此推断研究过程会无限加速。",
          "判断一个自我改进系统的价值，仍然需要观察它实际减少了哪些等待、提高了多少实验质量，以及消耗了多少计算资源。"
        ]
      ],
      [
        "productivity",
        "“效率提升 20—50 倍”应该如何理解？",
        [
          "Jeff Dean 提出，Discovery Loop 希望让领域专家的生产力提升至原来的 20—50 倍。这是演讲中提出的目标，并非已经公开验证的普遍效果。",
          "这一目标背后的工作方式，是让少量领域专家借助自动化系统，探索原本需要更多人力才能推进的问题。研究者仍然需要判断哪些问题值得解决、实验评价是否合理，以及结果具有怎样的实际意义。",
          "AI 系统则可以承担更多实验执行、方案比较和反馈整理工作。如果这种协作方式有效，研究者就能将更多精力放在研究方向和关键判断上。"
        ]
      ],
      [
        "mathematics",
        "AI 生成数学证明之后，人类还需要理解吗？",
        [
          "讨论延伸到数学时，李飞飞提出：当 AI 能够参与解决困难的数学问题，数学家如何理解其中的价值？Jeff Dean 谈到，一些机器生成的证明即使能够通过形式化检查，也可能十分冗长，难以让人看出清晰、简洁的推理路径。",
          "这提示我们区分两种研究收获：一是确认某个结论成立，二是理解它为什么成立，并从中提炼能够用于其他问题的方法。数学中的优雅证明，往往还带来可迁移的新技巧。",
          "AI 参与数学研究之后，人类研究者依然可以继续追问：能否找到更简洁的证明？能否揭示更一般的结构？能否用新的方法解决相关问题？证明的验证与知识的理解，仍然是值得分别推进的工作。"
        ]
      ],
      [
        "skills",
        "AI 时代，年轻人应该培养什么能力？",
        [
          "在对话最后，李飞飞谈到了学生对就业和未来的焦虑。Jeff Dean 建议理解正在出现的新技术，并学会将这些技术用于自己最关心的领域。",
          "这一建议适用于软件开发，也适用于医学、文学和其他专业。随着 AI 工具变化，具体工作方法会不断调整，而专业判断仍然影响一个人能否提出有价值的问题、识别结果中的错误，并将工具用于真实需求。"
        ],
        [
          "建立领域基础：理解专业问题、评价标准和常见错误。",
          "练习使用 AI 工具：将其纳入检索、编程、分析或实验流程。",
          "训练判断能力：检查输出是否可靠，判断结果是否真正解决问题。"
        ]
      ],
      [
        "takeaways",
        "Discovery Loop 带来的启示",
        [
          "Discovery Loop 展示了一种值得关注的科研工作方式：让 AI 持续参与提出方案、执行实验、评估结果和规划下一步行动。",
          "这种方式能取得多大进展，取决于实验反馈是否可靠、任务能否清晰评估，以及系统能否从历史尝试中获得有效经验。",
          "AI 科学发现的关键，既包括模型能力，也包括实验基础设施和人类研究者的判断。将这几部分有效连接起来，才有可能让更多想法进入验证阶段，并最终形成有意义的新知识。"
        ]
      ]
    ],
    "faq": [
      [
        "Discovery Loop 主要研究什么？",
        "根据本次演讲，Discovery Loop 希望通过自动化实验循环解决科学与工程问题，当时优先关注机器学习研究、机器学习工程和硬件设计。"
      ],
      [
        "AI Agent 如何帮助科学研究？",
        "AI Agent 可以连接模型与工具，参与实验设计、代码执行、结果评估和后续方案调整，使多个研究步骤形成持续迭代的工作流。"
      ],
      [
        "递归自我改进是否意味着 AI 会无限加速？",
        "不能这样推断。Jeff Dean 在对话中指出，计算、训练和实验仍然受到实际时间与物理条件的约束。"
      ],
      [
        "Discovery Loop 已经实现效率提升 20—50 倍了吗？",
        "本次演讲将这一数字作为提升领域专家生产力的目标，不能据此视为已经验证的通用成果。"
      ]
    ]
  }
}
