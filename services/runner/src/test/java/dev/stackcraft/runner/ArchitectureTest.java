package dev.stackcraft.runner;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Hexagonal rules, enforced by the build instead of by code review. */
@AnalyzeClasses(packages = "dev.stackcraft.runner", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPlainJava = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "..adapter..", "..application..");

    @ArchTest
    static final ArchRule applicationDependsOnPortsNotAdapters = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..adapter..");

    @ArchTest
    static final ArchRule portsAreFrameworkFree = noClasses().that().resideInAPackage("..port..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..adapter..");
}
