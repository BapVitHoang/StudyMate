package com.hcmute.studymate.model;

public class RetrievedChunk {
    private final NoteChunk chunk;
    private final double score;
    private final String source;

    public RetrievedChunk(NoteChunk chunk, double score, String source) {
        this.chunk = chunk;
        this.score = score;
        this.source = source;
    }

    public NoteChunk getChunk() {
        return chunk;
    }

    public double getScore() {
        return score;
    }

    public String getSource() {
        return source;
    }
}
