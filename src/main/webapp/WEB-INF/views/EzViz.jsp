<%--
    Document   : EzViz
    Created on : Jan 6, 2015, 11:27:02 PM
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


        <link rel="stylesheet" href="<c:url value="/resources/javascripts/bootstrap-3.3.1/css/bootstrap.min.css"  />">
        <link rel="stylesheet" href="<c:url value="/resources/javascripts/jquery-ui-1.11.2.custom/jquery-ui.css"  />">
        <link rel="stylesheet" href="<c:url value="/resources/stylesheets/todo.css"  />">
        <link rel="stylesheet" href="<c:url value="/resources/stylesheets/jquery.fileupload.css"  />">

    </head>
    <body>

        <%@include file="visualization-settings.jspf" %>

    </body>
</html>