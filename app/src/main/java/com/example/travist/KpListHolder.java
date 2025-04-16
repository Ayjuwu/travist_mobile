package com.example.travist;

import java.util.ArrayList;
import java.util.List;

public class KpListHolder {
    // Liste des keypoints sélectionnés
    public static List<Keypoint> selectedKeypoints = new ArrayList<>();

    public static void addKeypoint(Keypoint kp) {
        // On vérifie qu’on n'ajout pas un keypoint deux fois
        for (Keypoint existing : selectedKeypoints) {
            if (existing.id == kp.id) {
                return;
            }
        }
        selectedKeypoints.add(kp);
    }

    public static void resetKeypoints() {
        selectedKeypoints.clear();
        // Réinitialise également les maps de dates dans l'adaptateur
        SelectedKeypointsAdapter.visitStartDates.clear();
        SelectedKeypointsAdapter.visitEndDates.clear();
    }

    public static float calculateIndividualPrice() {
        float sum = 0f;
        for (Keypoint selected : selectedKeypoints) {
            sum += selected.price;
        }
        return sum;
    }

    public static float calculateTotalPrice(int np) {
        return calculateIndividualPrice() * np;
    }
}
