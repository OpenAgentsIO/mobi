(function() {
    'use strict';

    angular
        .module('ontology-editor', ['file-input', 'removeIriFromArray', 'ontologyManager', 'stateManager', 'prefixManager', 'annotationManager', 'responseObj'])
        .controller('OntologyEditorController', OntologyEditorController);

    OntologyEditorController.$inject = ['$scope', '$timeout', '$filter', '$q', 'ontologyManagerService', 'stateManagerService', 'prefixManagerService', 'annotationManagerService', 'responseObj'];

    function OntologyEditorController($scope, $timeout, $filter, $q, ontologyManagerService, stateManagerService, prefixManagerService, annotationManagerService, responseObj) {
        var vm = this;

        vm.ontologies = ontologyManagerService.getList();
        vm.propertyTypes = ontologyManagerService.getPropertyTypes();
        vm.state = stateManagerService.getState();
        vm.selected = ontologyManagerService.getObject(vm.state);

        /* State Management */
        vm.setTreeTab = function(tab) {
            stateManagerService.setTreeTab(tab);
            vm.state = stateManagerService.getState();
            vm.selected = ontologyManagerService.getObject(vm.state);
        }

        vm.setEditorTab = function(tab) {
            stateManagerService.setEditorTab(tab);
            vm.state = stateManagerService.getState();
        }

        /* Ontology Management */
        vm.uploadOntology = function(isValid, file, namespace, localName) {
            ontologyManagerService.uploadThenGet(isValid, file)
                .then(function(response) {
                    vm.selectItem('ontology-editor', vm.ontologies.length - 1, undefined, undefined);
                    vm.showUploadOverlay = false;
                }, function(response) {
                    vm.uploadError = response.data.error;
                });
        }

        vm.deleteOntology = function() {
            ontologyManagerService.delete(vm.selected, vm.state)
                .then(function(response) {
                    stateManagerService.clearState(vm.state.oi);
                    vm.selectItem('default', undefined, undefined, undefined);
                });
        }

        vm.selectItem = function(editor, oi, ci, pi) {
            stateManagerService.setState(editor, oi, ci, pi);
            stateManagerService.setEditorTab('basic');
            vm.state = stateManagerService.getState();
            vm.selected = ontologyManagerService.getObject(vm.state);
            vm.ontology = ontologyManagerService.getOntology(oi);
            vm.rdfs = ontologyManagerService.getOntologyProperty(vm.ontology, 'rdfs');
            vm.owl = ontologyManagerService.getOntologyProperty(vm.ontology, 'owl');
        }

        vm.submitEdit = function(isValid) {
            ontologyManagerService.edit(isValid, vm.selected, vm.state);
        }

        vm.submitCreate = function(isValid) {
            ontologyManagerService.create(isValid, vm.selected, vm.state);
            stateManagerService.setStateToNew(vm.state, vm.ontologies);
            stateManagerService.setEditorTab('basic');
            vm.state = stateManagerService.getState();
        }

        vm.editIRI = function() {
            ontologyManagerService.editIRI(vm.iriBegin, vm.iriThen, vm.iriEnd, vm.iriUpdate, vm.selected, vm.ontologies[vm.state.oi]);
            vm.showIriOverlay = false;
        }

        vm.isObjectProperty = function() {
            return ontologyManagerService.isObjectProperty(vm.selected, vm.ontology);
        }

        /* Prefix (Context) Management */
        vm.editPrefix = function(edit, old, index) {
            prefixManagerService.editPrefix(edit, old, index, vm.selected);
        }

        vm.editValue = function(edit, key, value, index) {
            prefixManagerService.editValue(edit, key, value, index, vm.selected);
        }

        vm.addPrefix = function(key, value) {
            prefixManagerService.add(key, value, vm.selected)
                .then(function(response) {
                    vm.key = '';
                    vm.value = '';
                }, function(response) {
                    vm.showDuplicateMessage = true;
                });
        }

        vm.removePrefix = function(key) {
            prefixManagerService.remove(key, vm.selected);
        }

        vm.getItemIri = function(item) {
            return responseObj.getItemIri(item);
        }

        /* Annotation Management */
        function resetAnnotationOverlay() {
            vm.showAnnotationOverlay = false;
            vm.selected.matonto.currentAnnotationKey = '';
            vm.selected.matonto.currentAnnotationValue = '';
            vm.selected.matonto.currentAnnotationSelect = null;
        }

        vm.addAnnotation = function() {
            annotationManagerService.add(vm.selected, vm.ontologies[vm.state.oi].matonto.annotations);
            resetAnnotationOverlay();
        }

        vm.editClicked = function(key, index) {
            vm.editingAnnotation = true;
            vm.showAnnotationOverlay = true;
            vm.selected.matonto.currentAnnotationKey = key;
            vm.selected.matonto.currentAnnotationValue = vm.selected[key][index]['@value'];
            vm.selected.matonto.currentAnnotationIndex = index;
        }

        vm.editAnnotation = function() {
            annotationManagerService.edit(vm.selected, vm.selected.matonto.currentAnnotationKey, vm.selected.matonto.currentAnnotationValue, vm.selected.matonto.currentAnnotationIndex);
            resetAnnotationOverlay();
        }

        vm.removeAnnotation = function(key, index) {
            annotationManagerService.remove(vm.selected, key, index);
        }

        vm.getPattern = function() {
            return annotationManagerService.getPattern();
        }

        vm.getItemNamespace = function(item) {
            return ontologyManagerService.getItemNamespace(item);
        }

        vm.sortAnnotations = function(item) {
            return annotationManagerService.sort(item);
        }
    }
})();
