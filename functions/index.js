const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");
const {GoogleGenerativeAI} = require("@google/generative-ai");

admin.initializeApp();
setGlobalOptions({region: "asia-southeast1"});

const geminiApiKey = defineSecret("GEMINI_API_KEY");
const MAX_CONTENT_CHARS = 12000;
const MODEL_NAME = "gemini-2.0-flash";

/**
 * Callable AI gateway for Phase 1 structured note summaries.
 * Auth required. Prefer noteId so the server loads authoritative Firestore content.
 */
exports.summarizeNote = onCall(
    {
      secrets: [geminiApiKey],
      timeoutSeconds: 60,
      memory: "256MiB",
    },
    async (request) => {
      if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Sign in required to summarize notes.");
      }

      const uid = request.auth.uid;
      const data = request.data || {};
      const locale = sanitizeText(data.locale, 32) || "en";
      const noteId = sanitizeText(data.noteId, 128);

      const noteContext = await resolveNoteContext(uid, noteId, data);
      if (!noteContext.content) {
        throw new HttpsError(
            "invalid-argument",
            "Note content is empty. Save note content before summarizing.",
        );
      }

      const apiKey = resolveApiKey();
      if (!apiKey) {
        throw new HttpsError(
            "failed-precondition",
            "GEMINI_API_KEY is not configured on the AI gateway.",
        );
      }

      try {
        const structured = await generateStructuredSummary({
          apiKey,
          locale,
          title: noteContext.title,
          category: noteContext.category,
          content: noteContext.content,
        });

        return {
          summaryText: structured.summaryText,
          bullets: structured.bullets,
          keyTerms: structured.keyTerms,
          confidence: structured.confidence,
          source: "gemini-cloud",
          model: MODEL_NAME,
          noteId: noteContext.noteId || null,
          generatedAt: Date.now(),
        };
      } catch (error) {
        console.error("summarizeNote failed", error);
        if (error instanceof HttpsError) {
          throw error;
        }
        throw new HttpsError(
            "internal",
            error && error.message ? error.message : "Unable to generate summary.",
        );
      }
    },
);

/**
 * Grounded Q&A over locally retrieved note passages (Phase 2 RAG).
 * Client performs embed/rerank on-device, then sends passages here for generation.
 * Passages are ownership-verified against the caller's notes before generation.
 */
exports.answerFromNotes = onCall(
    {
      secrets: [geminiApiKey],
      timeoutSeconds: 60,
      memory: "256MiB",
    },
    async (request) => {
      if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Sign in required to ask notes.");
      }

      const uid = request.auth.uid;
      const data = request.data || {};
      const question = sanitizeText(data.question, 1000);
      const locale = sanitizeText(data.locale, 32) || "en";
      const rawPassages = normalizePassages(data.passages);
      const verified = await verifyOwnedPassages(uid, rawPassages);

      if (!question) {
        throw new HttpsError("invalid-argument", "Question is required.");
      }
      if (verified.passages.length === 0) {
        throw new HttpsError(
            "invalid-argument",
            verified.rejected > 0
              ? "All passages were rejected (notes not found for this account)."
              : "At least one passage is required.",
        );
      }

      const apiKey = resolveApiKey();
      if (!apiKey) {
        throw new HttpsError(
            "failed-precondition",
            "GEMINI_API_KEY is not configured on the AI gateway.",
        );
      }

      try {
        const result = await generateGroundedAnswer({
          apiKey,
          locale,
          question,
          passages: verified.passages,
        });
        return {
          answer: result.answer,
          citations: result.citations,
          source: "gemini-cloud",
          model: MODEL_NAME,
          generatedAt: Date.now(),
          rejectedPassages: verified.rejected,
        };
      } catch (error) {
        console.error("answerFromNotes failed", error);
        if (error instanceof HttpsError) {
          throw error;
        }
        throw new HttpsError(
            "internal",
            error && error.message ? error.message : "Unable to answer from notes.",
        );
      }
    },
);

/**
 * Multi-turn grounded study tutor.
 */
