package org.matonto.ontology.core.impl.owlapi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matonto.ontology.core.api.Annotation;
import org.matonto.ontology.core.api.Ontology;
import org.matonto.ontology.core.api.OntologyId;
import org.matonto.ontology.core.api.axiom.Axiom;
import org.matonto.ontology.utils.api.SesameTransformer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SimpleOntologyValues.class)
public class SimpleOntologyTest {

    OntologyId ontologyIdMock;
    SesameTransformer transformer;

    @Before
    public void setUp() {
        ontologyIdMock = mock(OntologyId.class);
        transformer = mock(SesameTransformer.class);
    }

    @Test
    public void testGetOntologyIdReturnsAnEqualObject() throws Exception {
        replay(ontologyIdMock);

        Ontology ontology = new SimpleOntology(ontologyIdMock, transformer);
        assertEquals(ontologyIdMock, ontology.getOntologyId());
    }

    @Test
    public void testStreamConstructor() throws Exception {
        replay(ontologyIdMock);

        InputStream stream = this.getClass().getResourceAsStream("/test.owl");
        Ontology ontology = new SimpleOntology(stream, ontologyIdMock, transformer);
        assertEquals(ontologyIdMock, ontology.getOntologyId());
    }

    @Test
    public void testFileConstructor() throws Exception {
        replay(ontologyIdMock);

        File file = Paths.get(getClass().getResource("/test.owl").toURI()).toFile();
        Ontology ontology = new SimpleOntology(file, ontologyIdMock, transformer);
        assertEquals(ontologyIdMock, ontology.getOntologyId());
    }

    @Test
    public void testEquals() throws Exception {
        replay(ontologyIdMock);

        InputStream stream1 = this.getClass().getResourceAsStream("/test.owl");
        InputStream stream2 = this.getClass().getResourceAsStream("/test.owl");
        File file = Paths.get(getClass().getResource("/test.owl").toURI()).toFile();

        Ontology ontology1 = new SimpleOntology(ontologyIdMock, transformer);
        Ontology ontology2 = new SimpleOntology(ontologyIdMock, transformer);

        Ontology ontology3 = new SimpleOntology(file, ontologyIdMock, transformer);
        Ontology ontology4 = new SimpleOntology(file, ontologyIdMock, transformer);

        Ontology ontology5 = new SimpleOntology(stream1, ontologyIdMock, transformer);
        Ontology ontology6 = new SimpleOntology(stream2, ontologyIdMock, transformer);

        assertEquals(ontology1, ontology2);
        assertEquals(ontology3, ontology4);
        assertEquals(ontology3, ontology5);
        assertEquals(ontology5, ontology6);
    }

    @Test
    public void testNotEquals() throws Exception {
        replay(ontologyIdMock);

        InputStream stream1 = this.getClass().getResourceAsStream("/test.owl");
        InputStream stream2 = this.getClass().getResourceAsStream("/travel.owl");

        Ontology ontology1 = new SimpleOntology(ontologyIdMock, transformer);
        Ontology ontology2 = new SimpleOntology(stream1, ontologyIdMock, transformer);
        Ontology ontology3 = new SimpleOntology(stream2, ontologyIdMock, transformer);

        assertNotEquals(ontology1, ontology2);
        assertNotEquals(ontology1, ontology3);
        assertNotEquals(ontology2, ontology3);
    }

    @Test
    @Ignore
    public void testHashCode() throws Exception {
        replay(ontologyIdMock);

        InputStream stream1 = this.getClass().getResourceAsStream("/test.owl");
        InputStream stream2 = this.getClass().getResourceAsStream("/test.owl");
        File file = Paths.get(getClass().getResource("/test.owl").toURI()).toFile();

        Ontology ontology1 = new SimpleOntology(ontologyIdMock, transformer);
        Ontology ontology2 = new SimpleOntology(ontologyIdMock, transformer);

        Ontology ontology3 = new SimpleOntology(file, ontologyIdMock, transformer);
        Ontology ontology4 = new SimpleOntology(file, ontologyIdMock, transformer);

        Ontology ontology5 = new SimpleOntology(stream1, ontologyIdMock, transformer);
        Ontology ontology6 = new SimpleOntology(stream2, ontologyIdMock, transformer);

        assertEquals(ontology1.hashCode(), ontology2.hashCode());
        assertEquals(ontology3.hashCode(), ontology4.hashCode());
        assertEquals(ontology3.hashCode(), ontology5.hashCode());
        assertEquals(ontology5.hashCode(), ontology6.hashCode());
    }

    @Test
    public void annotationsAreEmptyForEmptyOntology() throws Exception {
        replay(ontologyIdMock);

        Ontology ontology = new SimpleOntology(ontologyIdMock, transformer);
        Set<Annotation> annotations = ontology.getAnnotations();
        assertTrue(annotations.size() == 0);
    }

    @Test
    public void annotationsAreCorrectForNonemptyOntology() throws Exception {
        // Behaviors
        mockStatic(SimpleOntologyValues.class);
        expect(SimpleOntologyValues.matontoAnnotation(isA(OWLAnnotation.class))).andReturn(mock(Annotation.class)).anyTimes();
        replay(ontologyIdMock, SimpleOntologyValues.class);

        // Setup
        InputStream stream = this.getClass().getResourceAsStream("/test.owl");
        Ontology ontology = new SimpleOntology(stream, ontologyIdMock, transformer);

        // Test
        Set<Annotation> annotations = ontology.getAnnotations();

        // Assertions
        assertTrue(annotations.size() == 1);
    }

    @Test
    public void axiomsAreEmptyForEmptyOntology() throws Exception {
        Ontology ontology = new SimpleOntology(ontologyIdMock, transformer);
        Set<Axiom> axioms = ontology.getAxioms();
        assertTrue(axioms.size() == 0);
    }

    @Test
    public void axiomsAreCorrectForNonemptyOntology() throws Exception {
        // Behaviors
        mockStatic(SimpleOntologyValues.class);
        expect(SimpleOntologyValues.matontoAxiom(isA(OWLAxiom.class))).andReturn(mock(Axiom.class)).anyTimes();
        replay(ontologyIdMock, SimpleOntologyValues.class);

        // Setup
        InputStream stream = this.getClass().getResourceAsStream("/test.owl");
        Ontology ontology = new SimpleOntology(stream, ontologyIdMock, transformer);

        // Test
        Set<Axiom> axioms = ontology.getAxioms();

        // Assertions
        assertTrue(axioms.size() == 1);
    }

    // TODO: Test asModel

    // TODO: Test asTurtle

    // TODO: Test asRdfXml

    // TODO: Test asOwlXml

    // TODO: Test asJsonLD
}
