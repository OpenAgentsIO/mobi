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

import { find, get } from 'lodash';

import './sidebar.component.scss';

const template = require('./filtered/sidebar.component.html');

/**
 * @ngdoc component
 * @name shared.component:sidebar
 * @requires $rootScope
 * @requires $state
 * @requires shared.service:loginManagerService
 * @requires shared.service:userManagerService
 *
 * @description
 * `sidebar` is a component that creates the main sidebar of the application. It contains a display of the currently
 * {@link shared.service:loginManagerService logged in user} that also serves as a button to go to the
 * {@link settings.component:settingsPage}, buttons to navigate to the main modules of the application, an
 * "Administration" button to go to the {@link user-management.component:userManagementPage user management page}, a
 * button to get help for the application, a button to logout, and version information.
 */
const sidebarComponent = {
    template,
    bindings: {},
    controllerAs: 'dvm',
    controller: sidebarComponentCtrl
};

sidebarComponentCtrl.$inject = ['$rootScope', '$state', 'loginManagerService', 'userManagerService'];

function sidebarComponentCtrl($rootScope, $state, loginManagerService, userManagerService) {
    var dvm = this;
    dvm.lm = loginManagerService;
    dvm.um = userManagerService;
    dvm.perspectives = [];

    dvm.$onInit = function() {
        dvm.perspectives = [
            { icon: 'home', sref: 'root.home', isActive: $state.is('root.home'), name: 'Home' },
            { icon: 'book', sref: 'root.catalog', isActive: $state.is('root.catalog'), name: 'Catalog' },
            { icon: 'pencil-square-o', sref: 'root.ontology-editor', isActive: $state.is('root.ontology-editor'), name: 'Ontology Editor'},
            { icon: 'envelope-o', sref: 'root.merge-requests', isActive: $state.is('root.merge-requests'), name: 'Merge Requests' },
            { icon: 'map-o', sref: 'root.mapper', isActive: $state.is('root.mapper'), name: 'Mapping Tool' },
            { icon: 'database', sref: 'root.datasets', isActive: $state.is('root.datasets'), name: 'Datasets' },
            { icon: 'search', sref: 'root.discover', isActive: $state.is('root.discover'), name: 'Discover' },
        ];
    }
    dvm.toggle = function() {
        $rootScope.collapsedNav = !$rootScope.collapsedNav;
    }
    dvm.getUserDisplay = function() {
        var user = find(dvm.um.users, {iri: dvm.lm.currentUserIRI});
        return get(user, 'firstName', '') || get(user, 'username', '');
    }
}

export default sidebarComponent;
