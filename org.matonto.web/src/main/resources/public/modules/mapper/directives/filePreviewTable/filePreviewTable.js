(function() {
    'use strict';

    angular
        .module('filePreviewTable', [])
        .directive('filePreviewTable', filePreviewTable);

        filePreviewTable.$inject = ['$timeout'];

        function filePreviewTable($timeout) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {
                    headers: '=',
                    rows: '=',
                    mappedColumns: '=',
                    highlightIdx: '=',
                    isClickable: '=',
                    onClick: '&'
                },
                bindToController: {
                    tableHeight: '='
                },
                link: function(scope, elem, attrs, ctrl) {
                    ["transitionend","webkitTransitionEnd","mozTransitionEnd"].forEach(function(transitionEnd) {
                        elem[0].querySelector("#table-container").addEventListener(transitionEnd, function() {
                            if (ctrl.big) {
                                ctrl.small = false;
                                scope.$digest();
                            }
                        });
                    });
                },
                controller: function($scope, $element) {
                    var dvm = this;
                    var buttonHeight = $element[0].querySelector("#toggle-table").offsetHeight;
                    dvm.big = false;
                    dvm.small = true;
                    dvm.containerTop = '0px';
                    dvm.rows = _.take($scope.rows, 5);

                    $scope.$watch('rows', function(newVal, oldVal) {
                        if (!angular.equals(newVal, oldVal)) {
                            setHeightDefaults();
                            dvm.rows = _.take(newVal, 5);
                        }
                    });

                    dvm.toggleTable = function() {
                        dvm.big = !dvm.big;
                        if (dvm.big) {
                            dvm.rows = $scope.rows;
                            var top = -$element[0].offsetTop;
                            var parentHeight = $element[0].parentNode.offsetHeight;
                            dvm.containerTop = `${top}px`;
                            dvm.containerHeight = `${parentHeight}px`;
                        } else {
                            dvm.rows = _.take($scope.rows, 5);
                            dvm.containerTop = '0px';
                            dvm.containerHeight = dvm.initialHeight + 'px';
                            dvm.small = true;
                        }
                    }
                    function setHeightDefaults() {
                        $timeout(function() {
                            dvm.tableHeight = dvm.initialHeight = $element[0].querySelector("#file-preview").offsetHeight + buttonHeight;
                            dvm.containerHeight = dvm.initialHeight + 'px';
                        });
                    }

                    setHeightDefaults();
                },
                templateUrl: 'modules/mapper/directives/filePreviewTable/filePreviewTable.html'
            }
        }
})();
