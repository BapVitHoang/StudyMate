package com.hcmute.studymate.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hcmute.studymate.model.UserProfile;
import com.hcmute.studymate.utils.AuthCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class FirebaseAuthRepository implements AuthRepository {
    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public FirebaseAuthRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseAuthRepository(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> getCurrentUserProfile(callback))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void register(String email, String password, String displayName, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError(new IllegalStateException("Unable to read registered user"));
                        return;
                    }

                    String normalizedDisplayName = normalizeDisplayName(displayName, email);
                    UserProfile profile = new UserProfile(
                            firebaseUser.getUid(),
                            email,
                            normalizedDisplayName,
                            System.currentTimeMillis()
                    );
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(normalizedDisplayName)
                            .build();

                    firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> saveUserProfile(profile, callback));
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void signOut(OperationCallback callback) {
        firebaseAuth.signOut();
        callback.onSuccess();
    }

    @Override
    public UserProfile getCurrentUser() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            return null;
        }
        return buildProfileFromFirebaseUser(firebaseUser);
    }

    @Override
    public void getCurrentUserProfile(AuthCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(new IllegalStateException("User is not logged in"));
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(document -> handleUserProfileDocument(firebaseUser, document, callback))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public String getCurrentUserId() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        return firebaseUser == null ? null : firebaseUser.getUid();
    }

    private void handleUserProfileDocument(FirebaseUser firebaseUser, DocumentSnapshot document, AuthCallback callback) {
        if (document.exists()) {
            UserProfile profile = document.toObject(UserProfile.class);
            if (profile != null) {
                if (isBlank(profile.getUid())) {
                    profile.setUid(firebaseUser.getUid());
                }
                callback.onSuccess(profile);
                return;
            }
        }

        UserProfile fallbackProfile = buildProfileFromFirebaseUser(firebaseUser);
        fallbackProfile.setCreatedAt(System.currentTimeMillis());
        saveUserProfile(fallbackProfile, callback);
    }

    private void saveUserProfile(UserProfile profile, AuthCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(profile.getUid())
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(callback::onError);
    }

    private UserProfile buildProfileFromFirebaseUser(FirebaseUser firebaseUser) {
        String email = firebaseUser.getEmail();
        String displayName = normalizeDisplayName(firebaseUser.getDisplayName(), email);
        return new UserProfile(firebaseUser.getUid(), email, displayName, 0L);
    }

    private String normalizeDisplayName(String displayName, String email) {
        if (!isBlank(displayName)) {
            return displayName.trim();
        }
        if (!isBlank(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "StudyMate Student";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
