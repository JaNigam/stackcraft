package dev.stackcraft.api;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal layering, enforced by the build. The domain is plain Java; use cases talk to ports, never adapters;
 * only adapters touch Spring MVC or JPA.
 */
@AnalyzeClasses(packages = "dev.stackcraft.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPlainJava = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "tools.jackson..", "com.fasterxml..",
                    "..adapter..", "..application..", "..port..");

    @ArchTest
    static final ArchRule useCasesDependOnPortsNotAdapters = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "..adapter..");

    @ArchTest
    static final ArchRule portsAreFrameworkFree = noClasses().that().resideInAPackage("..port..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "..adapter..");

    @ArchTest
    static final ArchRule onlyAdaptersUseJpa = noClasses().that().resideOutsideOfPackage("..adapter.jpa..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..");

    @ArchTest
    static final ArchRule controllersLiveInAdapters = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule onlyTheSecurityAdapterTouchesSpringSecurity = noClasses().that().resideOutsideOfPackage("..identity.adapter.security..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..", "com.nimbusds..");

    @ArchTest
    static final ArchRule gradersOnlyGrade = noClasses().that().resideInAPackage("..quiz.grading..")
            .should().dependOnClassesThat().resideInAnyPackage("..progress..", "..content..", "..execution..");
}
