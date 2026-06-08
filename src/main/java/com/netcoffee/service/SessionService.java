package com.netcoffee.service;

import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.ActiveSessionWithUserResponse;
import com.netcoffee.dto.response.SessionResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SessionService {

    SessionResponse getOrStartSession(Long userId, Long machineId);

    SessionResponse startSession(StartSessionRequest request);

    SessionResponse endSession(Long sessionId, Long requestingUserId);

    SessionResponse forceEndSession(Long sessionId);

    SessionResponse findById(Long sessionId);

    List<SessionResponse> findByUserId(Long userId);

    Page<SessionResponse> findByUserIdPaged(Long userId, Pageable pageable);

    SessionResponse findActiveByUserId(Long userId);

    List<ActiveSessionWithUserResponse> findAllActiveWithUserInfo();

    void heartbeat(Long sessionId, Long userId);

    void cleanUpStaleSessions();
}
