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
    mockDiscoverState,
    mockExplore,
    mockExploreUtils,
    mockUtil,
    mockModal
} from '../../../../../../../test/js/Shared';

describe('Class Block Header component', function() {
    var $compile, scope, $q, discoverStateSvc, exploreSvc, exploreUtils, util, modalSvc;

    beforeEach(function() {
        angular.mock.module('explore');
        mockComponent('discover', 'datasetSelect');
        mockDiscoverState();
        mockExplore();
        mockExploreUtils();
        mockUtil();
        mockModal();

        inject(function(_$compile_, _$rootScope_, _$q_, _discoverStateService_, _exploreService_, _exploreUtilsService_, _utilService_, _modalService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            $q = _$q_;
            discoverStateSvc = _discoverStateService_;
            exploreSvc = _exploreService_;
            exploreUtils = _exploreUtilsService_;
            util = _utilService_;
            modalSvc = _modalService_;
        });

        this.element = $compile(angular.element('<class-block-header></class-block-header>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('classBlockHeader');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        $q = null;
        exploreSvc = null;
        discoverStateSvc = null;
        exploreUtils = null;
        util = null;
        modalSvc = null;
        this.element.remove();
    });

    describe('contains the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toBe('CLASS-BLOCK-HEADER');
        });
        it('with a .form-group', function() {
            expect(this.element.querySelectorAll('.form-group').length).toBe(1);
        });
        it('with a custom-label', function() {
            expect(this.element.find('custom-label').length).toBe(1);
        });
        it('with a dataset-select', function() {
            expect(this.element.find('dataset-select').length).toBe(1);
        });
        it('with a .btn.btn-primary', function() {
            expect(this.element.querySelectorAll('.btn.btn-primary').length).toBe(1);
        });
        it('with a .fa.fa-refresh', function() {
            expect(this.element.querySelectorAll('.fa.fa-refresh').length).toBe(1);
        });
        it('with a .btn.btn-link', function() {
            expect(this.element.querySelectorAll('.btn.btn-link').length).toBe(1);
        });
        it('with a .fa.fa-plus', function() {
            expect(this.element.querySelectorAll('.fa.fa-plus').length).toBe(1);
        });
        it('depending on whether a dataset is selected', function() {
            var refreshButton = angular.element(this.element.querySelectorAll('.btn.btn-primary')[0]);
            var createButton = angular.element(this.element.querySelectorAll('.btn.btn-link')[0]);
            expect(refreshButton.attr('disabled')).toBeTruthy();
            expect(createButton.attr('disabled')).toBeTruthy();

            discoverStateSvc.explore.recordId = 'dataset';
            scope.$digest();
            expect(refreshButton.attr('disabled')).toBeFalsy();
            expect(createButton.attr('disabled')).toBeFalsy();
        });
    });
    describe('controller methods', function() {
        describe('showCreate calls the proper methods when getClasses', function() {
            beforeEach(function() {
                discoverStateSvc.explore.recordId = 'recordId';
            });
            it('resolves', function() {
                exploreUtils.getClasses.and.returnValue($q.when([{}]));
                this.controller.showCreate();
                scope.$apply();
                expect(modalSvc.openModal).toHaveBeenCalledWith('newInstanceClassOverlay', {classes: [{}]});
            });
            it('rejects', function() {
                exploreUtils.getClasses.and.returnValue($q.reject('Error message'));
                this.controller.showCreate();
                scope.$apply();
                expect(util.createErrorToast).toHaveBeenCalledWith('Error message');
                expect(modalSvc.openModal).not.toHaveBeenCalled();
            });
        });
        describe('onSelect calls the proper methods', function() {
            it('when value parameter is not an empty string', function() {
                spyOn(this.controller, 'refresh');
                this.controller.onSelect('recordId');
                scope.$apply();
                expect(discoverStateSvc.explore.recordId).toEqual('recordId');
                expect(this.controller.refresh).toHaveBeenCalled();
            });
            it('when value parameter is an empty string', function() {
                spyOn(this.controller, 'refresh');
                this.controller.onSelect('');
                scope.$apply();
                expect(discoverStateSvc.explore.recordId).toEqual('');
                expect(this.controller.refresh).not.toHaveBeenCalled();
            });
        });
        describe('refresh calls the proper methods when getClassDetails', function() {
            beforeEach(function() {
                discoverStateSvc.explore.classDetails = [{}];
                discoverStateSvc.explore.recordId = 'recordId';
            });
            it('resolves', function() {
                exploreSvc.getClassDetails.and.returnValue($q.when([{prop: 'details'}]));
                this.controller.refresh();
                scope.$apply();
                expect(exploreSvc.getClassDetails).toHaveBeenCalledWith('recordId');
                expect(discoverStateSvc.explore.classDetails).toEqual([{prop: 'details'}]);
            });
            it('rejects', function() {
                exploreSvc.getClassDetails.and.returnValue($q.reject('error'));
                this.controller.refresh();
                scope.$apply();
                expect(exploreSvc.getClassDetails).toHaveBeenCalledWith('recordId');
                expect(discoverStateSvc.explore.classDetails).toEqual([]);
                expect(util.createErrorToast).toHaveBeenCalledWith('error');
            });
        });
    });
});