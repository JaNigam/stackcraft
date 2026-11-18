package dev.stackcraft.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-first, enforced: the set of {@code METHOD path} pairs in docs/api/openapi.yaml must equal the set the
 * controllers actually serve under /api/v1. Add an endpoint without documenting it (or the reverse) and this fails.
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenApiContractTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void controllersMatchTheContract() {
        JsonNode spec = YAMLMapper.builder().build().readTree(Path.of("../../docs/api/openapi.yaml").toFile());
        Set<String> documented = new TreeSet<>();
        for (var path : spec.get("paths").properties()) {
            for (var op : path.getValue().properties()) {
                if (HTTP_METHODS.contains(op.getKey())) documented.add(op.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey());
            }
        }

        Set<String> served = new TreeSet<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith("/api/v1")) continue;
                info.getMethodsCondition().getMethods().forEach(m -> served.add(m.name() + " " + pattern));
            }
        }

        assertThat(served).as("served by controllers").isEqualTo(documented);
    }
}
