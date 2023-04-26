## ChatGPT - 3.5

### 用户「对话记录表」设计

![](https://xiaoj-1309630359.cos.ap-nanjing.myqcloud.com/202304072123514.png)

+ 未登录状态下不能使用

设计中遇到的问题：用户提问时需要扣除相应的提问次数，那怎么样设计才能让用户点开右上角的账户详情时，实时查看剩余的提问词数？

+ 扣除完提问次数后，将 userInfo 设为空，这样用户再次刷新页面时，系统会判定此次为第一次登录，会重新查 user 信息存入
  userInfo。缺点：需要重新刷新页面
+ 专门写一个函数，在每个可能更改用户余额的操作后，追加一个重新查 userInfo 的操作
+ 只有在用户点击右上角的用户详情时，才会去重新查 user 的信息 ✓
  + 进一步优化：可以在前端的 userInfo 中添加一个修改次数（modcount）字段，只有当 modcount 不为 0 时才去重新查。当然，这需要在可能使
    userInfo 变化的代码后面添加 ++modcount
  + 增加接口访问次数限制（通用模块）
+ 加一个提示，在剩余次数后面显示一个「刷新」的按钮，引导用户点击刷新按钮。

### Chat Completion (长对话)

提问的实现：

1. 在提问时，若上下文超出最大限制，保留对话中用户的第一次提问和回答、最后一次提问、以及从最近的对话开始，在限制内的最远上下文。
2. 在返回时，若输出的回答达到了最大限制，此时会停止输出，需要用户再发一条 ”继续“，然后进行 1 的判断。

#### 一些细节

GPT-3.5-turbo 接口限制「提问 + 回答」不超过 4096 token。

其中「提问 (prompt)」包括当前的问题和所有上下文。「回答 (answer)」是指 ”你期望接口最多返回多少内容“，是自己设定的 (
max_tokens)。

实际中会出现一种情况：接口**将要**返回的内容大于设定的 max_tokens，此时接口会停止返回内容（需要用户发一条 ”继续“）。

用户发一条 ”继续“ 后，程序就会根据 `1.` 的逻辑进行缩减上下文。缩减的示意图如下：

![](https://xiaoj-1309630359.cos.ap-nanjing.myqcloud.com/202304251703630.png)

在本项目中，max_tokens 设置为 1024 token。也就是说，用户的提问，包括所有上下文，最多不能超过 4096 - 1024 = 3072 token。给接口预留了
1024 token 的返回空间。若接口实际将要返回的内容大于 1024，接口返回停止；小于 1024，无事发生。

#### 提问执行的逻辑

先访问 `/gpt/payForAns` 接口，在这个接口中完成用户输入内容是否有效的判断，用户输入内容有效，再扣除提问次数（修复了上下文过多导致接口终止，但还是扣除提问次数了的情况）。

之后在 `/gpt/completions/stream/` 接口中完成访问官方接口，并实现流返回。

#### 意外情况

+ 若用户输入为空：前端判断，提示 ”输入不能为空“

+ 用户输入字符过多（超过 3072 token）：直接在 `/gpt/payForAns` 中拦截

+ 无休止的在同一次对话中提问：不断缩减上下文，保证每次提问的总 token 小于 3072。

# Contribution

Anyone is welcome and encouraged to contribute. If you discover a bug, or think the project could use an enhancement,
follow these steps:

1. Create an issue and offer to code a solution. We can discuss the issue and decide whether any code would be a good
   addition to the project.
2. Fork the project. [https://github.com/nlpie/biomedicus-tokenizer/fork]
3. Create Feature branch (`git checkout -b feature-name`)
4. Code your solution.

- Follow the [Google style guide for Java](https://google.github.io/styleguide/javaguide.html). There are IDE profiles
  available [here](https://github.com/google/styleguide).
- Write unit tests for any non-trivial aspects of your code. If you are fixing a bug write a regression test: one that
  confirms the behavior you fixed stays fixed.

1. Commit to branch. (`git commit -am 'Summary of changes'`)
2. Push to GitHub (`git push origin feature-name`)
3. Create a pull request on this repository from your forked project. We will review and discuss your code and merge it.
