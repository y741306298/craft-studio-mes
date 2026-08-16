package com.mes.application.command.orderPreprocessing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppOrderPreprocessingServiceTest {

    @Test
    void ignoresAlgorithmPieceWhenWidthIsLessThanTwoCentimeters() {
        assertTrue(AppOrderPreprocessingService.shouldIgnoreAlgorithmPiece(19.99D, 20D));
    }

    @Test
    void ignoresAlgorithmPieceWhenHeightIsLessThanTwoCentimeters() {
        assertTrue(AppOrderPreprocessingService.shouldIgnoreAlgorithmPiece(20D, 19.99D));
    }

    @Test
    void keepsAlgorithmPieceWhenBothDimensionsAreAtLeastTwoCentimeters() {
        assertFalse(AppOrderPreprocessingService.shouldIgnoreAlgorithmPiece(20D, 20D));
    }

    @Test
    void keepsAlgorithmPieceWhenDimensionsCannotBeResolved() {
        assertFalse(AppOrderPreprocessingService.shouldIgnoreAlgorithmPiece(null, null));
    }
}
