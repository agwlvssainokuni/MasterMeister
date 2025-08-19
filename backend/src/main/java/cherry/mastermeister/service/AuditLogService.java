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

package cherry.mastermeister.service;

import cherry.mastermeister.entity.AuditLog;
import cherry.mastermeister.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logLoginSuccess(String username, HttpServletRequest request) {
        AuditLog auditLog = createBaseAuditLog(username, "LOGIN", "AUTH", request);
        auditLog.setSuccess(true);
        auditLog.setDetails("User successfully logged in");
        auditLogRepository.save(auditLog);
    }

    public void logLoginFailure(String username, String errorMessage, HttpServletRequest request) {
        AuditLog auditLog = createBaseAuditLog(username, "LOGIN", "AUTH", request);
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setDetails("Login attempt failed");
        auditLogRepository.save(auditLog);
    }

    public void logLogout(String username, HttpServletRequest request) {
        AuditLog auditLog = createBaseAuditLog(username, "LOGOUT", "AUTH", request);
        auditLog.setSuccess(true);
        auditLog.setDetails("User successfully logged out");
        auditLogRepository.save(auditLog);
    }

    public void logTokenRefresh(String username, HttpServletRequest request) {
        AuditLog auditLog = createBaseAuditLog(username, "TOKEN_REFRESH", "AUTH", request);
        auditLog.setSuccess(true);
        auditLog.setDetails("Access token refreshed successfully");
        auditLogRepository.save(auditLog);
    }

    public void logTokenRefreshFailure(String username, String errorMessage, HttpServletRequest request) {
        AuditLog auditLog = createBaseAuditLog(username, "TOKEN_REFRESH", "AUTH", request);
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setDetails("Token refresh attempt failed");
        auditLogRepository.save(auditLog);
    }

    public void logAdminAction(String username, String action, String target, String details, HttpServletRequest request) {
        AuditLog auditLog = createBaseAuditLog(username, action, target, request);
        auditLog.setSuccess(true);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    private AuditLog createBaseAuditLog(String username, String action, String target, HttpServletRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setTarget(target);
        auditLog.setIpAddress(extractClientIpAddress(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        return auditLog;
    }

    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-Forの最初のIPアドレスを取得
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
