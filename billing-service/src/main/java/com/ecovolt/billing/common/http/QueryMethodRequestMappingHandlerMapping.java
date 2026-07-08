package com.ecovolt.billing.common.http;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

public final class QueryMethodRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private static final QueryMethodRequestCondition QUERY_METHOD_CONDITION = new QueryMethodRequestCondition();

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        if (AnnotatedElementUtils.hasAnnotation(method, QueryMethod.class)) {
            return QUERY_METHOD_CONDITION;
        }
        return super.getCustomMethodCondition(method);
    }
}
