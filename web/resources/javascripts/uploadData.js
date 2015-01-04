var app = app||angular.module('sequenceBundleApp', []);

app.controller('uploadDataController', ['$scope', function($scope) {
  $scope.master = {};

  $scope.uploadFile = function(data) {
    alert("file: "+data.filename);

    $scope.master = angular.copy(data.filename);
  };

  $scope.uploadPaste = function(data) {
    alert("paste: "+data.paste);
    $scope.master = angular.copy(data.paste);
  };

}]);