exports.tutorChat = onCall(
    {
      secrets: [geminiApiKey],
      timeoutSeconds: 90,
      memory: "256MiB",
    },
    async (request) => {
      requireAuth(request);
      const uid = request.auth.uid;
      const data = request.data || {};
      const locale = sanitizeText(data.locale, 32) || "en";
      const messages = normalizeMessages(data.messages);
      const verified = await verifyOwnedPassages(uid, normalizePassages(data.passages));

      if (messages.length === 0) {
        throw new HttpsError("invalid-argument", "At least one chat message is required.");
      }
      if (verified.passages.length === 0) {
        throw new HttpsError("invalid-argument", "At least one owned passage is required.");
      }

      const apiKey = requireApiKey();
      try {
        const result = await generateTutorReply({
          apiKey,
          locale,
          messages,
          passages: verified.passages,
        });
        return {
          answer: result.answer,
          citations: result.citations,
          suggestedFollowUps: result.suggestedFollowUps,
          source: "gemini-cloud",
          model: MODEL_NAME,
          generatedAt: Date.now(),
          rejectedPassages: verified.rejected,
        };
      } catch (error) {
        console.error("tutorChat failed", error);
        throw toHttpsError(error, "Unable to continue tutor chat.");
      }
    },
);

/**
 * Multi-note exam prep synthesizer.
 */
exports.synthesizeExamPrep = onCall(
    {
      secrets: [geminiApiKey],
      timeoutSeconds: 90,
      memory: "256MiB",
    },
    async (request) => {
      requireAuth(request);
      const uid = request.auth.uid;
      const data = request.data || {};
      const locale = sanitizeText(data.locale, 32) || "en";
      const mode = sanitizeText(data.mode, 40) || "outline";
      const topic = sanitizeText(data.topic, 200);
      const verified = await verifyOwnedPassages(uid, normalizePassages(data.passages, 12));

      if (verified.passages.length === 0) {
        throw new HttpsError("invalid-argument", "At least one owned passage is required.");
      }
      if (!["outline", "cheatsheet", "practice_outline"].includes(mode)) {
        throw new HttpsError("invalid-argument", "mode must be outline, cheatsheet, or practice_outline.");
      }

      const apiKey = requireApiKey();
      try {
        const result = await generateExamPrep({
          apiKey,
          locale,
          mode,
          topic,
          passages: verified.passages,
        });
        return {
          title: result.title,
          mode,
          sections: result.sections,
          citations: result.citations,
          coverageNoteIds: result.coverageNoteIds,
          source: "gemini-cloud",
          model: MODEL_NAME,
          generatedAt: Date.now(),
          rejectedPassages: verified.rejected,
        };
      } catch (error) {
        console.error("synthesizeExamPrep failed", error);
        throw toHttpsError(error, "Unable to synthesize exam prep.");
      }
    },
);

/**
 * Grounded quiz / flashcard generation.
 */
exports.generateQuiz = onCall(
    {
      secrets: [geminiApiKey],
      timeoutSeconds: 90,
      memory: "256MiB",
    },
    async (request) => {
      requireAuth(request);
      const uid = request.auth.uid;
      const data = request.data || {};
      const locale = sanitizeText(data.locale, 32) || "en";
      const questionCount = clampInt(data.questionCount, 3, 10, 5);
      const types = normalizeQuizTypes(data.types);
      const verified = await verifyOwnedPassages(uid, normalizePassages(data.passages, 10));

      if (verified.passages.length === 0) {
        throw new HttpsError("invalid-argument", "At least one owned passage is required.");
      }

      const apiKey = requireApiKey();
      try {
        const result = await generateQuizQuestions({
          apiKey,
          locale,
          questionCount,
          types,
          passages: verified.passages,
        });
        return {
          questions: result.questions,
          source: "gemini-cloud",
          model: MODEL_NAME,
          generatedAt: Date.now(),
          rejectedPassages: verified.rejected,
        };
      } catch (error) {
        console.error("generateQuiz failed", error);
        throw toHttpsError(error, "Unable to generate quiz.");
      }
    },
);

/**
 * Concept extraction for knowledge graph.
 */
