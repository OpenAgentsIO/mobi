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
describe('Remove Property Overlay directive', function() {
    var $compile, scope, $q, ontologyStateSvc, ontologyManagerSvc, propertyManagerSvc, isolatedScope, element, controller;

    beforeEach(function() {
        module('templates');
        module('removePropertyOverlay');
        mockOntologyState();
        mockPropertyManager();
        mockOntologyManager();

        inject(function(_$compile_, _$rootScope_, _$q_, _ontologyStateService_, _ontologyManagerService_, _propertyManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            $q = _$q_;
            ontologyStateSvc = _ontologyStateService_;
            ontologyManagerSvc = _ontologyManagerService_;
            propertyManagerSvc = _propertyManagerService_;
        });

        scope.index = 0;
        scope.key = 'key';
        scope.onSubmit = jasmine.createSpy('onSubmit');
        scope.overlayFlag = true;

        element = $compile(angular.element('<remove-property-overlay index="index" key="key" on-submit="onSubmit()" overlay-flag="overlayFlag"></remove-property-overlay>'))(scope);
        scope.$digest();

        controller = element.controller('removePropertyOverlay');
        isolatedScope = element.isolateScope();
    });

    describe('controller bound variables', function() {
        it('index should be one way bound', function() {
            controller.index = 1;
            scope.$digest();
            expect(scope.index).toEqual(0);
        });
        it('key should be one way bound', function() {
            controller.key = 'new key';
            scope.$digest();
            expect(scope.key).toEqual('key');
        });
        it('overlayFlag should be two way bound', function() {
            controller.overlayFlag = false;
            scope.$digest();
            expect(scope.overlayFlag).toEqual(false);
        });
        it('onSubmit should be triggered on the scope', function() {
            controller.onSubmit();
            scope.$digest();
            expect(scope.onSubmit).toHaveBeenCalled();
        });
    });
    describe('replaces the element with the correct html', function() {
        it('for a div', function() {
            expect(element.prop('tagName')).toBe('DIV');
        });
        it('based on .overlay', function() {
            expect(element.hasClass('overlay')).toBe(true);
        });
        it('based on form', function() {
            expect(element.find('form').length).toBe(1);
        });
        it('based on h6', function() {
            expect(element.find('h6').length).toBe(1);
        });
        _.forEach(['main', 'btn-container', 'btn-primary', 'btn-default'], function(item) {
            it('based on .' + item, function() {
                expect(element.querySelectorAll('.' + item).length).toBe(1);
            });
        });
    });
    describe('controller methods', function() {
        it('removeProperty calls the correct methods', function() {
            _.set(ontologyStateSvc.selected, 'key[0]', 'value');
            controller.removeProperty();
            expect(scope.onSubmit).toHaveBeenCalled();
            expect(ontologyManagerSvc.addToDeletions).toHaveBeenCalledWith(ontologyStateSvc.listItem.ontologyId,
                jasmine.any(Object));
            expect(propertyManagerSvc.remove).toHaveBeenCalledWith(ontologyStateSvc.selected, controller.key,
                controller.index);
            expect(controller.overlayFlag).toBe(false);
        });
    });
});