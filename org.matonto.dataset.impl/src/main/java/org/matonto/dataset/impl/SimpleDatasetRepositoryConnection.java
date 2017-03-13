package org.matonto.dataset.impl;

import org.apache.commons.lang.NotImplementedException;
import org.matonto.dataset.api.DatasetConnection;
import org.matonto.dataset.ontology.dataset.Dataset;
import org.matonto.persistence.utils.Statements;
import org.matonto.query.api.BooleanQuery;
import org.matonto.query.api.GraphQuery;
import org.matonto.query.api.Operation;
import org.matonto.query.api.TupleQuery;
import org.matonto.query.api.Update;
import org.matonto.query.exception.MalformedQueryException;
import org.matonto.rdf.api.IRI;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.Statement;
import org.matonto.rdf.api.Value;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.repository.api.RepositoryConnection;
import org.matonto.repository.base.RepositoryConnectionWrapper;
import org.matonto.repository.base.RepositoryResult;
import org.matonto.repository.exception.RepositoryException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SimpleDatasetRepositoryConnection extends RepositoryConnectionWrapper implements DatasetConnection {

    private Resource dataset;
    private String repositoryId;
    private ValueFactory valueFactory;

    public SimpleDatasetRepositoryConnection(RepositoryConnection delegate, Resource dataset, String repositoryId, ValueFactory valueFactory) {
        setDelegate(delegate);
        this.dataset = dataset;
        this.repositoryId = repositoryId;
        this.valueFactory = valueFactory;
    }

    @Override
    public void add(Statement stmt, Resource... contexts) throws RepositoryException {

    }

    @Override
    public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {

    }

    @Override
    public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {

    }

    @Override
    public void remove(Statement stmt, Resource... contexts) throws RepositoryException {

    }

    @Override
    public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {

    }

    @Override
    public void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {

    }

    @Override
    public void clear(Resource... contexts) throws RepositoryException {

    }

    @Override
    public long size(Resource... contexts) throws RepositoryException {
        // TODO: Would this be more efficient with a sparql query? I probably wouldn't need a value factory.
        Set<Resource> graphs = new HashSet<>();
        getGraphs(graphs, Dataset.systemDefaultNamedGraph_IRI);
        getGraphs(graphs, Dataset.defaultNamedGraph_IRI);
        getGraphs(graphs, Dataset.namedGraph_IRI);

        if (varargsPresent(contexts)) {
            graphs.retainAll(Arrays.asList(contexts));
        }

        if (graphs.size() == 0) {
            return 0;
        }
        return getDelegate().size(graphs.toArray(new Resource[graphs.size()]));
    }

    @Override
    public RepositoryResult<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
        return null;
    }

    @Override
    public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
        return null;
    }

    @Override
    public Operation prepareQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Operation prepareQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public TupleQuery prepareTupleQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public TupleQuery prepareTupleQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public GraphQuery prepareGraphQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public GraphQuery prepareGraphQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public BooleanQuery prepareBooleanQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public BooleanQuery prepareBooleanQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Update prepareUpdate(String update) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Update prepareUpdate(String update, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Resource getDataset() {
        return dataset;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    private boolean varargsPresent(Object[] varargs) {
        return !(varargs == null || varargs.length == 0 || varargs[0] == null);
    }

    private void getGraphs(Set<Resource> graphs, String predString) {
        getDelegate().getStatements(getDataset(), valueFactory.createIRI(predString), null)
                .forEach(stmt -> Statements.objectResource(stmt).ifPresent(graphs::add));
    }
}