exports.extractConcepts = onCall(
    {
      secrets: [geminiApiKey],
      timeoutSeconds: 90,
      memory: "256MiB",
    },
    async (request) => {
      requireAuth(request);
      const uid = request.auth.uid;
      const data = request.data || {};
      const locale = sanitizeText(data.locale, 32) || "en";
      const noteId = sanitizeText(data.noteId, 128);
      const title = sanitizeText(data.title, 200);
      const content = truncate(sanitizeText(data.content, MAX_CONTENT_CHARS), MAX_CONTENT_CHARS);

      if (noteId) {
        const owned = await noteExists(uid, noteId);
        if (!owned) {
          throw new HttpsError("not-found", "Note not found for this account.");
        }
      }
      if (!content) {
        throw new HttpsError("invalid-argument", "Note content is required.");
      }

      const apiKey = requireApiKey();
      try {
        const result = await generateConcepts({
          apiKey,
          locale,
          noteId,
          title,
          content,
        });
        return {
          concepts: result.concepts,
          edges: result.edges,
          source: "gemini-cloud",
          model: MODEL_NAME,
          generatedAt: Date.now(),
          noteId: noteId || null,
        };
      } catch (error) {
        console.error("extractConcepts failed", error);
        throw toHttpsError(error, "Unable to extract concepts.");
      }
    },
);

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign in required.");
  }
}

function requireApiKey() {
  const apiKey = resolveApiKey();
  if (!apiKey) {
    throw new HttpsError(
        "failed-precondition",
        "GEMINI_API_KEY is not configured on the AI gateway.",
    );
  }
  return apiKey;
}

function toHttpsError(error, fallback) {
  if (error instanceof HttpsError) {
    return error;
  }
  return new HttpsError("internal", error && error.message ? error.message : fallback);
}

function resolveApiKey() {
  try {
    const secretValue = geminiApiKey.value();
    if (secretValue && String(secretValue).trim()) {
      return String(secretValue).trim();
    }
  } catch (error) {
    // Emulator / missing secret binding may throw; fall through to env.
  }
  const envValue = process.env.GEMINI_API_KEY;
  return envValue && envValue.trim() ? envValue.trim() : null;
}

async function resolveNoteContext(uid, noteId, data) {
  if (noteId) {
    const snapshot = await admin
        .firestore()
        .collection("users")
        .doc(uid)
        .collection("notes")
        .doc(noteId)
        .get();

    if (!snapshot.exists) {
      throw new HttpsError("not-found", "Note not found for this account.");
    }

    const note = snapshot.data() || {};
    const content = truncate(sanitizeText(note.content, MAX_CONTENT_CHARS), MAX_CONTENT_CHARS);
    return {
      noteId,
      title: sanitizeText(note.title, 200) || sanitizeText(data.title, 200) || "",
      category: sanitizeText(note.category, 80) || sanitizeText(data.category, 80) || "General",
      content,
    };
  }

  const content = truncate(sanitizeText(data.content, MAX_CONTENT_CHARS), MAX_CONTENT_CHARS);
  return {
    noteId: null,
    title: sanitizeText(data.title, 200) || "",
    category: sanitizeText(data.category, 80) || "General",
    content,
  };
}

async function noteExists(uid, noteId) {
  if (!noteId) {
    return false;
  }
  const snapshot = await admin
      .firestore()
      .collection("users")
      .doc(uid)
      .collection("notes")
      .doc(noteId)
      .get();
  return snapshot.exists;
}

async function verifyOwnedPassages(uid, passages) {
  const uniqueIds = [...new Set(passages.map((passage) => passage.noteId).filter(Boolean))];
  const owned = new Set();
  await Promise.all(uniqueIds.map(async (noteId) => {
    if (await noteExists(uid, noteId)) {
      owned.add(noteId);
    }
  }));

  const kept = [];
  let rejected = 0;
  for (const passage of passages) {
    if (!owned.has(passage.noteId)) {
      rejected += 1;
      continue;
    }
    kept.push(passage);
  }
  return {passages: kept, rejected};
}

