<%-- Document : SBWeb Created on : Dec 30, 2014, 9:42:11 PM Author : joelau --%>
    <%@page contentType="text/html" pageEncoding="UTF-8" %>
        <%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
            <!DOCTYPE html>
            <html>

            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <title>SB Web Page</title>
                <%@include file="head.jspf" %>
                    <script>
                    try {
                        Typekit.load();
                    } catch (e) {}
                    </script>
            </head>

            <body>
                <div class="container">
                    <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="false">
                        <%@include file="visualize-settings.jspf" %>
                    </div>
                </div>
                <%@include file="footer-scripts.jspf" %>
            </body>

            </html>