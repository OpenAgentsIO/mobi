/*-
 * #%L
 * org.matonto.web
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
(function() {
    'use strict';

    angular
        .module('openOntologyTab', [])
        .directive('openOntologyTab', openOntologyTab);

        openOntologyTab.$inject = ['$filter', 'ontologyManagerService', 'ontologyStateService', 'prefixes',
            'catalogManagerService', 'stateManagerService'];

        function openOntologyTab($filter, ontologyManagerService, ontologyStateService, prefixes,
            catalogManagerService, stateManagerService) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'modules/ontology-editor/directives/openOntologyTab/openOntologyTab.html',
                scope: {
                    listItem: '='
                },
                controllerAs: 'dvm',
                controller: ['$scope', function($scope) {
                    var dvm = this;
                    var cm = catalogManagerService;
                    var sm = stateManagerService;
                    var catalogId = _.get(cm.localCatalog, '@id', '');

                    dvm.om = ontologyManagerService;
                    dvm.os = ontologyStateService;
                    dvm.begin = 0;
                    dvm.limit = 10;
                    dvm.ontologyRecords = [];
                    dvm.filteredList = [];
                    dvm.type = 'ontology';

                    dvm.open = function() {
                        dvm.om.openOntology(dvm.ontologyId, dvm.recordId, dvm.type)
                            .then(ontologyId => {
                                var listItem = dvm.om.getListItemByRecordId(dvm.recordId);
                                dvm.os.addState(dvm.recordId, dvm.om.getOntologyIRI(listItem.ontology), dvm.type);
                                dvm.os.setState(dvm.recordId);
                            }, errorMessage => {
                                dvm.errorMessage = errorMessage;
                            });
                    }

                    dvm.getPage = function(direction) {
                        if (direction === 'next') {
                            dvm.begin += dvm.limit;
                        } else {
                            dvm.begin -= dvm.limit;
                        }
                    }

                    dvm.getRecordValue = function(record, property) {
                        return _.get(record, "['" + prefixes.dcterms + property + "'][0]['@value']");
                    }

                    dvm.showDeleteConfirmationOverlay = function(record) {
                        dvm.recordId = _.get(record, '@id', '');
                        dvm.recordTitle = dvm.getRecordValue(record, 'title');
                        dvm.errorMessage = '';
                        dvm.showDeleteConfirmation = true;
                    }

                    dvm.deleteOntology = function() {
                        cm.deleteRecord(dvm.recordId, catalogId)
                            .then(response => {
                                _.remove(dvm.ontologyRecords, record => _.get(record, '@id', '') === dvm.recordId);
                                var state = sm.getOntologyStateByRecordId(dvm.recordId);
                                if (!_.isEmpty(state)) {
                                    sm.deleteState(_.get(state, 'id', ''));
                                }
                                dvm.showDeleteConfirmation = false;
                            }, errorMessage => dvm.errorMessage = errorMessage);
                    }

                    dvm.download = function(id) {
                        dvm.os.downloadId = id;
                        dvm.os.showDownloadOverlay = true;
                    }

                    dvm.getAllOntologyRecords = function(sortingOption) {
                        dvm.om.getAllOntologyRecords(sortingOption)
                            .then(ontologyRecords => {
                                dvm.ontologyRecords = ontologyRecords;
                                dvm.filteredList = getFilteredRecords(dvm.ontologyRecords);
                            });
                    }

                    $scope.$watch(function() {
                        return dvm.filterText + dvm.om.list;
                    }, function handleFilterTextChange(newValue, oldValue) {
                        dvm.filteredList = $filter('filter')(getFilteredRecords(dvm.ontologyRecords), dvm.filterText,
                            (actual, expected) => {
                                expected = _.lowerCase(expected);
                                return _.includes(_.lowerCase(dvm.getRecordValue(actual, 'title')), expected)
                                    || _.includes(_.lowerCase(dvm.getRecordValue(actual, 'description')), expected)
                                    || _.includes(_.lowerCase(dvm.getRecordValue(actual, 'identifier')), expected);
                            });
                    });

                    dvm.getAllOntologyRecords();

                    function getFilteredRecords(records) {
                        return _.reject(records, record => _.find(dvm.om.list, {recordId: record['@id']}));
                    }
                }]
            }
        }
})();