async function generateStructuredSummary({apiKey, locale, title, category, content}) {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    generationConfig: {
      temperature: 0.2,
      responseMimeType: "application/json",
    },
  });

  const prompt = [
    "You are StudyMate, an academic study-note assistant.",
    "Summarize the note for a university student.",
    "Respond with ONLY valid JSON matching this schema:",
    "{",
    '  "summaryText": string,   // 2-4 sentence overview',
    '  "bullets": string[],     // 3-5 concise key points',
    '  "keyTerms": string[],    // 3-8 important terms/concepts',
    '  "confidence": number     // 0 to 1, how complete the note content is',
    "}",
    "Rules:",
    "- Do not invent facts that are not supported by the note.",
    "- Keep bullets short and study-friendly.",
    "- Prefer the student's language when possible; locale hint: " + locale,
    "",
    "Title: " + (title || "(untitled)"),
    "Category: " + (category || "General"),
    "Note content:",
    content,
  ].join("\n");

  const result = await model.generateContent(prompt);
  const text = result && result.response ? result.response.text() : "";
  return parseStructuredSummary(text);
}

async function generateGroundedAnswer({apiKey, locale, question, passages}) {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    generationConfig: {
      temperature: 0.2,
      responseMimeType: "application/json",
    },
  });

  const passageBlock = formatPassageBlock(passages);
  const prompt = [
    "You are StudyMate, a grounded study tutor.",
    "Answer the student using ONLY the provided passages.",
    "If the passages are insufficient, say you cannot find that in the notes.",
    "Respond with ONLY valid JSON matching this schema:",
    "{",
    '  "answer": string,',
    '  "citations": [{"noteId": string, "title": string, "excerpt": string}]',
    "}",
    "Rules:",
    "- Every factual claim must be supported by a passage.",
    "- citations.noteId must come from the passages.",
    "- excerpts must be short quotes or paraphrases from those passages.",
    "- Prefer the student's language; locale hint: " + locale,
    "",
    "Question:",
    question,
    "",
    "Passages:",
    passageBlock,
  ].join("\n");

  const result = await model.generateContent(prompt);
  const text = result && result.response ? result.response.text() : "";
  return parseGroundedAnswer(text, passages);
}

async function generateTutorReply({apiKey, locale, messages, passages}) {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    generationConfig: {
      temperature: 0.3,
      responseMimeType: "application/json",
    },
  });

  const historyBlock = messages.map((message) => {
    return message.role.toUpperCase() + ": " + message.content;
  }).join("\n");

  const prompt = [
    "You are StudyMate, a multi-turn grounded study tutor.",
    "Continue the conversation using ONLY the provided passages plus chat history.",
    "Do not invent facts outside the passages.",
    "Respond with ONLY valid JSON:",
    "{",
    '  "answer": string,',
    '  "citations": [{"noteId": string, "title": string, "excerpt": string}],',
    '  "suggestedFollowUps": string[]  // 2-3 short follow-up questions',
    "}",
    "Prefer the student's language; locale hint: " + locale,
    "",
    "Chat history:",
    historyBlock,
    "",
    "Passages:",
    formatPassageBlock(passages),
  ].join("\n");

  const result = await model.generateContent(prompt);
  const text = result && result.response ? result.response.text() : "";
  const grounded = parseGroundedAnswer(text, passages);
  const parsed = safeJsonParse(text);
  const suggestedFollowUps = asStringArray(parsed.suggestedFollowUps, 3, 120);
  return {
    answer: grounded.answer,
    citations: grounded.citations,
    suggestedFollowUps,
  };
}

async function generateExamPrep({apiKey, locale, mode, topic, passages}) {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    generationConfig: {
      temperature: 0.25,
      responseMimeType: "application/json",
    },
  });

  const prompt = [
    "You are StudyMate, an exam-prep synthesizer.",
    "Create a " + mode + " from ONLY the provided passages.",
    "Respond with ONLY valid JSON:",
    "{",
    '  "title": string,',
    '  "sections": [{"heading": string, "bullets": string[]}],',
    '  "citations": [{"noteId": string, "title": string, "excerpt": string}],',
    '  "coverageNoteIds": string[]',
    "}",
    "Rules:",
    "- 3-8 sections, study-friendly bullets.",
    "- coverageNoteIds and citation noteIds must come from passages.",
    "- Prefer student language; locale: " + locale,
    "- Topic focus (may be empty): " + (topic || "(general)"),
    "",
    "Passages:",
    formatPassageBlock(passages),
  ].join("\n");

  const result = await model.generateContent(prompt);
  const text = result && result.response ? result.response.text() : "";
  return parseExamPrep(text, passages);
}

