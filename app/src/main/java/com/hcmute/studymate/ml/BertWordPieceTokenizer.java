package com.hcmute.studymate.ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BERT WordPiece tokenizer compatible with all-MiniLM-L6-v2 / ms-marco MiniLM vocab.
 */
public class BertWordPieceTokenizer {
    private static final String CLS = "[CLS]";
    private static final String SEP = "[SEP]";
    private static final String UNK = "[UNK]";
    private static final String PAD = "[PAD]";
    private static final Pattern BASIC_TOKEN = Pattern.compile("[A-Za-z0-9]+|[^\\sA-Za-z0-9]");

    private final Map<String, Integer> tokenToId = new HashMap<>();
    private final int unkId;
    private final int clsId;
    private final int sepId;
    private final int padId;

    public BertWordPieceTokenizer(File vocabFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(vocabFile), StandardCharsets.UTF_8))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                String token = line.trim();
                if (!token.isEmpty()) {
                    tokenToId.put(token, index);
                }
                index++;
            }
        }
        unkId = requireToken(UNK);
        clsId = requireToken(CLS);
        sepId = requireToken(SEP);
        padId = tokenToId.containsKey(PAD) ? tokenToId.get(PAD) : 0;
    }

    public EncodedText encode(String text, int maxLength) {
        return encodePair(text, null, maxLength);
    }

    public EncodedText encodePair(String textA, String textB, int maxLength) {
        List<Integer> tokens = new ArrayList<>();
        tokens.add(clsId);
        tokens.addAll(tokenizeToIds(textA));
        tokens.add(sepId);
        if (textB != null) {
            tokens.addAll(tokenizeToIds(textB));
            tokens.add(sepId);
        }

        if (tokens.size() > maxLength) {
            tokens = new ArrayList<>(tokens.subList(0, maxLength));
            tokens.set(maxLength - 1, sepId);
        }

        long[] inputIds = new long[maxLength];
        long[] attentionMask = new long[maxLength];
        long[] tokenTypeIds = new long[maxLength];
        for (int i = 0; i < maxLength; i++) {
            if (i < tokens.size()) {
                inputIds[i] = tokens.get(i);
                attentionMask[i] = 1;
            } else {
                inputIds[i] = padId;
                attentionMask[i] = 0;
            }
        }
        return new EncodedText(inputIds, attentionMask, tokenTypeIds);
    }

    private List<Integer> tokenizeToIds(String text) {
        List<Integer> ids = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return ids;
        }
        String normalized = text.toLowerCase(Locale.US).trim();
        Matcher matcher = BASIC_TOKEN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            ids.addAll(wordPiece(token));
        }
        return ids;
    }

    private List<Integer> wordPiece(String token) {
        List<Integer> output = new ArrayList<>();
        if (tokenToId.containsKey(token)) {
            output.add(tokenToId.get(token));
            return output;
        }

        int start = 0;
        boolean isBad = false;
        List<Integer> subTokens = new ArrayList<>();
        while (start < token.length()) {
            int end = token.length();
            Integer currentId = null;
            while (start < end) {
                String substr = token.substring(start, end);
                if (start > 0) {
                    substr = "##" + substr;
                }
                if (tokenToId.containsKey(substr)) {
                    currentId = tokenToId.get(substr);
                    break;
                }
                end--;
            }
            if (currentId == null) {
                isBad = true;
                break;
            }
            subTokens.add(currentId);
            start = end;
        }

        if (isBad || subTokens.isEmpty()) {
            output.add(unkId);
        } else {
            output.addAll(subTokens);
        }
        return output;
    }

    private int requireToken(String token) {
        Integer id = tokenToId.get(token);
        if (id == null) {
            throw new IllegalStateException("Vocabulary is missing required token: " + token);
        }
        return id;
    }

    public static final class EncodedText {
        public final long[] inputIds;
        public final long[] attentionMask;
        public final long[] tokenTypeIds;

        public EncodedText(long[] inputIds, long[] attentionMask, long[] tokenTypeIds) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
            this.tokenTypeIds = tokenTypeIds;
        }
    }
}
