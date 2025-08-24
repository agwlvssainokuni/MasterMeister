/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister.aspect;

import cherry.mastermeister.annotation.RequirePermission;
import cherry.mastermeister.exception.PermissionDeniedException;
import cherry.mastermeister.model.PermissionCheckResult;
import cherry.mastermeister.service.PermissionAuthService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class PermissionAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionAuthService permissionAuthService;

    public PermissionAspect(
            PermissionAuthService permissionAuthService
    ) {
        this.permissionAuthService = permissionAuthService;
    }

    @Before("@annotation(cherry.mastermeister.annotation.RequirePermission) || " +
            "@within(cherry.mastermeister.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequirePermission annotation = getRequirePermissionAnnotation(method);

        if (annotation == null) {
            return;
        }

        logger.debug("Checking permission for method: {}.{}",
                joinPoint.getTarget().getClass().getSimpleName(), method.getName());

        Map<String, Object> parameterMap = getParameterMap(joinPoint);

        Long connectionId = extractParameterValue(parameterMap, annotation.connectionIdParam(), Long.class);
        String schemaName = extractParameterValue(parameterMap, annotation.schemaNameParam(), String.class);
        String tableName = extractParameterValue(parameterMap, annotation.tableNameParam(), String.class);
        String columnName = extractParameterValue(parameterMap, annotation.columnNameParam(), String.class);

        if (connectionId == null) {
            logger.warn("Connection ID not found in method parameters for permission check");
            throw new PermissionDeniedException("Connection ID required for permission check", null);
        }

        PermissionCheckResult result = permissionAuthService.checkPermission(
                connectionId, annotation.value(), schemaName, tableName, columnName);

        if (!result.granted()) {
            logger.warn("Permission denied for method: {}.{}, reason: {}",
                    joinPoint.getTarget().getClass().getSimpleName(), method.getName(), result.reason());

            throw new PermissionDeniedException(
                    annotation.message(),
                    annotation.value(),
                    connectionId,
                    schemaName,
                    tableName,
                    columnName,
                    result
            );
        }

        logger.debug("Permission granted for method: {}.{}, scope: {}",
                joinPoint.getTarget().getClass().getSimpleName(), method.getName(), result.effectiveScope());
    }

    /**
     * Get RequirePermission annotation from method or class
     */
    private RequirePermission getRequirePermissionAnnotation(Method method) {
        RequirePermission annotation = AnnotationUtils.findAnnotation(method, RequirePermission.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequirePermission.class);
        }
        return annotation;
    }

    /**
     * Build parameter name to value map
     */
    private Map<String, Object> getParameterMap(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        Map<String, Object> parameterMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            parameterMap.put(parameters[i].getName(), args[i]);
        }

        return parameterMap;
    }

    /**
     * Extract parameter value by name and type
     */
    @SuppressWarnings("unchecked")
    private <T> T extractParameterValue(Map<String, Object> parameterMap, String parameterName, Class<T> type) {
        if (parameterName == null || parameterName.isEmpty()) {
            return null;
        }

        Object value = parameterMap.get(parameterName);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return (T) value;
        }

        // Try to convert string to target type
        if (value instanceof String && type == Long.class) {
            try {
                return (T) Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert parameter '{}' value '{}' to Long", parameterName, value);
                return null;
            }
        }

        return null;
    }
}
