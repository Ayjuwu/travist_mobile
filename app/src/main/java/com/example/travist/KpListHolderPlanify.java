package com.example.travist;

import java.util.ArrayList;
import java.util.List;

public class KpListHolderPlanify {
    // Liste des keypoints sélectionnés
    public static List<Keypoint> selectedKeypointsPlanify = new ArrayList<>();

    public static void addKeypoint(Keypoint kp) {
        // On vérifie qu’on n'ajout pas un keypoint deux fois
        for (Keypoint existing : selectedKeypointsPlanify) {
            if (existing.id == kp.id) {
                return;
            }
        }
        selectedKeypointsPlanify.add(kp);
    }

    public static void resetKeypoints() {
        selectedKeypointsPlanify.clear();
        // Réinitialise également les maps de dates dans l'adaptateur
        SelectedKeypointsPlanifyAdapter.visitStartDates.clear();
        SelectedKeypointsPlanifyAdapter.visitEndDates.clear();
    }

    public static float calculateIndividualPrice() {
        float sum = 0f;
        for (Keypoint selected : selectedKeypointsPlanify) {
            sum += selected.price;
        }
        return sum;
    }

    public static float calculateTotalPrice(int np) {
        return calculateIndividualPrice() * np;
    }
}