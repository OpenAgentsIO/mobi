package org.matonto.ontology.core.impl.owlapi.classexpression;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.matonto.ontology.core.api.classexpression.ClassExpression;
import org.matonto.ontology.core.api.propertyexpression.ObjectPropertyExpression;
import org.matonto.ontology.core.api.classexpression.ObjectSomeValuesFrom;
import org.matonto.ontology.core.api.types.ClassExpressionType;


public class SimpleObjectSomeValuesFrom 	
	implements ObjectSomeValuesFrom {

	private ObjectPropertyExpression property;
	private ClassExpression classExpression;
	
	public SimpleObjectSomeValuesFrom(@Nonnull ObjectPropertyExpression property, @Nonnull ClassExpression classExpression)
	{
		this.property = property;
		this.classExpression = classExpression;
	}
	
	
	@Override
	public ClassExpressionType getClassExpressionType()
	{
		return ClassExpressionType.OBJECT_SOME_VALUES_FROM;
	}

	
	@Override
	public ObjectPropertyExpression getProperty()
	{
		return property;
	}
	
	
	@Override
	public ClassExpression getClassExpression()
	{
		return classExpression;
	}
	
	
	@Override
	public Set<ClassExpression> asConjunctSet() 
	{
		Set<ClassExpression> conjuncts = new HashSet<ClassExpression>();
		conjuncts.add(this);	
		return conjuncts;
	}

	@Override
	public boolean containsConjunct(@Nonnull ClassExpression ce) 
	{
		return (this.equals(ce) || classExpression.equals(ce));
	}

	@Override
	public Set<ClassExpression> asDisjunctSet() 
	{
		Set<ClassExpression> disjuncts = new HashSet<ClassExpression>();
		disjuncts.add(this);
		disjuncts.add(this.getClassExpression());
		return disjuncts;
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
		    return true;
		
		if (obj instanceof ObjectSomeValuesFrom) {
			ObjectSomeValuesFrom other = (ObjectSomeValuesFrom) obj;
			if(other.getClassExpression().equals(getClassExpression()))
				return other.getProperty().equals(getProperty());
		}
		return false;
	}

}
