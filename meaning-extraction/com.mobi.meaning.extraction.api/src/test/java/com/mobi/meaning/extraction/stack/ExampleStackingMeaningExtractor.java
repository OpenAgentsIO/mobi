package com.mobi.meaning.extraction.stack;

/*-
 * #%L
 * meaning.extraction.api
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2017 iNovex Information Systems, Inc.
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

import com.mobi.meaning.extraction.MeaningExtractionException;
import com.mobi.meaning.extraction.ontology.ExtractedOntology;
import com.mobi.rdf.api.Model;

import java.io.InputStream;
import java.nio.file.Path;

public class ExampleStackingMeaningExtractor extends AbstractStackingMeaningExtractor<ExampleStackItem> {

    @Override
    public Model extractMeaning(Path rawFile, ExtractedOntology managedOntology) throws MeaningExtractionException {
        throw new MeaningExtractionException("Not implemented in this test object");
    }

    @Override
    public Model extractMeaning(InputStream dataStream, String entityIdentifier, ExtractedOntology managedOntology) throws MeaningExtractionException {
        throw new MeaningExtractionException("Not implemented in this test object");
    }
}
