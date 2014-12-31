<%-- 
    Document   : SBWeb
    Created on : Dec 30, 2014, 9:42:11 PM
    Author     : joelau
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>SB Web Page</title>


        <script src="<c:url value="/resources/javascripts/jquery-1.11.1.min.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jquery-ui-1.11.2.custom/jquery-ui.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/bootstrap-3.3.1/js/bootstrap.min.js"  />"></script>

        <script src="<c:url value="/resources/javascripts/angular.min.js"  />"></script>

        <!-- The basic File Upload plugin -->
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/main.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/jquery.fileupload.js"  />"></script>


        <script src = "<c:url value="/resources/javascripts/visualizationSettings.js "  />"></script>
        <script src = "<c:url value="/resources/javascripts/uploadData.js"  />"></script>


        <link rel="stylesheet" href="<c:url value="/resources/javascripts/bootstrap-3.3.1/css/bootstrap.min.css"  />">
        <link rel="stylesheet" href="<c:url value="/resources/javascripts/jquery-ui-1.11.2.custom/jquery-ui.css"  />">
        <link rel="stylesheet" href="<c:url value="/resources/stylesheets/todo.css"  />">
        <link rel="stylesheet" href="<c:url value="/resources/stylesheets/jquery.fileupload.css"  />">

    </head>
    <body>
        
        <%@include file="navbar-main.jspf" %>

        <hr>
        <div class="container">
            <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="headingOne">
                        <h4 class="panel-title">
                            <a data-toggle="collapse" data-parent="#accordion" href="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
                                <h1 class="header-left">1</h1>
                                <div class="content-right">UPLOAD <br/> YOUR DATA</div>
                            </a>
                        </h4>
                    </div>
                    <div id="collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="headingOne">
                        <div class="panel-body">
                            <h4>MULTIPLE SEQUENCE ALIGNMENT </h4>
                            
                            <%@include file="upload.jspf" %>
                        </div>
                    </div>
                </div>
                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="headingTwo">
                        <h4 class="panel-title">
                            <a class="collapsed" data-toggle="collapse" data-parent="#accordion" href="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">
                                2 PREVIEW AND VISUALIZATON
                            </a>
                        </h4>
                    </div>
                    <div id="collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingTwo">
                        <div class="panel-body">
                            <div id="tabs">
                                <ul class="nav nav-tabs nav-justified">
                                    <li class="nav active"><a href="#tabs-1" data-toggle="tab">SEQUENCE BUNDLES PREVIEW</a></li>
                                    <li class="nav"><a href="#tabs-2" data-toggle="tab">VISUALIZATON SETTINGS</a></li>
                                    <li class="nav"><a href="#tabs-3" data-toggle="tab">MSA VIEW</a></li>
                                </ul>
                                <div class="tab-content">
                                    <div class="tab-pane active" id="tabs-1">
                                        
                                        <%@include file="navbar-sequence.jspf" %>
                                        <div id="sequenceBundleImage">
                                            <img src="<c:url value="/resources/images/sequence-bundles-visualise.png"  />">

                                        </div>
                                    </div>
                                    <div class="tab-pane"  id="tabs-2">
                                        
                                        <%@include file="visualization-settings.jspf" %>

                                    </div>
                                    <div class="tab-pane"  id="tabs-3">
                                        <p> tab three content </p>
                                    </div>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="headingThree">
                        <h4 class="panel-title">
                            <a class="collapsed" data-toggle="collapse" data-parent="#accordion" href="#collapseThree" aria-expanded="false" aria-controls="collapseThree">
                                3 DOWNLOAD AND SHARE
                            </a>
                        </h4>
                    </div>
                    <div id="collapseThree" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree">
                        <div class="panel-body">
                            
                            <%@include file="download-share.jspf" %>

                        </div>
                    </div>
                </div>
            </div>
        </div>




        <hr>


        
        <%@include file="footer.jspf" %>

    </body>
</html>
