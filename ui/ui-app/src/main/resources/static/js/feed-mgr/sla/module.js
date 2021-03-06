define(['angular', 'feed-mgr/sla/module-name','kylo-utils/LazyLoadUtil','kylo-common', 'kylo-services','kylo-feedmgr','jquery'], function (angular,moduleName,lazyLoadUtil) {
    var module = angular.module(moduleName, []);

    /**
     * LAZY loaded in from /app.js
     */
    module.config(['$stateProvider',function ($stateProvider) {
        $stateProvider.state('service-level-agreements',{
            url:'/service-level-agreements',
            params: {
            },
            views: {
                'content': {
                    templateUrl: 'js/feed-mgr/sla/service-level-agreements-view.html'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['feed-mgr/sla/service-level-agreement'])
            },
            data:{
                breadcrumbRoot:true,
                displayName:'Service Level Agreements',
                module:moduleName
            }
        })

        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,'feed-mgr/sla/module-require');
        }

    }]);









    return module;
});



