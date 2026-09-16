package net.runelite.client.plugins.microbot.util.dialogues;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2DialogueTest {
    @Test
    public void isContinuePromptTextAcceptsContinuePrompt() {
        assertTrue(Rs2Dialogue.isContinuePromptText("<col=ffffff>Click here to continue</col>"));
    }

    @Test
    public void isContinuePromptTextRejectsChatboxInputs() {
        assertFalse(Rs2Dialogue.isContinuePromptText("Enter amount:"));
        assertFalse(Rs2Dialogue.isContinuePromptText("Search"));
        assertFalse(Rs2Dialogue.isContinuePromptText("abyssal whip"));
    }

    @Test
    public void isQuestStartQuestionAcceptsVariousQuestPrompts() {
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Start the Dig Site quest?", "The Dig Site"));
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Start the Digsite quest?", "The Dig Site"));
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Start the dig site quest", "The Dig Site"));
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Start the Romeo & Juliet quest?", "Romeo & Juliet"));
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Would you like to start the Cook's Assistant quest?", "Cook's Assistant"));
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Start The Dig Site?", "The Dig Site"));
        assertTrue(Rs2Dialogue.isQuestStartQuestion("Do you want to start the quest?", null));
    }

    @Test
    public void isQuestStartQuestionRejectsNonQuestPrompts() {
        assertFalse(Rs2Dialogue.isQuestStartQuestion("Would you like to start a fire?", null));
        assertFalse(Rs2Dialogue.isQuestStartQuestion("What would you like to say?", null));
        assertFalse(Rs2Dialogue.isQuestStartQuestion("Select an Option", null));
        assertFalse(Rs2Dialogue.isQuestStartQuestion(null, null));
    }
}
