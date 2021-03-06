package com.mobi.cache.impl.repository;

/*-
 * #%L
 * com.mobi.cache.impl.repository
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2019 iNovex Information Systems, Inc.
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

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import com.mobi.dataset.api.DatasetManager;
import com.mobi.ontology.utils.cache.repository.OntologyDatasets;
import com.mobi.persistence.utils.RepositoryResults;
import com.mobi.rdf.api.Statement;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.repository.api.Repository;
import com.mobi.repository.api.RepositoryConnection;
import com.mobi.repository.api.RepositoryManager;
import org.apache.karaf.scheduler.Job;
import org.apache.karaf.scheduler.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * CleanRepositoryCache is a reoccurring Scheduler job that runs based on a Quartz Cron statement. A
 * "scheduler.expression" set to "0 0 * * * ?" will have the job run every hour on the hour. The job determines if a
 * dataset living in the repository cache is past the expiration time. This time is set by the `expiry` property
 * (in seconds) in the configuration. If the time at which the job runs is past the last accessed time of the dataset
 * plus the expiry time, then a safe delete is performed on the dataset.
 */
@Component(
        immediate = true,
        name = CleanRepositoryCache.COMPONENT_NAME,
        properties = {
                "scheduler.name=CleanRepositoryCache",
                "scheduler.concurrent:Boolean=false"
        },
        configurationPolicy = ConfigurationPolicy.require
)
public class CleanRepositoryCache implements Job {
    private final Logger log = LoggerFactory.getLogger(CleanRepositoryCache.class);
    static final String COMPONENT_NAME = "com.mobi.cache.impl.repository.CleanRepositoryCache";

    private DatasetManager datasetManager;
    private RepositoryManager repositoryManager;
    private ValueFactory vf;
    private String repoId;
    private long expirySeconds;

    @Reference
    public void setDatasetManager(DatasetManager datasetManager) {
        this.datasetManager = datasetManager;
    }

    @Reference
    public void setRepositoryManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    @Reference
    public void setValueFactory(ValueFactory vf) {
        this.vf = vf;
    }

    @Activate
    public void start(Map<String, Object> props) {
        CleanRepositoryCacheConfig config = Configurable.createConfigurable(CleanRepositoryCacheConfig.class, props);

        if (props.containsKey("repoId")) {
            this.repoId = config.repoId();
        } else {
            this.repoId = "ontologyCache";
        }

        if (props.containsKey("expiry")) {
            this.expirySeconds = config.expiry();
        } else {
            this.expirySeconds = 1800;
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        start(props);
    }

    @Override
    public void execute(JobContext context) {
        log.trace("Starting CleanRepositoryCache Job");
        long startTime = System.currentTimeMillis();
        Repository cacheRepo = repositoryManager.getRepository(repoId)
                .orElseThrow(() -> new IllegalStateException("Ontology Cache Repository" + repoId + " must exist"));

        try (RepositoryConnection conn = cacheRepo.getConnection()) {
            List<Statement> statements = RepositoryResults.asList(
                    conn.getStatements(null, vf.createIRI(OntologyDatasets.TIMESTAMP_IRI_STRING), null));

            OffsetDateTime now = OffsetDateTime.now();
            statements.forEach(statement -> {
                if (now.isAfter(OffsetDateTime.parse(statement.getObject().stringValue()).plusSeconds(expirySeconds))) {
                    log.debug("Evicting expired dataset: " + statement.getSubject().stringValue());
                    datasetManager.safeDeleteDataset(statement.getSubject(), repoId, false);
                }
            });
        }
        log.trace("CleanRepositoryCache Job complete in " + (System.currentTimeMillis() - startTime) + " ms");
    }
}
