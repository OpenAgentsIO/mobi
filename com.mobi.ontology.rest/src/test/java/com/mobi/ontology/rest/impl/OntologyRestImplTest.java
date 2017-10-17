package com.mobi.ontology.rest.impl;

/*-
 * #%L
 * com.mobi.ontology.rest
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static com.mobi.rest.util.RestUtils.encode;
import static com.mobi.rest.util.RestUtils.modelToJsonld;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import com.mobi.catalog.api.CatalogManager;
import com.mobi.catalog.api.CatalogProvUtils;
import com.mobi.catalog.api.PaginatedSearchParams;
import com.mobi.catalog.api.PaginatedSearchResults;
import com.mobi.catalog.api.builder.Difference;
import com.mobi.catalog.api.ontologies.mcat.Branch;
import com.mobi.catalog.api.ontologies.mcat.BranchFactory;
import com.mobi.catalog.api.ontologies.mcat.CatalogFactory;
import com.mobi.catalog.api.ontologies.mcat.Commit;
import com.mobi.catalog.api.ontologies.mcat.CommitFactory;
import com.mobi.catalog.api.ontologies.mcat.InProgressCommit;
import com.mobi.catalog.api.ontologies.mcat.InProgressCommitFactory;
import com.mobi.catalog.api.ontologies.mcat.Record;
import com.mobi.catalog.api.versioning.VersioningManager;
import com.mobi.exception.MobiException;
import com.mobi.jaas.api.engines.EngineManager;
import com.mobi.jaas.api.ontologies.usermanagement.User;
import com.mobi.jaas.api.ontologies.usermanagement.UserFactory;
import com.mobi.ontology.core.api.Annotation;
import com.mobi.ontology.core.api.Individual;
import com.mobi.ontology.core.api.NamedIndividual;
import com.mobi.ontology.core.api.Ontology;
import com.mobi.ontology.core.api.OntologyId;
import com.mobi.ontology.core.api.OntologyManager;
import com.mobi.ontology.core.api.builder.OntologyRecordConfig;
import com.mobi.ontology.core.api.classexpression.OClass;
import com.mobi.ontology.core.api.datarange.Datatype;
import com.mobi.ontology.core.api.ontologies.ontologyeditor.OntologyRecord;
import com.mobi.ontology.core.api.ontologies.ontologyeditor.OntologyRecordFactory;
import com.mobi.ontology.core.api.propertyexpression.AnnotationProperty;
import com.mobi.ontology.core.api.propertyexpression.DataProperty;
import com.mobi.ontology.core.api.propertyexpression.ObjectProperty;
import com.mobi.ontology.core.impl.owlapi.SimpleAnnotation;
import com.mobi.ontology.core.impl.owlapi.SimpleNamedIndividual;
import com.mobi.ontology.core.impl.owlapi.SimpleOntologyManager;
import com.mobi.ontology.core.impl.owlapi.classexpression.SimpleClass;
import com.mobi.ontology.core.impl.owlapi.datarange.SimpleDatatype;
import com.mobi.ontology.core.impl.owlapi.propertyExpression.SimpleAnnotationProperty;
import com.mobi.ontology.core.impl.owlapi.propertyExpression.SimpleDataProperty;
import com.mobi.ontology.core.impl.owlapi.propertyExpression.SimpleObjectProperty;
import com.mobi.ontology.core.utils.MobiOntologyException;
import com.mobi.ontology.utils.cache.OntologyCache;
import com.mobi.persistence.utils.api.SesameTransformer;
import com.mobi.prov.api.ontologies.mobiprov.CreateActivity;
import com.mobi.prov.api.ontologies.mobiprov.CreateActivityFactory;
import com.mobi.prov.api.ontologies.mobiprov.DeleteActivity;
import com.mobi.prov.api.ontologies.mobiprov.DeleteActivityFactory;
import com.mobi.query.TupleQueryResult;
import com.mobi.rdf.api.IRI;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.ModelFactory;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.api.Statement;
import com.mobi.rdf.api.Value;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.rdf.core.impl.sesame.LinkedHashModelFactory;
import com.mobi.rdf.core.impl.sesame.SimpleValueFactory;
import com.mobi.rdf.core.utils.Values;
import com.mobi.rdf.orm.conversion.ValueConverterRegistry;
import com.mobi.rdf.orm.conversion.impl.DefaultValueConverterRegistry;
import com.mobi.rdf.orm.conversion.impl.DoubleValueConverter;
import com.mobi.rdf.orm.conversion.impl.FloatValueConverter;
import com.mobi.rdf.orm.conversion.impl.IRIValueConverter;
import com.mobi.rdf.orm.conversion.impl.IntegerValueConverter;
import com.mobi.rdf.orm.conversion.impl.LiteralValueConverter;
import com.mobi.rdf.orm.conversion.impl.ResourceValueConverter;
import com.mobi.rdf.orm.conversion.impl.ShortValueConverter;
import com.mobi.rdf.orm.conversion.impl.StringValueConverter;
import com.mobi.rdf.orm.conversion.impl.ValueValueConverter;
import com.mobi.repository.api.RepositoryManager;
import com.mobi.repository.impl.core.SimpleRepositoryManager;
import com.mobi.rest.util.MobiRestTestNg;
import com.mobi.rest.util.UsernameTestFilter;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.openrdf.rio.WriterConfig;
import org.openrdf.rio.helpers.JSONLDMode;
import org.openrdf.rio.helpers.JSONLDSettings;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.cache.Cache;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class OntologyRestImplTest extends MobiRestTestNg {
    private OntologyRestImpl rest;

    @Mock
    private OntologyManager ontologyManager;

    @Mock
    private CatalogManager catalogManager;

    @Mock
    private EngineManager engineManager;

    @Mock
    private OntologyId ontologyId;

    @Mock
    private OntologyId importedOntologyId;

    @Mock
    private Ontology ontology;

    @Mock
    private Ontology importedOntology;

    @Mock
    private SesameTransformer sesameTransformer;

    @Mock
    private PaginatedSearchResults<Record> results;

    @Mock
    private OntologyCache ontologyCache;

    @Mock
    private VersioningManager versioningManager;

    @Mock
    private Cache<String, Ontology> mockCache;

    @Mock
    private CatalogProvUtils provUtils;

    private ModelFactory mf;
    private ValueFactory vf;
    private ValueConverterRegistry vcr;
    private CatalogFactory catalogFactory;
    private CommitFactory commitFactory;
    private BranchFactory branchFactory;
    private OntologyRecordFactory ontologyRecordFactory;
    private InProgressCommitFactory inProgressCommitFactory;
    private UserFactory userFactory;
    private CreateActivityFactory createActivityFactory;
    private DeleteActivityFactory deleteActivityFactory;
    private CreateActivity createActivity;
    private DeleteActivity deleteActivity;
    private IRI catalogId;
    private IRI recordId;
    private OntologyRecord record;
    private IRI inProgressCommitId;
    private InProgressCommit inProgressCommit;
    private IRI commitId;
    private Commit commit;
    private IRI branchId;
    private Branch branch;
    private IRI userId;
    private User user;
    private IRI classId;
    private Difference difference;
    private Model additions;
    private Model deletions;
    private Model ontologyModel;
    private Model importedOntologyModel;
    private Model constructs;
    private String entityUsagesConstruct;
    private Set<Annotation> annotations;
    private Set<AnnotationProperty> annotationProperties;
    private Set<OClass> classes;
    private Set<Datatype> datatypes;
    private Set<ObjectProperty> objectProperties;
    private Set<DataProperty> dataProperties;
    private Set<Individual> individuals;
    private Set<IRI> derivedConcepts;
    private Set<IRI> derivedConceptSchemes;
    private Set<IRI> failedImports;
    private IRI derivedConceptIri;
    private IRI derivedConceptSchemeIri;
    private IRI classIRI;
    private IRI datatypeIRI;
    private IRI objectPropertyIRI;
    private IRI dataPropertyIRI;
    private IRI individualIRI;
    private Set<Ontology> importedOntologies;
    private IRI ontologyIRI;
    private IRI importedOntologyIRI;
    private JSONObject entityUsagesResult;
    private JSONObject subClassesOfResult;
    private JSONObject individualsOfResult;
    private JSONObject subObjectPropertiesOfResult;
    private JSONObject subDatatypePropertiesOfResult;
    private JSONObject subAnnotationPropertiesOfResult;
    private JSONObject conceptHierarchyResult;
    private JSONObject conceptSchemeHierarchyResult;
    private JSONObject searchResults;
    private SimpleOntologyManager simpleOntologyManager;
    private OutputStream ontologyJsonLd;
    private OutputStream importedOntologyJsonLd;
    private JSONArray importedOntologyResults;
    private static String INVALID_JSON = "{id: 'invalid";
    private IRI missingIRI;
    private RepositoryManager repoManager = new SimpleRepositoryManager();

    @Override
    protected Application configureApp() throws Exception {
        MockitoAnnotations.initMocks(this);

        vcr = new DefaultValueConverterRegistry();
        mf = LinkedHashModelFactory.getInstance();
        vf = SimpleValueFactory.getInstance();

        catalogFactory = new CatalogFactory();
        catalogFactory.setModelFactory(mf);
        catalogFactory.setValueFactory(vf);
        catalogFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(catalogFactory);

        commitFactory = new CommitFactory();
        commitFactory.setModelFactory(mf);
        commitFactory.setValueFactory(vf);
        commitFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(commitFactory);

        branchFactory = new BranchFactory();
        branchFactory.setModelFactory(mf);
        branchFactory.setValueFactory(vf);
        branchFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(branchFactory);

        ontologyRecordFactory = new OntologyRecordFactory();
        ontologyRecordFactory.setModelFactory(mf);
        ontologyRecordFactory.setValueFactory(vf);
        ontologyRecordFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(ontologyRecordFactory);

        inProgressCommitFactory = new InProgressCommitFactory();
        inProgressCommitFactory.setModelFactory(mf);
        inProgressCommitFactory.setValueFactory(vf);
        inProgressCommitFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(inProgressCommitFactory);

        userFactory = new UserFactory();
        userFactory.setModelFactory(mf);
        userFactory.setValueFactory(vf);
        userFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(userFactory);

        createActivityFactory = new CreateActivityFactory();
        createActivityFactory.setModelFactory(mf);
        createActivityFactory.setValueFactory(vf);
        createActivityFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(createActivityFactory);

        deleteActivityFactory = new DeleteActivityFactory();
        deleteActivityFactory.setModelFactory(mf);
        deleteActivityFactory.setValueFactory(vf);
        deleteActivityFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(deleteActivityFactory);

        vcr.registerValueConverter(new ResourceValueConverter());
        vcr.registerValueConverter(new IRIValueConverter());
        vcr.registerValueConverter(new DoubleValueConverter());
        vcr.registerValueConverter(new IntegerValueConverter());
        vcr.registerValueConverter(new FloatValueConverter());
        vcr.registerValueConverter(new ShortValueConverter());
        vcr.registerValueConverter(new StringValueConverter());
        vcr.registerValueConverter(new ValueValueConverter());
        vcr.registerValueConverter(new LiteralValueConverter());

        rest = new OntologyRestImpl();
        rest.setModelFactory(mf);
        rest.setValueFactory(vf);
        rest.setOntologyManager(ontologyManager);
        rest.setCatalogManager(catalogManager);
        rest.setEngineManager(engineManager);
        rest.setSesameTransformer(sesameTransformer);
        rest.setOntologyCache(ontologyCache);
        rest.setVersioningManager(versioningManager);
        rest.setProvUtils(provUtils);

        simpleOntologyManager = new SimpleOntologyManager();
        simpleOntologyManager.setModelFactory(mf);
        simpleOntologyManager.setValueFactory(vf);
        simpleOntologyManager.setRepositoryManager(repoManager);

        catalogId = vf.createIRI("http://mobi.com/catalog");
        recordId = vf.createIRI("http://mobi.com/record");
        record = ontologyRecordFactory.createNew(recordId);
        inProgressCommitId = vf.createIRI("http://mobi.com/in-progress-commit");
        inProgressCommit = inProgressCommitFactory.createNew(inProgressCommitId);
        commitId = vf.createIRI("http://mobi.com/commit");
        commit = commitFactory.createNew(commitId);
        branchId = vf.createIRI("http://mobi.com/branch");
        branch = branchFactory.createNew(branchId);
        userId = vf.createIRI("http://mobi.com/users/" + UsernameTestFilter.USERNAME);
        user = userFactory.createNew(userId);
        record.setMasterBranch(branch);
        classId = vf.createIRI("http://mobi.com/ontology#Class1a");
        IRI titleIRI = vf.createIRI(DCTERMS.TITLE.stringValue());
        additions = mf.createModel();
        additions.add(catalogId, titleIRI, vf.createLiteral("Addition"));
        deletions = mf.createModel();
        deletions.add(catalogId, titleIRI, vf.createLiteral("Deletion"));
        difference = new Difference.Builder()
                .additions(additions)
                .deletions(deletions)
                .build();
        WriterConfig config = new WriterConfig();
        config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.FLATTEN);
        InputStream testOntology = getClass().getResourceAsStream("/test-ontology.ttl");
        ontologyModel = mf.createModel(Values.mobiModel(Rio.parse(testOntology, "", RDFFormat.TURTLE)));
        ontologyJsonLd = new ByteArrayOutputStream();
        Rio.write(Values.sesameModel(ontologyModel), ontologyJsonLd, RDFFormat.JSONLD, config);
        InputStream testVocabulary = getClass().getResourceAsStream("/test-vocabulary.ttl");
        importedOntologyModel = mf.createModel(Values.mobiModel(Rio.parse(testVocabulary, "",
                RDFFormat.TURTLE)));
        importedOntologyJsonLd = new ByteArrayOutputStream();
        Rio.write(Values.sesameModel(importedOntologyModel), importedOntologyJsonLd, RDFFormat.JSONLD, config);
        IRI annotationPropertyIRI = vf.createIRI("http://mobi.com/annotation-property");
        annotationProperties = Collections.singleton(new SimpleAnnotationProperty(annotationPropertyIRI));
        IRI annotationIRI = vf.createIRI("http://mobi.com/annotation");
        AnnotationProperty annotationProperty = new SimpleAnnotationProperty(annotationIRI);
        annotations = Collections.singleton(new SimpleAnnotation(annotationProperty, vf.createLiteral("word"),
                Collections.emptySet()));
        classIRI = vf.createIRI("http://mobi.com/ontology#Class1a");
        classes = Collections.singleton(new SimpleClass(classIRI));
        datatypeIRI = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
        datatypes = Collections.singleton(new SimpleDatatype(datatypeIRI));
        objectPropertyIRI = vf.createIRI("http://mobi.com/ontology#objectProperty1a");
        objectProperties = Collections.singleton(new SimpleObjectProperty(objectPropertyIRI));
        dataPropertyIRI = vf.createIRI("http://mobi.com/ontology#dataProperty1a");
        dataProperties = Collections.singleton(new SimpleDataProperty(dataPropertyIRI));
        individualIRI = vf.createIRI("http://mobi.com/ontology#Individual1a");
        individuals = Collections.singleton(new SimpleNamedIndividual(individualIRI));
        derivedConceptIri = vf.createIRI("https://mobi.com/vocabulary#ConceptSubClass");
        derivedConcepts = Collections.singleton(derivedConceptIri);
        derivedConceptSchemeIri = vf.createIRI("https://mobi.com/vocabulary#ConceptSchemeSubClass");
        derivedConceptSchemes = Collections.singleton(derivedConceptSchemeIri);
        importedOntologies = Stream.of(ontology, importedOntology).collect(Collectors.toSet());
        ontologyIRI = vf.createIRI("http://mobi.com/ontology-id");
        importedOntologyIRI = vf.createIRI("http://mobi.com/imported-ontology-id");
        entityUsagesResult = getResource("/entity-usages-results.json");
        subClassesOfResult = getResource("/sub-classes-of-results.json");
        individualsOfResult = getResource("/individuals-of-results.json");
        subObjectPropertiesOfResult = getResource("/sub-object-properties-of-results.json");
        subDatatypePropertiesOfResult = getResource("/sub-datatype-properties-of-results.json");
        subAnnotationPropertiesOfResult = getResource("/sub-annotation-properties-of-results.json");
        conceptHierarchyResult = getResource("/concept-hierarchy-results.json");
        conceptSchemeHierarchyResult = getResource("/concept-scheme-hierarchy-results.json");
        searchResults = getResource("/search-results.json");
        importedOntologyResults = getResourceArray("/imported-ontology-results.json");
        missingIRI = vf.createIRI("http://mobi.com/missing");
        Resource class1b = vf.createIRI("http://mobi.com/ontology#Class1b");
        IRI subClassOf = vf.createIRI("http://www.w3.org/2000/01/rdf-schema#subClassOf");
        Value class1a = vf.createIRI("http://mobi.com/ontology#Class1a");
        Resource individual1a = vf.createIRI("http://mobi.com/ontology#Individual1a");
        IRI type = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        constructs = mf.createModel(Stream.of(vf.createStatement(class1b, subClassOf, class1a),
                vf.createStatement(individual1a, type, class1a)).collect(Collectors.toSet()));
        failedImports = Collections.singleton(importedOntologyIRI);
        createActivity = createActivityFactory.createNew(vf.createIRI("http://test.org/activity/create"));
        deleteActivity = deleteActivityFactory.createNew(vf.createIRI("http://test.org/activity/delete"));

        return new ResourceConfig()
                .register(rest)
                .register(MultiPartFeature.class)
                .register(UsernameTestFilter.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }

    @BeforeMethod
    public void setupMocks() {
        reset(engineManager, versioningManager, ontologyId, ontology, importedOntologyId, importedOntology, catalogManager,
                ontologyManager, sesameTransformer, results, mockCache, ontologyCache, provUtils);

        final IRI skosConcept = vf.createIRI(SKOS.CONCEPT.stringValue());
        final IRI skosConceptScheme = vf.createIRI(SKOS.CONCEPT_SCHEME.stringValue());

        when(results.getPage()).thenReturn(Collections.emptyList());
        when(results.getPageNumber()).thenReturn(0);
        when(results.getPageSize()).thenReturn(0);
        when(results.getTotalSize()).thenReturn(0);

        when(versioningManager.commit(eq(catalogId), eq(recordId), eq(branchId), eq(user), anyString(), any(Model.class), any(Model.class))).thenReturn(commitId);

        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.of(user));

        when(ontologyId.getOntologyIdentifier()).thenReturn(ontologyIRI);
        when(ontologyId.getOntologyIRI()).thenReturn(Optional.of(ontologyIRI));

        when(ontology.getOntologyId()).thenReturn(ontologyId);
        when(ontology.asModel(mf)).thenReturn(ontologyModel);
        when(ontology.getAllAnnotations()).thenReturn(annotations);
        when(ontology.getAllAnnotationProperties()).thenReturn(annotationProperties);
        when(ontology.getAllClasses()).thenReturn(classes);
        when(ontology.getAllDatatypes()).thenReturn(datatypes);
        when(ontology.getAllObjectProperties()).thenReturn(objectProperties);
        when(ontology.getAllDataProperties()).thenReturn(dataProperties);
        when(ontology.getAllIndividuals()).thenReturn(individuals);
        when(ontology.getImportsClosure()).thenReturn(importedOntologies);
        when(ontology.asJsonLD(anyBoolean())).thenReturn(ontologyJsonLd);
        when(ontology.getUnloadableImportIRIs()).thenReturn(failedImports);

        when(importedOntologyId.getOntologyIdentifier()).thenReturn(importedOntologyIRI);
        when(importedOntologyId.getOntologyIRI()).thenReturn(Optional.of(importedOntologyIRI));

        when(importedOntology.getOntologyId()).thenReturn(importedOntologyId);
        when(importedOntology.asModel(mf)).thenReturn(importedOntologyModel);
        when(importedOntology.getAllAnnotations()).thenReturn(annotations);
        when(importedOntology.getAllAnnotationProperties()).thenReturn(annotationProperties);
        when(importedOntology.getAllClasses()).thenReturn(classes);
        when(importedOntology.getAllDatatypes()).thenReturn(datatypes);
        when(importedOntology.getAllObjectProperties()).thenReturn(objectProperties);
        when(importedOntology.getAllDataProperties()).thenReturn(dataProperties);
        when(importedOntology.getAllIndividuals()).thenReturn(individuals);
        when(importedOntology.asJsonLD(anyBoolean())).thenReturn(importedOntologyJsonLd);
        when(importedOntology.getImportsClosure()).thenReturn(Collections.singleton(importedOntology));

        when(catalogManager.getLocalCatalogIRI()).thenReturn(catalogId);
        when(catalogManager.findRecord(any(Resource.class), any(PaginatedSearchParams.class))).thenReturn(results);
        when(catalogManager.getRecord(eq(catalogId), eq(recordId), any(OntologyRecordFactory.class))).thenReturn(Optional.of(record));
        when(catalogManager.removeRecord(catalogId, recordId, ontologyRecordFactory)).thenReturn(record);
        when(catalogManager.createInProgressCommit(any(User.class))).thenReturn(inProgressCommit);
        when(catalogManager.getInProgressCommit(catalogId, recordId, user)).thenReturn(Optional.of(inProgressCommit));
        when(catalogManager.getInProgressCommit(catalogId, recordId, inProgressCommitId)).thenReturn(Optional.of(inProgressCommit));
        when(catalogManager.createCommit(eq(inProgressCommit), anyString(), any(Commit.class), any(Commit.class))).thenReturn(commit);
        when(catalogManager.applyInProgressCommit(eq(inProgressCommitId), any(Model.class))).thenReturn(mf
                .createModel());
        when(catalogManager.getDiff(any(Model.class), any(Model.class))).thenReturn(difference);

        when(ontologyManager.createOntologyRecord(any(OntologyRecordConfig.class))).thenReturn(record);
        when(ontologyManager.createOntology(any(FileInputStream.class))).thenReturn(ontology);
        when(ontologyManager.createOntology(anyString())).thenReturn(ontology);
        when(ontologyManager.createOntology(any(Model.class))).thenReturn(ontology);
        when(ontologyManager.deleteOntology(eq(recordId))).thenReturn(record);
        when(ontologyManager.retrieveOntology(eq(recordId), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.of(ontology));
        when(ontologyManager.retrieveOntology(eq(recordId), any(Resource.class))).thenReturn(Optional.of(ontology));
        when(ontologyManager.retrieveOntology(recordId)).thenReturn(Optional.of(ontology));
        when(ontologyManager.retrieveOntology(eq(importedOntologyIRI), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.of(importedOntology));
        when(ontologyManager.retrieveOntology(eq(importedOntologyIRI), any(Resource.class)))
                .thenReturn(Optional.of(importedOntology));
        when(ontologyManager.retrieveOntology(importedOntologyIRI)).thenReturn(Optional.of(importedOntology));
        simpleOntologyManager.setRepositoryManager(repoManager);
        TupleQueryResult subClassesOf = simpleOntologyManager.getSubClassesOf(ontology);
        when(ontologyManager.getSubClassesOf(ontology)).thenReturn(subClassesOf);
        TupleQueryResult conceptsOf = simpleOntologyManager.getSubClassesFor(ontology, skosConcept);
        when(ontologyManager.getSubClassesFor(eq(ontology), eq(skosConcept))).thenReturn(conceptsOf);
        TupleQueryResult conceptSchemesOf = simpleOntologyManager.getSubClassesFor(ontology, skosConceptScheme);
        when(ontologyManager.getSubClassesFor(eq(ontology), eq(skosConceptScheme))).thenReturn(conceptSchemesOf);

        TupleQueryResult importedConceptsOf = simpleOntologyManager.getSubClassesFor(importedOntology, skosConcept);
        when(ontologyManager.getSubClassesFor(eq(importedOntology), eq(skosConcept))).thenReturn(importedConceptsOf);
        TupleQueryResult importedConceptSchemesOf = simpleOntologyManager.getSubClassesFor(ontology, skosConceptScheme);
        when(ontologyManager.getSubClassesFor(eq(importedOntology), eq(skosConceptScheme))).thenReturn(importedConceptSchemesOf);

        TupleQueryResult individualsOf = simpleOntologyManager.getClassesWithIndividuals(ontology);
        when(ontologyManager.getClassesWithIndividuals(ontology)).thenReturn(individualsOf);
        TupleQueryResult subObjectPropertiesOf = simpleOntologyManager.getSubObjectPropertiesOf(ontology);
        when(ontologyManager.getSubObjectPropertiesOf(ontology)).thenReturn(subObjectPropertiesOf);
        TupleQueryResult subDatatypePropertiesOf = simpleOntologyManager.getSubDatatypePropertiesOf(ontology);
        when(ontologyManager.getSubDatatypePropertiesOf(ontology)).thenReturn(subDatatypePropertiesOf);
        TupleQueryResult subAnnotationPropertiesOf = simpleOntologyManager.getSubAnnotationPropertiesOf(ontology);
        when(ontologyManager.getSubAnnotationPropertiesOf(ontology)).thenReturn(subAnnotationPropertiesOf);
        TupleQueryResult classesWithIndividuals = simpleOntologyManager.getClassesWithIndividuals(ontology);
        when(ontologyManager.getClassesWithIndividuals(ontology)).thenReturn(classesWithIndividuals);
        TupleQueryResult conceptRelationships = simpleOntologyManager.getConceptRelationships(importedOntology);
        when(ontologyManager.getConceptRelationships(ontology)).thenReturn(conceptRelationships);
        TupleQueryResult conceptSchemeRelationships = simpleOntologyManager.getConceptSchemeRelationships(importedOntology);
        when(ontologyManager.getConceptSchemeRelationships(ontology)).thenReturn(conceptSchemeRelationships);
        TupleQueryResult entityUsages = simpleOntologyManager.getEntityUsages(ontology, classId);
        when(ontologyManager.getEntityUsages(eq(ontology), any(Resource.class))).thenReturn(entityUsages);
        Model constructModel = simpleOntologyManager.constructEntityUsages(ontology, classId);
        when(ontologyManager.constructEntityUsages(eq(ontology), any(Resource.class))).thenReturn(constructModel);
        when(ontologyManager.getSearchResults(eq(ontology), anyString())).thenAnswer(invocationOnMock ->
                simpleOntologyManager.getSearchResults(ontology, invocationOnMock.getArgumentAt(1, String.class)));

        when(sesameTransformer.mobiModel(any(org.openrdf.model.Model.class))).thenAnswer(invocationOnMock ->
                Values.mobiModel(invocationOnMock.getArgumentAt(0, org.openrdf.model.Model.class)));
        when(sesameTransformer.mobiIRI(any(org.openrdf.model.IRI.class))).thenAnswer(invocationOnMock ->
                Values.mobiIRI(invocationOnMock.getArgumentAt(0, org.openrdf.model.IRI.class)));
        when(sesameTransformer.sesameModel(any(Model.class))).thenAnswer(invocationOnMock ->
                Values.sesameModel(invocationOnMock.getArgumentAt(0, Model.class)));
        when(sesameTransformer.sesameStatement(any(Statement.class))).thenAnswer(invocationOnMock ->
                Values.sesameStatement(invocationOnMock.getArgumentAt(0, Statement.class)));

        entityUsagesConstruct = modelToJsonld(constructs, sesameTransformer);

        when(ontologyCache.getOntologyCache()).thenReturn(Optional.of(mockCache));

        when(provUtils.startCreateActivity(any(User.class))).thenReturn(createActivity);
        when(provUtils.startDeleteActivity(any(User.class), any(IRI.class))).thenReturn(deleteActivity);
    }

    private JSONObject getResource(String path) throws Exception {
        return JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream(path)));
    }

    private JSONArray getResourceArray(String path) throws Exception {
        return JSONArray.fromObject(IOUtils.toString(getClass().getResourceAsStream(path)));
    }

    private void assertGetInProgressCommitIRI(boolean hasInProgressCommit) {
        assertGetUserFromContext();
        verify(catalogManager, atLeastOnce()).getInProgressCommit(any(Resource.class), any(Resource.class), any(User.class));
        if (!hasInProgressCommit) {
            verify(catalogManager).createInProgressCommit(any(User.class));
            verify(catalogManager).addInProgressCommit(any(Resource.class), any(Resource.class), any(InProgressCommit.class));
        }
    }

    private void assertGetUserFromContext() {
        verify(engineManager, atLeastOnce()).retrieveUser(anyString());
    }

    private void assertGetOntology(boolean hasInProgressCommit) {
        assertGetUserFromContext();
        verify(catalogManager, atLeastOnce()).getInProgressCommit(any(Resource.class), any(Resource.class), any(User.class));
        if (hasInProgressCommit) {
            verify(catalogManager).applyInProgressCommit(any(Resource.class), any(Model.class));
            verify(ontologyManager).createOntology(any(Model.class));
        }
    }

    private JSONObject createJsonIRI(IRI iri) {
        return new JSONObject()
                .element("namespace", iri.getNamespace())
                .element("localName", iri.getLocalName());
    }

    private void assertAnnotations(JSONObject responseObject, Set<AnnotationProperty> propSet, Set<Annotation> annSet) {
        JSONArray jsonAnnotations = responseObject.optJSONArray("annotationProperties");
        assertNotNull(jsonAnnotations);
        assertEquals(jsonAnnotations.size(), propSet.size() + annSet.size());
        propSet.forEach(annotationProperty ->
                assertTrue(jsonAnnotations.contains(createJsonIRI(annotationProperty.getIRI()))));
        annSet.forEach(annotation ->
                assertTrue(jsonAnnotations.contains(createJsonIRI(annotation.getProperty().getIRI()))));
    }

    private void assertClasses(JSONObject responseObject, Set<OClass> set) {
        JSONArray jsonClasses = responseObject.optJSONArray("classes");
        assertNotNull(jsonClasses);
        assertEquals(jsonClasses.size(), set.size());
        set.forEach(oClass -> assertTrue(jsonClasses.contains(createJsonIRI(oClass.getIRI()))));
    }

    private void assertDatatypes(JSONObject responseObject, Set<Datatype> set) {
        JSONArray jsonDatatypes = responseObject.optJSONArray("datatypes");
        assertNotNull(jsonDatatypes);
        assertEquals(jsonDatatypes.size(), set.size());
        set.forEach(datatype -> assertTrue(jsonDatatypes.contains(createJsonIRI(datatype.getIRI()))));
    }

    private void assertObjectProperties(JSONObject responseObject, Set<ObjectProperty> set) {
        JSONArray jsonObjectProperties = responseObject.optJSONArray("objectProperties");
        assertNotNull(jsonObjectProperties);
        assertEquals(jsonObjectProperties.size(), set.size());
        set.forEach(objectProperty -> assertTrue(jsonObjectProperties.contains(createJsonIRI(objectProperty
                .getIRI()))));
    }

    private void assertDataPropertyIRIs(JSONObject responseObject, Set<DataProperty> set) {
        JSONArray jsonDataProperties = responseObject.optJSONArray("dataProperties");
        assertNotNull(jsonDataProperties);
        assertEquals(jsonDataProperties.size(), set.size());
        set.forEach(dataProperty -> assertTrue(jsonDataProperties.contains(createJsonIRI(dataProperty
                .getIRI()))));
    }

    private void assertDataProperties(JSONArray responseArray, Set<DataProperty> set) {
        assertNotNull(responseArray);
        assertEquals(responseArray.size(), set.size());
    }

    private void assertIndividuals(JSONObject responseObject, Set<Individual> set) {
        JSONArray jsonIndividuals = responseObject.optJSONArray("namedIndividuals");
        assertNotNull(jsonIndividuals);
        assertEquals(jsonIndividuals.size(), set.size());
        set.forEach(individual -> assertTrue(jsonIndividuals.contains(createJsonIRI(
                ((NamedIndividual) individual).getIRI()))));
    }

    private void assertDerivedConcepts(JSONObject responseObject, Set<IRI> set) {
        JSONArray jsonConcepts = responseObject.optJSONArray("derivedConcepts");
        assertNotNull(jsonConcepts);
        assertEquals(jsonConcepts.size(), set.size());
        set.forEach(derivedConceptIri -> assertTrue(jsonConcepts.contains(derivedConceptIri)));
    }

    private void assertDerivedConceptSchemes(JSONObject responseObject, Set<IRI> set) {
        JSONArray jsonConceptSchemes = responseObject.optJSONArray("derivedConceptSchemes");
        assertNotNull(jsonConceptSchemes);
        assertEquals(jsonConceptSchemes.size(), set.size());
        set.forEach(derivedConceptSchemeIri -> assertTrue(jsonConceptSchemes.contains(derivedConceptSchemeIri)));
    }

    private void assertAdditionsToInProgressCommit(boolean hasInProgressCommit) {
        assertGetInProgressCommitIRI(hasInProgressCommit);
        verify(catalogManager).updateInProgressCommit(any(Resource.class), any(Resource.class), any(Resource.class), any(Model.class), eq(null));
    }

    private void assertDeletionsToInProgressCommit(boolean hasInProgressCommit) {
        assertGetInProgressCommitIRI(hasInProgressCommit);
        verify(catalogManager).updateInProgressCommit(any(Resource.class), any(Resource.class), any(Resource.class), eq(null), any(Model.class));
    }

    private void assertImportedOntologies(JSONArray responseArray, Consumer<JSONObject> assertConsumer) {
        for (Object o : responseArray) {
            JSONObject jsonO = (JSONObject) o;
            String ontologyId = jsonO.get("id").toString();
            assertNotEquals(importedOntologies.stream()
                    .filter(ont -> ont.getOntologyId().getOntologyIdentifier().stringValue().equals(ontologyId))
                    .collect(Collectors.toList()).size(), 0);
            assertConsumer.accept(jsonO);
        }
    }

    private void setNoInProgressCommit() {
        when(catalogManager.getInProgressCommit(any(Resource.class), any(Resource.class), any(User.class)))
                .thenReturn(Optional.empty());
    }

    private JSONObject getResponse(Response response) {
        return JSONObject.fromObject(response.readEntity(String.class));
    }

    private JSONArray getResponseArray(Response response) {
        return JSONArray.fromObject(response.readEntity(String.class));
    }

    private JSONObject createJsonOfType(String type) {
        return new JSONObject().element("@type", JSONArray.fromObject(Collections.singleton(type)));
    }

    // Test upload file

    @Test
    public void testUploadFile() {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/test-ontology.ttl"), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        fd.field("title", "title");
        fd.field("description", "description");
        fd.field("keywords", "keyword1,keyword2");

        Response response = target().path("ontologies").request().post(Entity.entity(fd,
                MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), 201);
        assertGetUserFromContext();
        verify(ontologyManager).createOntology(any(FileInputStream.class));
        verify(ontology, atLeastOnce()).getOntologyId();
        verify(ontologyId).getOntologyIdentifier();
        verify(ontologyId).getOntologyIRI();
        verify(catalogManager, atLeastOnce()).getLocalCatalogIRI();
        verify(ontologyManager).createOntologyRecord(any(OntologyRecordConfig.class));
        verify(catalogManager).addRecord(catalogId, record);
        verify(versioningManager).commit(eq(catalogId), eq(recordId), eq(branchId), eq(user), anyString(), any(Model.class), eq(null));
        verify(mockCache).put(anyString(), any(Ontology.class));
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).endCreateActivity(createActivity, record.getResource());
    }

    @Test
    public void testUploadFileWithoutTitle() {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/test-ontology.ttl"), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        fd.field("description", "description");
        fd.field("keywords", "keyword1,keyword2");

        Response response = target().path("ontologies").request().post(Entity.entity(fd,
                MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), 400);
        verify(provUtils, times(0)).startCreateActivity(user);
    }

    @Test
    public void testUploadFileWithoutFile() {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("title", "title");
        fd.field("description", "description");
        fd.field("keywords", "keyword1,keyword2");

        Response response = target().path("ontologies").request().post(Entity.entity(fd,
                MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), 400);
        verify(provUtils, times(0)).startCreateActivity(user);
    }

    @Test
    public void testUploadInvalidOntologyFile() {
        when(ontologyManager.createOntology(any(FileInputStream.class)))
                .thenThrow(new MobiOntologyException("Error"));

        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/search-results.json"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        fd.field("title", "title");
        fd.field("description", "description");
        fd.field("keywords", "keyword1,keyword2");

        Response response = target().path("ontologies").request().post(Entity.entity(fd,
                MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), 500);
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).removeActivity(createActivity);
    }

    @Test
    public void testUploadExistingOntologyFile() {
        // Setup:
        when(ontologyManager.ontologyIriExists(any(IRI.class))).thenReturn(true);
        record.setOntologyIRI(ontologyIRI);
        when(results.getPage()).thenReturn(Collections.singletonList(record));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/test-ontology.ttl"), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        fd.field("title", "title");
        fd.field("description", "description");
        fd.field("keywords", "keyword1,keyword2");

        Response response = target().path("ontologies").request().post(Entity.entity(fd,
                MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).removeActivity(createActivity);
    }

    // Test upload ontology json

    @Test
    public void testUploadOntologyJson() {
        JSONObject ontologyJson = new JSONObject().element("@id", "http://mobi.com/ontology");

        Response response = target().path("ontologies").queryParam("title", "title").queryParam("description",
                "description").queryParam("keywords", "keyword1,keyword2").request().post(Entity.json(ontologyJson));

        assertEquals(response.getStatus(), 201);
        assertGetUserFromContext();
        verify(ontologyManager).createOntology(ontologyJson.toString());
        verify(ontology, atLeastOnce()).getOntologyId();
        verify(ontologyId).getOntologyIdentifier();
        verify(ontologyId).getOntologyIRI();
        verify(catalogManager, atLeastOnce()).getLocalCatalogIRI();
        verify(ontologyManager).createOntologyRecord(any(OntologyRecordConfig.class));
        verify(catalogManager).addRecord(catalogId, record);
        verify(versioningManager).commit(eq(catalogId), eq(recordId), eq(branchId), eq(user), anyString(), any(Model.class), eq(null));
        verify(mockCache).put(anyString(), any(Ontology.class));
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).endCreateActivity(createActivity, record.getResource());
    }

    @Test
    public void testUploadOntologyJsonWithoutTitle() {
        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies").queryParam("description", "description")
                .queryParam("keywords", "keyword1,keyword2").request().post(Entity.json(entity));
        assertEquals(response.getStatus(), 400);
        verify(provUtils, times(0)).startCreateActivity(user);
    }

    @Test
    public void testUploadOntologyJsonWithoutJson() {
        Response response = target().path("ontologies").queryParam("title", "title").queryParam("description",
                "description").queryParam("keywords", "keyword1,keyword2").request().post(Entity.json(""));
        assertEquals(response.getStatus(), 400);
        verify(provUtils, times(0)).startCreateActivity(user);
    }

    @Test
    public void testUploadInvalidOntologyJson() {
        when(ontologyManager.createOntology(anyString())).thenThrow(new MobiOntologyException("Error"));
        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies").queryParam("title", "title").queryParam("description",
                "description").queryParam("keywords", "keyword1,keyword2").request().post(Entity.json(entity));
        assertEquals(response.getStatus(), 500);
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).removeActivity(createActivity);
    }

    @Test
    public void testUploadExistingOntologyJson() {
        // Setup:
        when(ontologyManager.ontologyIriExists(any(IRI.class))).thenReturn(true);
        record.setOntologyIRI(ontologyIRI);
        when(results.getPage()).thenReturn(Collections.singletonList(record));
        JSONObject ontologyJson = new JSONObject().element("@id", "http://mobi.com/ontology");

        Response response = target().path("ontologies").queryParam("title", "title").queryParam("description",
                "description").queryParam("keywords", "keyword1,keyword2").request().post(Entity.json(ontologyJson));
        assertEquals(response.getStatus(), 400);
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).removeActivity(createActivity);
    }

    // Test get ontology

    @Test
    public void testDownloadOntologyFile() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(true);
    }

    @Test
    public void testDownloadOntologyFileWithNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(false);
    }

    @Test
    public void testDownloadOntologyFileWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("commitId", commitId.stringValue()).queryParam("entityId", catalogId.stringValue())
                .request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDownloadOntologyFileMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).request()
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(true);
    }

    @Test
    public void testDownloadOntologyFileWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();

        assertEquals(response.getStatus(), 400);
    }

    // Test download ontology file

    @Test
    public void testGetOntologyCacheHit() {
        when(mockCache.containsKey(anyString())).thenReturn(true);
        when(mockCache.get(anyString())).thenReturn(ontology);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), ontologyJsonLd.toString());
        verify(ontologyCache, times(0)).removeFromCache(anyString(), anyString(), anyString());
        verify(mockCache).containsKey(anyString());
        verify(mockCache).get(anyString());
        verify(mockCache, times(0)).put(anyString(), any(Ontology.class));
    }

    @Test
    public void testGetOntologyCacheMiss() {
        when(mockCache.containsKey(anyString())).thenReturn(false);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), ontologyJsonLd.toString());
        verify(ontologyCache, times(0)).removeFromCache(anyString(), anyString(), anyString());
        verify(mockCache).containsKey(anyString());
        verify(mockCache, times(0)).get(anyString());
        // OntologyManger will handle caching the ontology
        verify(mockCache, times(0)).put(anyString(), any(Ontology.class));
    }

    @Test
    public void testGetOntologyClearCache() {
        when(mockCache.containsKey(anyString())).thenReturn(false);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("clearCache", true).request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), ontologyJsonLd.toString());
        verify(ontologyCache).removeFromCache(recordId.stringValue(), branchId.stringValue(), commitId.stringValue());
        verify(mockCache).containsKey(anyString());
        verify(mockCache, times(0)).get(anyString());
        // OntologyManger will handle caching the ontology
        verify(mockCache, times(0)).put(anyString(), any(Ontology.class));
    }

    @Test
    public void testGetOntologyWithNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(false);
        assertEquals(response.readEntity(String.class), ontologyJsonLd.toString());
        verify(ontologyCache, times(0)).removeFromCache(anyString(), anyString(), anyString());
    }

    @Test
    public void testGetOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("commitId", commitId.stringValue()).queryParam("entityId", catalogId.stringValue())
                .request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 200);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), ontologyJsonLd.toString());
        verify(ontologyCache, times(0)).removeFromCache(anyString(), anyString(), anyString());
    }

    @Test
    public void testGetOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().accept(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(response.getStatus(), 400);
    }

    // Test save changes to ontology

    @Test
    public void testSaveChangesToOntology() {
        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("entityId", catalogId.stringValue()).request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertGetInProgressCommitIRI(true);
        verify(catalogManager).updateInProgressCommit(eq(catalogId), eq(recordId), eq(inProgressCommitId), any(Model.class), any(Model.class));
    }

    @Test
    public void testSaveChangesToOntologyWithNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("entityId", catalogId.stringValue()).request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertGetInProgressCommitIRI(false);
        verify(catalogManager).updateInProgressCommit(eq(catalogId), eq(recordId), eq(inProgressCommitId), any(Model.class), any(Model.class));
    }

    @Test
    public void testSaveChangesToOntologyWithNoDifference() {
        when(catalogManager.getDiff(any(Model.class), any(Model.class))).thenReturn(new Difference.Builder()
                .build());

        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("entityId", catalogId.stringValue()).request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertGetInProgressCommitIRI(true);
        verify(catalogManager).updateInProgressCommit(catalogId, recordId, inProgressCommitId, null, null);
    }

    @Test
    public void testSaveChangesToOntologyWithCommitIdAndMissingBranchId() {
        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("commitId", commitId.stringValue()).queryParam("entityId", catalogId.stringValue())
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testSaveChangesToOntologyMissingCommitId() {
        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("entityId", catalogId.stringValue())
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertGetInProgressCommitIRI(true);
        verify(catalogManager).updateInProgressCommit(eq(catalogId), eq(recordId), eq(inProgressCommitId), any(Model.class), any(Model.class));
    }

    @Test
    public void testSaveChangesToOntologyMissingBranchIdAndMissingCommitId() {
        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("entityId", catalogId.stringValue()).request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertGetInProgressCommitIRI(true);
        verify(catalogManager).updateInProgressCommit(eq(catalogId), eq(recordId), eq(inProgressCommitId), any(Model.class), any(Model.class));
    }

    @Test
    public void testSaveChangesToOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        JSONObject entity = new JSONObject().element("@id", "http://mobi.com/entity");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("entityId", catalogId.stringValue()).request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    // Test get IRIs in ontology

    @Test
    public void testGetIRIsInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        JSONObject responseObject = getResponse(response);
        assertAnnotations(responseObject, annotationProperties, annotations);
        assertClasses(responseObject, classes);
        assertDatatypes(responseObject, datatypes);
        assertObjectProperties(responseObject, objectProperties);
        assertDataPropertyIRIs(responseObject, dataProperties);
        assertIndividuals(responseObject, individuals);
        assertDerivedConcepts(responseObject, derivedConcepts);
        assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
    }

    @Test
    public void testGetIRIsInOntologyWithNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        JSONObject responseObject = getResponse(response);
        assertAnnotations(responseObject, annotationProperties, annotations);
        assertClasses(responseObject, classes);
        assertDatatypes(responseObject, datatypes);
        assertObjectProperties(responseObject, objectProperties);
        assertDataPropertyIRIs(responseObject, dataProperties);
        assertIndividuals(responseObject, individuals);
        assertDerivedConcepts(responseObject, derivedConcepts);
        assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
    }

    @Test
    public void testGetIRIsInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/iris")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetIRIsInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/iris")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        JSONObject responseObject = getResponse(response);
        assertAnnotations(responseObject, annotationProperties, annotations);
        assertClasses(responseObject, classes);
        assertDatatypes(responseObject, datatypes);
        assertObjectProperties(responseObject, objectProperties);
        assertDataPropertyIRIs(responseObject, dataProperties);
        assertIndividuals(responseObject, individuals);
        assertDerivedConcepts(responseObject, derivedConcepts);
        assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
    }

    @Test
    public void testGetIRIsInOntologyMissingBranchIdAndMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/iris").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        JSONObject responseObject = getResponse(response);
        assertAnnotations(responseObject, annotationProperties, annotations);
        assertClasses(responseObject, classes);
        assertDatatypes(responseObject, datatypes);
        assertObjectProperties(responseObject, objectProperties);
        assertDataPropertyIRIs(responseObject, dataProperties);
        assertIndividuals(responseObject, individuals);
        assertDerivedConcepts(responseObject, derivedConcepts);
        assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
    }

    @Test
    public void testGetIRIsInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get annotations in ontology

    @Test
    public void testGetAnnotationsInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertAnnotations(getResponse(response), annotationProperties, annotations);
    }

    @Test
    public void testGetAnnotationsInOntologyWithNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertAnnotations(getResponse(response), annotationProperties, annotations);
    }

    @Test
    public void testGetAnnotationsInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetAnnotationsInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(false);
        assertAnnotations(getResponse(response), annotationProperties, annotations);
    }

    @Test
    public void testGetAnnotationsInOntologyMissingBranchIdAndMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(false);
        assertAnnotations(getResponse(response), annotationProperties, annotations);
    }

    @Test
    public void testGetAnnotationsInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetAnnotationsInOntologyWhenNoAnnotations() {
        when(ontology.getAllAnnotationProperties()).thenReturn(Collections.EMPTY_SET);
        when(ontology.getAllAnnotations()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertAnnotations(getResponse(response), Collections.EMPTY_SET, Collections.EMPTY_SET);
    }

    // Test add annotation to ontology

    @Test
    public void testAddAnnotationToOntology() {
        JSONObject entity = createJsonOfType(OWL.ANNOTATIONPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-annotation");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(true);
    }

    @Test
    public void testAddWrongTypedAnnotationToOntology() {
        JSONObject entity = new JSONObject()
                .element("@id", "http://mobi.com/new-annotation");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddInvalidAnnotationToOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations").request()
                .post(Entity.json(INVALID_JSON));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddAnnotationToOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = createJsonOfType(OWL.ANNOTATIONPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-annotation");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(false);
    }

    // Test delete annotation from ontology

    @Test
    public void testDeleteAnnotationFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteAnnotationFromOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDeletionsToInProgressCommit(false);
    }

    @Test
    public void testDeleteAnnotationFromOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(classId.stringValue())).queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteAnnotationFromOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteAnnotationFromOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(classId.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteAnnotationFromOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteMissingAnnotationFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/annotations/"
                + encode(missingIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    // Test get classes in ontology

    @Test
    public void testGetClassesInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertClasses(getResponse(response), classes);
    }

    @Test
    public void testGetClassesInOntologyWithNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertClasses(getResponse(response), classes);
    }

    @Test
    public void testGetClassesInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetClassesInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertClasses(getResponse(response), classes);
    }

    @Test
    public void testGetClassesInOntologyMissingBranchIdAndMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes").request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertClasses(getResponse(response), classes);
    }

    @Test
    public void testGetClassesInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetClassesInOntologyWhenNoClasses() {
        when(ontology.getAllClasses()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertClasses(getResponse(response), Collections.EMPTY_SET);
    }

    // Test add class to ontology

    @Test
    public void testAddClassToOntology() {
        JSONObject entity = createJsonOfType(OWL.CLASS.stringValue())
                .element("@id", "http://mobi.com/new-class");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(true);
    }

    @Test
    public void testAddWrongTypedClassToOntology() {
        JSONObject entity = new JSONObject()
                .element("@id", "http://mobi.com/new-class");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddInvalidClassToOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes").request()
                .post(Entity.json(INVALID_JSON));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddClassToOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = createJsonOfType(OWL.CLASS.stringValue())
                .element("@id", "http://mobi.com/new-class");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(false);
    }

    // Test delete class from ontology

    @Test
    public void testDeleteClassFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteClassFromOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDeletionsToInProgressCommit(false);
    }

    @Test
    public void testDeleteClassFromOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(classId.stringValue())).queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteClassFromOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteClassFromOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(classId.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteClassFromOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteMissingClassFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/classes/"
                + encode(missingIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    // Test get datatypes in ontology

    @Test
    public void testGetDatatypesInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDatatypes(getResponse(response), datatypes);
    }

    @Test
    public void testGetDatatypesInOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDatatypes(getResponse(response), datatypes);
    }

    @Test
    public void testGetDatatypesInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDatatypesInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDatatypes(getResponse(response), datatypes);
    }

    @Test
    public void testGetDatatypesInOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes").request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDatatypes(getResponse(response), datatypes);
    }

    @Test
    public void testGetDatatypesInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDatatypesInOntologyWhenNoDatatypes() {
        when(ontology.getAllDatatypes()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDatatypes(getResponse(response), Collections.EMPTY_SET);
    }

    // Test add datatype to ontology

    @Test
    public void testAddDatatypeToOntology() {
        JSONObject entity = createJsonOfType(OWL.DATATYPEPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-datatype");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(true);
    }

    @Test
    public void testAddWrongTypedDatatypeToOntology() {
        JSONObject entity = new JSONObject()
                .element("@id", "http://mobi.com/new-datatype");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddInvalidDatatypeToOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes").request()
                .post(Entity.json(INVALID_JSON));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddDatatypeToOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = createJsonOfType(OWL.DATATYPEPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-datatype");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes").request()
                .post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(false);
    }

    // Test delete datatype from ontology

    @Test
    public void testDeleteDatatypeFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(datatypeIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteDatatypeFromOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(datatypeIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDeletionsToInProgressCommit(false);
    }

    @Test
    public void testDeleteDatatypeFromOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(datatypeIRI.stringValue())).queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteDatatypeFromOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(datatypeIRI.stringValue())).queryParam("branchId", branchId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteDatatypeFromOntologyMissingBranchIdAndMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(datatypeIRI.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteDatatypeFromOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(datatypeIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteMissingDatatypeFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/datatypes/"
                + encode(missingIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    // Test get object properties in ontology

    @Test
    public void testGetObjectPropertiesInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertObjectProperties(getResponse(response), objectProperties);
    }

    @Test
    public void testGetObjectPropertiesInOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertObjectProperties(getResponse(response), objectProperties);
    }

    @Test
    public void testGetObjectPropertiesInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetObjectPropertiesInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertObjectProperties(getResponse(response), objectProperties);
    }

    @Test
    public void testGetObjectPropertiesInOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertObjectProperties(getResponse(response), objectProperties);
    }

    @Test
    public void testGetObjectPropertiesInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetObjectPropertiesInOntologyWhenNoObjectProperties() {
        when(ontology.getAllObjectProperties()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertObjectProperties(getResponse(response), Collections.EMPTY_SET);
    }

    // Test add object property to ontology

    @Test
    public void testAddObjectPropertyToOntology() {
        JSONObject entity = createJsonOfType(OWL.OBJECTPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-object-property");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(true);
    }

    @Test
    public void testAddWrongTypedObjectPropertyToOntology() {
        JSONObject entity = new JSONObject()
                .element("@id", "http://mobi.com/new-object-property");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddInvalidObjectPropertyToOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .request().post(Entity.json(INVALID_JSON));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddObjectPropertyToOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = createJsonOfType(OWL.OBJECTPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-object-property");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(false);
    }

    // Test delete object property from ontology

    @Test
    public void testDeleteObjectPropertyFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(objectPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteObjectPropertyFromOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(objectPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDeletionsToInProgressCommit(false);
    }

    @Test
    public void testDeleteObjectPropertyFromOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(objectPropertyIRI.stringValue())).queryParam("commitId", commitId.stringValue()).request()
                .delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteObjectPropertyFromOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(objectPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue()).request()
                .delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteObjectPropertyFromOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(objectPropertyIRI.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteObjectPropertyFromOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(objectPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteMissingObjectPropertyFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/object-properties/"
                + encode(missingIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    // Test get data properties in ontology

    @Test
    public void testGetDataPropertiesInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDataProperties(getResponseArray(response), dataProperties);
    }

    @Test
    public void testGetDataPropertiesInOntologyWhenNoInProgressCommit() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDataProperties(getResponseArray(response), dataProperties);
    }

    @Test
    public void testGetDataPropertiesInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDataPropertiesInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDataProperties(getResponseArray(response), dataProperties);
    }

    @Test
    public void testGetDataPropertiesInOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDataProperties(getResponseArray(response), dataProperties);
    }

    @Test
    public void testGetDataPropertiesInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDataPropertiesInOntologyWhenNoDataProperties() {
        when(ontology.getAllDataProperties()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDataProperties(getResponseArray(response), Collections.EMPTY_SET);
    }

    // Test add data property to ontology

    @Test
    public void testAddDataPropertyToOntology() {
        JSONObject entity = createJsonOfType(OWL.DATATYPEPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-data-property");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(true);
    }

    @Test
    public void testAddWrongTypedDataPropertyToOntology() {
        JSONObject entity = new JSONObject()
                .element("@id", "http://mobi.com/new-data-property");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddInvalidDataPropertyToOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .request().post(Entity.json(INVALID_JSON));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddDataPropertyToOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = createJsonOfType(OWL.DATATYPEPROPERTY.stringValue())
                .element("@id", "http://mobi.com/new-data-property");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(false);
    }

    // Test delete data property from ontology

    @Test
    public void testDeleteDataPropertyFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(dataPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteDataPropertyFromOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(dataPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDeletionsToInProgressCommit(false);
    }

    @Test
    public void testDeleteDataPropertyFromOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(dataPropertyIRI.stringValue())).queryParam("commitId", commitId.stringValue()).request()
                .delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteDataPropertyFromOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(dataPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue()).request()
                .delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteDataPropertyFromOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(dataPropertyIRI.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteDataPropertyFromOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(dataPropertyIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteMissingDataPropertyFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties/"
                + encode(missingIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    // Test get named individuals in ontology

    @Test
    public void testGetNamedIndividualsInOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertIndividuals(getResponse(response), individuals);
    }

    @Test
    public void testGetNamedIndividualsInOntologyWhenNoInProgressCommit() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertIndividuals(getResponse(response), individuals);
    }

    @Test
    public void testGetNamedIndividualsInOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetNamedIndividualsInOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertIndividuals(getResponse(response), individuals);
    }

    @Test
    public void testGetNamedIndividualsInOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertIndividuals(getResponse(response), individuals);
    }

    @Test
    public void testGetNamedIndividualsInOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetNamedIndividualsInOntologyWhenNoNamedIndividuals() {
        when(ontology.getAllIndividuals()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertIndividuals(getResponse(response), Collections.EMPTY_SET);
    }

    // Test add named individual to ontology

    @Test
    public void testAddNamedIndividualToOntology() {
        JSONObject entity = createJsonOfType(OWL.INDIVIDUAL.stringValue())
                .element("@id", "http://mobi.com/new-named-individual");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(true);
    }

    @Test
    public void testAddWrongTypedNamedIndividualToOntology() {
        JSONObject entity = new JSONObject()
                .element("@id", "http://mobi.com/new-named-individual");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddInvalidNamedIndividualToOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .request().post(Entity.json(INVALID_JSON));

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testAddNamedIndividualToOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        JSONObject entity = createJsonOfType(OWL.INDIVIDUAL.stringValue())
                .element("@id", "http://mobi.com/new-named-individual");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 201);
        assertAdditionsToInProgressCommit(false);
    }

    @Test
    public void testAddNamedIndividualToOntologyWhenGetRecordIsEmpty() {
        when(catalogManager.getRecord(catalogId, recordId, ontologyRecordFactory)).thenReturn(Optional.empty());

        JSONObject entity = createJsonOfType(OWL.INDIVIDUAL.stringValue())
                .element("@id", "http://mobi.com/new-named-individual");

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/data-properties")
                .request().post(Entity.json(entity));

        assertEquals(response.getStatus(), 400);
    }

    // Test delete named individual from ontology

    @Test
    public void testDeleteNamedIndividualFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(individualIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteNamedIndividualFromOntologyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(individualIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertDeletionsToInProgressCommit(false);
    }

    @Test
    public void testDeleteNamedIndividualFromOntologyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(individualIRI.stringValue())).queryParam("commitId", commitId.stringValue()).request()
                .delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteNamedIndividualFromOntologyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(individualIRI.stringValue())).queryParam("branchId", branchId.stringValue()).request()
                .delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteNamedIndividualFromOntologyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(individualIRI.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertDeletionsToInProgressCommit(true);
    }

    @Test
    public void testDeleteNamedIndividualFromOntologyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(individualIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteMissingNamedIndividualFromOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/named-individuals/"
                + encode(missingIRI.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 400);
    }

    // Test get IRIs in imported ontologies

    @Test
    public void testGetIRIsInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) -> {
            assertAnnotations(responseObject, annotationProperties, annotations);
            assertClasses(responseObject, classes);
            assertDatatypes(responseObject, datatypes);
            assertObjectProperties(responseObject, objectProperties);
            assertDataPropertyIRIs(responseObject, dataProperties);
            assertIndividuals(responseObject, individuals);
            assertDerivedConcepts(responseObject, derivedConcepts);
            assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
        });
    }

    @Test
    public void testGetIRIsInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) -> {
            assertAnnotations(responseObject, annotationProperties, annotations);
            assertClasses(responseObject, classes);
            assertDatatypes(responseObject, datatypes);
            assertObjectProperties(responseObject, objectProperties);
            assertDataPropertyIRIs(responseObject, dataProperties);
            assertIndividuals(responseObject, individuals);
            assertDerivedConcepts(responseObject, derivedConcepts);
            assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
        });
    }

    @Test
    public void testGetIRIsInImportedOntologiesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetIRIsInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) -> {
            assertAnnotations(responseObject, annotationProperties, annotations);
            assertClasses(responseObject, classes);
            assertDatatypes(responseObject, datatypes);
            assertObjectProperties(responseObject, objectProperties);
            assertDataPropertyIRIs(responseObject, dataProperties);
            assertIndividuals(responseObject, individuals);
            assertDerivedConcepts(responseObject, derivedConcepts);
            assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
        });
    }

    @Test
    public void testGetIRIsInImportedOntologiesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) -> {
            assertAnnotations(responseObject, annotationProperties, annotations);
            assertClasses(responseObject, classes);
            assertDatatypes(responseObject, datatypes);
            assertObjectProperties(responseObject, objectProperties);
            assertDataPropertyIRIs(responseObject, dataProperties);
            assertIndividuals(responseObject, individuals);
            assertDerivedConcepts(responseObject, derivedConcepts);
            assertDerivedConceptSchemes(responseObject, derivedConceptSchemes);
        });
    }

    @Test
    public void testGetIRIsInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetIRIsInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-iris")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get imports closure

    @Test
    public void testGetImportsClosure() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertEquals(JSONArray.fromObject(response.readEntity(String.class)), importedOntologyResults);
    }

    @Test
    public void testGetImportsClosureWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertEquals(JSONArray.fromObject(response.readEntity(String.class)), importedOntologyResults);
    }

    @Test
    public void testGetImportsClosureWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetImportsClosureMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertEquals(JSONArray.fromObject(response.readEntity(String.class)), importedOntologyResults);
    }

    @Test
    public void testGetImportsClosureMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertEquals(JSONArray.fromObject(response.readEntity(String.class)), importedOntologyResults);
    }

    @Test
    public void testGetImportsClosureWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetImportsClosureWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-ontologies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get annotations in imported ontologies

    @Test
    public void testGetAnnotationsInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertAnnotations(responseObject, annotationProperties, annotations));
    }

    @Test
    public void testGetAnnotationsInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertAnnotations(responseObject, annotationProperties, annotations));
    }

    @Test
    public void testGetAnnotationsInImportedOntologiesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetAnnotationsInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertAnnotations(responseObject, annotationProperties, annotations));
    }

    @Test
    public void testGetAnnotationsInImportedOntologiesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertAnnotations(responseObject, annotationProperties, annotations));
    }

    @Test
    public void testGetAnnotationsInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetAnnotationsInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-annotations")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get classes in imported ontologies

    @Test
    public void testGetClassesInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertClasses(responseObject, classes));
    }

    @Test
    public void testGetClassesInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertClasses(responseObject, classes));
    }

    @Test
    public void testGetClassesInImportedOntologiesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetClassesInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertClasses(responseObject, classes));
    }

    @Test
    public void testGetClassesInImportedOntologiesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertClasses(responseObject, classes));
    }

    @Test
    public void testGetClassesInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetClassesInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-classes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get datatypes in imported ontologies

    @Test
    public void testGetDatatypesInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDatatypes(responseObject, datatypes));
    }

    @Test
    public void testGetDatatypesInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDatatypes(responseObject, datatypes));
    }

    @Test
    public void testGetDatatypesInImportedOntologiesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDatatypesInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDatatypes(responseObject, datatypes));
    }

    @Test
    public void testGetDatatypesInImportedOntologiesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDatatypes(responseObject, datatypes));
    }

    @Test
    public void testGetDatatypesInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDatatypesInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/imported-datatypes")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                        commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get object properties in imported ontologies

    @Test
    public void testGetObjectPropertiesInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertObjectProperties(responseObject, objectProperties));
    }

    @Test
    public void testGetObjectPropertiesInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertObjectProperties(responseObject, objectProperties));
    }

    @Test
    public void testGetObjectPropertiesInImportedOntologiesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetObjectPropertiesInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertObjectProperties(responseObject, objectProperties));
    }

    @Test
    public void testGetObjectPropertiesInImportedOntologiesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertObjectProperties(responseObject, objectProperties));
    }

    @Test
    public void testGetObjectPropertiesInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetObjectPropertiesInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-object-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get data properties in imported ontologies

    @Test
    public void testGetDataPropertiesInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDataPropertyIRIs(responseObject, dataProperties));
    }

    @Test
    public void testGetDataPropertiesInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDataPropertyIRIs(responseObject, dataProperties));
    }

    @Test
    public void testGetDataPropertiesInImportedOntologiesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDataPropertiesInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDataPropertyIRIs(responseObject, dataProperties));
    }

    @Test
    public void testGetDataPropertiesInImportedOntologiesMissingBranchIdAndCommmitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertDataPropertyIRIs(responseObject, dataProperties));
    }

    @Test
    public void testGetDataPropertiesInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetDataPropertiesInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-data-properties").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get named individuals in imported ontologies

    @Test
    public void testGetNamedIndividualsInImportedOntologies() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertIndividuals(responseObject, individuals));
    }

    @Test
    public void testGetNamedIndividualsInImportedOntologiesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertIndividuals(responseObject, individuals));
    }

    @Test
    public void testGetNamedIndividualsInImportedOntologiesWithCommitIdMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetNamedIndividualsInImportedOntologiesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertIndividuals(responseObject, individuals));
    }

    @Test
    public void testGetNamedIndividualsInImportedOntologiesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertImportedOntologies(JSONArray.fromObject(response.readEntity(String.class)), (responseObject) ->
                assertIndividuals(responseObject, individuals));
    }

    @Test
    public void testGetNamedIndividualsInImportedOntologiesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetNamedIndividualsInImportedOntologiesWhenNoImports() {
        when(ontology.getImportsClosure()).thenReturn(Collections.EMPTY_SET);

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/imported-named-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 204);
    }

    // Test get ontology class hierarchy

    @Test
    public void testGetOntologyClassHierarchy() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/class-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubClassesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subClassesOfResult);
    }

    @Test
    public void testGetOntologyClassHierarchyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/class-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubClassesOf(ontology);
        assertGetOntology(false);
        assertEquals(getResponse(response), subClassesOfResult);
    }

    @Test
    public void testGetOntologyClassHierarchyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/class-hierarchies")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetOntologyClassHierarchyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/class-hierarchies")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getSubClassesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subClassesOfResult);
    }

    @Test
    public void testGetOntologyClassHierarchyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/class-hierarchies")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getSubClassesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subClassesOfResult);
    }

    @Test
    public void testGetOntologyClassHierarchyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/class-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get ontology object property hierarchy

    @Test
    public void testGetOntologyObjectPropertyHierarchy() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/object-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubObjectPropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subObjectPropertiesOfResult);
    }

    @Test
    public void testGetOntologyObjectPropertyHierarchyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/object-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubObjectPropertiesOf(ontology);
        assertGetOntology(false);
        assertEquals(getResponse(response), subObjectPropertiesOfResult);
    }

    @Test
    public void testGetOntologyObjectPropertyHierarchyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/object-property-hierarchies").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetOntologyObjectPropertyHierarchyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/object-property-hierarchies").queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getSubObjectPropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subObjectPropertiesOfResult);
    }

    @Test
    public void testGetOntologyObjectPropertyHierarchyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/object-property-hierarchies").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getSubObjectPropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subObjectPropertiesOfResult);
    }

    @Test
    public void testGetOntologyObjectPropertyHierarchyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/object-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get ontology data property hierarchy

    @Test
    public void testGetOntologyDataPropertyHierarchy() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/data-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubDatatypePropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subDatatypePropertiesOfResult);
    }

    @Test
    public void testGetOntologyDataPropertyHierarchyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/data-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubDatatypePropertiesOf(ontology);
        assertGetOntology(false);
        assertEquals(getResponse(response), subDatatypePropertiesOfResult);
    }

    @Test
    public void testGetOntologyDataPropertyHierarchyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/data-property-hierarchies").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetOntologyDataPropertyHierarchyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/data-property-hierarchies").queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getSubDatatypePropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subDatatypePropertiesOfResult);
    }

    @Test
    public void testGetOntologyDataPropertyHierarchyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/data-property-hierarchies").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getSubDatatypePropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subDatatypePropertiesOfResult);
    }

    @Test
    public void testGetOntologyDataPropertyHierarchyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/data-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get ontology annotation property hierarchy

    @Test
    public void testGetOntologyAnnotationPropertyHierarchy() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/annotation-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubAnnotationPropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subAnnotationPropertiesOfResult);
    }

    @Test
    public void testGetOntologyAnnotationPropertyHierarchyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/annotation-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getSubAnnotationPropertiesOf(ontology);
        assertGetOntology(false);
        assertEquals(getResponse(response), subAnnotationPropertiesOfResult);
    }

    @Test
    public void testGetOntologyAnnotationPropertyHierarchyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/annotation-property-hierarchies").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetOntologyAnnotationPropertyHierarchyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/annotation-property-hierarchies").queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getSubAnnotationPropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subAnnotationPropertiesOfResult);
    }

    @Test
    public void testGetOntologyAnnotationPropertyHierarchyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/annotation-property-hierarchies").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getSubAnnotationPropertiesOf(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), subAnnotationPropertiesOfResult);
    }

    @Test
    public void testGetOntologyAnnotationPropertyHierarchyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/annotation-property-hierarchies").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get concept hierarchy

    @Test
    public void testGetConceptHierarchy() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getConceptRelationships(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), conceptHierarchyResult);
    }

    @Test
    public void testGetConceptHierarchyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getConceptRelationships(ontology);
        assertGetOntology(false);
        assertEquals(getResponse(response), conceptHierarchyResult);
    }

    @Test
    public void testGetConceptHierarchyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-hierarchies")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetConceptHierarchyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-hierarchies")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getConceptRelationships(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), conceptHierarchyResult);
    }

    @Test
    public void testGetConceptHierarchyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-hierarchies")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getConceptRelationships(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), conceptHierarchyResult);
    }

    @Test
    public void testGetConceptHierarchyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get concept scheme hierarchy

    @Test
    public void testGetConceptSchemeHierarchy() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-scheme-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getConceptSchemeRelationships(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), conceptSchemeHierarchyResult);
    }

    @Test
    public void testGetConceptSchemeHierarchyWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-scheme-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getConceptSchemeRelationships(ontology);
        assertGetOntology(false);
        assertEquals(getResponse(response), conceptSchemeHierarchyResult);
    }

    @Test
    public void testGetConceptSchemeHierarchyWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-scheme-hierarchies")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetConceptSchemeHierarchyMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-scheme-hierarchies")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getConceptSchemeRelationships(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), conceptSchemeHierarchyResult);
    }

    @Test
    public void testGetConceptSchemeHierarchyMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-scheme-hierarchies")
                .request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getConceptSchemeRelationships(ontology);
        assertGetOntology(true);
        assertEquals(getResponse(response), conceptSchemeHierarchyResult);
    }

    @Test
    public void testGetConceptSchemeHierarchyWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/concept-scheme-hierarchies")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get classes with individuals

    @Test
    public void testGetClassesWithIndividuals() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/classes-with-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertEquals(getResponse(response), individualsOfResult);
    }

    @Test
    public void testGetClassesWithIndividualsWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/classes-with-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertEquals(getResponse(response), individualsOfResult);
    }

    @Test
    public void testGetClassesWithIndividualsWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/classes-with-individuals").queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetClassesWithIndividualsMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/classes-with-individuals").queryParam("branchId", branchId.stringValue()).request().get();
        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertEquals(getResponse(response), individualsOfResult);
    }

    @Test
    public void testGetClassesWithIndividualsMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/classes-with-individuals").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertEquals(getResponse(response), individualsOfResult);
    }

    @Test
    public void testGetClassesWithIndividualsWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue())
                + "/classes-with-individuals").queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get entity usages when queryType is "select"

    @Test
    public void testGetEntityUsages() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "select").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getEntityUsages(ontology, classId);
        assertGetOntology(true);
        assertEquals(getResponse(response), entityUsagesResult);
    }

    @Test
    public void testGetEntityUsagesWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "select").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).getEntityUsages(ontology, classId);
        assertGetOntology(false);
        assertEquals(getResponse(response), entityUsagesResult);
    }

    @Test
    public void testGetEntityUsagesWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("commitId", commitId.stringValue())
                .queryParam("queryType", "select").request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetEntityUsagesMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("queryType", "select").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).getEntityUsages(ontology, classId);
        assertGetOntology(true);
        assertEquals(getResponse(response), entityUsagesResult);
    }

    @Test
    public void testGetEntityUsagesMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("queryType", "select").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).getEntityUsages(ontology, classId);
        assertGetOntology(true);
        assertEquals(getResponse(response), entityUsagesResult);
    }

    @Test
    public void testGetEntityUsagesWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "select").request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get entity usages when queryType is "construct"

    @Test
    public void testGetEntityUsagesWhenConstruct() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "construct").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).constructEntityUsages(ontology, classId);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), entityUsagesConstruct);
    }

    @Test
    public void testGetEntityUsagesWhenNoInProgressCommitWhenConstruct() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "construct").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        verify(ontologyManager).constructEntityUsages(ontology, classId);
        assertGetOntology(false);
        assertEquals(response.readEntity(String.class), entityUsagesConstruct);
    }

    @Test
    public void testGetEntityUsagesWithCommitIdAndMissingBranchIdWhenConstruct() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("commitId", commitId.stringValue())
                .queryParam("queryType", "construct").request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetEntityUsagesMissingCommitIdWhenConstruct() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue())
                .queryParam("queryType", "construct").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        verify(ontologyManager).constructEntityUsages(ontology, classId);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), entityUsagesConstruct);
    }

    @Test
    public void testGetEntityUsagesMissingBranchIdAndCommitIdWhenConstruct() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("queryType", "construct").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        verify(ontologyManager).constructEntityUsages(ontology, classId);
        assertGetOntology(true);
        assertEquals(response.readEntity(String.class), entityUsagesConstruct);
    }

    @Test
    public void testGetEntityUsagesWhenRetrieveOntologyIsEmptyWhenConstruct() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "construct").request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get entity usages when queryType is "wrong"

    @Test
    public void testGetEntityUsagesWhenQueryTypeIsWrong() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/entity-usages/"
                + encode(classId.stringValue())).queryParam("branchId", branchId.stringValue()).queryParam("commitId",
                commitId.stringValue()).queryParam("queryType", "wrong").request().get();

        assertEquals(response.getStatus(), 400);
    }

    // Test get search results

    @Test
    public void testGetSearchResults() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("searchText", "class").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
        assertEquals(getResponse(response), searchResults);
    }

    @Test
    public void testGetSearchResultsWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("searchText", "class").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(false);
        assertEquals(getResponse(response), searchResults);
    }

    @Test
    public void testGetSearchResultsWithNoMatches() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("searchText", "nothing").request().get();

        assertEquals(response.getStatus(), 204);
        verify(ontologyManager).retrieveOntology(recordId, branchId, commitId);
        assertGetOntology(true);
    }

    @Test
    public void testGetSearchResultsMissingSearchText() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue()).request()
                .get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetSearchResultsWithCommitIdAndMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("commitId", commitId.stringValue()).queryParam("searchText", "class").request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetSearchResultsMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("branchId", branchId.stringValue()).queryParam("searchText", "class").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId, branchId);
        assertGetOntology(true);
        assertEquals(getResponse(response), searchResults);
    }

    @Test
    public void testGetSearchResultsMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("searchText", "class").request().get();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).retrieveOntology(recordId);
        assertGetOntology(true);
        assertEquals(getResponse(response), searchResults);
    }

    @Test
    public void testGetSearchResultsWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/search-results")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .queryParam("searchText", "class").request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDeleteOntology() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue())).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).deleteOntology(recordId);
        verify(provUtils).startDeleteActivity(user, recordId);
        verify(provUtils).endDeleteActivity(deleteActivity, record);
    }

    @Test
    public void testDeleteOntologyError() {
        Mockito.doThrow(new MobiException("I'm an exception!")).when(ontologyManager).deleteOntology(eq(recordId));
        Response response = target().path("ontologies/" + encode(recordId.stringValue())).request().delete();

        assertEquals(response.getStatus(), 500);
        verify(ontologyManager, times(0)).deleteOntologyBranch(any(), any());
    }

    @Test
    public void testDeleteOntologyBranch() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 200);
        verify(ontologyManager).deleteOntologyBranch(recordId, branchId);
    }

    @Test
    public void testDeleteOntologyBranchError() {
        Mockito.doThrow(new MobiException("I'm an exception!")).when(ontologyManager).deleteOntologyBranch(eq(recordId), eq(branchId));
        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue()).request().delete();

        assertEquals(response.getStatus(), 500);
        verify(ontologyManager, times(0)).deleteOntology(any());
    }

    // Test upload changes

    @Test
    public void testUploadChangesToOntology() {
        when(catalogManager.getCompiledResource(eq(recordId), eq(branchId), eq(commitId)))
                .thenReturn(ontologyModel);
        when(catalogManager.getInProgressCommit(eq(catalogId), eq(recordId),
                any(User.class))).thenReturn(Optional.empty());

        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/test-ontology.ttl"), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue())
                .request()
                .put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertGetUserFromContext();
        verify(ontologyManager).createOntology(any(FileInputStream.class));
        verify(catalogManager).getCompiledResource(eq(recordId), eq(branchId), eq(commitId));
        verify(catalogManager).getDiff(any(Model.class), any(Model.class));
        verify(catalogManager, times(2)).getInProgressCommit(eq(catalogId), eq(recordId), any(User.class));
        verify(catalogManager).updateInProgressCommit(eq(catalogId), eq(recordId), any(IRI.class), any(), any());
    }

    @Test
    public void testUploadChangesToOntologyWithoutBranchId() {
        when(catalogManager.getCompiledResource(eq(recordId), eq(branchId), eq(commitId)))
                .thenReturn(ontologyModel);
        when(catalogManager.getInProgressCommit(eq(catalogId), eq(recordId),
                any(User.class))).thenReturn(Optional.empty());
        when(catalogManager.getMasterBranch(eq(catalogId), eq(recordId))).thenReturn(branch);
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/test-ontology.ttl"), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("commitId", commitId.stringValue())
                .request()
                .put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        verify(catalogManager, times(0)).getMasterBranch(eq(catalogId), eq(recordId));
        verify(ontologyManager, times(0)).createOntology(any(FileInputStream.class));
        verify(catalogManager, times(0)).getCompiledResource(eq(recordId), eq(branchId), eq(commitId));
        verify(catalogManager, times(0)).getDiff(any(Model.class), any(Model.class));
        verify(catalogManager, times(1)).getInProgressCommit(eq(catalogId), eq(recordId), any(User.class));
        verify(catalogManager, times(0)).updateInProgressCommit(eq(catalogId), eq(recordId), any(IRI.class), any(), any());
    }

    @Test
    public void testUploadChangesToOntologyWithoutCommitId() {
        when(catalogManager.getCompiledResource(eq(commitId), eq(branchId), eq(recordId)))
                .thenReturn(ontologyModel);
        when(catalogManager.getInProgressCommit(eq(catalogId), eq(recordId),
                any(User.class))).thenReturn(Optional.empty());
        when(catalogManager.getHeadCommit(eq(catalogId), eq(recordId), eq(branchId))).thenReturn(commit);
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/test-ontology.ttl"), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue())
                .request()
                .put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertGetUserFromContext();
        verify(catalogManager).getHeadCommit(eq(catalogId), eq(recordId), eq(branchId));
        verify(ontologyManager).createOntology(any(FileInputStream.class));
        verify(catalogManager).getCompiledResource(eq(recordId), eq(branchId), eq(commitId));
        verify(catalogManager).getDiff(any(Model.class), any(Model.class));
        verify(catalogManager, times(2)).getInProgressCommit(eq(catalogId), eq(recordId), any(User.class));
        verify(catalogManager).updateInProgressCommit(eq(catalogId), eq(recordId), any(IRI.class), any(), any());
    }

    @Test
    public void testUploadChangesToOntologyWithExistingInProgressCommit() {
        when(catalogManager.getInProgressCommit(eq(catalogManager.getLocalCatalogIRI()), eq(recordId), any(User.class)))
                .thenReturn(Optional.of(inProgressCommit));

        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("file", getClass().getResourceAsStream("/search-results.json"), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Response response = target().path("ontologies/" + encode(recordId.stringValue()))
                .queryParam("branchId", branchId.stringValue())
                .queryParam("commitId", commitId.stringValue())
                .request()
                .put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));

        assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testGetFailedImports() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/failed-imports")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().get();

        assertEquals(response.getStatus(), 200);
        JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
        assertEquals(array.size(), 1);
        assertEquals(array.get(0), importedOntologyIRI.stringValue());
        assertGetOntology(true);
    }

    @Test
    public void testGetFailedImportsWhenNoInProgressCommit() {
        setNoInProgressCommit();

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/failed-imports")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().get();

        assertEquals(response.getStatus(), 200);
        JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
        assertEquals(array.size(), 1);
        assertEquals(array.get(0), importedOntologyIRI.stringValue());
        assertGetOntology(false);
    }

    @Test
    public void testGetFailedImportsMissingBranchId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/failed-imports")
                .queryParam("commitId", commitId.stringValue()).request().get();

        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testGetFailedImportsMissingCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/failed-imports")
                .queryParam("branchId", branchId.stringValue()).request().get();

        assertEquals(response.getStatus(), 200);
        JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
        assertEquals(array.size(), 1);
        assertEquals(array.get(0), importedOntologyIRI.stringValue());
        assertGetOntology(true);
    }

    @Test
    public void testGetFailedImportsMissingBranchIdAndCommitId() {
        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/failed-imports")
                .request().get();

        assertEquals(response.getStatus(), 200);
        JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
        assertEquals(array.size(), 1);
        assertEquals(array.get(0), importedOntologyIRI.stringValue());
        assertGetOntology(true);
    }

    @Test
    public void testGetFailedImportsWhenRetrieveOntologyIsEmpty() {
        when(ontologyManager.retrieveOntology(any(Resource.class), any(Resource.class), any(Resource.class)))
                .thenReturn(Optional.empty());

        Response response = target().path("ontologies/" + encode(recordId.stringValue()) + "/failed-imports")
                .queryParam("branchId", branchId.stringValue()).queryParam("commitId", commitId.stringValue())
                .request().get();

        assertEquals(response.getStatus(), 400);
    }
}