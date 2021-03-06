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
import { filter, join, has, findIndex, find, some, pick, get } from 'lodash';

const template = require('./everythingTree.component.html');

/**
 * @ngdoc component
 * @name ontology-editor.component:everythingTree
 * @requires shared.service:ontologyManagerService
 * @requires shared.service:ontologyStateService
 * @requires shared.service:utilService
 *
 * @description
 * `everythingTree` is a component that creates a a `div` containing a {@link shared.component:searchBar} and
 * hierarchy of {@link ontology-editor.component:treeItem}. When search text is provided, the hierarchy filters
 * what is shown based on value matches with predicates in the
 * {@link shared.service:ontologyManagerService entityNameProps}.
 *
 * @param {Object[]} hierarchy An array which represents a flattened everything hierarchy
 * @param {Function} updateSearch A function to update the state variable used to track the search filter text
 */
const everythingTreeComponent = {
    template,
    bindings: {
        updateSearch: '&',
        hierarchy: '<'
    },
    controllerAs: 'dvm',
    controller: everythingTreeComponentCtrl
};

everythingTreeComponentCtrl.$inject = ['ontologyManagerService', 'ontologyStateService', 'utilService', 'INDENT'];

function everythingTreeComponentCtrl(ontologyManagerService, ontologyStateService, utilService, INDENT) {
    var dvm = this;
    var util = utilService
    dvm.indent = INDENT;
    dvm.om = ontologyManagerService;
    dvm.os = ontologyStateService;
    dvm.searchText = '';
    dvm.filterText = '';
    dvm.filteredHierarchy = [];
    dvm.preFilteredHierarchy = [];

    dvm.$onInit = function() {
        update();
    }
    dvm.$onChanges = function(changesObj) {
        if (!changesObj.hierarchy.isFirstChange()) {
            update();
        }
    }
    dvm.onKeyup = function() {
        dvm.filterText = dvm.searchText;
        update();
    }
    dvm.toggleOpen = function(node) {
        node.isOpened = !node.isOpened;
        if (!node.title) {
            dvm.os.setOpened(join(node.path, '.'), node.isOpened);
        } else {
            node.set(dvm.os.listItem.ontologyRecord.recordId, node.isOpened);
        }
        dvm.filteredHierarchy = filter(dvm.preFilteredHierarchy, dvm.isShown);
    }
    dvm.searchFilter = function(node) {
        delete node.underline;
        delete node.parentNoMatch;
        delete node.displayNode;
        delete node.isOpened;
        if (node.title) {
            if (dvm.filterText) {
                node.set(dvm.os.listItem.ontologyRecord.recordId, true);
            }
            node.isOpened = node.get(dvm.os.listItem.ontologyRecord.recordId);
        } else {
            node.isOpened = dvm.os.getOpened(dvm.os.joinPath(node.path));
            if (dvm.filterText) {
                var searchValues = pick(node, dvm.om.entityNameProps);
                var match = false;
                some(Object.keys(searchValues), key => some(searchValues[key], value => {
                    if (value['@value'].toLowerCase().includes(dvm.filterText.toLowerCase()))
                        match = true;
                }));
                if (util.getBeautifulIRI(node['@id']).toLowerCase().includes(dvm.filterText.toLowerCase())) {
                    match = true;
                }
                if (match) {
                    var path = node.path[0];
                    for (var i = 1; i < node.path.length - 1; i++) {
                        var iri = node.path[i];
                        path = path + '.' + iri;
                        dvm.os.setOpened(path, true);

                        var parentNode = find(dvm.hierarchy, {'@id': iri});
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
            }
        }
        return true;
    }
    dvm.isShown = function(node) {
        var displayNode = !has(node, '@id') || (has(node, 'get') && node.get(dvm.os.listItem.ontologyRecord.recordId)) || (!has(node, 'get') && node.indent > 0 && dvm.os.areParentsOpen(node)) || (node.indent === 0 && get(node, 'path', []).length === 2);
        if (dvm.filterText && node['title']) {
            var position = findIndex(dvm.preFilteredHierarchy, 'title');
            if (position === dvm.preFilteredHierarchy.length - 1) {
                node.set(dvm.os.listItem.ontologyRecord.recordId, false);
                return false;
            }
        }
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

export default everythingTreeComponent;