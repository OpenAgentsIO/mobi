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
        .module('annotationOverlay', [])
        .directive('annotationOverlay', annotationOverlay);

        annotationOverlay.$inject = ['responseObj', 'ontologyManagerService', 'propertyManagerService', 'ontologyStateService', 'utilService', 'ontologyUtilsManagerService'];

        function annotationOverlay(responseObj, ontologyManagerService, propertyManagerService, ontologyStateService, utilService, ontologyUtilsManagerService) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'modules/ontology-editor/directives/annotationOverlay/annotationOverlay.html',
                scope: {},
                controllerAs: 'dvm',
                controller: function() {
                    var dvm = this;
                    var ontoUtils = ontologyUtilsManagerService;
                    dvm.pm = propertyManagerService;
                    dvm.om = ontologyManagerService;
                    dvm.ro = responseObj;
                    dvm.sm = ontologyStateService;
                    dvm.util = utilService;

                    function createJson(value, language) {
                        var valueObj = {'@value': value};
                        if (language) {
                            _.set(valueObj, '@language', language);
                        }
                        return dvm.util.createJson(dvm.sm.selected['@id'], dvm.ro.getItemIri(dvm.sm.annotationSelect), valueObj);
                    }

                    dvm.addAnnotation = function() {
                        dvm.pm.add(dvm.sm.selected, dvm.ro.getItemIri(dvm.sm.annotationSelect), dvm.sm.annotationValue, _.get(dvm.sm.annotationType, '@id'), dvm.sm.annotationLanguage);
                        dvm.om.addToAdditions(dvm.sm.listItem.recordId, createJson(dvm.sm.annotationValue, dvm.sm.annotationLanguage));
                        dvm.sm.showAnnotationOverlay = false;
                        ontoUtils.saveCurrentChanges();
                    }

                    dvm.editAnnotation = function() {
                        var property = dvm.ro.getItemIri(dvm.sm.annotationSelect);
                        var oldObj = _.get(dvm.sm.selected, "['" + property + "']['" + dvm.sm.annotationIndex + "']");
                        dvm.om.addToDeletions(dvm.sm.listItem.recordId, createJson(_.get(oldObj, '@value'), _.get(oldObj, '@language')));
                        dvm.pm.edit(dvm.sm.selected, property, dvm.sm.annotationValue, dvm.sm.annotationIndex, _.get(dvm.sm.annotationType, '@id'), dvm.sm.annotationLanguage);
                        dvm.om.addToAdditions(dvm.sm.listItem.recordId, createJson(dvm.sm.annotationValue, dvm.sm.annotationLanguage));
                        dvm.sm.showAnnotationOverlay = false;
                        ontoUtils.saveCurrentChanges();
                    }
                }
            }
        }
})();
