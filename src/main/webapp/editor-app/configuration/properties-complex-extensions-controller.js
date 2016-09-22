/*
 * Complex Extensions
 */

angular.module('activitiModeler').controller('ActivitiComplexExtensionsCtrl', ['$scope', '$modal', function ($scope, $modal) {

    // Config for the modal window
    var opts = {
        template: 'editor-app/configuration/properties/complex-extensions-popup.html?version=' + Date.now(),
        scope: $scope
    };

    // Open the dialog
    $modal(opts);
}]);

//Need a separate controller for the modal window due to https://github.com/angular-ui/bootstrap/issues/259
// Will be fixed in a newer version of Angular UI
angular.module('activitiModeler').controller('ActivitiComplexExtensionsPopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing mesage definitions on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null && $scope.property.value.length > 0) {

            if ($scope.property.value.constructor == String) {
                $scope.complexExtensions = JSON.parse($scope.property.value);
            }
            else {
                // Note that we clone the json object rather then setting it directly,
                // this to cope with the fact that the user can click the cancel button and no changes should have happened
                $scope.complexExtensions = angular.copy($scope.property.value);
            }

        } else {
            $scope.complexExtensions = [];
        }

        // Array to contain selected mesage definitions (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedExtensions = [];
        $scope.translationsRetrieved = false;

        $scope.labels = {};

        var namePromise = $translate('PROPERTY.COMPLEXEXTENSIONS.NAME');
        var valuePromise = $translate('PROPERTY.COMPLEXEXTENSIONS.VALUE');

        $q.all([namePromise, valuePromise]).then(function (results) {

            $scope.labels.nameLabel = results[0];
            $scope.labels.valueLabel = results[1];
            $scope.translationsRetrieved = true;

         // Config for grid
            $scope.gridOptions = {
                data: 'complexExtensions',
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                keepLastSelected : false,
                selectedItems: $scope.selectedExtensions,
                columnDefs: [
                    {field: 'name', displayName: $scope.labels.nameLabel},
                    {field: 'value', displayName: $scope.labels.valueLabel}]
            };
        });

        // Click handler for add button
        $scope.addNewComplexExtension = function () {
            var newComplexExtension = {name: '', value: ''};

            $scope.complexExtensions.push(newComplexExtension);
            $timeout(function () {
            	$scope.gridOptions.selectItem($scope.complexExtensions.length - 1, true);
            });
        };

        // Click handler for remove button
        $scope.removeComplexExtension = function () {
        	if ($scope.selectedExtensions && $scope.selectedExtensions.length > 0) {
            	var index = $scope.complexExtensions.indexOf($scope.selectedExtensions[0]);
                $scope.gridOptions.selectItem(index, false);
                $scope.complexExtensions.splice(index, 1);

                $scope.selectedExtensions.length = 0;
                if (index < $scope.complexExtensions.length) {
                    $scope.gridOptions.selectItem(index + 1, true);
                } else if ($scope.complexExtensions.length > 0) {
                    $scope.gridOptions.selectItem(index - 1, true);
                }
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.complexExtensions.length > 0) {
                $scope.property.value = $scope.complexExtensions;
            } else {
                $scope.property.value = null;
            }

            $scope.updatePropertyInModel($scope.property);
            $scope.close();
        };

        $scope.cancel = function () {
            $scope.property.mode = 'read';
            $scope.$hide();
        };

        // Close button handler
        $scope.close = function () {
            $scope.property.mode = 'read';
            $scope.$hide();
        };

    }]);