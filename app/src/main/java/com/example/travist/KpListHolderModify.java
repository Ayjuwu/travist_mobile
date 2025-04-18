package com.example.travist;

import java.util.ArrayList;
import java.util.List;

public class KpListHolderModify {
    // Liste des keypoints sélectionnés
    public static List<Keypoint> selectedKeypointsModify = new ArrayList<>();

    public static void addKeypoint(Keypoint kp) {
        // On vérifie qu’on n'ajout pas un keypoint deux fois
        for (Keypoint existing : selectedKeypointsModify) {
            if (existing.id == kp.id) {
                return;
            }
        }
        selectedKeypointsModify.add(kp);
    }

    public static void resetKeypoints() {
        selectedKeypointsModify.clear();
        // Réinitialise également les maps de dates dans l'adaptateur
        SelectedKeypointsPlanifyAdapter.visitStartDates.clear();
        SelectedKeypointsPlanifyAdapter.visitEndDates.clear();
    }

    public static float calculateIndividualPrice() {
        float sum = 0f;
        for (Keypoint selected : selectedKeypointsModify) {
            sum += selected.price;
        }
        return sum;
    }

    public static float calculateTotalPrice(int np) {
        return calculateIndividualPrice() * np;
    }
}