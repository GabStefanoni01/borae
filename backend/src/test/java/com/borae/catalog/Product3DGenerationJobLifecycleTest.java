package com.borae.catalog;

import com.borae.identity.UserAccount;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class Product3DGenerationJobLifecycleTest {

    private Product3DGenerationJob newQueuedJob() {
        return new Product3DGenerationJob(
                mock(MenuProduct.class),
                UUID.randomUUID(),
                mock(UserAccount.class)
        );
    }

    @Test
    void newJobStartsQueued() {
        Product3DGenerationJob job = newQueuedJob();

        assertEquals(Product3DGenerationJobStatus.QUEUED, job.getStatus());
        assertNull(job.getStartedAt());
        assertNull(job.getCompletedAt());
    }

    @Test
    void processingJobCanComplete() {
        Product3DGenerationJob job = newQueuedJob();

        job.start("triposr");
        job.complete();

        assertEquals(Product3DGenerationJobStatus.COMPLETED, job.getStatus());
        assertEquals("triposr", job.getEngine());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void processingJobCanFailWithSanitizedErrorMetadata() {
        Product3DGenerationJob job = newQueuedJob();

        job.start("triposr");
        job.fail("ENGINE_UNAVAILABLE", "O serviço de geração está temporariamente indisponível.");

        assertEquals(Product3DGenerationJobStatus.FAILED, job.getStatus());
        assertEquals("ENGINE_UNAVAILABLE", job.getErrorCode());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void queuedJobCanBeCancelledButProcessingJobCannot() {
        Product3DGenerationJob queued = newQueuedJob();
        queued.cancel();
        assertEquals(Product3DGenerationJobStatus.CANCELLED, queued.getStatus());

        Product3DGenerationJob processing = newQueuedJob();
        processing.start("triposr");
        assertThrows(IllegalStateException.class, processing::cancel);
    }

    @Test
    void terminalJobCannotBeStartedOrCompletedAgain() {
        Product3DGenerationJob job = newQueuedJob();
        job.start("triposr");
        job.complete();

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> job.start("triposr")),
                () -> assertThrows(IllegalStateException.class, job::complete),
                () -> assertThrows(IllegalStateException.class, () -> job.fail("ERROR", "failure"))
        );
    }
}
