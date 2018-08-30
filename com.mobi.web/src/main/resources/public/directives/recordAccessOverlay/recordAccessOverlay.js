/*-
 * #%L
 * com.mobi.web
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
(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name recordAccessOverlay
         *
         * @description
         * TODO
         */
        .module('recordAccessOverlay', [])
        /** TODO
         */
        .directive('recordAccessOverlay', recordAccessOverlay);

        recordAccessOverlay.$inject = ['utilService', 'userManagerService', 'policyManagerService', 'prefixes']

        function recordAccessOverlay(utilService, userManagerService, policyManagerService, prefixes) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {

                },
                bindToController: {
                    overlayFlag: '=',
                    resource: '<',
                    ruleId: '@',
                    masterBranch: '<'
                },
                controller: function() {
                    var dvm = this;
                    var util = utilService;
                    var um = userManagerService;
                    var pm = policyManagerService;
                    var groupAttributeId = 'http://mobi.com/policy/prop-path(' + encodeURIComponent('^<' + prefixes.foaf + 'member' + '>') + ')';
                    var userRole = 'http://mobi.com/roles/user';
                    var userPrefix = 'http://mobi.com/users/';

                    dvm.policy = '';
                    dvm.ruleTitle = '';

                    dvm.getPolicy = function(resourceId) {
                        pm.getPolicies(resourceId)
                            .then(result => {
                                dvm.policy = _.chain(result)
                                    .map(policy => ({
                                        policy,
                                        id: policy.PolicyId,
                                        changed: false,
                                        everyone: false,
                                        users: [],
                                        groups: [],
                                        selectedUsers: [],
                                        selectedGroups: [],
                                        userSearchText: '',
                                        groupSearchText: '',
                                        selectedUser: undefined,
                                        selectedGroup: undefined
                                    }))
                                    .forEach(setInfo)
                                    .value()[0];
                            }, util.createErrorToast);
                    }

                    function setInfo(item) {
                        var matches = undefined;
                        if (!dvm.masterBranch) {
                            matches = _.chain(item.policy)
                                .get('Rule', [])
                                .filter(rule => rule.RuleId === dvm.ruleId)
                                .get('[0]Target.AnyOf', [])
                                .map(anyOf => _.map(anyOf,
                                    allOfArray => _.map(allOfArray,
                                        allOf => _.map(allOf,
                                            matchArray => _.map(matchArray,
                                                match => ({
                                                    id: _.get(match, 'AttributeDesignator.AttributeId'),
                                                    value: _.get(match, 'AttributeValue.content[0]')
                                                }))))))
                                .flattenDepth(4)
                                .value();
                        } else {
                            matches = _.chain(item.policy)
                                .get('Rule', [])
                                .filter(rule => rule.RuleId === dvm.ruleId)
                                .get('[0]Condition.Expression.value.Expression')
                                .map(expression => _.map(expression.value.Expression,
                                        nestedExpression => _.map(nestedExpression.value.Expression,
                                                nestedExpression2 => _.map(nestedExpression2,
                                                        values => { if(typeof values.content != 'undefined')
                                                            return values.content[0]
                                                }))))
                                .flattenDepth(3)
                                .filter(n => n)
                                .filter(n => _.startsWith(n, userPrefix))
                                .map(n => ({
                                    id: pm.subjectId,
                                    value: n
                                }))
                                .value();
                        }

                        if (_.find(matches, {id: prefixes.user + 'hasUserRole', value: userRole}) && !dvm.masterBranch) {
                            item.everyone = true;
                        } else {
                            item.selectedUsers = sortUsers(_.chain(matches)
                                .filter({id: pm.subjectId})
                                .map(obj => _.find(um.users, {iri: obj.value}))
                                .reject(_.isNull)
                                .value());
                            item.selectedGroups = sortGroups(_.chain(matches)
                                .filter({id: groupAttributeId})
                                .map(obj => _.find(um.groups, {iri: obj.value}))
                                .reject(_.isNull)
                                .value());
                        }
                        item.users = sortUsers(_.difference(um.users, item.selectedUsers));
                        item.groups = sortGroups(_.difference(um.groups, item.selectedGroups));
                    }

                    dvm.cancel = function() {
                        dvm.overlayFlag = false;
                    }

                    dvm.save = function() {
                        dvm.overlayFlag = false;
                        pm.updatePolicy(dvm.policy)
                            .then(() => {
                                dvm.policy.changed = false;
                                dvm.util.createSuccessToast('Permissions updated')
                            }, utilService.createErrorToast);
                    }

                    function sortUsers(users) {
                        return _.sortBy(users, 'username');
                    }

                    function sortGroups(groups) {
                        return _.sortBy(groups, 'title');
                    }

                    function getRuleTitle() {
                        switch (dvm.ruleId) {
                            case 'urn:read':
                                dvm.ruleTitle = "View Record";
                                break;
                            case 'urn:delete':
                                dvm.ruleTitle = "Delete Record";
                                break;
                            case 'urn:': //TODO: MANAGE A RECORD?
                                break;
                            case 'urn:modify':
                                if (dvm.masterBranch) {
                                    dvm.ruleTitle = 'Modify Master Branch';
                                } else {
                                    dvm.ruleTitle = 'Modify Record';
                                }
                                break;
                        }
                    }

                    dvm.getPolicy(dvm.resource);
                    getRuleTitle();
                },
                templateUrl: 'directives/recordAccessOverlay/recordAccessOverlay.html'
            }
        }
})();
