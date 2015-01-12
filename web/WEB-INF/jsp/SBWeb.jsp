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

        <!-- The basic File Upload plugin -->
        <script src="<c:url value="/resources/javascripts/jquery-1.11.1.min.js"  />"></script>
        <!--<script src="<c:url value="/resources/javascripts/jquery-ui-1.11.2.custom/jquery-ui.js"  />"></script>-->
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/vendor/jquery.ui.widget.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/bootstrap-3.3.1/js/bootstrap.min.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/jquery.iframe-transport.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/jquery.fileupload.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/jquery.fileupload-process.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/jquery.fileupload-validate.js"  />"></script>

        <script src="<c:url value="/resources/javascripts/jquery.imagefit-0.2.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jquery.cookie.js"  />"></script>
        <script src="<c:url value="/resources/javascripts/jqueryFileUpload/main.js"  />"></script>

        <script src="<c:url value="/resources/javascripts/visualizationSettingsController.js"  />"></script>


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
                                1 UPLOAD YOUR DATA
                                <!--                                <h1 class="header-left">1</h1>
                                <div class="content-right">UPLOAD <br/> YOUR DATA</div>-->
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
                                2 PREVIEW AND VISUALIZATION
                            </a>
                        </h4>
                    </div>
                    <div id="collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingTwo">
                        <div class="panel-body">
                            <div id="tabs">
                                <ul class="nav nav-tabs nav-justified" id="visualizationTabs">
                                    <li class="nav active"><a href="#tabs-1" data-toggle="tab">SEQUENCE BUNDLES PREVIEW</a></li>
                                    <li class="nav"><a href="#tabs-2" data-toggle="tab">VISUALIZATON SETTINGS</a></li>
                                </ul>
                                <div class="tab-content">
                                    <div class="tab-pane active" id="tabs-1">

                                        <%@include file="navbar-sequence.jspf" %>
                                        <div id="renderProgress" class="progress" >
                                            <div class="progress-bar progress-bar-success"></div>
                                        </div>
                                        <div id="sequenceBundleImage" class="scrolly">
                                            <!--<img src="<c:url value="/resources/images/sequence-bundles-visualise.png"  />">-->
                                        </div>
                                    </div>
                                    <div class="tab-pane"  id="tabs-2">
                                        <%@include file="visualization-settings.jspf" %>
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
