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
import {
    mockComponent,
    mockOntologyState,
    mockOntologyUtilsManager,
    mockOntologyManager
} from '../../../../../../test/js/Shared';

describe('Property Values component', function() {
    var $compile, scope, ontologyUtilsManagerSvc, ontologyManagerSvc, ontologyStateSvc;

    beforeEach(function() {
        angular.mock.module('ontology-editor');
        mockComponent('ontology-editor', 'blankNodeValueDisplay');
        mockOntologyState();
        mockOntologyUtilsManager();
        mockOntologyManager();

        inject(function(_$compile_, _$rootScope_, _ontologyUtilsManagerService_, _ontologyManagerService_, _ontologyStateService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyUtilsManagerSvc = _ontologyUtilsManagerService_;
            ontologyManagerSvc = _ontologyManagerService_;
            ontologyStateSvc = _ontologyStateService_;
        });

        ontologyManagerSvc.isBlankNodeId.and.callFake(id => id === '_:genid0');
        ontologyUtilsManagerSvc.isLinkable.and.callFake(id => id === 'value1');

        scope.edit = jasmine.createSpy('edit');
        scope.remove = jasmine.createSpy('remove');
        scope.entity = {'prop': [{'@id': 'value1'}, {'@id': '_:genid0'}]};
        scope.property = 'prop';
        this.element = $compile(angular.element('<property-values property="property" entity="entity" edit="edit(property, index)" remove="remove(iri, index)" highlight-iris="" highlight-text=""></property-values>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('propertyValues');
        scope.$apply();
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        ontologyUtilsManagerSvc = null;
        ontologyManagerSvc = null;
        ontologyStateSvc = null;
        this.element.remove();
    });

    describe('controller bound variable', function() {
        it('property should be one way bound', function() {
            this.controller.property = 'test';
            scope.$digest();
            expect(scope.property).toEqual('prop');
        });
        it('entity should be one way bound', function() {
            var entity = angular.copy(this.controller.entity);
            this.controller.entity = {test: 'test'};
            scope.$digest();
            expect(angular.copy(scope.entity)).toEqual(entity);
        });
        it('edit should be called in the parent scope', function() {
            this.controller.edit();
            expect(scope.edit).toHaveBeenCalled();
        });
        it('remove should be called in the parent scope', function() {
            this.controller.remove();
            expect(scope.remove).toHaveBeenCalled();
        });
    });
    describe('contains the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toEqual('PROPERTY-VALUES');
            expect(this.element.querySelectorAll('.property-values').length).toEqual(1);
            expect(this.element.querySelectorAll('.prop-header').length).toEqual(1);
        });
        it('based on the number of values', function() {
            var values = this.element.querySelectorAll('.prop-value-container');
            expect(values.length).toEqual(2);
        });
        it('depending on whether a value is a blank node and user can modify', function() {
            ontologyStateSvc.canModify.and.returnValue(true);
            scope.$digest();
            var blankNodeValue = this.element.querySelectorAll('.prop-value-container .value-display-wrapper blank-node-value-display');
            expect(blankNodeValue.length).toEqual(1);
            var editButtons = this.element.querySelectorAll('.prop-value-container [title=Edit]');
            expect(editButtons.length).toEqual(1);
            var deleteButtons = this.element.querySelectorAll('.prop-value-container [title=Delete]');
            expect(deleteButtons.length).toEqual(2);
        });
        it('depending on whether a value is a blank node and user cannot modify', function() {
            ontologyStateSvc.canModify.and.returnValue(false);
            scope.$digest();
            var blankNodeValue = this.element.querySelectorAll('.prop-value-container .value-display-wrapper blank-node-value-display');
            expect(blankNodeValue.length).toEqual(1);
            var editButtons = this.element.querySelectorAll('.prop-value-container [title=Edit]');
            expect(editButtons.length).toEqual(0);
            var deleteButtons = this.element.querySelectorAll('.prop-value-container [title=Delete]');
            expect(deleteButtons.length).toEqual(0);
        });
        it('depending on whether a value is linkable', function() {
            var link = this.element.querySelectorAll('.prop-value-container .value-display-wrapper a');
            expect(link.length).toEqual(1);
            expect(angular.element(link[0]).text()).toContain('value1');
        });
    });
    it('should call edit when the appropriate button is clicked', function() {
        ontologyStateSvc.canModify.and.returnValue(true);
        scope.$digest();
        var editButton = angular.element(this.element.querySelectorAll('.prop-value-container [title=Edit]')[0]);
        editButton.triggerHandler('click');
        expect(scope.edit).toHaveBeenCalledWith(this.controller.property, 0);
    });
    it('should call remove when the appropriate button is clicked', function() {
        ontologyStateSvc.canModify.and.returnValue(true);
        scope.$digest();
        var removeButton = angular.element(this.element.querySelectorAll('.prop-value-container [title=Delete]')[0]);
        removeButton.triggerHandler('click');
        expect(scope.remove).toHaveBeenCalledWith(this.controller.property, 0);
    });
});