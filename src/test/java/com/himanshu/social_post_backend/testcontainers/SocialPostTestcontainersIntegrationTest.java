package com.himanshu.social_post_backend.testcontainers;

import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.repository.PostRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exp 3.2.1: Infrastructure Testing with Testcontainers.
 * Validates real containerized infrastructure behavior within the CI/CD pipeline.
 * In GitHub Actions runners, Docker is available and manages live ephemeral containers.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class SocialPostTestcontainersIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SocialPostTestcontainersIntegrationTest.class);

    @Autowired
    private PostRepository postRepository;

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379);

    @Test
    @DisplayName("Testcontainers: Validates Docker daemon connectivity and container lifecycle")
    void testDockerContainerEnvironment() {
        boolean isDockerRunning = DockerClientFactory.instance().isDockerAvailable();
        log.info("[Testcontainers] Docker daemon availability evaluated: {}", isDockerRunning);

        if (!isDockerRunning) {
            log.warn("[Testcontainers] Local Docker daemon is stopped. In GitHub Actions CI runner, Docker will launch containers automatically.");
            Assumptions.assumeTrue(false, "Docker is not available in local dev environment; skipping container launch.");
            return;
        }

        assertThat(redisContainer.isRunning()).isTrue();
        log.info("[Testcontainers] Ephemeral container running on host port: {}", redisContainer.getFirstMappedPort());
    }

    @Test
    @DisplayName("Testcontainers & Spring Data: Verifies data persistence isolation")
    void testDataPersistenceWithInfrastructure() {
        Post post = new Post(
                "Testcontainers Production Parity",
                "testcontainers-production-parity",
                "Testing against real containers eliminates environment mismatch.",
                "himanshu",
                PostStatus.PUBLISHED,
                Set.of("ci", "testcontainers", "docker")
        );

        Post saved = postRepository.save(post);
        assertThat(saved.getId()).isNotNull();

        Optional<Post> fetched = postRepository.findById(saved.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getTitle()).isEqualTo("Testcontainers Production Parity");
    }
}
