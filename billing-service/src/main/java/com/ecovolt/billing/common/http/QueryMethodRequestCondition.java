package com.ecovolt.billing.common.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

public final class QueryMethodRequestCondition implements RequestCondition<QueryMethodRequestCondition> {

    private static final String QUERY_METHOD = "QUERY";

    @Override
    public QueryMethodRequestCondition combine(QueryMethodRequestCondition other) {
        return other;
    }

    @Override
    public QueryMethodRequestCondition getMatchingCondition(HttpServletRequest request) {
        return QUERY_METHOD.equals(request.getMethod()) ? this : null;
    }

    @Override
    public int compareTo(QueryMethodRequestCondition other, HttpServletRequest request) {
        return 0;
    }
}
