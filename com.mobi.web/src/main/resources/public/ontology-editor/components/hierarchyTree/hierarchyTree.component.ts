/*-
 * #%L
 * com.mobi.web
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
import { join, filter, some, pick, find } from 'lodash';

import './hierarchyTree.component.scss';

const template = require('./hierarchyTree.component.html');

/**
 * @ngdoc component
 * @name ontology-editor.component:hierarchyTree
 * @requires shared.service:ontologyManagerService
 * @requires shared.service:ontologyStateService
 * @requires ontology-editor.service:ontologyUtilsManagerService
 * @requires shared.service:utilService
 * @requires shared.service:prefixes
 *
 * @description
 * `hierarchyTree` is a component which creates a `div` containing a {@link shared.component:searchBar} and
 * hierarchy of {@link ontology-editor.component:treeItem}. When search text is provided, the hierarchy filters what is
 * shown based on value matches with predicates in the {@link shared.service:ontologyManagerService entityNameProps}.
 *
 * @param {Object[]} hierarchy An array which represents a flattened hierarchy
 * @param {Function} updateSearch A function to update the state variable used to track the search filter text
 */
const hierarchyTreeComponent = {
    template,
    bindings: {
        hierarchy: '<',
        index: '<',
        updateSearch: '&',
        resetIndex: '&',
        clickItem: '&?'
    },
    controllerAs: 'dvm',
    controller: hierarchyTreeComponentCtrl
};

hierarchyTreeComponentCtrl.$inject = ['ontologyManagerService', 'ontologyStateService', 'utilService', 'INDENT'];

function hierarchyTreeComponentCtrl(ontologyManagerService, ontologyStateService, utilService, INDENT) {
    var dvm = this;
    var om = ontologyManagerService;
    var util = utilService;
    dvm.indent = INDENT;
    dvm.os = ontologyStateService;
    dvm.searchText = '';
    dvm.filterText = '';
    dvm.filteredHierarchy = [];
    dvm.preFilteredHierarchy = [];

    dvm.$onInit = function() {
        update();
    }
    dvm.$onChanges = function(changesObj) {
        if (!changesObj.hierarchy || !changesObj.hierarchy.isFirstChange()) {
            update();
        }
    }
    dvm.$onDestroy = function() {
        if (dvm.os.listItem.editorTabStates) {
            dvm.resetIndex();
        }
    }
    dvm.click = function(entityIRI) {
        dvm.os.selectItem(entityIRI);
        if (dvm.clickItem) {
            dvm.clickItem({iri: entityIRI});
        }
    }
    dvm.onKeyup = function() {
        dvm.filterText = dvm.searchText;
        update();
    }
    dvm.toggleOpen = function(node) {
        node.isOpened = !node.isOpened;
        dvm.os.setOpened(join(node.path, '.'), node.isOpened);
        dvm.filteredHierarchy = filter(dvm.preFilteredHierarchy, dvm.isShown);
    }
    dvm.searchFilter = function(node) {
        delete node.underline;
        delete node.parentNoMatch;
        delete node.displayNode;
        delete node.entity;
        delete node.isOpened;
        node.entity = dvm.os.getEntityByRecordId(dvm.os.listItem.ontologyRecord.recordId, node.entityIRI);
        node.isOpened = dvm.os.getOpened(dvm.os.joinPath(node.path));
        if (dvm.filterText) {
            var searchValues = pick(node.entity, om.entityNameProps);
            var match = false;
            some(Object.keys(searchValues), key => some(searchValues[key], value => {
                if (value['@value'].toLowerCase().includes(dvm.filterText.toLowerCase()))
                    match = true;
            }));
            if (util.getBeautifulIRI(node.entity['@id']).toLowerCase().includes(dvm.filterText.toLowerCase())) {
                match = true;
            }
            if (match) {
                var path = node.path[0];
                for (var i = 1; i < node.path.length - 1; i++) {
                    var iri = node.path[i];
                    path = path + '.' + iri;
                    dvm.os.setOpened(path, true);

                    var parentNode = find(dvm.hierarchy, {'entityIRI': iri});
                    parentNode.isOpened = true;
                    parentNode.displayNode = true;
                }
                node.underline = true;
            }
            if (!match && node.hasChildren) {
                node.parentNoMatch = true;
                return true;
            }
            return match;
        } else {
            return true;
        }
    }
    dvm.isShown = function (node) {
        var displayNode = (node.indent > 0 && dvm.os.areParentsOpen(node)) || node.indent === 0;
        if (dvm.filterText && node.parentNoMatch) {
            if (node.displayNode === undefined) {
                return false;
            } else {
                return displayNode && node.displayNode;
            }
        }
        return displayNode;
    }

    function update() {
        dvm.updateSearch({value: dvm.filterText});
        dvm.preFilteredHierarchy = filter(dvm.hierarchy, dvm.searchFilter);
        dvm.filteredHierarchy = filter(dvm.preFilteredHierarchy, dvm.isShown);
    }
}

export default hierarchyTreeComponent;