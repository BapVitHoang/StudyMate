package com.hcmute.studymate.service;

import com.hcmute.studymate.ml.LocalEmbeddingEngine;
import com.hcmute.studymate.ml.ModelManager;
import com.hcmute.studymate.ml.VectorMath;
import com.hcmute.studymate.model.Concept;
import com.hcmute.studymate.model.ConceptEdge;
import com.hcmute.studymate.model.LearningGap;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.repository.CloudConceptRepository;
import com.hcmute.studymate.repository.FirestoreConceptRepository;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KnowledgeGraphService {
    private final CloudConceptRepository cloudConceptRepository;
    private final FirestoreConceptRepository conceptRepository;
    private final ModelManager modelManager;
    private final LocalEmbeddingEngine embeddingEngine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public KnowledgeGraphService(CloudConceptRepository cloudConceptRepository,
                                 FirestoreConceptRepository conceptRepository,
                                 ModelManager modelManager,
                                 LocalEmbeddingEngine embeddingEngine) {
        this.cloudConceptRepository = cloudConceptRepository;
        this.conceptRepository = conceptRepository;
        this.modelManager = modelManager;
        this.embeddingEngine = embeddingEngine;
    }

    public void extractFromNote(String userId, Note note, String locale, OperationCallback callback) {
        if (note == null || note.getId() == null) {
            callback.onError(new IllegalArgumentException("Note required"));
            return;
        }
        String content = (note.getTitle() == null ? "" : note.getTitle()) + "\n"
                + (note.getContent() == null ? "" : note.getContent());
        cloudConceptRepository.extract(note.getId(), note.getTitle(), content, locale,
                new DataCallback<CloudConceptRepository.ExtractResult>() {
                    @Override
                    public void onSuccess(CloudConceptRepository.ExtractResult data) {
                        executor.execute(() -> {
                            try {
                                enrichEmbeddings(data.concepts);
                                mergeExistingNoteIds(userId, data.concepts);
                                List<ConceptEdge> linked = new ArrayList<>(data.edges);
                                linked.addAll(suggestLinks(userId, data.concepts));
                                conceptRepository.upsertConceptsAndEdges(
                                        userId, data.concepts, linked, new OperationCallback() {
                                            @Override
                                            public void onSuccess() {
                                                rebuildGaps(userId, callback);
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                callback.onError(exception);
                                            }
                                        });
                            } catch (Exception exception) {
                                callback.onError(exception);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
    }

    public void rebuildGaps(String userId, OperationCallback callback) {
        conceptRepository.listConcepts(userId, new ListCallback<Concept>() {
            @Override
            public void onSuccess(List<Concept> concepts) {
                List<LearningGap> gaps = new ArrayList<>();
                long now = System.currentTimeMillis();
                for (Concept concept : concepts) {
                    int mentions = concept.getNoteIds() == null ? 0 : concept.getNoteIds().size();
                    boolean thinDefinition = concept.getDefinition() == null
                            || concept.getDefinition().trim().length() < 20;
                    if (mentions <= 1 || thinDefinition) {
                        LearningGap gap = new LearningGap();
                        gap.setConceptName(concept.getName());
                        gap.setRelatedNoteIds(concept.getNoteIds());
                        gap.setUpdatedAt(now);
                        if (thinDefinition && mentions <= 1) {
                            gap.setReason("Thin coverage — add a clearer definition note.");
                            gap.setSeverity(0.9);
                        } else if (thinDefinition) {
                            gap.setReason("Definition looks incomplete.");
                            gap.setSeverity(0.7);
                        } else {
                            gap.setReason("Only mentioned in one note.");
                            gap.setSeverity(0.55);
                        }
                        gaps.add(gap);
                    }
                }
                gaps.sort(Comparator.comparingDouble(LearningGap::getSeverity).reversed());
                if (gaps.size() > 20) {
                    gaps = gaps.subList(0, 20);
                }
                conceptRepository.replaceGaps(userId, gaps, callback);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void listConcepts(String userId, ListCallback<Concept> callback) {
        conceptRepository.listConcepts(userId, callback);
    }

    public void listEdges(String userId, ListCallback<ConceptEdge> callback) {
        conceptRepository.listEdges(userId, callback);
    }

    public void listGaps(String userId, ListCallback<LearningGap> callback) {
        conceptRepository.listGaps(userId, callback);
    }

    public void studyNext(String userId, DataCallback<List<LearningGap>> callback) {
        conceptRepository.listGaps(userId, new ListCallback<LearningGap>() {
            @Override
            public void onSuccess(List<LearningGap> items) {
                List<LearningGap> sorted = new ArrayList<>(items == null ? new ArrayList<>() : items);
                sorted.sort(Comparator.comparingDouble(LearningGap::getSeverity).reversed());
                if (sorted.size() > 5) {
                    sorted = sorted.subList(0, 5);
                }
                callback.onSuccess(sorted);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private void enrichEmbeddings(List<Concept> concepts) throws Exception {
        if (!modelManager.isReady()) {
            modelManager.ensureReadySync();
        }
        if (!embeddingEngine.isReady()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Concept concept : concepts) {
            String text = concept.getName() + ". " + (concept.getDefinition() == null ? "" : concept.getDefinition());
            float[] embedding = embeddingEngine.embed(text);
            concept.setEmbedding(VectorMath.toDoubleList(embedding));
            concept.setUpdatedAt(now);
        }
    }

    private void mergeExistingNoteIds(String userId, List<Concept> freshConcepts) throws Exception {
        List<Concept> existing = awaitConcepts(userId);
        java.util.Map<String, Concept> byName = new java.util.HashMap<>();
        for (Concept concept : existing) {
            if (concept.getName() != null) {
                byName.put(concept.getName().toLowerCase(Locale.US), concept);
            }
        }
        for (Concept fresh : freshConcepts) {
            if (fresh.getName() == null) {
                continue;
            }
            Concept prior = byName.get(fresh.getName().toLowerCase(Locale.US));
            if (prior == null || prior.getNoteIds() == null) {
                continue;
            }
            Set<String> merged = new HashSet<>();
            if (fresh.getNoteIds() != null) {
                merged.addAll(fresh.getNoteIds());
            }
            merged.addAll(prior.getNoteIds());
            fresh.setNoteIds(new ArrayList<>(merged));
            if ((fresh.getDefinition() == null || fresh.getDefinition().isEmpty())
                    && prior.getDefinition() != null) {
                fresh.setDefinition(prior.getDefinition());
            }
        }
    }

    private List<Concept> awaitConcepts(String userId) throws Exception {
        final Object lock = new Object();
        final List<Concept>[] existing = new List[]{null};
        final Exception[] error = new Exception[1];
        conceptRepository.listConcepts(userId, new ListCallback<Concept>() {
            @Override
            public void onSuccess(List<Concept> items) {
                synchronized (lock) {
                    existing[0] = items == null ? new ArrayList<>() : items;
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(Exception exception) {
                synchronized (lock) {
                    error[0] = exception;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            while (existing[0] == null && error[0] == null) {
                lock.wait();
            }
        }
        if (error[0] != null) {
            throw error[0];
        }
        return existing[0];
    }

    private List<ConceptEdge> suggestLinks(String userId, List<Concept> freshConcepts) throws Exception {
        List<Concept> existing = awaitConcepts(userId);
        List<ConceptEdge> suggestions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Concept left : freshConcepts) {
            float[] leftVec = VectorMath.toFloatArray(left.getEmbedding());
            if (leftVec.length == 0) {
                continue;
            }
            leftVec = VectorMath.l2Normalize(leftVec);
            Concept best = null;
            double bestScore = 0.72;
            for (Concept right : existing) {
                if (right.getName() == null || left.getName() == null) {
                    continue;
                }
                if (right.getName().equalsIgnoreCase(left.getName())) {
                    continue;
                }
                float[] rightVec = VectorMath.toFloatArray(right.getEmbedding());
                if (rightVec.length == 0) {
                    continue;
                }
                double score = VectorMath.cosineSimilarity(leftVec, VectorMath.l2Normalize(rightVec));
                if (score > bestScore) {
                    bestScore = score;
                    best = right;
                }
            }
            if (best != null) {
                String key = left.getName().toLowerCase(Locale.US) + "|" + best.getName().toLowerCase(Locale.US);
                if (seen.add(key)) {
                    ConceptEdge edge = new ConceptEdge();
                    edge.setFromName(left.getName());
                    edge.setToName(best.getName());
                    edge.setRelation("relatedTo");
                    suggestions.add(edge);
                }
            }
        }
        return suggestions;
    }
}
