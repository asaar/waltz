/*
 *  Waltz
 * Copyright (c) David Watkins. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 *
 */
import {appViewResolver} from "./resolvers";
import {
    loadDataFlows,
    loadChangeLog,
    loadInvolvements,
    loadAuthSources,
    loadServers,
    loadSoftwareCatalog,
    loadDatabases
} from "./data-load";
import {prepareSlopeGraph} from "../data-flow/directives/slope-graph/slope-graph-utils";
import {mkAppRatingsGroup, calculateHighestRatingCount} from "../ratings/directives/common";


function controller(appView,
                    authSourcesStore,
                    changeLogStore,
                    involvementStore,
                    dataFlowStore,
                    orgUnitStore,
                    serverInfoStore,
                    perspectiveStore,
                    ratingStore,
                    displayNameService,
                    complexityStore,
                    catalogStore,
                    databaseStore,
                    $state,
                    $q) {

    const { id, organisationalUnitId } = appView.app;

    const vm = this;

    const perspectiveCode = 'BUSINESS';

    $q.all([
        perspectiveStore.findByCode(perspectiveCode),
        ratingStore.findByParentAndPerspective('APPLICATION', id, perspectiveCode)
    ]).then(([perspective, ratings]) => {

        const appRef = { id: id, kind: 'APPLICATION', name: appView.app.name};

        const group = mkAppRatingsGroup(appRef, perspective.measurables, appView.capabilities, ratings);

        vm.ratings = {
            highestRatingCount: calculateHighestRatingCount([group]),
            tweakers: {
                subjectLabel: {
                    enter: (selection) => selection.on('click', (d) => $state.go('main.capabilities.view', { id: d.subject.id }))
                }
            },
            group
        };

    });


    Object.assign(vm, appView);

    const promises = [
        loadDataFlows(dataFlowStore, id, vm),
        loadChangeLog(changeLogStore, id, vm),
        loadInvolvements($q, involvementStore, id, vm),
        loadAuthSources(authSourcesStore, orgUnitStore, id, organisationalUnitId, vm),
        loadServers(serverInfoStore, id, vm),
        loadSoftwareCatalog(catalogStore, id, vm),
        loadDatabases(databaseStore, id, vm)
    ];

    $q.all(promises).then(() => {

        const graphData = prepareSlopeGraph(
            id,
            vm.flows,
            vm.dataTypes,
            vm.appAuthSources,
            vm.ouAuthSources,
            displayNameService,
            $state);

        vm.flow = graphData;
    });


    complexityStore.findByApplication(id).then(c => vm.complexity = c);

}


controller.$inject = [
    'appView',
    'AuthSourcesStore',
    'ChangeLogDataService',
    'InvolvementDataService',
    'DataFlowDataStore',
    'OrgUnitStore',
    'ServerInfoStore',
    'PerspectiveStore',
    'RatingStore',
    'WaltzDisplayNameService',
    'ComplexityStore',
    'SoftwareCatalogStore',
    'DatabaseStore',
    '$state',
    '$q'
];


function onAppViewEnter(appView, historyStore) {
    historyStore.put(
        appView.app.name,
        'APPLICATION',
        'main.app-view',
        { id: appView.app.id });
}


onAppViewEnter.$inject = ['appView', 'HistoryStore'];


export default {
    url: 'application/:id',
    views: {
        'content@': {
            template: require('./app-view.html'),
            controller,
            controllerAs: 'ctrl'
        }
    },
    resolve: {
        appView: appViewResolver
    },
    onEnter: onAppViewEnter
};