async function generateQuizQuestions({apiKey, locale, questionCount, types, passages}) {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    generationConfig: {
      temperature: 0.35,
      responseMimeType: "application/json",
    },
  });

  const prompt = [
    "You are StudyMate, a grounded quiz generator.",
    "Create " + questionCount + " study questions from ONLY the passages.",
    "Allowed types: " + types.join(", "),
    "Respond with ONLY valid JSON:",
    "{",
    '  "questions": [{',
    '    "type": "mcq" | "short",',
    '    "stem": string,',
    '    "choices": string[],',
    '    "answer": string,',
    '    "explanation": string,',
    '    "sourceNoteId": string,',
    '    "sourceChunkId": string',
    "  }]",
    "}",
    "Rules:",
    "- Every question must cite a sourceNoteId from passages.",
    "- For mcq provide exactly 4 choices and one correct answer.",
    "- Prefer student language; locale: " + locale,
    "",
    "Passages:",
    formatPassageBlock(passages),
  ].join("\n");

  const result = await model.generateContent(prompt);
  const text = result && result.response ? result.response.text() : "";
  return parseQuiz(text, passages);
}

async function generateConcepts({apiKey, locale, noteId, title, content}) {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    generationConfig: {
      temperature: 0.2,
      responseMimeType: "application/json",
    },
  });

  const prompt = [
    "You are StudyMate, a knowledge-graph builder for study notes.",
    "Extract concepts and relations from the note.",
    "Respond with ONLY valid JSON:",
    "{",
    '  "concepts": [{"name": string, "definition": string, "importance": number}],',
    '  "edges": [{"from": string, "to": string, "relation": "relatedTo"|"prerequisiteOf"}]',
    "}",
    "Rules:",
    "- 3-12 concepts, short definitions grounded in the note.",
    "- importance from 0 to 1.",
    "- Prefer student language; locale: " + locale,
    "",
    "noteId: " + (noteId || ""),
    "Title: " + (title || "(untitled)"),
    "Content:",
    content,
  ].join("\n");

  const result = await model.generateContent(prompt);
  const text = result && result.response ? result.response.text() : "";
  return parseConcepts(text);
}

function formatPassageBlock(passages) {
  return passages.map((passage, index) => {
    return [
      "[P" + (index + 1) + "]",
      "noteId: " + passage.noteId,
      "chunkId: " + (passage.chunkId || ""),
      "title: " + (passage.title || "(untitled)"),
      passage.text,
    ].join("\n");
  }).join("\n\n");
}

function normalizePassages(rawPassages, maxItems) {
  const limit = maxItems || 6;
  if (!Array.isArray(rawPassages)) {
    return [];
  }
  const passages = [];
  for (const item of rawPassages) {
    if (!item || typeof item !== "object") {
      continue;
    }
    const noteId = sanitizeText(item.noteId, 128);
    const text = sanitizeText(item.text, 2000);
    if (!noteId || !text) {
      continue;
    }
    passages.push({
      noteId,
      chunkId: sanitizeText(item.chunkId, 128),
      title: sanitizeText(item.title, 200),
      text,
    });
    if (passages.length >= limit) {
      break;
    }
  }
  return passages;
}

function normalizeMessages(rawMessages) {
  if (!Array.isArray(rawMessages)) {
    return [];
  }
  const messages = [];
  for (const item of rawMessages) {
    if (!item || typeof item !== "object") {
      continue;
    }
    const role = sanitizeText(item.role, 16).toLowerCase();
    const content = sanitizeText(item.content, 2000);
    if ((role !== "user" && role !== "assistant") || !content) {
      continue;
    }
    messages.push({role, content});
    if (messages.length >= 12) {
      break;
    }
  }
  return messages;
}

