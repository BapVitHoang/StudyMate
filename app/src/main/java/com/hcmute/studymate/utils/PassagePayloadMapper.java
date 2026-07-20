package com.hcmute.studymate.utils;

import com.hcmute.studymate.model.RetrievedChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PassagePayloadMapper {
    private PassagePayloadMapper() {
    }

    public static List<Map<String, Object>> toPayload(List<RetrievedChunk> passages) {
        List<Map<String, Object>> payloadPassages = new ArrayList<>();
        if (passages == null) {
            return payloadPassages;
        }
        for (RetrievedChunk passage : passages) {
            if (passage == null || passage.getChunk() == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("noteId", passage.getChunk().getNoteId());
            item.put("chunkId", passage.getChunk().getId());
            item.put("title", passage.getChunk().getTitle());
            item.put("text", passage.getChunk().getText());
            payloadPassages.add(item);
        }
        return payloadPassages;
    }
}
