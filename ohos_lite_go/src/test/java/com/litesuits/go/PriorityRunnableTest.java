package com.litesuits.go;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PriorityRunnableTest {

    @Test
    public void testGetPriority() {
        PriorityRunnable priorityRunnable = new PriorityRunnable(10) {
            @Override
            public void run() {

            }
        };
        assertEquals(10, priorityRunnable.getPriority());
    }

    @Test
    public void testSetPriority() {
        PriorityRunnable priorityRunnable = new PriorityRunnable(10) {
            @Override
            public void run() {

            }
        };
        priorityRunnable.setPriority(5);
        assertEquals(5, priorityRunnable.getPriority());

    }

}