function normalizeQuizTypes(rawTypes) {
  const allowed = new Set(["mcq", "short"]);
  const types = [];
  if (Array.isArray(rawTypes)) {
    for (const item of rawTypes) {
      const type = sanitizeText(item, 16).toLowerCase();
      if (allowed.has(type) && !types.includes(type)) {
        types.push(type);
      }
    }
  }
  return types.length > 0 ? types : ["mcq", "short"];
}

function parseGroundedAnswer(rawText, passages) {
  const parsed = safeJsonParse(rawText);
  const answer = sanitizeText(parsed.answer, 4000);
  if (!answer) {
    throw new Error("Model returned an empty answer.");
  }

  const allowedNoteIds = new Set(passages.map((passage) => passage.noteId));
  const titleByNoteId = {};
  for (const passage of passages) {
    titleByNoteId[passage.noteId] = passage.title || "";
  }

  const citations = [];
  if (Array.isArray(parsed.citations)) {
    for (const item of parsed.citations) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const noteId = sanitizeText(item.noteId, 128);
      if (!noteId || !allowedNoteIds.has(noteId)) {
        continue;
      }
      citations.push({
        noteId,
        title: sanitizeText(item.title, 200) || titleByNoteId[noteId] || "",
        excerpt: sanitizeText(item.excerpt, 280),
      });
      if (citations.length >= 5) {
        break;
      }
    }
  }

  if (citations.length === 0 && passages.length > 0) {
    citations.push({
      noteId: passages[0].noteId,
      title: passages[0].title || "",
      excerpt: sanitizeText(passages[0].text, 180),
    });
  }

  return {answer, citations};
}

function parseExamPrep(rawText, passages) {
  const parsed = safeJsonParse(rawText);
  const title = sanitizeText(parsed.title, 200) || "Exam prep";
  const allowedNoteIds = new Set(passages.map((passage) => passage.noteId));
  const sections = [];
  if (Array.isArray(parsed.sections)) {
    for (const item of parsed.sections) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const heading = sanitizeText(item.heading, 160);
      const bullets = asStringArray(item.bullets, 8, 280);
      if (!heading || bullets.length === 0) {
        continue;
      }
      sections.push({heading, bullets});
      if (sections.length >= 8) {
        break;
      }
    }
  }
  if (sections.length === 0) {
    sections.push({
      heading: "Key points",
      bullets: [sanitizeText(passages[0].text, 240)],
    });
  }

  const grounded = parseGroundedAnswer(JSON.stringify({
    answer: title,
    citations: parsed.citations,
  }), passages);

  const coverageNoteIds = [];
  if (Array.isArray(parsed.coverageNoteIds)) {
    for (const item of parsed.coverageNoteIds) {
      const noteId = sanitizeText(item, 128);
      if (noteId && allowedNoteIds.has(noteId) && !coverageNoteIds.includes(noteId)) {
        coverageNoteIds.push(noteId);
      }
    }
  }
  if (coverageNoteIds.length === 0) {
    for (const passage of passages) {
      if (!coverageNoteIds.includes(passage.noteId)) {
        coverageNoteIds.push(passage.noteId);
      }
    }
  }

  return {
    title,
    sections,
    citations: grounded.citations,
    coverageNoteIds,
  };
}

function parseQuiz(rawText, passages) {
  const parsed = safeJsonParse(rawText);
  const allowedNoteIds = new Set(passages.map((passage) => passage.noteId));
  const chunkByNote = {};
  for (const passage of passages) {
    if (!chunkByNote[passage.noteId]) {
      chunkByNote[passage.noteId] = passage.chunkId || "";
    }
  }
  const questions = [];
  if (Array.isArray(parsed.questions)) {
    for (const item of parsed.questions) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const type = sanitizeText(item.type, 16).toLowerCase() || "short";
      const stem = sanitizeText(item.stem, 400);
      const answer = sanitizeText(item.answer, 400);
      const sourceNoteId = sanitizeText(item.sourceNoteId, 128);
      if (!stem || !answer || !sourceNoteId || !allowedNoteIds.has(sourceNoteId)) {
        continue;
      }
      const choices = asStringArray(item.choices, 4, 200);
      questions.push({
        type: type === "mcq" ? "mcq" : "short",
        stem,
        choices: type === "mcq" ? choices : [],
        answer,
        explanation: sanitizeText(item.explanation, 400),
        sourceNoteId,
        sourceChunkId: sanitizeText(item.sourceChunkId, 128) || chunkByNote[sourceNoteId] || "",
      });
      if (questions.length >= 10) {
        break;
      }
    }
  }
  if (questions.length === 0) {
    throw new Error("Model returned no valid grounded quiz questions.");
  }
  return {questions};
}

