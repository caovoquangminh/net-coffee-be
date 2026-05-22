package com.netcoffee.service;

import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.SessionResponse;

import java.util.List;

public interface SessionService {

    SessionResponse getOrStartSession(Long userId, Long machineId);

    SessionResponse startSession(StartSessionRequest request);

    SessionResponse endSession(Long sessionId, Long requestingUserId);

    SessionResponse forceEndSession(Long sessionId);

    SessionResponse findById(Long sessionId);

    List<SessionResponse> findByUserId(Long userId);

    SessionResponse findActiveByUserId(Long userId);
}