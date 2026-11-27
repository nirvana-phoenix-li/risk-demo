// 基础字段补全：规范化 + 默认值 + 派生字段
def result = [:]

// 1. userId 统一转大写
if (input.userId) {
    result.userId = input.userId.toUpperCase()
}

// 2. scene 缺省值
if (!input.scene) {
    result.scene = "DEFAULT"
}

// 3. 派生字段：是否大额交易
result.highAmount = (input.amount != null && input.amount > 10000)

// 4. 补全处理时间戳
result.enrichTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())

return result
