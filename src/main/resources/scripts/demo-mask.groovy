// 敏感字段脱敏补全：手机号/身份证打码 + 校验标记
def result = [:]

if (input.phone) {
    def p = input.phone as String
    if (p.length() >= 7) {
        result.phone = p.substring(0, 3) + "****" + p.substring(p.length() - 4)
    } else {
        result.phone = "***"
    }
}

if (input.idCard) {
    def c = input.idCard as String
    if (c.length() >= 8) {
        result.idCard = c.substring(0, 4) + "**********" + c.substring(c.length() - 4)
    } else {
        result.idCard = "***"
    }
    // 补全校验标记：18 位视为有效
    result.idCardValid = (c.length() == 18)
}

return result
