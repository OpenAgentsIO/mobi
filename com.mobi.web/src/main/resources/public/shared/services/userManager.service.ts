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
import { map, get, find, filter, identity, set, noop, forEach, has, merge, remove, pull, assign, union, includes, flatten, without } from 'lodash';

userManagerService.$inject = ['$http', '$q', 'REST_PREFIX', 'ADMIN_USER_IRI', 'utilService', 'prefixes'];

/**
 * @ngdoc service
 * @name shared.service:userManagerService
 * @requires $http
 * @requires $q
 * @requires shared.service:utilService
 * @requires shared.service:prefixes
 *
 * @description
 * `userManagerService` is a service that provides access to the Mobi users and groups REST endpoints for adding,
 * removing, and editing Mobi users and groups.
 */
function userManagerService($http, $q, REST_PREFIX, ADMIN_USER_IRI, utilService, prefixes) {
    var self = this,
        userPrefix = REST_PREFIX + 'users',
        groupPrefix = REST_PREFIX + 'groups';
    var util = utilService;

    /**
     * @ngdoc property
     * @name groups
     * @propertyOf shared.service:userManagerService
     * @type {object[]}
     *
     * @description
     * `groups` holds a list of objects representing the groups in Mobi. The structure of
     * each object is:
     * ```
     * {
     *    jsonld: {},
     *    title: '',
     *    description: '',
     *    roles: [],
     *    members: []
     * }
     * ```
     */
    self.groups = [];
    /**
     * @ngdoc property
     * @name users
     * @propertyOf shared.service:userManagerService
     * @type {object[]}
     *
     * @description
     * `users` holds a list of objects representing the users in Mobi. The structure of
     * each object is:
     * ```
     * {
     *    jsonld: {},
     *    iri: '',
     *    username: '',
     *    firstName: '',
     *    lastName: '',
     *    email: '',
     *    roles: []
     * }
     * ```
     */
    self.users = [];

    /**
     * @ngdoc method
     * @name reset
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Resets all state variables.
     */
    self.reset = function() {
        self.users = [];
        self.groups = [];
    }
    /**
     * @ngdoc method
     * @name setUsers
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Initializes the `users` and `groups` lists. Uses the results of the GET /mobirest/users and the results of
     * the GET /mobirest/groups endpoints to retrieve the user and group lists, respectively. If an error occurs in
     * either of the HTTP calls, logs the error on the console. Returns a promise.
     *
     * @return {Promise} A Promise that indicates the function has completed.
     */
    self.initialize = function() {
        return self.getUsers()
            .then(data => {
                self.users = map(data, self.getUserObj);
                return self.getGroups();
            }, $q.reject)
            .then(data => {
                self.groups = map(data, self.getGroupObj)
            }, error => console.log(util.getErrorMessage(error)));
    }
    /**
     * @ngdoc method
     * @name getUsers
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the GET /mobirest/users endpoint which retrieves a list of Users without their passwords.
     *
     * @return {Promise} A promise that resolves with the list of Users or rejects with an error message.
     */
    self.getUsers = function() {
        return $http.get(userPrefix)
            .then(response => response.data, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name getGroups
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the GET /mobirest/groups endpoint which retrieves a list of Groups.
     *
     * @return {Promise} A promise that resolves with the list of Groups or rejects with an error message.
     */
    self.getGroups = function() {
        return $http.get(groupPrefix)
            .then(response => response.data, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name getUsername
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Finds the username of the user associated with the passed IRI. If it has not been found before, calls the GET
     * /mobirest/users/username endpoint and saves the result in the `users` list. If it has been found before,
     * grabs the username from the users list. Returns a Promise that resolves with the username and rejects if the
     * endpoint fails.
     *
     * @param {string} iri The user IRI to search for
     * @return {Promise} A Promise that resolves with the username if the user was found; rejects with an error
     * message otherwise
     */
    self.getUsername = function(iri) {
        var config = { params: { iri } };
        var user = find(self.users, { iri });
        if (user) {
            return $q.when(user.username);
        } else {
            return $http.get(userPrefix + '/username', config)
                .then(response => {
                    set(find(self.users, {username: response.data}), 'iri', iri);
                    return response.data;
                }, util.rejectError);
        }
    }

    /**
     * @ngdoc method
     * @name addUser
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the POST /mobirest/users endpoint to add the passed user to Mobi. Returns a Promise that resolves if
     * the addition was successful and rejects with an error message if it was not. Updates the `users` list
     * appropriately.
     *
     * @param {Object} newUser the new user to add
     * @param {string} newUser.username The required username for the user
     * @param {string[]} newUser.roles The required roles for the user
     * @param {string} newUser.firstName The optional first name of the user
     * @param {string} newUser.lastName The optional last name of the user
     * @param {string} newUser.email The optional email for the user
     * @param {string} password the password for the new user
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.addUser = function(newUser, password) {
        var fd = new FormData(),
            config = {
                transformRequest: identity,
                headers: {
                    'Content-Type': undefined
                },
            };
        fd.append('username', newUser.username);
        fd.append('password', password);
        forEach(get(newUser, 'roles', []), role => fd.append('roles', role));
        if (has(newUser, 'firstName')) {
            fd.append('firstName', newUser.firstName);
        }
        if (has(newUser, 'lastName')) {
            fd.append('lastName', newUser.lastName);
        }
        if (has(newUser, 'email')) {
            fd.append('email', newUser.email);
        }

        return $http.post(userPrefix, fd, config)
            .then(response => {
                return self.getUser(newUser.username);
            }, $q.reject)
            .then(noop, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name getUser
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the GET /mobirest/users/{username} endpoint to retrieve a Mobi user with passed username. Returns a
     * Promise that resolves with the result of the call if it was successful and rejects with an error message if
     * it was not.
     *
     * @param {string} username the username of the user to retrieve
     * @return {Promise} A Promise that resolves with the user if the request was successful; rejects with an error
     * message otherwise
     */
    self.getUser = function(username) {
        return $http.get(userPrefix + '/' + encodeURIComponent(username))
            .then(response => {
                var userObj = self.getUserObj(response.data);
                var existing = find(self.users, {iri: userObj.iri});
                if (existing) {
                    merge(existing, userObj);
                } else {
                    self.users.push(userObj);
                }
                return userObj;
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name updateUser
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/users/{username} endpoint to update a Mobi user specified by the passed username with
     * the passed new user. Returns a Promise that resolves if it was successful and rejects with an error message
     * if it was not. Updates the `users` list appropriately.
     *
     * @param {string} username the username of the user to retrieve
     * @param {Object} newUser an object containing all the new user information to save. The structure of the
     * object should be the same as the structure of the user objects in the `users` list
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.updateUser = function(username, newUser) {
        return $http.put(userPrefix + '/' + encodeURIComponent(username), newUser.jsonld)
            .then(response => {
                assign(find(self.users, {username}), newUser);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name changePassword
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the POST /mobirest/users/{username}/password endpoint to change the password of a Mobi user specified
     * by the passed username. Requires the user's current password to succeed. Returns a Promise that resolves if
     * it was successful and rejects with an error message if it was not.
     *
     * @param {string} username the username of the user to update
     * @param {string} password the current password of the user
     * @param {string} newPassword the new password to save for the user
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.changePassword = function(username, password, newPassword) {
        var config = {
            params: {
                currentPassword: password,
                newPassword
            }
        };
        return $http.post(userPrefix + '/' + encodeURIComponent(username) + '/password', null, config)
            .then(noop, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name resetPassword
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/users/{username}/password endpoint to reset the password of a Mobi user specified by
     * the passed username. Can only be performed by an admin user. Returns a Promise that resolves if it was
     * successful and rejects with an error message if it was not.
     *
     * @param {string} username the username of the user to update
     * @param {string} newPassword the new password to save for the user
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.resetPassword = function(username, newPassword) {
        var config = { params: { newPassword } };
        return $http.put(userPrefix + '/' + encodeURIComponent(username) + '/password', null, config)
            .then(noop, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name deleteUser
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the DELETE /mobirest/users/{username} endpoint to remove the Mobi user with passed username. Returns a
     * Promise that resolves if the deletion was successful and rejects with an error message if it was not. Updates
     * the `groups` list appropriately.
     *
     * @param {string} username the username of the user to remove
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.deleteUser = function(username) {
        return $http.delete(userPrefix + '/' + encodeURIComponent(username))
            .then(response => {
                remove(self.users, {username});
                forEach(self.groups, group => pull(group.members, username));
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name addUserRole
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/users/{username}/roles endpoint to add the passed roles to the Mobi user specified by
     * the passed username. Returns a Promise that resolves if the addition was successful and rejects with an error
     * message if not. Updates the `users` list appropriately.
     *
     * @param {string} username the username of the user to add a role to
     * @param {string[]} roles the roles to add to the user
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.addUserRoles = function(username, roles) {
        var config = { params: { roles } };
        return $http.put(userPrefix + '/' + encodeURIComponent(username) + '/roles', null, config)
            .then(response => {
                var user = find(self.users, {username});
                user.roles = union(get(user, 'roles', []), roles);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name deleteUserRole
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the DELETE /mobirest/users/{username}/roles endpoint to remove the passed role from the Mobi user
     * specified by the passed username. Returns a Promise that resolves if the deletion was successful and rejects
     * with an error message if not. Updates the `users` list appropriately.
     *
     * @param {string} username the username of the user to remove a role from
     * @param {string} role the role to remove from the user
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.deleteUserRole = function(username, role) {
        var config = { params: { role } };
        return $http.delete(userPrefix + '/' + encodeURIComponent(username) + '/roles', config)
            .then(response => {
                pull(get(find(self.users, {username}), 'roles'), role);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name addUserGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/users/{username}/groups endpoint to add the Mobi user specified by the passed
     * username to the group specified by the passed group title. Returns a Promise that resolves if the addition
     * was successful and rejects with an error message if not. Updates the `groups` list appropriately.
     *
     * @param {string} username the username of the user to add to the group
     * @param {string} groupTitle the title of the group to add the user to
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.addUserGroup = function(username, groupTitle) {
        var config = {
            params: {
                group: groupTitle
            }
        };
        return $http.put(userPrefix + '/' + encodeURIComponent(username) + '/groups', null, config)
            .then(response => {
                var group = find(self.groups, {title: groupTitle});
                group.members = union(get(group, 'members', []), [username]);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name deleteUserGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the DELETE /mobirest/users/{username}/groups endpoint to remove the Mobi user specified by the passed
     * username from the group specified by the passed group title. Returns a Promise that resolves if the deletion
     * was successful and rejects with an error message if not. Updates the `groups` list appropriately.
     *
     * @param {string} username the username of the user to remove from the group
     * @param {string} groupTitle the title of the group to remove the user from
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.deleteUserGroup = function(username, groupTitle) {
        var config = {
            params: {
                group: groupTitle
            }
        };
        return $http.delete(userPrefix + '/' + encodeURIComponent(username) + '/groups', config)
            .then(response => {
                var group = find(self.groups, {title: groupTitle});
                group.members = without(get(group, 'members'), username);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name addGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the POST /mobirest/groups endpoint to add the passed group to Mobi. Returns a Promise that resolves if
     * the addition was successful and rejects with an error message if it was not. Updates the `groups` list
     * appropriately.
     *
     * @param {Object} newGroup the new group to add
     * @param {string} newGroup.title the required title of the group
     * @param {string} newGroup.description the optional description of the group
     * @param {string[]} newGroup.roles the optional roles of the group
     * @param {string[]} newGroup.members the required members of the group
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.addGroup = function(newGroup) {
        var fd = new FormData(),
            config = {
                transformRequest: identity,
                headers: {
                    'Content-Type': undefined
                },
            };
        fd.append('title', newGroup.title);
        forEach(get(newGroup, 'members', []), member => fd.append('members', member));
        if (has(newGroup, 'description')) {
            fd.append('description', newGroup.description);
        }
        if (has(newGroup, 'roles')) {
            forEach(get(newGroup, 'roles', []), role => fd.append('roles', role));
        }

        return $http.post(groupPrefix, fd, config)
            .then(response => {
                return self.getGroup(newGroup.title);
            }, $q.reject)
            .then(noop, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name getGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the GET /mobirest/groups/{groupTitle} endpoint to retrieve a Mobi group with passed title. If the
     * group does not already exist in the `groups` list, adds it. Returns a Promise that resolves with the result
     * of the call if it was successful and rejects with an error message if it was not.
     *
     * @param {string} groupTitle the title of the group to retrieve
     * @return {Promise} A Promise that resolves with the group if the request was successful; rejects with an error
     * message otherwise
     */
    self.getGroup = function(groupTitle) {
        return $http.get(groupPrefix + '/' + encodeURIComponent(groupTitle))
            .then(response => {
                var groupObj = self.getGroupObj(response.data);
                var existing = find(self.groups, {iri: groupObj.iri});
                if (existing) {
                    merge(existing, groupObj);
                } else {
                    self.groups.push(groupObj);
                }
                return groupObj;
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name updateGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/groups/{groupTitle} endpoint to update a Mobi group specified by the passed title
     * with the passed new group. Returns a Promise that resolves if it was successful and rejects with an error
     * message if it was not. Updates the `groups` list appropriately.
     *
     * @param {string} groupTitle the title of the group to update
     * @param {Object} newGroup an object containing all the new group information to
     * save. The structure of the object should be the same as the structure of the group objects in the `groups`
     * list
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.updateGroup = function(groupTitle, newGroup) {
        return $http.put(groupPrefix + '/' + encodeURIComponent(groupTitle), newGroup.jsonld)
            .then(response => {
                assign(find(self.groups, {title: groupTitle}), newGroup);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name deleteGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the DELETE /mobirest/groups/{groupTitle} endpoint to remove the Mobi group with passed title. Returns a
     * Promise that resolves if the deletion was successful and rejects with an error message if it was not. Updates
     * the `groups` list appropriately.
     *
     * @param {string} groupTitle the title of the group to remove
     * @return {Promise} A Promise that resolves if the request was successful; rejects with an error message
     * otherwise
     */
    self.deleteGroup = function(groupTitle) {
        return $http.delete(groupPrefix + '/' + encodeURIComponent(groupTitle))
            .then(response => {
                remove(self.groups, {title: groupTitle});
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name addGroupRole
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/groups/{groupTitle}/roles endpoint to add the passed roles to the Mobi group
     * specified by the passed title. Returns a Promise that resolves if the addition was successful and rejects
     * with an error message if not. Updates the `groups` list appropriately.
     *
     * @param {string} groupTitle the title of the group to add a role to
     * @param {string[]} roles the roles to add to the group
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.addGroupRoles = function(groupTitle, roles) {
        var config = { params: { roles } };
        return $http.put(groupPrefix + '/' + encodeURIComponent(groupTitle) + '/roles', null, config)
            .then(response => {
                var group = find(self.groups, {title: groupTitle});
                group.roles = union(get(group, 'roles', []), roles);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name deleteGroupRole
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the DELETE /mobirest/groups/{groupTitle}/roles endpoint to remove the passed role from the Mobi group
     * specified by the passed title. Returns a Promise that resolves if the deletion was successful and rejects
     * with an error message if not. Updates the `groups` list appropriately.
     *
     * @param {string} groupTitle the title of the group to remove a role from
     * @param {string} role the role to remove from the group
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.deleteGroupRole = function(groupTitle, role) {
        var config = { params: { role } };
        return $http.delete(groupPrefix + '/' + encodeURIComponent(groupTitle) + '/roles', config)
            .then(response => {
                pull(get(find(self.groups, {title: groupTitle}), 'roles'), role);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name getGroupUsers
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the GET /mobirest/groups/{groupTitle}/users endpoint to retrieve the list of users assigned to the
     * Mobi group specified by the passed title. Returns a Promise that resolves with the result of the call is
     * successful and rejects with an error message if it was not.
     *
     * @param  {string} groupTitle the title of the group to retrieve users from
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.getGroupUsers = function(groupTitle) {
        return $http.get(groupPrefix + '/' + encodeURIComponent(groupTitle) + '/users')
            .then(response => response.data, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name addGroupUsers
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the PUT /mobirest/groups/{groupTitle}/users endpoint to add the Mobi users specified by the passed
     * array of usernames to the group specified by the passed group title. Returns a Promise that resolves if the
     * addition was successful and rejects with an error message if not. Updates the `groups` list appropriately.
     *
     * @param {string} groupTitle the title of the group to add users to
     * @param {string[]} users an array of usernames of users to add to the group
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.addGroupUsers = function(groupTitle, users) {
        var config = { params: { users } };
        return $http.put(groupPrefix + '/' + encodeURIComponent(groupTitle) + '/users', null, config)
            .then(response => {
                var group = find(self.groups, {title: groupTitle});
                group.members = union(get(group, 'members', []), users);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name deleteGroupUser
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Calls the DELETE /mobirest/groups/{groupTitle}/users endpoint to remove the Mobi user specified by the passed
     * username from the group specified by the passed group title. Returns a Promise that resolves if the deletion
     * was successful and rejects with an error message if not. Updates the `groups` list appropriately.
     *
     * @param {string} groupTitle the title of the group to remove the user from
     * @param {string} username the username of the user to remove from the group
     * @return {Promise} A Promise that resolves if the request is successful; rejects with an error message
     * otherwise
     */
    self.deleteGroupUser = function(groupTitle, username) {
        var config = {
            params: {
                user: username
            }
        };
        return $http.delete(groupPrefix + '/' + encodeURIComponent(groupTitle) + '/users', config)
            .then(response => {
                pull(get(find(self.groups, {title: groupTitle}), 'members'), username);
            }, util.rejectError);
    }
    /**
     * @ngdoc method
     * @name isAdmin
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Tests whether the user with the passed username is an admin or not by checking the roles assigned to the user
     * itself and the roles assigned to any groups the user is a part of.
     *
     * @param {string} username the username of the user to test whether they are an admin
     * @return {boolean} true if the user is an admin; false otherwise
     */
    self.isAdmin = function(username) {
        if (includes(get(find(self.users, {username}), 'roles', []), 'admin')) {
            return true;
        } else {
            var userGroups = filter(self.groups, group => {
                return includes(group.members, username);
            });
            return includes(flatten(map(userGroups, 'roles')), 'admin');
        }
    }

    self.isAdminUser = function(userIri) {
        return userIri === ADMIN_USER_IRI;
    }
    /**
     * @ngdoc method
     * @name isExternalUser
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Determines whether the provided JSON-LD object is an ExternalUser or not.
     *
     * @param {Object} jsonld a JSON-LD object
     * @return {boolean} true if the JSON-LD object is an ExternalUser; false otherwise
     */
    self.isExternalUser = function(jsonld) {
        return get(jsonld, '@type', []).includes(prefixes.user + 'ExternalUser');
    }
    /**
     * @ngdoc method
     * @name isExternalGroup
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Determines whether the provided JSON-LD object is an ExternalGroup or not.
     *
     * @param {Object} jsonld a JSON-LD object
     * @return {boolean} true if the JSON-LD object is ExternalGroup; false otherwise
     */
    self.isExternalGroup = function(jsonld) {
        return get(jsonld, '@type', []).includes(prefixes.user + 'ExternalGroup');
    }
    /**
     * @ngdoc method
     * @name getUserDisplay
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Returns a human readable form of a user. It will default to the "firstName lastName". If both of those
     * properties are not present, it will return the "username". If the username is not present, it will return
     * "[Not Available]".
     *
     * @param {object} userObject the object which represents a user.
     * @return {string} a string to identify for the provided user.
     */
    self.getUserDisplay = function(userObject) {
        return (get(userObject, 'firstName') && get(userObject, 'lastName')) ? userObject.firstName + ' ' + userObject.lastName : get(userObject, 'username', '[Not Available]');
    }
    /**
     * @ngdoc method
     * @name getUserObj
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Returns a user object from the provided JSON-LD. User object has a format of
     * ```
     * {
     *    jsonld: {},
     *    iri: '',
     *    username: '',
     *    firstName: '',
     *    lastName: '',
     *    email: '',
     *    roles: []
     * }
     * ```
     * @param jsonld The JSON-LD representation of a User
     * @return An object representing a user
     */
    self.getUserObj = function(jsonld) {
        return {
            jsonld,
            external: self.isExternalUser(jsonld),
            iri: jsonld['@id'],
            username: util.getPropertyValue(jsonld, prefixes.user + 'username'),
            firstName: util.getPropertyValue(jsonld, prefixes.foaf + 'firstName'),
            lastName: util.getPropertyValue(jsonld, prefixes.foaf + 'lastName'),
            email: util.getPropertyId(jsonld, prefixes.foaf + 'mbox'),
            roles: map(jsonld[prefixes.user + 'hasUserRole'], role => util.getBeautifulIRI(role['@id']).toLowerCase())
        }
    }
    /**
     * @ngdoc method
     * @name getGroupObj
     * @methodOf shared.service:userManagerService
     *
     * @description
     * Returns a group object from the provided JSON-LD. Group object has a format of
     * ```
     * {
     *    jsonld: {},
     *    title: '',
     *    description: '',
     *    roles: [],
     *    members: []
     * }
     * ```
     * @param jsonld The JSON-LD representation of a Group
     * @return An object representing a group
     */
    self.getGroupObj = function(jsonld) {
        return {
            jsonld,
            external: self.isExternalGroup(jsonld),
            iri: jsonld['@id'],
            title: util.getDctermsValue(jsonld, 'title'),
            description: util.getDctermsValue(jsonld, 'description'),
            members: map(jsonld[prefixes.foaf + 'member'], member => {
                var user = find(self.users, {'iri': member['@id']});
                if (user != undefined) {
                    return user.username;
                }
            }),
            roles: map(jsonld[prefixes.user + 'hasGroupRole'], role => util.getBeautifulIRI(role['@id']).toLowerCase())
        }
    }
}

export default userManagerService;