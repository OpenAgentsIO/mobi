package org.matonto.prov.api.builder;

/*-
 * #%L
 * org.matonto.prov.api
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

import org.matonto.jaas.api.ontologies.usermanagement.User;
import org.matonto.ontologies.provo.Activity;
import org.matonto.ontologies.provo.Entity;
import org.matonto.rdf.api.ModelFactory;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.rdf.orm.OrmFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActivityConfig {
    private Set<Class<? extends Activity>> types;
    private User user;
    private Set<Entity> generatedEntities = new HashSet<>();
    private Set<Entity> usedEntities = new HashSet<>();
    private Set<Entity> invalidatedEntities = new HashSet<>();

    private ActivityConfig(Builder builder) {
        this.types = builder.types;
        this.user = builder.user;
        this.generatedEntities = builder.generatedEntities;
        this.usedEntities = builder.usedEntities;
        this.invalidatedEntities = builder.invalidatedEntities;
    }

    public Set<Class<? extends Activity>> getTypes() {
        return types;
    }

    public User getUser() {
        return user;
    }

    public Set<Entity> getGeneratedEntities() {
        return generatedEntities;
    }

    public Set<Entity> getUsedEntities() {
        return usedEntities;
    }

    public Set<Entity> getInvalidatedEntities() {
        return invalidatedEntities;
    }

    public static class Builder {
        private Set<Class<? extends Activity>> types;
        private User user;
        private Set<Entity> generatedEntities = new HashSet<>();
        private Set<Entity> usedEntities = new HashSet<>();
        private Set<Entity> invalidatedEntities = new HashSet<>();

        public Builder(Set<Class<? extends Activity>> types, User user) {
            this.types = types;
            this.user = user;
        }

        public Builder generatedEntity(Entity entity) {
            this.generatedEntities.add(entity);
            return this;
        }

        public Builder usedEntity(Entity entity) {
            this.usedEntities.add(entity);
            return this;
        }

        public Builder invalidatedEntity(Entity entity) {
            this.invalidatedEntities.add(entity);
            return this;
        }

        public ActivityConfig build() {
            return new ActivityConfig(this);
        }
    }
}
