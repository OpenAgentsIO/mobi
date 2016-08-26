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
describe('Class Preview directive', function() {
    var $compile,
        scope,
        ontologyManagerSvc,
        mappingManagerSvc,
        controller;

    beforeEach(function() {
        module('templates');
        module('classPreview');
        mockPrefixes();
        mockOntologyManager();
        mockMappingManager();

        inject(function(_$compile_, _$rootScope_, _ontologyManagerService_, _mappingManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyManagerSvc = _ontologyManagerService_;
            mappingManagerSvc = _mappingManagerService_;
        });
    });

    describe('controller bound variable', function() {
        beforeEach(function() {
            scope.classObj = {};
            this.element = $compile(angular.element('<class-preview class-obj="classObj"></class-preview>'))(scope);
            scope.$digest();
            controller = this.element.controller('classPreview');
        });
        it('classObj should be two way bound', function() {
            controller.classObj = {'@id': ''};
            scope.$digest();
            expect(scope.classObj).toEqual({'@id': ''});
        });
    });
    describe('controller methods', function() {
        beforeEach(function() {
            scope.classObj = {'@id': ''};
            this.element = $compile(angular.element('<class-preview class-obj="classObj"></class-preview>'))(scope);
            scope.$digest();
            controller = this.element.controller('classPreview');
        });
        it('should create a title for classObj', function() {
            var result = controller.createTitle();
            expect(ontologyManagerSvc.getEntityName).toHaveBeenCalledWith(controller.classObj);
            expect(typeof result).toBe('string');
        });
        it('should create a description of the ontology', function() {
            var result = controller.createDescription();
            expect(typeof result).toBe('string');
        });
        it('should get properties from the ontology', function() {
            var result = controller.getProps();
            expect(ontologyManagerSvc.getClassProperties).toHaveBeenCalled();
            expect(_.isArray(result)).toBe(true);
        });
        describe('should get the list of properties to display', function() {
            beforeEach(function() {
                this.properties = [{}, {}, {}, {}, {}, {}, {}];
                spyOn(controller, 'getProps').and.returnValue(this.properties);
                ontologyManagerSvc.getEntityName.calls.reset();
            });
            it('if the list is full', function() {
                controller.full = true;
                var result = controller.getPropList();
                expect(_.isArray(result)).toBe(true);
                expect(result.length).toBe(this.properties.length);
                expect(ontologyManagerSvc.getEntityName.calls.count()).toBe(this.properties.length);
            });
            it('if the list is not full', function() {
                controller.full = false;
                var result = controller.getPropList();
                expect(_.isArray(result)).toBe(true);
                expect(result.length).toBe(controller.numPropPreview);
                expect(ontologyManagerSvc.getEntityName.calls.count()).toBe(controller.numPropPreview);
            });
        });
    });
    describe('replaces the element with the correct html', function() {
        beforeEach(function() {
            this.element = $compile(angular.element('<class-preview class-obj="classObj"></class-preview>'))(scope);
            scope.$digest();
        });
        it('for wrapping containers', function() {
            expect(this.element.hasClass('class-preview')).toBe(true);
        });
        it('depending on whether classObj was passed', function() {
            expect(this.element.children().length).toBe(0);

            scope.classObj = {};
            scope.$digest();
            expect(this.element.children().length).toBe(1);
        });
        it('depending on whether classObj has any properties', function() {
            scope.classObj = {};
            scope.$digest();
            var propList = angular.element(this.element.querySelectorAll('ul')[0]);
            expect(propList.html()).toContain('None');

            var properties = [{}];
            ontologyManagerSvc.getClassProperties.and.returnValue(properties);
            scope.$digest();
            expect(propList.html()).not.toContain('None');
            expect(propList.children().length).toBe(properties.length);
        });
        it('depending on how many properties are showing', function() {
            controller = this.element.controller('classPreview');
            ontologyManagerSvc.getClassProperties.and.returnValue([{}, {}, {}, {}, {}, {}]);
            scope.classObj = {};
            scope.$digest();
            var link = angular.element(this.element.querySelectorAll('a.header-link')[0]);
            expect(link.text()).toBe('See More');
            controller.full = true;
            scope.$digest();
            expect(link.text()).toBe('See Less');
        });
        it('with the correct number of list items for properties', function() {
            controller = this.element.controller('classPreview');
            var properties = [{}, {}, {}, {}, {}];
            ontologyManagerSvc.getClassProperties.and.returnValue(properties);
            scope.classObj = {};
            scope.$digest();
            expect(this.element.querySelectorAll('.props li').length).toBe(properties.length);
        });
    });
});