function parseConcepts(rawText) {
  const parsed = safeJsonParse(rawText);
  const concepts = [];
  const nameSet = new Set();
  if (Array.isArray(parsed.concepts)) {
    for (const item of parsed.concepts) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const name = sanitizeText(item.name, 80);
      if (!name || nameSet.has(name.toLowerCase())) {
        continue;
      }
      nameSet.add(name.toLowerCase());
      let importance = Number(item.importance);
      if (Number.isNaN(importance)) {
        importance = 0.5;
      }
      concepts.push({
        name,
        definition: sanitizeText(item.definition, 280),
        importance: Math.max(0, Math.min(1, importance)),
      });
      if (concepts.length >= 12) {
        break;
      }
    }
  }
  const edges = [];
  if (Array.isArray(parsed.edges)) {
    for (const item of parsed.edges) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const from = sanitizeText(item.from, 80);
      const to = sanitizeText(item.to, 80);
      let relation = sanitizeText(item.relation, 32) || "relatedTo";
      if (relation !== "prerequisiteOf") {
        relation = "relatedTo";
      }
      if (!from || !to || from.toLowerCase() === to.toLowerCase()) {
        continue;
      }
      edges.push({from, to, relation});
      if (edges.length >= 20) {
        break;
      }
    }
  }
  if (concepts.length === 0) {
    throw new Error("Model returned no concepts.");
  }
  return {concepts, edges};
}

function parseStructuredSummary(rawText) {
  const parsed = safeJsonParse(rawText);
  const summaryText = sanitizeText(parsed.summaryText, 1200);
  if (!summaryText) {
    throw new Error("Model returned empty summaryText.");
  }

  const bullets = asStringArray(parsed.bullets, 5, 240);
  const keyTerms = asStringArray(parsed.keyTerms, 8, 48);
  let confidence = Number(parsed.confidence);
  if (Number.isNaN(confidence)) {
    confidence = 0.7;
  }
  confidence = Math.max(0, Math.min(1, confidence));

  return {
    summaryText,
    bullets: bullets.length > 0 ? bullets : [summaryText],
    keyTerms,
    confidence,
  };
}

function safeJsonParse(rawText) {
  if (!rawText || !String(rawText).trim()) {
    throw new Error("Model returned an empty response.");
  }
  const text = String(rawText).trim();
  try {
    return JSON.parse(text);
  } catch (error) {
    const start = text.indexOf("{");
    const end = text.lastIndexOf("}");
    if (start >= 0 && end > start) {
      return JSON.parse(text.slice(start, end + 1));
    }
    throw new Error("Model returned invalid JSON.");
  }
}

function asStringArray(value, maxItems, maxLength) {
  if (!Array.isArray(value)) {
    return [];
  }
  const items = [];
  for (const item of value) {
    const text = sanitizeText(item, maxLength);
    if (text) {
      items.push(text);
    }
    if (items.length >= maxItems) {
      break;
    }
  }
  return items;
}

function clampInt(value, min, max, fallback) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return fallback;
  }
  return Math.max(min, Math.min(max, Math.round(number)));
}

function sanitizeText(value, maxLength) {
  if (value === null || value === undefined) {
    return "";
  }
  const text = String(value).replace(/\u0000/g, "").trim();
  if (!text) {
    return "";
  }
  if (!maxLength || text.length <= maxLength) {
    return text;
  }
  return text.slice(0, maxLength);
}

function truncate(value, maxLength) {
  if (!value || value.length <= maxLength) {
    return value || "";
  }
  return value.slice(0, maxLength);
}
