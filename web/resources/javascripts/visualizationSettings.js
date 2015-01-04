var app = app||angular.module('sequenceBundleApp', []);
app.controller('vsController', ['$scope', function($scope) {
  $scope.dataFormat = [
       { key: "Amino Acids", value: true, type: "checkbox"},
       { key: "DNA", value: false, type: "checkbox"},
       { key: "RNA", value: false, type: "checkbox"}
    ];

  $scope.isSelection = function(foo) {
    return foo.type.indexOf('selection')>=0;
  };

}]);




