(function() {
    'use strict';

    angular
        .module('previousCheckOverlay', ['mappingManager'])
        .directive('previousCheckOverlay', previousCheckOverlay);

        previousCheckOverlay.$inject = ['prefixes', 'ontologyManagerService', 'mappingManagerService'];

        function previousCheckOverlay(prefixes, ontologyManagerService, mappingManagerService) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {
                    onClickBack: '&',
                    onClickContinue: '&'
                },
                bindToController: {
                    mapping: '=',
                    filePreview: '=',
                    separator: '='
                },
                link: function(scope, elem, attrs, ctrl) {
                    if (scope.validateForm) {
                        scope.validateForm.$setValidity('validColumnMappings', ctrl.invalidColumns.length === 0);
                        scope.validateForm.$setValidity('matchingSeparators', ctrl.mappingSeparator === ctrl.separator);
                    }
                },
                controller: function() {
                    var dvm = this;
                    var mappedColumns = mappingManagerService.getMappedColumns(dvm.mapping);
                    dvm.invalidColumns = _.filter(mappedColumns, function(obj) {
                        return obj.index > dvm.filePreview.headers.length - 1;
                    });
                    dvm.mappingSeparator = mappingManagerService.getSeparator(dvm.mapping);

                    dvm.getDataMappingName = function(dataMappingId) {
                        var propId = _.get(_.find(dvm.mapping.jsonld, {'@id': dataMappingId}), 
                            "['" + prefixes.delim + "hasProperty'][0]['@id']");
                        var classId = _.get(mappingManagerService.findClassWithDataMapping(dvm.mapping.jsonld, dataMappingId), 
                            "['" + prefixes.delim + "mapsTo'][0]['@id']");
                        var className = ontologyManagerService.getEntityName(
                            ontologyManagerService.getClass(mappingManagerService.getSourceOntology(dvm.mapping), classId)
                        );
                        var propName = ontologyManagerService.getEntityName(
                            ontologyManagerService.getClassProperty(mappingManagerService.getSourceOntology(dvm.mapping), classId, propId)
                        );
                        return className + ": " + propName;
                    }
                    dvm.getSeparatorName = function(separator) {
                        switch (separator) {
                            case '\t':
                                return 'tab';
                            case ':':
                                return 'colon';
                            case ',':
                                return 'comma';
                            default:
                                return separator;
                        }
                    }
                },
                templateUrl: 'modules/mapper/directives/previousCheckOverlay/previousCheckOverlay.html'
            }
        }
})();
