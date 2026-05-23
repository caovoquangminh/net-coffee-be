package com.netcoffee.service;

import com.netcoffee.dto.ChatPresenceDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatPresenceService {

    private final ConcurrentHashMap<Long, ChatPresenceDto> activeChats = new ConcurrentHashMap<>();

    public void register(ChatPresenceDto dto) {
        activeChats.put(dto.getMachineId(), dto);
    }

    public void unregister(Long machineId) {
        activeChats.remove(machineId);
    }

    public List<ChatPresenceDto> getAll() {
        return new ArrayList<>(activeChats.values());
    }
}
