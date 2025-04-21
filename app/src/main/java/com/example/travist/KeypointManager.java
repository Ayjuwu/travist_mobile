package com.example.travist;

import java.util.ArrayList;
import java.util.List;

public class KeypointManager {

    // La liste des keypoints actuels
    private static List<Keypoint> currentKeypoints = new ArrayList<>();

    // Private constructor pour empêcher l'instanciation
    private KeypointManager() {}

    // Récupérer la liste des keypoints actuels
    public static List<Keypoint> getCurrentKeypoints() {
        return currentKeypoints;
    }

    // Ajouter un keypoint à la liste
    public static void addKeypoint(Keypoint keypoint) {
        currentKeypoints.add(keypoint);
    }

    // Supprimer un keypoint de la liste
    public static void removeKeypoint(Keypoint keypoint) {
        currentKeypoints.remove(keypoint);
    }

    // Vider la liste des keypoints
    public static void clearKeypoints() {
        currentKeypoints.clear();
    }

    public static void addKeypoints(List<Keypoint> keypoints) {
        if (keypoints != null) {
            for (Keypoint kp : keypoints) {
                if (!currentKeypoints.contains(kp)) {
                    currentKeypoints.add(kp);
                }
            }
        }
    }
}
