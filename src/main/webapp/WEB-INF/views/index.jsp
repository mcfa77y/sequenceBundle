<%-- Document : SBWeb Created on : Dec 30, 2014, 9:42:11 PM Author : joelau --%>
    <%@page contentType="text/html" pageEncoding="UTF-8" %>
        <%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
            <!DOCTYPE html>
            <html>
            <%@include file="head.jspf" %>

                <body>
                    <%@include file="header.jspf" %>
                        <!-- TAGLINE BEGINS -->
                        <div class="tagline">
                            <div class="tagline-text">
                                <h4 class="name-tagline-tool">Sequence Bundles web tool allows you to rapidly visualise protein&nbsp;sequence alignments, experiment with basic visualisation settings&nbsp;and&nbsp;discover hidden sequence motifs.</h4>
                            </div>
                        </div>
                        <div class="sb-divider-3"></div>
                        <!-- TAGLINE ENDS -->
                        <!-------- TOOL BODY BEGINS -------->
                        <%@include file="upload.jspf" %>
                            <%@include file="visualize.jspf" %>
                                <%@include file="download-share.jspf" %>
                                    <%@include file="footer.jspf" %>
                                        <%@include file="footer-scripts.jspf" %>
                </body>

            </html>