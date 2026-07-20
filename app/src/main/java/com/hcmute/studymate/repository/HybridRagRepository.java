package com.hcmute.studymate.repository;

import android.util.Log;

import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.DataCallback;

import java.util.List;

public class HybridRagRepository {
    private static final String TAG = "HybridRag";

    private final CloudRagRepository cloudRepository;
    private final LocalExtractiveRagRepository localRepository;

    public HybridRagRepository(CloudRagRepository cloudRepository,
                               LocalExtractiveRagRepository localRepository) {
        this.cloudRepository = cloudRepository;
        this.localRepository = localRepository;
    }

    public void answerFromNotes(String question, List<RetrievedChunk> passages, String locale,
                                DataCallback<RagAnswer> callback) {
        cloudRepository.answerFromNotes(question, passages, locale, new DataCallback<RagAnswer>() {
            @Override
            public void onSuccess(RagAnswer data) {
                callback.onSuccess(data);
            }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Cloud RAG failed, using extractive fallback", exception);
                localRepository.answerFromNotes(question, passages, locale, new DataCallback<RagAnswer>() {
                    @Override
                    public void onSuccess(RagAnswer data) {
                        data.setUsedFallback(true);
                        callback.onSuccess(data);
                    }

                    @Override
                    public void onError(Exception localException) {
                        callback.onError(localException);
                    }
                });
            }
        });
    }
}
