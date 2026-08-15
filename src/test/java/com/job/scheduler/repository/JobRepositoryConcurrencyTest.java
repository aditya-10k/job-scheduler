package com.job.scheduler.repository;

import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.model.JobType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scheduler-repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JobRepositoryConcurrencyTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void onlyOneConcurrentClaimSucceeds() throws Exception {
        Job job = jobRepository.saveAndFlush(Job.builder()
                .jobType(JobType.EMAIL)
                .jobStatus(JobStatus.PENDING)
                .payload(Map.of("to", "user@example.com"))
                .scheduledAt(Instant.now().minusSeconds(5))
                .maxRetries(3)
                .build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Integer> firstAttempt = executorService.submit(() -> claim(job.getId(), ready, start));
        Future<Integer> secondAttempt = executorService.submit(() -> claim(job.getId(), ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int successfulClaims = firstAttempt.get(5, TimeUnit.SECONDS) + secondAttempt.get(5, TimeUnit.SECONDS);

        assertThat(successfulClaims).isEqualTo(1);
        assertThat(jobRepository.findById(job.getId()))
                .get()
                .extracting(Job::getJobStatus)
                .isEqualTo(JobStatus.RUNNING);
    }

    private int claim(UUID jobId, CountDownLatch ready, CountDownLatch start) throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return template.execute(status -> jobRepository.claimDueJob(jobId, Instant.now()));
    }
}
