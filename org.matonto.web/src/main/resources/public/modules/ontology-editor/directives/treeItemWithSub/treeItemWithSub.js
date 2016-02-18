(function() {
    'use strict';

    angular
        .module('treeItemWithSub', [])
        .directive('treeItemWithSub', treeItemWithSub);

        function treeItemWithSub() {
            return {
                restrict: 'E',
                transclude: true,
                remove: true,
                scope: {
                    currentEntity: '=',
                    currentOntology: '=',
                    hasIcon: '=',
                    isActive: '=',
                    isOpened: '=',
                    onClick: '&'
                },
                templateUrl: 'modules/ontology-editor/directives/treeItemWithSub/treeItemWithSub.html'
            }
        }
})();
