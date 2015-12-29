package org.matonto.ontology.core.api.axiom;

import org.matonto.ontology.core.api.propertyexpression.DataPropertyExpression;
import org.matonto.rdf.api.Literal;
import org.matonto.ontology.core.api.Individual;


public interface NegativeDataPropertyAssertionAxiom extends AssertionAxiom {

	Individual getSubject();
	
	DataPropertyExpression getDataProperty();
	
	Literal getValue();
}
