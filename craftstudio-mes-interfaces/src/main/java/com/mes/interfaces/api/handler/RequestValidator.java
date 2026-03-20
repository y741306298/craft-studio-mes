package com.mes.interfaces.api.handler;
import com.mes.interfaces.api.dto.req.base.ApiRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequestValidator {
    @Before("execution(* com.mes.interfaces.api..*(..))")
    public void validateRequestParams(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            // 检查参数是否为ApiRequest类型且需要校验
            if (arg instanceof ApiRequest apiRequest) {
                if (!apiRequest.isValid()) {
                    String validationMessage = apiRequest.getValidationMessage();
                    throw new InvalidRequestException(
                        "请求参数错误" + ( "".equals(validationMessage) ? "":": "+validationMessage )
                    );
                }
            }
        }
    }
}
