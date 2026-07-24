package com.hcmute.studymate.repository;

import android.util.Log;

import com.hcmute.studymate.model.SummarizeRequest;
import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.utils.SummaryCallback;

public class HybridAiSummaryRepository implements AiSummaryRepository {
    private static final String TAG = "HybridAiSummary";

    private final AiSummaryRepository cloudRepository;
    private final AiSummaryRepository localRepository;

    public HybridAiSummaryRepository(AiSummaryRepository cloudRepository, AiSummaryRepository localRepository) {
        this.cloudRepository = cloudRepository;
        this.localRepository = localRepository;
    }

    @Override
    public void summarize(SummarizeRequest request, SummaryCallback callback) {
        cloudRepository.summarize(request, new SummaryCallback() {
            @Override
            public void onSuccess(SummaryResult result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Cloud summarize failed, using local fallback", exception);
                String cloudReason = exception == null || exception.getMessage() == null
                        ? "unknown cloud error"
                        : exception.getMessage();
                localRepository.summarize(request, new SummaryCallback() {
                    @Override
                    public void onSuccess(SummaryResult result) {
                        result.setUsedFallback(true);
                        result.setCloudFailureReason(cloudReason);
                        callback.onSuccess(result);
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
