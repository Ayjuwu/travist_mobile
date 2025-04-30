package com.example.travist;

import java.util.ArrayList;
import java.util.List;

public class KeypointManager {

    // La liste des keypoints actuels
    private static List<Keypoint> currentKeypoints = new ArrayList<>();

    // Private constructor pour empêcher l'instanciation
    private KeypointManager() {}

    // On récupère la liste des lieux actuels
    public static List<Keypoint> getCurrentKeypoints() {
        return currentKeypoints;
    }

    // On ajoute un lieu à la liste
    public static void addKeypoint(Keypoint keypoint) {
        currentKeypoints.add(keypoint);
    }

    // On supprime un lieu de la liste
    public static void removeKeypoint(Keypoint keypoint) {
        currentKeypoints.remove(keypoint);
    }

    // On la liste des lieux
    public static void clearKeypoints() {
        currentKeypoints.clear();
    }

    // Méthode pour ajouter les lieux
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
