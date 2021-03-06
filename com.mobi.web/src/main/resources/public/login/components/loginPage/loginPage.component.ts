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

import './loginPage.component.scss';

const template = require('./loginPage.component.html');

/**
 * @ngdoc component
 * @name login.component:loginPage
 * @requires shared.service:loginManagerService
 *
 * @description
 * `loginPage` is a component which creates the main login page of the application. The component contains a simple
 * login form for username and password and displays an error message if an error occurs.
 */
const loginPageComponent = {
    template,
    bindings: {},
    controllerAs: 'dvm',
    controller: loginPageComponentCtrl
};

loginPageComponentCtrl.$inject = ['loginManagerService'];

function loginPageComponentCtrl(loginManagerService) {
    var dvm = this;
    dvm.errorMessage = '';

    dvm.login = function() {
        loginManagerService.login(dvm.form.username, dvm.form.password)
            .then(() => dvm.errorMessage = '', errorMessage => dvm.errorMessage = errorMessage);
    }
}

export default loginPageComponent;