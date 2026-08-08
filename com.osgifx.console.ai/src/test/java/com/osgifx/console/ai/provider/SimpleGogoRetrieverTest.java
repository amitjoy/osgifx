package com.osgifx.console.ai.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.osgifx.console.ai.GogoExample;

public class SimpleGogoRetrieverTest {

    private SimpleGogoRetriever retriever;

    @BeforeEach
    public void setUp() {
        retriever = new SimpleGogoRetriever();
        // Manually activate to load the gogo-examples.txt resource
        retriever.activate();
    }

    @Test
    public void retrieveWithNullReturnsEmpty() {
        List<GogoExample> results = retriever.retrieve(null, 5);
        assertTrue(results.isEmpty(), "Results should be empty for null query");
    }

    @Test
    public void retrieveWithBlankReturnsEmpty() {
        List<GogoExample> results = retriever.retrieve("   ", 5);
        assertTrue(results.isEmpty(), "Results should be empty for blank query");
    }

    @Test
    public void retrieveWithKnownQueryReturnsResults() {
        // "bundle" is a synonym for many things and occurs in the dataset
        List<GogoExample> results = retriever.retrieve("bundle", 5);
        assertFalse(results.isEmpty(), "Results should not be empty for known query 'bundle'");
    }

    @Test
    public void retrieveRespectsMaxResults() {
        // Query "install" should have multiple matches
        List<GogoExample> results = retriever.retrieve("install", 2);
        assertTrue(results.size() <= 2, "Results should not exceed max results");
        assertFalse(results.isEmpty(), "Results should contain at least some matches");
    }

    @Test
    public void retrieveResultsOrderedByRelevance() {
        // Querying for "bundle stop" should return a highly relevant match first
        List<GogoExample> results = retriever.retrieve("bundle stop", 5);
        assertFalse(results.isEmpty(), "Should find matches for 'bundle stop'");
        
        GogoExample bestMatch = results.get(0);
        // The best match should be related to stopping a bundle
        assertTrue(bestMatch.intent().toLowerCase().contains("stop") 
                || bestMatch.commands().toLowerCase().contains("stop"),
                "The best match should be highly relevant to 'bundle stop'");
    }

    @Test
    public void retrieveWithStopWordsOnlyReturnsEmpty() {
        // "the is for a" consists strictly of stop words
        List<GogoExample> results = retriever.retrieve("the is for a", 5);
        assertTrue(results.isEmpty(), "Results should be empty when query consists only of stop words");
    }
}
