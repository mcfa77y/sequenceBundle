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

        <%@include file="scripts-styles.jspf" %>

        <script>try {
                Typekit.load();
            } catch (e) {
            }</script>
    </head>
    <body>

        <%@include file="header.jspf" %>

        <hr>
        <div class="container">
            <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="headingOne">
                        <h1 class="panel-number">1</h1><h1 class="panel-title"><a class="panel-title-link" data-toggle="collapse" data-parent="#accordion" href="#collapseOne" aria-expanded="true" aria-controls="collapseOne">SELECT DATA</a></h1><div style="clear:both"></div>
                    </div><!-- #panel-heading -->

                    <div aria-expanded="true" id="collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="headingOne">

                        <div class="panel-body">
                            <%@include file="upload.jspf" %>
                        </div>
                    </div>
                </div>
                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="headingTwo">
                        <h1 class="panel-number">2</h1><h1 class="panel-title"><a class="panel-title-link" data-toggle="collapse" data-parent="#accordion" href="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">PREVIEW AND <br />VISUALISATON</a></h1><div style="clear:both"></div>
                    </div><!-- #panel-heading -->

                </div>
                <div id="collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingTwo">
                    <div class="panel-body">
                        <div id="tabs">
                            <ul class="nav nav-tabs nav-justified">
                                <li class="nav active"><a aria-expanded="true" class="seq-tab preview" href="#tabs-1" data-toggle="tab">PREVIEW SEQUENCE BUNDLE</a></li>
                                <li class="nav"><a aria-expanded="false" class="seq-tab edit" href="#tabs-2" data-toggle="tab">EDIT VISUALISATION SETTINGS</a></li>
                            </ul>

                            <div class="tab-content">
                                <div class="tab-pane active" id="tabs-1">

                                    <%@include file="navbar-sequence.jspf" %>
                                    <div id="renderProgress" class="progress" >
                                        <div class="progress-bar progress-bar-success"></div>
                                    </div>
                                    <div id="loading" style="display: none">
                                        <img id="loading-image" src="<c:url value="/resources/images/ajax-loader.gif"/>" alt="Loading..." />
                                    </div>
                                    <div id="sequenceBundle" class="scrolly">
                                    </div>
                                    <div class="row submit-settings">
                                        <div class="col-md-12">
                                            <input id="downloadButton" class="next-btn" value="DOWNLOAD" type="submit">
                                        </div><!-- #col-12 -->
                                    </div><!-- #row -->
                                </div>
                                <div class="tab-pane"  id="tabs-2">
                                    <%@include file="visualization-settings.jspf" %>
                                </div>

                            </div>
                        </div>

                    </div>
                </div>


                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="headingThree">
                        <h1 class="panel-number">3</h1><h1 class="panel-title"><a class="panel-title-link collapsed" data-toggle="collapse" data-parent="#accordion" href="#collapseThree" aria-expanded="false" aria-controls="collapseThree">DOWNLOAD<br> AND SHARE</a></h1><div style="clear:both;"></div>
                    </div><!-- #panel-heading -->

                    <div style="height: 0px;" aria-expanded="false" id="collapseThree" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree">
                        <div class="panel-body">

                            <%@include file="download-share.jspf" %>


                        </div><!-- #panel-body -->
                    </div><!-- #panel-collapse -->
                </div>
            </div>




        </div>



        <%@include file="footer.jspf" %>


    </body>
</html>
