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
import * as angular from 'angular';
import { find } from 'lodash';

import './recordView.component.scss';

const template = require('./recordView.component.html');

/**
 * @ngdoc component
 * @name catalog.component:recordView
 * @requires shared.service:catalogStateService
 * @requires shared.service:catalogManagerService
 * @requires shared.service:ontologyStateService
 * @requires shared.service:policyEnforcementService
 * @requires shared.service:utilService
 * @requires shared.service:prefixes
 *
 * @description
 * `recordView` is a component which creates a div with a Bootstrap `row` containing columns displaying different
 * information about the currently {@link shared.service:catalogStateService selected catalog Record}. The
 * first column just contains a button to go back to the {@link catalog.component:catalogPage}. The second column
 * contains a display of the Record's title, description, and {@link catalog.component:recordIcon icon} along with a
 * {@link catalog.component:recordViewTabset}. The third column contains the Record's
 * {@link catalog.component:entityPublisher publisher}, modified date, issued date, and
 * {@link catalog.component:catalogRecordKeywords keywords}. On initialization of the component, it will re-retrieve
 * the Record to ensure that it still exists.
 */
const recordViewComponent = {
    template,
    bindings: {},
    controllerAs: 'dvm',
    controller: recordViewComponentCtrl
};

recordViewComponentCtrl.$inject = ['$q', 'catalogStateService', 'catalogManagerService', 'ontologyStateService', 'policyEnforcementService', 'utilService', 'prefixes'];

function recordViewComponentCtrl($q, catalogStateService, catalogManagerService, ontologyStateService, policyEnforcementService, utilService, prefixes) {
    var dvm = this;
    var state = catalogStateService;
    var cm = catalogManagerService;
    var pep = policyEnforcementService;
    var os = ontologyStateService;
    var util = utilService;
    dvm.record = undefined;
    dvm.completeRecord = undefined;
    dvm.title = '';
    dvm.description = '';
    dvm.modified = '';
    dvm.issued = '';
    dvm.canEdit = false;

    dvm.$onInit = function() {
        cm.getRecord(state.selectedRecord['@id'], util.getPropertyId(state.selectedRecord, prefixes.catalog + 'catalog'))
            .then(response => {
                setInfo(response);
                dvm.setCanEdit();
            }, (errorMessage) => {
                util.createErrorToast(errorMessage);
                state.selectedRecord = undefined;
            });
    }
    dvm.goBack = function() {
        state.selectedRecord = undefined;
    }
    dvm.updateRecord = function(newRecord) {
        var indexToUpdate = dvm.completeRecord.findIndex(oldRecord => oldRecord['@id'] === newRecord['@id']);
        if (indexToUpdate !== -1) {
            dvm.completeRecord[indexToUpdate] = newRecord;
        } else {
            util.createErrorToast("Could not find record: " + newRecord['@id']);
        }
        
        return cm.updateRecord(newRecord['@id'], util.getPropertyId(newRecord, prefixes.catalog + 'catalog'), dvm.completeRecord)
            .then(() => {
                util.createSuccessToast('Successfully updated the record');
                state.selectedRecord = newRecord;
                setInfo(dvm.completeRecord);
            }, errorMessage => {
                util.createErrorToast(errorMessage);
                return $q.reject();
            });
    }
    dvm.updateTitle = function(newTitle) {
        var openRecord = find(os.list, item => item.ontologyRecord.title === dvm.title);
        if (openRecord) {
            openRecord.ontologyRecord.title = newTitle;
        }
        util.updateDctermsValue(dvm.record, 'title', newTitle);
        return dvm.updateRecord(dvm.record);
    }
    dvm.updateDescription = function(newDescription) {
        util.updateDctermsValue(dvm.record, 'description', newDescription);
        return dvm.updateRecord(dvm.record);
    }
    dvm.setCanEdit = function() {
        var request = {
            resourceId: dvm.record['@id'],
            actionId: prefixes.policy + 'Update'
        };
        pep.evaluateRequest(request)
            .then(response => {
                dvm.canEdit = response !== pep.deny;
            }, () => {
                util.createWarningToast('Could not retrieve record permissions');
                dvm.canEdit = false;
            });
    }

    function setInfo(record) {
        dvm.completeRecord = angular.copy(record);
        var matchingRecord = find(record, ['@id', state.selectedRecord['@id']]);

        dvm.record = matchingRecord;
        dvm.title = util.getDctermsValue(dvm.record, 'title');
        dvm.description = util.getDctermsValue(dvm.record, 'description');
        dvm.modified = util.getDate(util.getDctermsValue(dvm.record, 'modified'), 'short');
        dvm.issued = util.getDate(util.getDctermsValue(dvm.record, 'issued'), 'short');
    }
}

export default recordViewComponent;